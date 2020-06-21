package honeyroasted.occurrence.generator;

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
