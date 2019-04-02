package backend.util;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;

import java.util.Iterator;
import java.util.List;

/**
 * A simplified version of ShadowMemory. Use id (int) instead of AsyncLocationTracker as the key
 */
public final class SimpleShadowMemory implements Iterable<IntObjectCursor<IntervalTree<Segment>>> {
    private final IntObjectHashMap<IntervalTree<Segment>> accesses = new IntObjectHashMap<>();

    public void add(int id, boolean isWrite, int[] event, int ticket, int tid) {
        IntervalTree entry = get(id);
        if (entry == null) {
            entry = Segment.makeInterval(Segment.make(isWrite, event, tid), new Interval(ticket));
            accesses.put(id, entry);
        } else {
            entry.add(Segment.makeInterval(Segment.make(isWrite, event, tid), new Interval(ticket)));
        }
    }

//    public void addAll(SimpleShadowMemory<T> other) {
//        for (IntObjectCursor<ShadowEntry<T>> cursor : other.accesses) {
//            ShadowEntry<T> entry = get(cursor.key);
//            entry.addAll(cursor.value);
//        }
//    }

    public void clear() {
        accesses.clear();
    }

//    public ShadowEntry<T> addAll(int id, AccessMode[] mode, Event<T>[] event, int[] ticket, List<Integer> indexList) {
//        ShadowEntry<T> entry = get(id);
//        for (int index : indexList) {
//            entry.add(mode[index], event[index], ticket[index]);
//        }
//        return entry;
//    }

    public IntervalTree get(int id) {
        int index = accesses.indexOf(id);
        IntervalTree shadow;
        if (accesses.indexExists(index)) {
            shadow = accesses.indexGet(index);
        } else {
            shadow = null;
        }
        return shadow;
    }

    @Override
    public String toString() {
        return accesses.toString();
    }

    public int getEntryNum() {
        return this.accesses.size();
    }

    @Override
    public Iterator<IntObjectCursor<IntervalTree<Segment>>> iterator() {
        return accesses.iterator();
    }
}
