package honeyroasted.occurrence.generator;

import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;

public interface FilterVisitor {

    String id();

    Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry);

    void visitCondition(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry);

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
