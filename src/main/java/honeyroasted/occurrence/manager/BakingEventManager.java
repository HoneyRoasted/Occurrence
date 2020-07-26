package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.HandleEventException;
import honeyroasted.occurrence.InvokeMethodException;
import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.generator.ConstructorParams;
import honeyroasted.occurrence.generator.ListenerWrapperGenerator;
import honeyroasted.occurrence.generator.NameProvider;
import honeyroasted.occurrence.generator.VisitorRegistry;
import honeyroasted.occurrence.generics.ReflectionUtil;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.ClassNode;
import honeyroasted.pecans.node.MethodNode;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.Throw;
import honeyroasted.pecans.node.instruction.TypedNode;
import honeyroasted.pecans.type.MethodSignature;
import honeyroasted.pecans.type.type.TypeParameterized;
import honeyroasted.pecans.util.ByteArrayClassLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class BakingEventManager<T> implements EventManager<T> {
    private static long uniqueSuffix = 0;

    private Baked baked = new Baked();

    private List<ListenerWrapper<T>> wrappers = EventManager.newSortedList();

    private PolicyRegistry policyRegistry;
    private VisitorRegistry visitorRegistry;
    private ClassLoader loader;

    public BakingEventManager() {
        this(PolicyRegistry.GLOBAL, VisitorRegistry.GLOBAL, ClassLoader.getSystemClassLoader());
    }

    public BakingEventManager(PolicyRegistry policyRegistry, VisitorRegistry visitorRegistry, ClassLoader loader) {
        this.policyRegistry = policyRegistry;
        this.visitorRegistry = visitorRegistry;
        this.loader = loader;
    }

    @Override
    public void post(T event) throws HandleEventException {
        this.baked.post(event);
    }

    @Override
    public void register(ListenerWrapper<T> wrapper) {
        this.wrappers.add(wrapper);
        this.rebake();
    }

    @Override
    public void unregister(Predicate<ListenerWrapper<T>> remove) {
        this.wrappers.removeIf(remove);
        this.rebake();
    }

    @Override
    public void register(Object listener) {
        this.wrappers.addAll((Collection) ListenerWrapperGenerator.genWrappers(
                ListenerWrapperGenerator.gen(listener, visitorRegistry, policyRegistry), new ByteArrayClassLoader(this.loader)));
        this.rebake();
    }

    @Override
    public void register(Class<?> listener) {
        this.wrappers.addAll((Collection) ListenerWrapperGenerator.genWrappers(
                ListenerWrapperGenerator.gen(listener, visitorRegistry, policyRegistry), new ByteArrayClassLoader(this.loader)));
        this.rebake();
    }

    @Override
    public Collection<ListenerWrapper<T>> listeners() {
        return Collections.unmodifiableCollection(new ArrayList<>(this.wrappers));
    }

    private void rebake() {
        if (this.wrappers.isEmpty()) {
            this.baked = new Baked();
            return;
        }

        TypeParameterized clsType = parameterized("Lhoneyroasted/occurrence/generated/bake/Baked$" + System.identityHashCode(this) + "$" + uniqueSuffix++ + ";");
        ClassNode baked = classDef(ACC_PUBLIC, classSignature(clsType)
                .setSuperclass(type(Baked.class)));
        ConstructorParams params = new ConstructorParams();
        NameProvider nameProvider = new NameProvider();

        MethodNode init = method(ACC_PUBLIC, "<init>", methodSignature(VOID));
        baked.add(init);

        MethodNode post = method(ACC_PUBLIC, "post", methodSignature(VOID))
                .param("event", OBJECT);
        baked.add(post);
        Sequence seq = sequence();
        post.body(seq);

        ListenerWrapper<?>[] wrappers = this.wrappers.toArray(new ListenerWrapper[0]);
        params.add("wrappers", wrappers, array(type(ListenerWrapper.class)));


        Map<Class<?>, String> insts = new HashMap<>();
        Class<?> prevEvent = null;
        String name = nameProvider.provide("handle");
        MethodSignature handlSig = methodSignature(type(List.class), OBJECT, type(List.class));
        MethodNode handls = method(ACC_PRIVATE, name, methodSignature(type(List.class)))
                .param("event", OBJECT)
                .param("errors", type(List.class).addPart(type(HandleEventException.Entry.class)));
        Sequence hSeq = sequence(def("wrappers", params.get("wrappers")));
        Sequence ifBod = sequence();
        handls.body(hSeq);

        int index = 0;
        for (ListenerWrapper<T> wrapper : this.wrappers) {
            if (!insts.containsKey(wrapper.event()) && !wrapper.event().equals(Object.class)) {
                String instName = nameProvider.provide("inst");
                insts.put(wrapper.event(), instName);
                hSeq.add(def(instName, instanceOf(get("event"), type(ReflectionUtil.box(wrapper.event())))));
            }

            if (!wrapper.event().equals(prevEvent)) {
                ifBod = sequence();
                if (wrapper.event().equals(Object.class)) {
                    hSeq.add(ifBod);
                } else {
                    hSeq.add(ifBlock(get(insts.get(wrapper.event())), ifBod));
                }
            }

            ifBod.add(
                    tryCatch(invokeInterface(arrayGet(get("wrappers"), constant(index)), "handle", methodSignature(VOID, OBJECT))
                                    .arg(get("event")),
                            type(Throwable.class), "ex",
                            sequence(
                                    ifBlock(equalsNull(get("errors")),
                                            set("errors", newObj(type(ArrayList.class), methodSignature(VOID)))),
                                    invokeInterface(get("errors"), "add", methodSignature(BOOLEAN, OBJECT))
                                            .arg(newObj(type(HandleEventException.Entry.class), methodSignature(VOID, type(Throwable.class), type(ListenerWrapper.class)))
                                                    .arg(get("ex")).arg(arrayGet(get("wrappers"), constant(index)))).pop()
                            ))
            );

            prevEvent = wrapper.event();

            index++;

            if (index >= 500) {
                index = 0;
                String nname = nameProvider.provide("handle");
                hSeq.add(ret(invokeVirtual(loadThis(), nname, handlSig)
                        .arg(get("event")).arg(get("errors"))));
                baked.add(handls);

                prevEvent = null;
                insts = new HashMap<>();
                ifBod = sequence();
                hSeq = sequence(def("wrappers", params.get("wrappers")));
                handls = method(ACC_PRIVATE, nname, methodSignature(type(List.class)))
                        .param("event", OBJECT)
                        .param("errors", type(List.class).addPart(type(HandleEventException.Entry.class)));
                handls.body(hSeq);
            }
        }
        hSeq.add(ret(get("errors")));
        baked.add(handls);

        seq.add(def("errors", invokeVirtual(loadThis(), name, handlSig)
                .arg(get("event"))
                .arg(constant(null))));
        seq.add(ifBlock(not(equalsNull(get("errors"))),
                throwEx(newObj(type(HandleEventException.class), methodSignature(VOID, OBJECT, type(List.class)))
                        .arg(get("event"))
                        .arg(get("errors")))));
        seq.add(ret());

        Sequence initSeq = sequence(invokeSpecial(type(Baked.class), loadThis(), "<init>", methodSignature(VOID)));
        init.body(initSeq);
        params.getParamTypes().forEach((param, type) -> {
            baked.add(field(ACC_PRIVATE, param, type));
            init.param(param, type);
            initSeq.add(set(loadThis(), param, get(param)));
        });
        initSeq.add(ret());

        ByteArrayClassLoader byteArrayClassLoader;
        if (loader instanceof ByteArrayClassLoader) {
            byteArrayClassLoader = (ByteArrayClassLoader) loader;
        } else {
            byteArrayClassLoader = new ByteArrayClassLoader(loader);
        }

        byte[] cls = baked.toByteArray();
        Class<?> loaded = byteArrayClassLoader.defineClass(baked.getSignature().writeInternalName().replace('/', '.'), cls);
        try {
            this.baked = (Baked) loaded.getDeclaredConstructors()[0].newInstance(params.genParams());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InvokeMethodException("Failed to invoke generated class constructor, this is likely an internal error", e);
        }
    }

    public static class Baked {

        public void post(Object event) throws HandleEventException {

        }

    }

}
