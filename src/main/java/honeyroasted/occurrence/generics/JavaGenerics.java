package honeyroasted.occurrence.generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaGenerics {
    private List<JavaType> parameters;

    public JavaGenerics(List<JavaType> parameters) {
        this.parameters = parameters;
    }

    public static JavaGenerics empty() {
        return new JavaGenerics(new ArrayList<>());
    }

    public static JavaGenerics ofTypeParameters(Class cls) {
        List<JavaType> params = new ArrayList<>();
        for (TypeVariable variable : cls.getTypeParameters()) {
            params.add(JavaType.of(variable));
        }
        return new JavaGenerics(params);
    }

    public static JavaGenerics of(Type... types) {
        List<JavaType> params = new ArrayList<>();
        for (Type type : types) {
            params.add(JavaType.of(type));
        }
        return new JavaGenerics(params);
    }

    public static JavaGenerics of(JavaType... types) {
        List<JavaType> params = new ArrayList<>();
        Collections.addAll(params, types);
        return new JavaGenerics(params);
    }

    public static JavaGenerics of(ParameterizedType type) {
        List<JavaType> types = new ArrayList<>();
        Stream.of(type.getActualTypeArguments()).forEach(t -> types.add(JavaType.of(t)));
        return new JavaGenerics(types);
    }

    public boolean isStrictlyAssignableTo(JavaGenerics other) {
        List<JavaType> otherTypes = other.parameters;
        if (otherTypes.size() == parameters.size()) {
            for (int i = 0; i < otherTypes.size(); i++) {
                if (!parameters.get(i).isAssignableFrom((other.parameters.get(i)))) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    public List<JavaType> getParameters() {
        return parameters;
    }

    public JavaType getActualParameter(int i) {
        if (i >= 0 && i < parameters.size()) {
            return parameters.get(i);
        }

        return JavaType.of(Object.class);
    }

    public boolean isStrictlyAssignableFrom(JavaGenerics other) {
        return other.isStrictlyAssignableTo(this);
    }

    public boolean isAssignableTo(JavaGenerics other) {
        return isStrictlyAssignableTo(other) || this.parameters.size() == 0 || other.parameters.size() == 0;
    }

    public boolean isAssignableFrom(JavaGenerics other) {
        return other.isAssignableTo(this);
    }

    public String toString() {
        if (this.parameters.isEmpty()) {
            return "";
        }

        return "<" + this.parameters.stream().map(JavaType::toString).collect(Collectors.joining(", ")) + ">";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        JavaGenerics generics = (JavaGenerics) object;
        return Objects.equals(parameters, generics.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }

}
