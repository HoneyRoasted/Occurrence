import honeyroasted.occurrence.annotation.Arg;
import honeyroasted.occurrence.annotation.Filter;
import honeyroasted.occurrence.annotation.Filters;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.event.CancellableEvent;
import honeyroasted.occurrence.manager.BakingEventManager;
import honeyroasted.occurrence.manager.EventManager;
import honeyroasted.occurrence.manager.SimpleEventManager;

import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        EventManager<Object> manager = new BakingEventManager<>();

        manager.register(new Test());
        manager.post("Hello world");
    }

    @Listener
    public void onStr(String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr1(String val) {
        System.out.println(val);
        throw new RuntimeException("Pepega");
    }

    @Listener
    public void onStr2(String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr3(String val) {
        System.out.println(val);
    }

    @Listener
    public void onStr4(String val) {
        System.out.println(val);
    }


}
