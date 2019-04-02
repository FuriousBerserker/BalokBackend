package backend.util;

import java.util.function.Consumer;

public final class Segment implements Consumer<Segment> {
    private SegmentEntryL pending;
    private SegmentEntryR current;

    public Segment(SegmentEntryL pending, SegmentEntryR current) {
        this.pending = pending;
        this.current = current;
    }

    @Override
    public void accept(Segment other) {
        this.current.check(other.pending);
        this.pending = this.pending.add(other.pending);
        this.current = this.current.add(other.current);
    }

//    public Iterable<Event<E>> getPendingReads() {
//        return pending.getReads();
//    }
//
//    public Iterable<Event<E>> getPendingWrites() {
//        return pending.getWrites();
//    }
//
//
//    public Iterable<E> getReads() {
//        return current.getReads();
//    }
//
//    public EpochSet<E> getReadSet() {
//        return current.getReadSet();
//    }
//
//    public Iterable<E> getWrites() {
//        return current.getWrites();
//    }

    public static Segment make(boolean isWrite, int[] event, int tid) {
        return new Segment(SegmentEntryL.make(isWrite, event), SegmentEntryR.make(isWrite, event[tid], tid));
    }

    public static IntervalTree<Segment> makeInterval(Segment tracker, Interval range) {
        return new IntervalTree<>(tracker, range);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Segment that = (Segment) o;

        if (!pending.equals(that.pending)) return false;
        return current.equals(that.current);
    }

//    public int size() {
//        return current.size() + pending.size();
//    }

    @Override
    public int hashCode() {
        int result = pending.hashCode();
        result = 31 * result + current.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(pending=" + pending + ", current=" + current + ")";
    }

    public SegmentEntryL getPending() {
        return pending;
    }

    public SegmentEntryR getCurrent() {
        return current;
    }
}
