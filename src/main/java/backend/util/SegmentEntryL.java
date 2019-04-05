package backend.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class SegmentEntryL {

    private SegmentEntryL() {}

    public abstract SegmentEntryL add(SegmentEntryL other);

    // only used by unit test
    abstract void addRead(int[] read);

    public static SegmentEntryL make(boolean isWrite, int[] event) {
        if (isWrite) {
            return new RW_L(event);
        } else {
            return new RR_L(event);
        }
    }

    static final class RW_L extends SegmentEntryL {

        public int[] write;

        public LinkedList<int[]> reads;

        public RW_L(int[] write) {
            this.write = write;
            this.reads = new LinkedList<>();
        }

        @Override
        public SegmentEntryL add(SegmentEntryL other) {
            return this;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("write: <");
            builder.append(Arrays.toString(Epoch.toClock(write)));
            builder.append(">, reads: <");
            Iterator<int[]> iter = reads.iterator();
            while (iter.hasNext()) {
                builder.append(Arrays.toString(Epoch.toClock(iter.next())));
                if (iter.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(">");
            return builder.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof RW_L) {
                RW_L o = (RW_L) other;
                if (!Arrays.equals(this.write, o.write)) {
                    return false;
                }
                if (this.reads.size() != o.reads.size()) {
                    return false;
                }
                Iterator<int[]> iter1 = this.reads.iterator();
                Iterator<int[]> iter2 = o.reads.iterator();
                while (iter1.hasNext()) {
                    if (!Arrays.equals(iter1.next(), iter2.next())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        void addRead(int[] read) {
            this.reads.add(read);
        }
    }

    static final class RR_L extends SegmentEntryL {

        public LinkedList<int[]> reads;

        public RR_L(int[] read) {
            this.reads = new LinkedList<>();
            this.reads.add(read);
        }

        @Override
        public SegmentEntryL add(SegmentEntryL other) {
            if (other instanceof RW_L) {
                RW_L rw = (RW_L) other;
                rw.reads.addAll(this.reads);
                return rw;
            } else {
                RR_L rr = (RR_L) other;
                this.reads.addAll(rr.reads);
                return this;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("reads: <");
            Iterator<int[]> iter = reads.iterator();
            while (iter.hasNext()) {
                builder.append(Arrays.toString(Epoch.toClock(iter.next())));
                if (iter.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(">");
            return builder.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof RR_L) {
                RR_L o = (RR_L) other;
                if (this.reads.size() != o.reads.size()) {
                    return false;
                }
                Iterator<int[]> iter1 = this.reads.iterator();
                Iterator<int[]> iter2 = o.reads.iterator();
                while (iter1.hasNext()) {
                    if (!Arrays.equals(iter1.next(), iter2.next())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        void addRead(int[] read) {
            this.reads.add(read);
        }
    }
}
