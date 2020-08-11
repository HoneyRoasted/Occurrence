package honeyroasted.occurrence.annotation;

import honeyroasted.occurrence.InvalidFilterException;
import honeyroasted.occurrence.manager.ReflectionUtil;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FilterWrapper {
    private String id;
    private Map<String, Object> mapValues;
    private List<Object> arrayValues;

    public FilterWrapper(String id, Map<String, Object> mapValues, List<Object> arrayValues) {
        this.id = id;
        this.mapValues = mapValues;
        this.arrayValues = arrayValues;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getMapValues() {
        return mapValues;
    }

    public List<Object> getArrayValues() {
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

    private <T> Optional<T> get(Object value, Class<T> type) {
        type = ReflectionUtil.box(type);

        if (value != null) {
            if (type.isInstance(value)) {
                return Optional.of((T) value);
            } else if (value.getClass().isArray() && Array.getLength(value) == 1 && type.isInstance(Array.get(value, 0))) {
                return Optional.of((T) Array.get(value, 0));
            } else if (type.isArray() && ReflectionUtil.box(type.getComponentType()).isInstance(value)) {
                Object arr = Array.newInstance(type.getComponentType(), 1);
                Array.set(arr, 0, value);
                return Optional.of((T) arr);
            }
        }

        return Optional.empty();
    }


}
