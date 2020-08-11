package honeyroasted.occurrence.manager;

import honeyroasted.pecans.node.instruction.operator.Mod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReflectionUtil {
    private static Map<Class, Class> boxByPrimitives = new HashMap<>();
    private static Map<Class, Class> primitivesByBox = new HashMap<>();
    private static Map<String, Class> primitivesByName = new HashMap<>();

    static {
        boxByPrimitives.put(byte.class, Byte.class);
        boxByPrimitives.put(short.class, Short.class);
        boxByPrimitives.put(char.class, Character.class);
        boxByPrimitives.put(int.class, Integer.class);
        boxByPrimitives.put(long.class, Long.class);
        boxByPrimitives.put(float.class, Float.class);
        boxByPrimitives.put(double.class, Double.class);
        boxByPrimitives.put(boolean.class, Boolean.class);
        boxByPrimitives.put(void.class, Void.class);

        primitivesByBox.put(Byte.class, byte.class);
        primitivesByBox.put(Short.class, short.class);
        primitivesByBox.put(Character.class, char.class);
        primitivesByBox.put(Integer.class, int.class);
        primitivesByBox.put(Long.class, long.class);
        primitivesByBox.put(Float.class, float.class);
        primitivesByBox.put(Double.class, double.class);
        primitivesByBox.put(Boolean.class, boolean.class);
        primitivesByBox.put(Void.class, void.class);

        primitivesByName.put("byte", byte.class);
        primitivesByName.put("short", short.class);
        primitivesByName.put("char", char.class);
        primitivesByName.put("int", int.class);
        primitivesByName.put("long", long.class);
        primitivesByName.put("float", float.class);
        primitivesByName.put("double", double.class);
        primitivesByName.put("boolean", boolean.class);
        primitivesByName.put("void", void.class);
    }

    public static List<List<Annotation>> getParameterAnnotations(Method method) {
        List<List<Annotation>> annotations = new ArrayList<>();
        for (int i = 0; i < method.getParameterCount(); i++) {
            annotations.add(new ArrayList<>());
        }

        walkMethodTree(method, m -> {
            Annotation[][] paramAnnots = m.getParameterAnnotations();
            for (int i = 0; i < paramAnnots.length; i++) {
                Collections.addAll(annotations.get(i), paramAnnots[i]);
            }
        });
        return annotations;
    }

    public static List<Annotation> getAnnotations(Method method) {
        List<Annotation> annotations = new ArrayList<>();
        walkMethodTree(method, m -> Collections.addAll(annotations, m.getAnnotations()));
        return annotations;
    }

    public static void walkMethodTree(Method method, Consumer<Method> consumer) {
        consumer.accept(method);
        walkMethodTree(method, method.getDeclaringClass(), consumer);
    }

    private static void walkMethodTree(Method method, Class<?> cls, Consumer<Method> consumer) {
        if (cls == null) {
            return;
        }

        try {
            Method res = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
            int mods = res.getModifiers();
            if (!Modifier.isFinal(mods) && !Modifier.isStatic(mods)) {
                consumer.accept(res);
            }
        } catch (NoSuchMethodException ignore) {
            return;
        }

        walkMethodTree(method, cls.getSuperclass(), consumer);
        for (Class<?> inter : cls.getInterfaces()) {
            walkMethodTree(method, inter, consumer);
        }
        return;
    }

    public static Class box(Class primitive) {
        return primitive.isPrimitive() ? boxByPrimitives.get(primitive) : primitive;
    }

    public static Class unbox(Class box) {
        return primitivesByBox.containsKey(box) ? primitivesByBox.get(box) : box;
    }

    public static Class getClass(String name) throws ClassNotFoundException {
        return primitivesByName.containsKey(name) ? primitivesByName.get(name) : Class.forName(name);
    }

    public static String nameAndSig(Method method) {
        Class<?> owner = method.getDeclaringClass();
        return owner.getName() + "#" + method.getName() +
                "(" + String.join(", ", Stream.of(method.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new)) + ")";
    }

    private static List<Class> append(List<Class> tests, Class test) {
        List<Class> cls = new ArrayList<>();
        cls.addAll(tests);
        cls.add(test);
        return cls;
    }
}
