package backend.util;

import java.util.Arrays;

public abstract class SegmentEntryR {

    private SegmentEntryR() {}

    public static boolean THROW_EXCEPTION = false;

    private static void reportError() {
        if (THROW_EXCEPTION) {
            throw new IllegalStateException();
        }
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
        if (tid >= vc.length || vc[tid] < epoch) {
            return false;
        } else {
            return true;
        }
    }

    // Check vc1 <= vc2
    private static boolean ensureHappensBefore(int[] vc1, int[] vc2) {
        int minLen = Math.min(vc1.length, vc2.length);

        for (int i = 0; i < minLen; i++) {
            if (vc1[i] > vc2[i]) {
                return false;
            }
        }

        for (int i = minLen; i < vc1.length; i++) {
            if (vc1[i] != 0) {
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
            result[i] = Math.max(other[i], result[i]);
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
                        reportError();
                    }
                }
                // check write-write race
                if (!ensureHappensBefore(tid, write, rw.write)) {
                    reportError();
                }

                // check read-write race
                if (!ensureHappensBefore(reads, rw.write)) {
                    reportError();
                }


            } else {
                SegmentEntryL.RR_L rr = (SegmentEntryL.RR_L) other;
                for (int[] rr_read : rr.reads) {
                    if (!ensureHappensBefore(tid, write, rr_read)) {
                        reportError();
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
                    reportError();
                }
            } else {

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
