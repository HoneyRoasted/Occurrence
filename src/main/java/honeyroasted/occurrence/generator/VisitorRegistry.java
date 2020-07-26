package honeyroasted.occurrence.generator;

import honeyroasted.occurrence.generator.visitors.CancelledFilter;
import honeyroasted.occurrence.generator.visitors.EqualFilter;
import honeyroasted.occurrence.generator.visitors.IncludeExcludeFilter;
import honeyroasted.occurrence.generator.visitors.InvokeFilter;
import honeyroasted.occurrence.generator.visitors.InvokeNewFilter;
import honeyroasted.occurrence.generator.visitors.IterableAllFilter;
import honeyroasted.occurrence.generator.visitors.IterableFirstLastFilter;
import honeyroasted.occurrence.generator.visitors.NonnullFilter;
import honeyroasted.occurrence.generator.visitors.NoopFilter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class VisitorRegistry {
    public static final VisitorRegistry GLOBAL = new VisitorRegistry();

    static {
        GLOBAL.registerDefaults();
    }

    private Map<String, FilterVisitor> visitors = new LinkedHashMap<>();

    public void registerDefaults() {
        register(new NoopFilter("listener"));

        register(new InvokeNewFilter());
        register(new InvokeFilter(false, false, "invoke"));
        register(new InvokeFilter(false, true, "invoke.predicate"));
        register(new InvokeFilter(true, false, "listener.invoke"));
        register(new InvokeFilter(true, true, "listener.invoke.predicate"));

        register(new IncludeExcludeFilter(true, "include"));
        register(new IncludeExcludeFilter(false, "exclude"));

        register(new NonnullFilter());
        register(new CancelledFilter());
        register(new EqualFilter());

        register(new IterableFirstLastFilter(true, "iterable.first"));
        register(new IterableFirstLastFilter(false, "iterable.last"));
        register(new IterableAllFilter());
    }

    public void register(FilterVisitor visitor) {
        if (this.visitors.containsKey(visitor.id())) {
            throw new IllegalArgumentException("Duplicated visitor id: " + visitor.id());
        }

        this.visitors.put(visitor.id(), visitor);
    }

    public Optional<FilterVisitor> get(String id) {
        return Optional.ofNullable(this.visitors.get(id));
    }

}
