import honeyroasted.occurrence.annotation.Arg;
import honeyroasted.occurrence.annotation.ExpandArgs;
import honeyroasted.occurrence.annotation.ExpandFilters;
import honeyroasted.occurrence.annotation.Filter;
import honeyroasted.occurrence.annotation.Filters;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.generics.ReflectionUtil;
import honeyroasted.occurrence.manager.EventManager;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        EventManager<Object> manager = EventManager.createNew();

        manager.register(Test.class);
        manager.post("Hello world");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(ConcatList.class)
    @Filter(id = "invoke", args = {@Arg(stringVal = "valueOf"), @Arg(name = "source", classVal = String.class)})
    @Filter(id = "invoke", args = {@Arg(stringVal = "concat"), @Arg(delegate = "value")})
    public @interface Concat {
        String value() default "1";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ConcatList {
        @ExpandFilters
        Concat[] value();
    }

    @Filters.Invoke(source = Repeat.Processor.class, value = "repeat")
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Repeat {
        @ExpandArgs(type = Filters.Invoke.class)
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
    public static void onStr(@Repeat(3) @Concat(" <- pretty") @Concat("yeet") String val) {
        System.out.println(val);
    }

    @Listener(event = String.class)
    @Filters.Invoke.Predicate(source = Test.class, value = "allow")
    public static void onStr1(@Concat("2") @Repeat(2) String val) {
        System.out.println(val);
    }

    public static boolean allow(String str) {
        return true;
    }


}
