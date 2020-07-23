package honeyroasted.occurrence.generator;

import honeyroasted.pecans.node.Nodes;
import honeyroasted.pecans.node.instruction.TypedNode;
import honeyroasted.pecans.type.Types;
import honeyroasted.pecans.type.type.TypeInformal;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConstructorParams {
    private Map<String, Object> params = new LinkedHashMap<>();
    private Map<String, TypeInformal> paramTypes = new LinkedHashMap<>();

    public String add(String key, Object object) {
        this.params.put(key, object);
        this.paramTypes.put(key, Types.type(object.getClass()));
        return key;
    }

    public TypedNode get(String key) {
        return Nodes.get(Nodes.loadThis(), key, this.paramTypes.get(key));
    }

    public String add(String key, Object object, TypeInformal type) {
        this.params.put(key, object);
        this.paramTypes.put(key, type);
        return key;
    }

    public TypeInformal getType(String key) {
        return paramTypes.get(key);
    }

    public Object getObj(String key) {
        return params.get(key);
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
