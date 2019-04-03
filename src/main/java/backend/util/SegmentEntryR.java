package backend.util;

import java.sql.SQLOutput;
import java.util.Arrays;

public abstract class SegmentEntryR {

    private SegmentEntryR() {}

    private static enum RaceType {
        RW {
            @Override
            public String toString() {
                return "read-write race";
            }
        },

        WW {
            @Override
            public String toString() {
                return "write-write race";
            }
        },

        WR {
            @Override
            public String toString() {
                return "write-read race";
            }
        }
    }

    public static boolean THROW_EXCEPTION = false;

    public static int TID_BITS = 5;

    public static final int CLOCK_BITS = Integer.SIZE - TID_BITS;

    public static final int MAX_CLOCK = ( ((int)1) << CLOCK_BITS) - 1;

    public static int clock(int epoch) {
        return epoch & MAX_CLOCK;
    }

    public static boolean leq(int/*epoch*/ e1, int/*epoch*/ e2) {
        // Assert.assertTrue(tid(e1) == tid(e2));
        return clock(e1) <= clock(e2);
    }

    public static int/*epoch*/ make(int tid, int/*epoch*/ clock) {
        return (((int/*epoch*/)tid) << CLOCK_BITS) | clock;
    }

    private static void reportError(int tid, int epoch, int[] vc, RaceType type) {
        if (THROW_EXCEPTION) {
            throw new IllegalStateException();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString());
        builder.append('\n');
        builder.append("access 1: ");
        builder.append(tid);
        builder.append("->");
        builder.append(epoch);
        builder.append('\n');
        builder.append("access 2: ");
        builder.append(Arrays.toString(vc));
        System.out.println(builder.toString());
    }

    private static void reportError(int[] vc1, int[] vc2, RaceType type) {
        if (THROW_EXCEPTION) {
            throw new IllegalStateException();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString());
        builder.append('\n');
        builder.append("access 1: ");
        builder.append(Arrays.toString(vc1));
        builder.append('\n');
        builder.append("access 2: ");
        builder.append(Arrays.toString(vc2));
        System.out.println(builder.toString());
    }

    public static SegmentEntryR make(boolean isWrite, int epoch, int tid) {
        if (isWrite) {
            return new WR_R(epoch, tid);
        } else {
            return new RR_R(epoch, tid);
        }
    }

    // Check epoch <= vc
    private static boolean ensureHappensBefore(int tid, int epoch, int[] vc) {
        if (tid >= vc.length || !leq(epoch, vc[tid])) {
            return false;
        } else {
            return true;
        }
    }

    // Check vc1 <= vc2
    private static boolean ensureHappensBefore(int[] vc1, int[] vc2) {
        int minLen = Math.min(vc1.length, vc2.length);

        for (int i = 0; i < minLen; i++) {
            if (!leq(vc1[i], vc2[i])) {
                return false;
            }
        }

        for (int i = minLen; i < vc1.length; i++) {
            if (vc1[i] != make(i, 0)) {
                return false;
            }
        }
        return true;
    }

    // Add vc1 and vc2. The result is stored in the longer array. This method has side effect.
    private static int[] addAll(int[] vc1, int[] vc2) {
        int[] result = vc1;
        int[] other = vc2;
        if (vc1.length < vc2.length) {
            result = vc2;
            other = vc1;
        }
        for (int i = 0; i < other.length; i++) {
            result[i] = make(i, Math.max(clock(other[i]), clock(result[i])));
        }
        return result;
    }

    public abstract void check(SegmentEntryL other);

    public abstract SegmentEntryR add(SegmentEntryR other);

    static final class WR_R extends SegmentEntryR {

        public int write;

        public int tid;

        public int[] reads;

        private final static int[] EMPTY = new int[0];

        public WR_R(int epoch, int tid) {
            this.write = epoch;
            this.tid = tid;
            this.reads = EMPTY;
        }

        @Override
        public void check(SegmentEntryL other) {
            if (other instanceof SegmentEntryL.RW_L) {
                // check write-read race
                SegmentEntryL.RW_L rw = (SegmentEntryL.RW_L) other;
                for (int[] rw_read : rw.reads) {
                    if (!ensureHappensBefore(tid, write, rw_read)) {
                        reportError(tid, write, rw_read, RaceType.WR);
                    }
                }
                // check write-write race
                if (!ensureHappensBefore(tid, write, rw.write)) {
                    reportError(tid, write, rw.write, RaceType.WW);
                }
                // check read-write race
                if (!ensureHappensBefore(reads, rw.write)) {
                    reportError(reads, rw.write, RaceType.RW);
                }
            } else {
                SegmentEntryL.RR_L rr = (SegmentEntryL.RR_L) other;
                // check write-read race
                for (int[] rr_read : rr.reads) {
                    if (!ensureHappensBefore(tid, write, rr_read)) {
                        reportError(tid, write, rr_read, RaceType.WR);
                    }
                }
            }
        }

        @Override
        public SegmentEntryR add(SegmentEntryR other) {
            if (other instanceof WR_R) {
                return other;
            } else {
                RR_R rr = (RR_R) other;
                this.reads = addAll(this.reads, rr.reads);
                return this;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("write: <");
            builder.append(tid);
            builder.append("->");
            builder.append(write);
            builder.append(">, reads: <");
            builder.append(Arrays.toString(reads));
            builder.append(">");
            return builder.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof WR_R) {
                WR_R o = (WR_R) other;
                if (this.write != o.write) {
                    return false;
                }
                if (this.tid != o.tid) {
                    return false;
                }
                if (!Arrays.equals(this.reads, o.reads)) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    static final class RR_R extends SegmentEntryR {

        public int[] reads;

        public RR_R (int epoch, int tid) {
            this.reads = new int[tid + 1];
            this.reads[tid] = epoch;
        }

        @Override
        public void check(SegmentEntryL other) {
            if (other instanceof SegmentEntryL.RW_L) {
                SegmentEntryL.RW_L rw = (SegmentEntryL.RW_L) other;
                // check read-write error
                if (!ensureHappensBefore(reads, rw.write)) {
                    reportError(reads, rw.write, RaceType.RW);
                }
            }
        }

        @Override
        public SegmentEntryR add(SegmentEntryR other) {
            if (other instanceof WR_R) {
                return other;
            } else {
                RR_R rr = (RR_R) other;
                this.reads = addAll(this.reads, rr.reads);
                return this;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("reads: <");
            builder.append(Arrays.toString(reads));
            builder.append(">");
            return builder.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof RR_R) {
                RR_R o = (RR_R) other;
                if (!Arrays.equals(this.reads, o.reads)) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
