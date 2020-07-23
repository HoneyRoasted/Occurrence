package honeyroasted.occurrence.generics;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static Class box(Class primitive) {
        return primitive.isPrimitive() ? boxByPrimitives.get(primitive) : primitive;
    }

    public static Class unbox(Class box) {
        return primitivesByBox.containsKey(box) ? primitivesByBox.get(box) : box;
    }

    public static Class getClass(String name) throws ClassNotFoundException {
        return primitivesByName.containsKey(name) ? primitivesByName.get(name) : Class.forName(name);
    }

    public static Collection<Class> boxes() {
        return primitivesByBox.keySet();
    }

    public static Collection<Class> primitives() {
        return boxByPrimitives.keySet();
    }

    public static String nameAndSig(Method method) {
        Class<?> owner = method.getDeclaringClass();
        return owner.getName() + "#" + method.getName() +
                "(" + String.join(", ", Stream.of(method.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new)) + ")";
    }

    public static Optional<JavaType> getWithRespect(JavaType sub, Class parent) {
        return getHierarchy(sub.getActualType(), parent).map(hierarchy -> {
            Map<String, JavaType> paramMap = new LinkedHashMap<>();
            JavaGenerics subParams = JavaGenerics.ofTypeParameters(sub.getActualType());
            for (int i = 0; i < subParams.getParameters().size(); i++) {
                paramMap.put(subParams.getActualParameter(i).getName(), sub.getGenerics().getActualParameter(i));
            }


            for (int i = 0; i < hierarchy.size() - 1; i++) {
                Map<String, JavaType> newParamMap = new LinkedHashMap<>();

                Class cls = hierarchy.get(i);
                Class sup = hierarchy.get(i + 1);

                JavaGenerics superclassParams = JavaGenerics.ofTypeParameters(sup);
                JavaGenerics superclassFilled = JavaType.of(getInherited(cls, sup).get()).getGenerics();

                for (int j = 0; j < superclassParams.getParameters().size(); j++) {
                    JavaType typeParam = superclassParams.getActualParameter(j);
                    JavaType filledParam = superclassFilled.getActualParameter(j);

                    if (filledParam.isVariable()) {
                        newParamMap.put(typeParam.getName(), paramMap.get(filledParam.getName()));
                    } else {
                        newParamMap.put(typeParam.getName(), filledParam);
                    }
                }

                paramMap = newParamMap;
            }

            JavaGenerics parentGenerics = JavaGenerics.ofTypeParameters(parent);
            JavaGenerics generics = JavaGenerics.empty();

            for (JavaType param : parentGenerics.getParameters()) {
                JavaType target = paramMap.get(param.getName());
                if (target.isVariable()) {
                    for (int i = 0; i < subParams.getParameters().size(); i++) {
                        JavaType subParam = subParams.getActualParameter(i);
                        JavaType filled = sub.getGenerics().getActualParameter(i);

                        if (subParam.getName().equals(target.getName())) {
                            generics.getParameters().add(filled);
                            break;
                        }
                    }
                } else {
                    generics.getParameters().add(target);
                }
            }

            return JavaType.of(parent, generics);
        });
    }

    private static JavaType getParam(JavaGenerics generics, int i) {
        if (i >= 0 && i < generics.getParameters().size()) {
            return generics.getParameters().get(i);
        } else {
            return JavaType.of(Object.class);
        }
    }

    public static Optional<List<Class>> getHierarchy(Class sub, Class parent) {
        List<List<Class>> tests = new ArrayList<>();
        List<Class> firstTest = new ArrayList<>();
        firstTest.add(sub);
        tests.add(firstTest);

        while (!tests.isEmpty()) {
            List<List<Class>> newTests = new ArrayList<>();
            for (List<Class> test : tests) {
                Class target = test.get(test.size() - 1);
                if (target.equals(parent)) {
                    return Optional.of(test);
                } else {
                    Class superClass = target.getSuperclass();
                    if (superClass != null) {
                        newTests.add(append(test, superClass));
                    }

                    Class[] interfaces = target.getInterfaces();
                    for (Class face : interfaces) {
                        newTests.add(append(test, face));
                    }
                }
            }
            tests = newTests;
        }

        return Optional.empty();
    }

    public static Optional<Type> getInherited(Class cls, Class target) {
        if (target.equals(cls.getSuperclass())) {
            return Optional.ofNullable(cls.getGenericSuperclass());
        }

        Type[] interfaces = cls.getGenericInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (cls.getInterfaces()[i].equals(target)) {
                return Optional.ofNullable(interfaces[i]);
            }
        }

        return Optional.empty();
    }

    public static Class getCommonParent(List<Class> cls) {
        if (cls.isEmpty()) {
            return Object.class;
        }

        List<Class> current = new ArrayList<>();
        current.addAll(cls);

        while (current.stream().noneMatch(s -> cls.stream().allMatch(c -> s.isAssignableFrom(c)))) {
            List<Class> newCurrent = new ArrayList<>();
            for (Class c : current) {
                if (c.getSuperclass() != null) {
                    newCurrent.add(c.getSuperclass());
                }
                Collections.addAll(newCurrent, c.getInterfaces());
            }
            current = newCurrent;
        }

        return current.stream().filter(s -> cls.stream().allMatch(c -> s.isAssignableFrom(c))).findFirst().get();
    }

    public static Class getArrayClass(Class base, int arrayDepth) {
        return Array.newInstance(base, new int[arrayDepth]).getClass();
    }

    private static List<Class> append(List<Class> tests, Class test) {
        List<Class> cls = new ArrayList<>();
        cls.addAll(tests);
        cls.add(test);
        return cls;
    }

}
