package honeyroasted.occurrence.generator.bytecode;

import honeyroasted.javatype.JavaType;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;

import java.lang.reflect.Method;

public interface FilterVisitor {

    String id();

    Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod);

    default boolean filterOnly() {
        return false;
    }

    class Result {
        private String variable;
        private JavaType returnType;

        public Result(String variable, JavaType returnType) {
            this.variable = variable;
            this.returnType = returnType;
        }

        public String getVariable() {
            return variable;
        }

        public JavaType getReturnType() {
            return returnType;
        }
    }

}
