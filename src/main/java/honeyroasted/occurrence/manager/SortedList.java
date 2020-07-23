package honeyroasted.occurrence.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SortedList<T extends Comparable<T>> extends ArrayList<T> {

    public SortedList(List<T> val) {
        super();
        this.addAll(val);
    }

    public SortedList() {
        super();
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        for (T t : collection) {
            this.add(t);
        }
        return !collection.isEmpty();
    }

    @Override
    public boolean add(T o) {
        if (this.isEmpty()) {
            return super.add(o);
        } else if (this.get(this.size() - 1).compareTo(o) <= 0) {
            return super.add(o);
        } else if (this.get(0).compareTo(o) >= 0) {
            super.add(0, o);
        } else {
            binaryInsert(o, 0, size() - 1);
        }

        return true;
    }

    private void binaryInsert(T o, int low, int high) {
        int index = 0;

        while (low <= high) {
            int mid = (low + high) / 2;
            T midVal = this.get(mid);
            int cmp = midVal.compareTo(o);

            if (cmp < 0) {
                low = mid + 1;
                index = low;
            } else if (cmp > 0) {
                high = mid - 1;
                index = high;
            } else {
                index = mid;
                break;
            }
        }

        super.add(index, o);
    }

}