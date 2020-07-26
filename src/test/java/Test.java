import honeyroasted.occurrence.annotation.Arg;
import honeyroasted.occurrence.annotation.Expand;
import honeyroasted.occurrence.annotation.Filter;
import honeyroasted.occurrence.annotation.Filters;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.manager.EventManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Test {

    public static void main(String[] args) {
        EventManager<Object> manager = EventManager.createNew();

        manager.register(Test.class);
        manager.post("Hello world");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "invoke", args = {@Arg(stringVal = "valueOf"), @Arg(name = "source", classVal = String.class)})
    @Filter(id = "invoke", args = {@Arg(stringVal = "concat"), @Arg(delegate = "value")})
    public @interface Concat {
        String value() default "1";
    }

    @Filters.Invoke(source = Repeat.Processor.class, value = "repeat")
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Repeat {
        @Expand(type = Filters.Invoke.class)
        int value() default 1;

        class Processor {
            public static String repeat(Object val, int times) {
                StringBuilder str = new StringBuilder(String.valueOf(val));
                for (int i = 0; i < times; i++) {
                    str.append(val);
                }
                return str.toString();
            }
        }
    }

    @Listener
    public static void onStr(@Repeat(3) @Concat(" <- pretty") String val) {
        System.out.println(val);
    }

    @Listener
    @Filters.Include(String.class)
    @Filters.Invoke.Predicate(source = Test.class, value = "allow")
    public static void onStr1(@Concat("2") @Repeat(2) String val) {
        System.out.println(val);
    }

    public static boolean allow(String str) {
        return true;
    }


}
