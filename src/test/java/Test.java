import honeyroasted.javatype.JavaType;
import honeyroasted.javatype.Token;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.event.GenericEvent;
import honeyroasted.occurrence.manager.EventManager;

import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        EventManager<Object> manager = EventManager.createNew();

        manager.register(Test.class);
        manager.post(new Ev<>(new Token<Ev<String>>() {}.resolve()));
    }

    @Listener
    public static void onEv(Ev<String> ev) {
        System.out.println("str");
    }

    @Listener
    public static void onEv2(Ev<Integer> ev) {
        System.out.println("int");
    }

    public static class Ev<T> implements GenericEvent {
        private JavaType type;

        public Ev(JavaType type) {
            this.type = type;
        }

        public List<String> get() {
            return Arrays.asList("Hello", "world", "!");
        }

        @Override
        public JavaType type() {
            return this.type;
        }
    }


}
