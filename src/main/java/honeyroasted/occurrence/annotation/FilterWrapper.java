package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.InvalidFilterException;
import honeyroasted.occurrence.generics.ReflectionUtil;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FilterWrapper {
    private String id;
    private Map<String, Value> mapValues;
    private List<Value> arrayValues;

    public FilterWrapper(String id, Map<String, Value> mapValues, List<Value> arrayValues) {
        this.id = id;
        this.mapValues = mapValues;
        this.arrayValues = arrayValues;
    }

    public static class Value {
        private Object value;
        private boolean forceArray;

        public Value(Object value, boolean forceArray) {
            this.value = value;
            this.forceArray = forceArray;
        }

        public Object getValue() {
            return value;
        }

        public boolean isForceArray() {
            return forceArray;
        }
    }

    public String getId() {
        return id;
    }

    public Map<String, Value> getMapValues() {
        return mapValues;
    }

    public List<Value> getArrayValues() {
        return arrayValues;
    }

    public int arraySize() {
        return arrayValues.size();
    }

    public <T> Optional<T> get(String name, Class<T> type) {
        return get(this.mapValues.get(name), type);
    }

    public <T> T require(String name, Class<T> type) {
        return get(name, type).orElseThrow(() -> new InvalidFilterException("No arg: " + name + " of type: " + type.getName()));
    }

    public <T> Optional<T> get(int index, Class<T> type) {
        return get((index >= 0 && index < arrayValues.size()) ? arrayValues.get(index) : null, type);
    }

    public <T> T require(int index, Class<T> type) {
        return get(index, type).orElseThrow(() -> new InvalidFilterException("No arg: " + index + " of type: " + type.getName()));
    }

    private <T> Optional<T> get(Value value, Class<T> type) {
        type = ReflectionUtil.box(type);

        if (value != null) {
            Object val = value.getValue();
            boolean force = value.isForceArray();
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


}
