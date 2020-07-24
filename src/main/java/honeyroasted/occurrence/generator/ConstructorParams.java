package honeyroasted.occurrence.generator;

import honeyroasted.pecans.node.Nodes;
import honeyroasted.pecans.node.instruction.Node;
import honeyroasted.pecans.node.instruction.TypedNode;
import honeyroasted.pecans.type.Types;
import honeyroasted.pecans.type.type.TypeArray;
import honeyroasted.pecans.type.type.TypeInformal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class ConstructorParams {
    private Map<String, Object> params = new LinkedHashMap<>();
    private Map<String, TypeInformal> paramTypes = new LinkedHashMap<>();

    private Map<String, TypedNode> constants = new LinkedHashMap<>();
    private Map<String, TypeInformal> constantTypes = new LinkedHashMap<>();

    public String add(String key, Object object) {
        return add(key, object, object == null ? nullType() : type(object.getClass()));
    }

    public TypedNode get(String key) {
        if (this.paramTypes.containsKey(key)) {
            return Nodes.get(loadThis(), key, this.paramTypes.get(key));
        } else {
            return this.constants.get(key);
        }
    }

    public String add(String key, Object object, TypeInformal type) {
        if (object == null || isConstantAble(object, type)) {
            addConstant(key, val(object), type);
        } else if (type.isArray()) {
            TypeArray typeArray = (TypeArray) type;
            int size = Array.getLength(object);
            if (size == 0) {
                addConstant(key, newArray(typeArray.element(), constant(0)), typeArray);
            } else if (isConstantAble(Array.get(object, 0), typeArray.element())) {
                List<TypedNode> values = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    values.add(val(Array.get(object, i)));
                }
                addConstant(key, newArray(typeArray.element(), values), typeArray);
            } else {
                addParam(key, object, type);
            }
        } else {
            addParam(key, object, type);
        }

        return key;
    }

    private TypedNode val(Object object) {
        if (object instanceof Enum) {
            Enum enu = (Enum) object;
            return Nodes.get(type(enu.getDeclaringClass()), enu.name(), type(enu.getDeclaringClass()));
        } else {
            return constant(object);
        }
    }

    private boolean isConstantAble(Object object, TypeInformal type) {
        return object instanceof Enum ||
                type.isPrimitive() || type.equals(STRING) || type.equals(type(Class.class));
    }

    public String addParam(String key, Object object) {
        return addParam(key, object, type(object.getClass()));
    }

    public String addParam(String key, Object object, TypeInformal type) {
        if (object == null) {
            throw new IllegalArgumentException("Param may not be null");
        }

        this.params.put(key, object);
        this.paramTypes.put(key, type);
        return key;
    }

    private void addConstant(String key, TypedNode node, TypeInformal type) {
        this.constants.put(key, node);
        this.constantTypes.put(key, type);
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
