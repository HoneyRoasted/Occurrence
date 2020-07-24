package honeyroasted.occurrence.generics;

import honeyroasted.pecans.type.type.TypeFill;
import honeyroasted.pecans.type.type.TypeInformal;
import honeyroasted.pecans.type.type.TypeVarRef;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaType {
    protected String name;
    protected JavaGenerics generics;
    protected Class cls;
    protected Class actual;
    protected int array;

    public static JavaType of(Token token) {
        return JavaType.of(((ParameterizedType) token.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    private JavaType(String name, JavaGenerics generics, Class cls, Class actual, int array) {
        this.name = name;
        this.generics = generics;
        this.cls = cls;
        this.actual = actual;
        this.array = array;
    }

    public JavaType addGeneric(JavaType type) {
        this.generics.getParameters().add(type);
        return this;
    }

    public JavaType addGeneric(Type type) {
        return addGeneric(JavaType.of(type));
    }

    public JavaType addGenerics(JavaType... types) {
        for (int i = 0; i < types.length; i++) {
            addGeneric(types[i]);
        }
        return this;
    }

    public JavaType addGenerics(Type... types) {
        for (int i = 0; i < types.length; i++) {
            addGeneric(types[i]);
        }
        return this;
    }

    public boolean isPrimitive() {
        return getEffectiveType().isPrimitive();
    }

    public TypeInformal toPecansType() {
        TypeFill fill = new TypeFill(org.objectweb.asm.Type.getType(getEffectiveType()));
        for (JavaType type : this.generics.getParameters()) {
            fill.addPart(type.toPecansType());
        }
        return fill;
    }

    public boolean isNumericPrimitive() {
        return getEffectiveType().isPrimitive() && !getEffectiveType().equals(boolean.class) && !getEffectiveType().equals(void.class);
    }

    public JavaType generics(JavaGenerics generics) {
        return new JavaType(this.name, generics, this.cls, this.actual, this.array);
    }

    public JavaType array(int array) {
        return new JavaType(this.name, this.generics, this.cls, array == 0 ? this.cls : ReflectionUtil.getArrayClass(this.cls, array), array);
    }

    public boolean isStrictlyAssignableTo(JavaType other) {
        if (other instanceof Variable) {
            return other.isStrictlyAssignableFrom(this);
        }

        return other.cls.isAssignableFrom(this.cls) && this.generics.isStrictlyAssignableTo(other.generics);
    }

    public boolean isStrictlyAssignableFrom(JavaType other) {
        if (other instanceof Variable) {
            return other.isStrictlyAssignableTo(this);
        }

        return this.cls.isAssignableFrom(other.cls) && other.generics.isStrictlyAssignableTo(this.generics);
    }

    public boolean isAssignableTo(JavaType other) {
        if (other instanceof Variable) {
            return other.isAssignableFrom(this);
        }

        return other.cls.isAssignableFrom(this.cls) && this.generics.isAssignableTo(other.generics);
    }

    public boolean isAssignableFrom(JavaType other) {
        if (other instanceof Variable) {
            return other.isAssignableTo(this);
        }

        return this.cls.isAssignableFrom(other.cls) && other.generics.isAssignableTo(this.generics);
    }

    public String getName() {
        return name;
    }

    public JavaGenerics getGenerics() {
        return generics;
    }

    public Class getType() {
        return cls;
    }

    public Class getActualType() {
        return this.actual;
    }

    public Class getEffectiveType() {
        return this.actual;
    }

    public int getArray() {
        return array;
    }

    public boolean isWildcard() {
        return this.getName().equals("?");
    }

    public boolean isVariable() {
        return false;
    }

    public JavaType getSuperType(Class type) {
        return ReflectionUtil.getWithRespect(this, type).get();
    }

    public String toString() {
        String res = getName() + this.generics.toString();
        for (int i = 0; i < array; i++) {
            res += "[]";
        }
        return res;
    }

    public static JavaType of(Class cls) {
        return of(cls, 0);
    }

    public static JavaType of(Class cls, int arr) {
        Class c = cls;
        while (cls.isArray()) {
            arr++;
            cls = cls.getComponentType();
        }
        return new JavaType(cls.getName(), JavaGenerics.empty(), cls, c, arr);
    }

    public static JavaType of(Class cls, JavaGenerics generics) {
        return of(cls, generics, 0);
    }

    private static JavaType of(Class cls, JavaGenerics generics, int arr) {
        Class c = cls;
        while (cls.isArray()) {
            arr++;
            cls = cls.getComponentType();
        }
        return new JavaType(cls.getName(), generics, cls, c, arr);
    }

    public static JavaType of(Type type) {
        return of(type, 0);
    }

    public static JavaType of(Type type, int arr) {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            Type raw = ptype.getRawType();
            if (raw instanceof Class) {
                return JavaType.of((Class) raw, JavaGenerics.of(ptype), arr);
            } else {
                throw new IllegalArgumentException("Unknown raw type: " + raw.getClass());
            }
        } else if (type instanceof Class) {
            return JavaType.of((Class) type, arr);
        } else if (type instanceof WildcardType) {
            WildcardType wtype = (WildcardType) type;
            List<JavaType> upper = Stream.of(wtype.getUpperBounds()).map(JavaType::of).collect(Collectors.toList());
            List<JavaType> lower = Stream.of(wtype.getLowerBounds()).map(JavaType::of).collect(Collectors.toList());
            return new Variable("?", upper, lower, arr);
        } else if (type instanceof TypeVariable) {
            TypeVariable variable = (TypeVariable) type;
            List<JavaType> upper = Stream.of(variable.getBounds()).map(JavaType::of).collect(Collectors.toList());
            return new Variable(variable.getName(), upper, new ArrayList<>(), arr);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType array = (GenericArrayType) type;
            return JavaType.of(array.getGenericComponentType(), arr + 1);
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.getClass());
        }
    }

    public static class Variable extends JavaType {
        private List<JavaType> upper;
        private List<JavaType> lower;
        private Class effective;

        public Variable(String name, List<JavaType> upper, List<JavaType> lower, int array) {
            super(name, JavaGenerics.empty(), null, null, array);
            this.upper = upper;
            this.lower = lower;
            if (!upper.isEmpty()) {
                this.effective = ReflectionUtil.getCommonParent(upper.stream().map(JavaType::getEffectiveType).collect(Collectors.toList()));
            } else {
                this.effective = Object.class;
            }
            if (array != 0) {
                this.effective = ReflectionUtil.getArrayClass(this.effective, array);
            }
        }

        @Override
        public TypeInformal toPecansType() {
            return new TypeVarRef(name);
        }

        public JavaType generics(JavaGenerics generics) {
            Variable variable = new Variable(this.getName(), this.upper, this.lower, this.getArray());
            variable.generics = generics;
            return variable;
        }

        public JavaType array(int array) {
            return new Variable(this.getName(), this.upper, this.lower, array);
        }

        public List<JavaType> getUpper() {
            return upper;
        }

        public List<JavaType> getLower() {
            return lower;
        }

        public Class getEffectiveType() {
            return effective;
        }

        @Override
        public boolean isVariable() {
            return true;
        }

        @Override
        public boolean isStrictlyAssignableFrom(JavaType other) {
            return !this.lower.isEmpty() && this.lower.stream().allMatch(t -> t.isStrictlyAssignableFrom(other));
        }

        @Override
        public boolean isStrictlyAssignableTo(JavaType other) {
            return !this.upper.isEmpty() && this.upper.stream().anyMatch(t -> t.isStrictlyAssignableTo(other));
        }

        @Override
        public boolean isAssignableFrom(JavaType other) {
            return !this.lower.isEmpty() && this.lower.stream().allMatch(t -> t.isAssignableFrom(other));
        }

        @Override
        public boolean isAssignableTo(JavaType other) {
            return !this.upper.isEmpty() && this.upper.stream().anyMatch(t -> t.isAssignableTo(other));
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.getName());

            for (int i = 0; i < this.array; i++) {
                builder.append("[]");
            }

            if (!this.upper.isEmpty()) {
                builder.append(" extends ");
                for (int i = 0; i < this.upper.size(); i++) {
                    builder.append(this.upper.get(i));
                    if (i != this.upper.size() - 1) {
                        builder.append("& ");
                    }
                }
            }

            if (!this.lower.isEmpty()) {
                if (!this.upper.isEmpty()) {
                    builder.append(" |");
                }
                builder.append(" super ");
                for (int i = 0; i < this.lower.size(); i++) {
                    builder.append(this.lower.get(i));
                    if (i != this.lower.size() - 1) {
                        builder.append("& ");
                    }
                }
            }

            return builder.toString();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            if (!super.equals(object)) return false;
            Variable variable = (Variable) object;
            return Objects.equals(upper, variable.upper) &&
                    Objects.equals(lower, variable.lower);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), upper, lower);
        }
    }

    public static abstract class Token<T> {

    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof JavaType)) return false;
        JavaType type = (JavaType) object;
        return array == type.array &&
                Objects.equals(name, type.name) &&
                Objects.equals(generics, type.generics) &&
                Objects.equals(cls, type.cls) &&
                Objects.equals(actual, type.actual);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, generics, cls, actual, array);
    }

}
