package backend.util;

import com.carrotsearch.hppc.ObjectStack;

import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 *     A container (structured as a binary-search tree) of intervals.
 *     Intervals are coalesced and their tagged values merged when they are connected, according to a
 *     user supplied function.
 * </p>
 * @see Interval
 * @param <T> The type of the contained elements.
 */
public final class IntervalTree<T extends Consumer<T>> implements Iterable<IntervalTree<T>> {
    private T value;
    private IntervalTree<T> smallerRange;
    private IntervalTree<T> biggerRange;
    private Interval interval;

    public IntervalTree(T value, Interval rng) {
        if (rng == null) {
            throw new IllegalArgumentException("interval cannot be null");
        }
        this.value = value;
        this.interval = rng;
    }

    public Interval getInterval() {
        return interval;
    }

    public T getValue() {
        return value;
    }

    public IntervalTree<T> getSmallerRange() {
        return smallerRange;
    }

    public IntervalTree<T> getBiggerRange() {
        return biggerRange;
    }

    protected void updateHighChild() {
        if (biggerRange == null) {
            return;
        }
        IntervalTree<T> other = biggerRange.getFirst();
        if (other.getInterval().succeeds(interval)) {
            biggerRange.removeChild(this, other);
            this.interval.add(other.interval);
            this.value.accept(other.value);
        }
    }

    protected void updateLowChild() {
        if (smallerRange == null) {
            return;
        }
        IntervalTree<T> other = smallerRange.getLast();
        if (interval.succeeds(other.getInterval())) {
            smallerRange.removeChild(this, other);
            this.interval.add(other.interval);
            other.value.accept(this.value);
            this.value = other.value;
        }
    }

    public static <T extends Consumer<T>> IntervalTree<T> merge(IntervalTree<T> left, IntervalTree<T> right) {
        if (left == null) {
            return right;
        }
        left.add(right);
        return left;
    }

    public static <T extends Consumer<T>> IntervalTree<T> sum(Iterable<IntervalTree<T>> tree) {
        Iterator<IntervalTree<T>> iter = tree.iterator();
        if (! iter.hasNext()) {
            throw new IllegalArgumentException("Iterator must be non-empty.");
        }
        IntervalTree<T> elem = iter.next();
        while (iter.hasNext()) {
            elem.add(iter.next());
        }
        return elem;
    }

    public void add(IntervalTree<T> other) {
        switch (this.interval.match(other.interval)) {
            case CONNECT_BEHIND:
                interval.add(other.interval);
                other.value.accept(this.value);
                value = other.value;
                updateLowChild();
                break;
            case CONNECT_AHEAD:
                interval.add(other.interval);
                this.value.accept(other.value);
                updateHighChild();
                break;
            case AHEAD:
                if (this.biggerRange == null) {
                    this.biggerRange = other;
                } else {
                    this.biggerRange.add(other);
                }
                break;
            case BEHIND:
                if (this.smallerRange == null) {
                    this.smallerRange = other;
                } else {
                    this.smallerRange.add(other);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public Iterator<IntervalTree<T>> iterator() {
        return new IntRangeIterator<>(this);
    }

    private static class IntRangeIterator<T extends Consumer<T>> implements Iterator<IntervalTree<T>> {
        private ObjectStack<IntervalTree<T>> next = new ObjectStack<>();

        public IntRangeIterator(IntervalTree<T> range) {
            next.add(range);
        }

        @Override
        public boolean hasNext() {
            return next.size() > 0;
        }

        @Override
        public IntervalTree<T> next() {
            if (next.isEmpty()) {
                throw new NoSuchElementException();
            }
            IntervalTree<T> elem = next.pop();
            IntervalTree<T> biggerRange = elem.getBiggerRange();
            if (biggerRange != null) {
                next.push(biggerRange);
            }
            IntervalTree<T> smallerRange = elem.getSmallerRange();
            if (smallerRange != null) {
                next.push(smallerRange);
            }
            return elem;
        }
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IntervalTree)) {
            return false;
        }
        IntervalTree<T> other = (IntervalTree<T>) obj;
        List<IntervalTree<T>> lst1 = this.toList();
        List<IntervalTree<T>> lst2 = other.toList();
        if  (lst1.size() != lst2.size()) {
            return false;
        }
        Iterator<IntervalTree<T>> iter1 = lst1.iterator();
        Iterator<IntervalTree<T>> iter2 = lst2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            if (! shallowEquals(iter1.next(), iter2.next())) {
                return false;
            }
        }
        return iter1.hasNext() == iter2.hasNext();
    }

