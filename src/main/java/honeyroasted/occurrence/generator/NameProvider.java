package honeyroasted.occurrence.generator;

import java.util.HashMap;
import java.util.Map;

public class NameProvider {
    private Map<String, Integer> indicies = new HashMap<>();

    public String provide() {
        return provide("var");
    }

    public String provide(String base) {
        int target = indicies.computeIfAbsent(base, b -> 0);
        indicies.put(base, target + 1);
        return base + target;
    }

}
