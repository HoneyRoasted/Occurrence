package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.InvalidFilterException;
import honeyroasted.occurrence.InvokeMethodException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilterWrapperBuilder {

    public static List<FilterWrapper> of(Annotation... annotations) {
        List<FilterWrapper> wrappers = new ArrayList<>();
        for (Annotation annotation : annotations) {
            wrappers.addAll(of(annotation));
        }
        return wrappers;
    }

    public static List<FilterWrapper> of(Annotation annotation) {
        return of(annotation, new HashMap<>(), new ArrayList<>(), new HashSet<>());
    }

    public static List<FilterWrapper> of(Annotation annotation, Map<String, Object> parent, List<Object> expansions, Set<Class<?>> seen) {
        List<FilterWrapper> res = new ArrayList<>();
        if (!seen.contains(annotation.annotationType())) {
            seen.add(annotation.annotationType());

            if (annotation instanceof Filter || annotation instanceof FilterList) {
                List<Filter> filters = get(annotation);
                for (Filter filter : filters) {
                    String id = filter.id();
                    Map<String, Object> mapArgs = new HashMap<>();
                    List<Object> arrayArgs = new ArrayList<>();

                    for (Arg arg : filter.args()) {
                        Object obj = value(arg, parent);

                        if (filter.array() || (arg.name().equals("value") && mapArgs.containsKey("value"))) {
                            if (arg.expand().equals(Filter.DEFAULT)) {
                                arrayArgs.add(obj);
                            }
                        } else {
                            mapArgs.put(arg.name(), obj);
                        }
                    }

                    for (Object expand : expansions) {
                        if (expand instanceof Arg) {
                            Arg arg = (Arg) expand;
                            arrayArgs.add(value(arg, parent));
                        } else if (expand instanceof Arg[]) {
                            for (Arg arg : (Arg[]) expand) {
                                arrayArgs.add(value(arg, parent));
                            }
                        } else {
                            arrayArgs.add(expand);
                        }
                    }
                    res.add(new FilterWrapper(id, mapArgs, arrayArgs));
                }
            } else {
                Map<String, Object> vals = getValues(annotation);
                for (Annotation child : annotation.annotationType().getAnnotations()) {
                    Map<String, Object> delegated = new HashMap<>(vals);
                    List<Object> expanded = new ArrayList<>(expansions);

                    for (Method method : annotation.annotationType().getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Delegate.class)) {
                            Delegate delegate = method.getAnnotation(Delegate.class);
                            if (delegate.type().isAssignableFrom(child.annotationType())) {
                                try {
                                    delegated.put(delegate.target(), method.invoke(annotation));
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new InvokeMethodException("Failed to invoke method", e);
                                }
                            }
                        } else if (method.isAnnotationPresent(ExpandArgs.class)) {
                            ExpandArgs expand = method.getAnnotation(ExpandArgs.class);
                            if (expand.type().isAssignableFrom(child.annotationType())) {
                                try {
                                    Object obj = method.invoke(annotation);
                                    if (obj.getClass().isArray()) {
                                        for (int i = 0; i < Array.getLength(obj); i++) {
                                            expanded.add(Array.get(obj, i));
                                        }
                                    } else {
                                        expanded.add(obj);
                                    }
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new InvokeMethodException("Failed to invoke method", e);
                                }
                            }
                        }
                    }

                    res.addAll(of(child, delegated, expanded, seen));
                }

                for (Method method : annotation.annotationType().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(ExpandFilters.class)) {
                        try {
                            Object obj = method.invoke(annotation);
                            if (obj.getClass().isArray()) {
                                for (int i = 0; i < Array.getLength(obj); i++) {
                                    Object el = Array.get(obj, i);
                                    if (el instanceof Annotation) {
                                        res.addAll(of((Annotation) el, new HashMap<>(), new ArrayList<>(), new HashSet<>()));
                                    }
                                }
                            } else if (obj instanceof Annotation) {
                                res.addAll(of((Annotation) obj, new HashMap<>(), new ArrayList<>(), new HashSet<>()));
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new InvokeMethodException("Failed to invoke method", e);
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Filter> get(Annotation annotation) {
        List<Filter> filters = new ArrayList<>();
        if (annotation instanceof Filter) {
            filters.add((Filter) annotation);
        } else if (annotation instanceof FilterList) {
            Collections.addAll(filters, ((FilterList) annotation).value());
        }

        return filters;
    }

    public static Map<String, Object> getValues(Annotation annotation) {
        Map<String, Object> vals = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                vals.put(method.getName(), method.invoke(annotation));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new InvokeMethodException("Failed to invoke method", e);
            }
        }
        return vals;
    }

    private static Object value(Arg arg, Map<String, Object> parent) {
        if (arg.boolVal().length != 0) {
            return arg.boolVal();
        } else if (arg.byteVal().length != 0) {
            return arg.byteVal();
        } else if (arg.shortVal().length != 0) {
            return arg.shortVal();
        } else if (arg.charVal().length != 0) {
            return arg.charVal();
        } else if (arg.intVal().length != 0) {
            return arg.intVal();
        } else if (arg.longVal().length != 0) {
            return arg.longVal();
        } else if (arg.floatVal().length != 0) {
            return arg.floatVal();
        } else if (arg.doubleVal().length != 0) {
            return arg.doubleVal();
        } else if (arg.stringVal().length != 0) {
            return arg.stringVal();
        } else if (arg.classVal().length != 0) {
            return arg.classVal();
        } else if (arg.enumVal().val().length != 0) {
            Arg.Enum enu = arg.enumVal();
            Enum[] vals = new Enum[enu.val().length];
            Enum[] possible = enu.type().getEnumConstants();


            for (int i = 0; i < enu.val().length; i++) {
                String name = enu.val()[i];
                for (Enum v : possible) {
                    if (v.name().equals(name)) {
                        vals[i] = v;
                        break;
                    }
                }

                throw new InvalidFilterException("No enum constant: " + name + " for: " + enu.type().getName());
            }

            return vals;
        } else if (!arg.delegate().equals(Filter.DEFAULT)) {
            if (parent != null) {
                if (parent.containsKey(arg.delegate())) {
                    Object val = parent.get(arg.delegate());
                    if (val instanceof Arg) {
                        return value((Arg) val, null);
                    } else {
                        return val;
                    }
                } else {
                    throw new InvalidFilterException("No target called: " + arg.delegate() + ", cannot delegate");
                }
            } else {
                throw new InvalidFilterException("No parent for arg: " + arg.name() + ", cannot delegate");
            }
        } else {
            return new Object[0];
        }
    }

}
