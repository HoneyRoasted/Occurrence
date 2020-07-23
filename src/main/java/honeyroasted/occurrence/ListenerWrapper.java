package honeyroasted.occurrence;

public interface ListenerWrapper<T> extends Comparable<ListenerWrapper<T>> {

    void handle(T event) throws Throwable;

    String name();

    int priority();

    Class<T> event();

    default Object listener() {
        return this;
    }

    @Override
    default int compareTo(ListenerWrapper<T> o) {
        return Integer.compare(this.priority(), o.priority());
    }

}
