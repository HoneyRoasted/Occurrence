package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.InvalidFilterException;
import honeyroasted.occurrence.InvokeMethodException;
import honeyroasted.occurrence.generics.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FilterWrapper {
    private String id;
    private Map<String, Object> values;
    private Set<String> forceArray;
    private int numIndexed;

    public FilterWrapper(Filter filter, Annotation parent) {
        this.id = filter.id();
        this.values = new LinkedHashMap<>();
        this.forceArray = new HashSet<>();

        int counter = 0;

        for (Arg arg : filter.args()) {
            if (!arg.expand().equals(Filter.DEFAULT)) {
                if (parent != null) {
                    try {
                        Method target = parent.annotationType().getMethod(arg.expand());
                        Arg[] parts = (Arg[]) target.invoke(parent);

                        for (Arg part : parts) {
                            String key = String.valueOf(counter++);
                            values.put(key, value(part, null));
                            if (part.forceArray()) {
                                forceArray.add(key);
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        throw new InvalidFilterException("Cannot expand from: " + arg.expand() + ", no such method exists", e);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new InvokeMethodException("Failed to invoke method", e);
                    }
                } else {
                    throw new InvalidFilterException("No parent for arg: " + arg.name() + ", cannot expand");
                }
            } else {
                this.values.put(arg.name(), value(arg, parent));

                if (arg.forceArray()) {
                    forceArray.add(arg.name());
                }
            }
        }

        this.numIndexed = 0;
        while (this.values.containsKey(String.valueOf(this.numIndexed))) {
            this.numIndexed++;
        }
    }


    public FilterWrapper(Filter filter) {
        this(filter, null);
    }

    public static List<FilterWrapper> of(Annotation[] annotations) {
        List<FilterWrapper> wrappers = new ArrayList<>();
        for (Annotation annotation : annotations) {
            wrappers.addAll(of(annotation));
        }
        return wrappers;
    }

    public static Collection<FilterWrapper> of(Annotation annotation) {
        List<FilterWrapper> wrappers = new ArrayList<>();
        if (annotation instanceof Filter) {
            wrappers.add(new FilterWrapper((Filter) annotation));
        } else if (annotation instanceof FilterList) {
            for (Filter filter : ((FilterList) annotation).value()) {
                wrappers.add(new FilterWrapper(filter, annotation));
            }
        } else if (annotation.annotationType().isAnnotationPresent(Filter.class)) {
            wrappers.add(new FilterWrapper(annotation.annotationType().getAnnotation(Filter.class), annotation));
        } else if (annotation.annotationType().isAnnotationPresent(FilterList.class)) {
            for (Filter filter : annotation.annotationType().getAnnotation(FilterList.class).value()) {
                wrappers.add(new FilterWrapper(filter, annotation));
            }
        }

        return wrappers;
    }

    private static Object value(Arg arg, Annotation parent) {
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
        }  else if (arg.longVal().length != 0) {
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
                try {
                    Method target = parent.annotationType().getMethod(arg.delegate());
                    Object val = target.invoke(parent);
                    if (val instanceof Arg) {
                        return value((Arg) val, null);
                    } else {
                        return val;
                    }
                } catch (NoSuchMethodException e) {
                    throw new InvalidFilterException("Cannot delegate to: " + arg.delegate() + ", no such method exists", e);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new InvokeMethodException("Failed to invoke method", e);
                }
            } else {
                throw new InvalidFilterException("No parent for arg: " + arg.name() + ", cannot delegate");
            }
        } else {
            return new Object[0];
        }
    }

    public String getId() {
        return id;
    }

    public int numIndexed() {
        return this.numIndexed;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public boolean contains(String name) {
        return this.values.containsKey(name);
    }

    public <T> Optional<T> get(String name, Class<T> type) {
        type = ReflectionUtil.box(type);

        boolean force = forceArray.contains(name);

        Object val = this.values.get(name);
        if (val != null) {
            boolean inst = type.isInstance(val);
            if (force && inst) {
                return Optional.of((T) val);
            } else if (val.getClass().isArray() && Array.getLength(val) == 1) {
                Object element = Array.get(val, 0);
                if (type.isInstance(element)) {
                    return Optional.of((T) element);
                } else if (inst) {
                    return Optional.of((T) val);
                }
            } else if (inst) {
                return Optional.of((T) val);
            }
        }

        return Optional.empty();
    }

    public <T> T require(String name, Class<T> type) {
        return get(name, type).orElseThrow(() -> new InvalidFilterException("No arg: " + name + " of type: " + type.getName()));
    }

}