    private static <T extends Consumer<T>> boolean shallowEquals(IntervalTree<T> left, IntervalTree<T> right) {
        if  (left == null || right == null) {
            return left == right;
        }
        if (! left.getInterval().equals(right.getInterval())) {
            return false;
        }
        if  (left.getValue() == null || right.getValue() == null) {
            return right.getValue() == left.getValue();
        }
        return left.getValue().equals(right.getValue());
    }

    private List<IntervalTree<T>> toList() {
        ArrayList<IntervalTree<T>> result = new ArrayList<>();
        for (IntervalTree<T> entry : this) {
            result.add(entry);
        }
        return result;
    }

    private Map<Interval, T> toMap() {
        Map<Interval, T> result = new TreeMap<>();
        for (IntervalTree<T> entry : this) {
            result.put(entry.interval, entry.value);
        }
        return result;
    }

    private static class Entry<T extends Consumer<T>> implements Map.Entry<Interval, T> {
        private IntervalTree<T> current;
        @Override
        public Interval getKey() {
            return current.interval;
        }

        @Override
        public T getValue() {
            return current.value;
        }

        @Override
        public T setValue(T value) {
            T old = current.value;
            current.value = value;
            return old;
        }
    }

    public IntervalTree<T> getFirst() {
        IntervalTree<T> node = this;
        for (; node != null && node.smallerRange != null; node = node.smallerRange) {
        }
        return node;
    }

    public IntervalTree<T> getLast() {
        IntervalTree<T> node = this;
        for (; node != null && node.biggerRange != null; node = node.biggerRange) {
        }
        return node;
    }

//    public boolean remove(IntervalTree<T> other) {
//        if (this.smallerRange == other) {
//            this.smallerRange = null;
//            return true;
//        } else if (this.biggerRange == other) {
//            this.biggerRange = null;
//            return true;
//        }
//        int cmp = interval.compareTo(other.interval);
//        if (cmp < 0 && smallerRange != null) {
//            return smallerRange.remove(other);
//        } else if (cmp > 0 && biggerRange != null) {
//            return biggerRange.remove(other);
//        }
//        return false;
//    }

    boolean removeChild(IntervalTree<T> parent, IntervalTree<T> other) {
        if (this == other) {
            if (this.smallerRange == null) {
                if (this.biggerRange != null) {
                    if (parent.smallerRange == this) {
                        parent.smallerRange = this.biggerRange;
                    } else {
                        parent.biggerRange = this.biggerRange;
                    }
                } else {
                    if (parent.smallerRange == this) {
                        parent.smallerRange = null;
                    } else {
                        parent.biggerRange = null;
                    }
                }
            } else if (this.biggerRange == null) {
                if (parent.smallerRange == this) {
                    parent.smallerRange = this.smallerRange;
                } else {
                    parent.biggerRange = this.smallerRange;
                }
            } else {
                IntervalTree<T> rightMin = this.biggerRange.getFirst();
                this.interval = rightMin.interval;
                this.value = rightMin.value;
                this.biggerRange.removeChild(this, rightMin);
            }
            return true;
        } else {
            boolean result;
            switch (interval.match(other.getInterval())) {
                case CONNECT_BEHIND:
                case BEHIND:
                    if (smallerRange != null) {
                        result = smallerRange.removeChild(this, other);
                    } else {
                        result =  false;
                    }
                    break;
                case CONNECT_AHEAD:
                case AHEAD:
                    if (biggerRange != null) {
                        result =  biggerRange.removeChild(this, other);
                    } else {
                        result = false;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            return result;
        }
    }

    public int size() {
        return 1 + (biggerRange == null ? 0 : biggerRange.size()) + (smallerRange == null ? 0 : smallerRange.size());
    }

    @Override
    public String toString() {
        return toMap().toString();
    }
}
