package honeyroasted.occurrence;

public interface ListenerWrapper<T> extends Comparable<ListenerWrapper<T>> {

    void handle(T event) throws Throwable;

    String name();

    int priority();

    @Override
    default int compareTo(ListenerWrapper<T> o) {
        return Integer.compare(this.priority(), o.priority());
    }

}
