import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.generator.ListenerWrapperGenerator;
import honeyroasted.occurrence.generator.VisitorRegistry;
import honeyroasted.occurrence.policy.PolicyRegistry;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

public class Test {

    public static void main(String[] args) throws Throwable {
        ListenerWrapperGenerator.gen(new Test(), VisitorRegistry.GLOBAL, PolicyRegistry.GLOBAL).forEach(c -> {
            try {
                c.getClassNode().writeIn(Paths.get("bytecode_tests"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        ListenerWrapperGenerator.gen(Test.class, VisitorRegistry.GLOBAL, PolicyRegistry.GLOBAL).forEach(c -> {
            try {
                c.getClassNode().writeIn(Paths.get("bytecode_tests"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Listener
    public void onList(List<String> val) {

    }

    @Listener
    public static void onOtherThing(Collection<Integer> val) {

    }

}
