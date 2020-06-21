package honeyroasted.occurrence.generator;

import honeyroasted.pecans.type.Types;
import honeyroasted.pecans.type.type.TypeInformal;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConstructorParams {
    private Map<String, Object> params = new LinkedHashMap<>();
    private Map<String, TypeInformal> paramTypes = new LinkedHashMap<>();

    public String add(String key, Object object) {
        if (params.values().stream().anyMatch(t -> t == object)) {
            return params.entrySet().stream().filter(e -> e.getValue() == object).findFirst().get().getKey();
        } else {
            this.params.put(key, object);
            this.paramTypes.put(key, Types.type(object.getClass()));
            return key;
        }
    }

    public String add(String key, Object object, TypeInformal type) {
        if (params.values().stream().anyMatch(t -> t == object)) {
            return params.entrySet().stream().filter(e -> e.getValue() == object).findFirst().get().getKey();
        } else {
            this.params.put(key, object);
            this.paramTypes.put(key, type);
            return key;
        }
    }

    public Map<String, TypeInformal> getParamTypes() {
        return paramTypes;
    }

    public Object[] genParams() {
        return this.params.values().toArray(Object[]::new);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
