import honeyroasted.occurrence.annotation.Arg;
import honeyroasted.occurrence.annotation.Filter;
import honeyroasted.occurrence.annotation.Filters;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.event.CancellableEvent;
import honeyroasted.occurrence.manager.BakingEventManager;
import honeyroasted.occurrence.manager.EventManager;
import honeyroasted.occurrence.manager.SimpleEventManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        EventManager<Object> manager = new BakingEventManager<>();

        manager.register(new Test());
        manager.post("Hello world");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Filter(id = "invoke", args = {@Arg(stringVal = "valueOf"), @Arg(name = "source", classVal = String.class)})
    @Filter(id = "invoke", args = {@Arg(stringVal = "concat"), @Arg(name = "0", delegate = "value")})
    public @interface MyAnnotation {
        String value();
    }

    @Listener
    public void onStr0(@MyAnnotation("1") String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr1(@MyAnnotation("2") String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr2(@MyAnnotation("3") String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr3(@MyAnnotation("4") String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr4(@MyAnnotation("5") String val) {
        System.out.println(val);
    }


}
