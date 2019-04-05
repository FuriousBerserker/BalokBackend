package backend.util;

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

    private static void reportError(int epoch, int[] vc, RaceType type) {
        if (THROW_EXCEPTION) {
            throw new IllegalStateException();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString());
        builder.append('\n');
        builder.append("access 1: ");
        builder.append(Epoch.tid(epoch));
        builder.append("->");
        builder.append(Epoch.clock(epoch));
        builder.append('\n');
        builder.append("access 2: ");
        builder.append(Arrays.toString(Epoch.toClock(vc)));
        //System.out.println(builder.toString());
    }

    private static void reportError(int[] vc1, int[] vc2, RaceType type) {
        if (THROW_EXCEPTION) {
            throw new IllegalStateException();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString());
        builder.append('\n');
        builder.append("access 1: ");
        builder.append(Arrays.toString(Epoch.toClock(vc1)));
        builder.append('\n');
        builder.append("access 2: ");
        builder.append(Arrays.toString(Epoch.toClock(vc2)));
        //System.out.println(builder.toString());
    }

    public static SegmentEntryR make(boolean isWrite, int epoch) {
        if (isWrite) {
            return new WR_R(epoch);
        } else {
            return new RR_R(epoch);
        }
    }

    // Check epoch <= vc
    private static boolean ensureHappensBefore(int epoch, int[] vc) {
        int tid = Epoch.tid(epoch);
        if (tid >= vc.length || !Epoch.leq(epoch, vc[tid])) {
            return false;
        } else {
            return true;
        }
    }

    // Check vc1 <= vc2
    private static boolean ensureHappensBefore(int[] vc1, int[] vc2) {
        int minLen = Math.min(vc1.length, vc2.length);

        for (int i = 0; i < minLen; i++) {
            if (!Epoch.leq(vc1[i], vc2[i])) {
                return false;
            }
        }

        for (int i = minLen; i < vc1.length; i++) {
            if (vc1[i] != Epoch.make(i, 0)) {
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
            result[i] = Epoch.make(i, Math.max(Epoch.clock(other[i]), Epoch.clock(result[i])));
        }
        return result;
    }

    public abstract void check(SegmentEntryL other);

    public abstract SegmentEntryR add(SegmentEntryR other);

    static final class WR_R extends SegmentEntryR {

        public int write;

        public int[] reads;

        private final static int[] EMPTY = new int[0];

        public WR_R(int epoch) {
            this.write = epoch;
            this.reads = EMPTY;
        }

        @Override
        public void check(SegmentEntryL other) {
            if (other instanceof SegmentEntryL.RW_L) {
                // check write-read race
                SegmentEntryL.RW_L rw = (SegmentEntryL.RW_L) other;
                for (int[] rw_read : rw.reads) {
                    if (!ensureHappensBefore(write, rw_read)) {
                        reportError(write, rw_read, RaceType.WR);
                    }
                }
                // check write-write race
                if (!ensureHappensBefore(write, rw.write)) {
                    reportError(write, rw.write, RaceType.WW);
                }
                // check read-write race
                if (!ensureHappensBefore(reads, rw.write)) {
                    reportError(reads, rw.write, RaceType.RW);
                }
            } else {
                SegmentEntryL.RR_L rr = (SegmentEntryL.RR_L) other;
                // check write-read race
                for (int[] rr_read : rr.reads) {
                    if (!ensureHappensBefore(write, rr_read)) {
                        reportError(write, rr_read, RaceType.WR);
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
            builder.append(Epoch.tid(write));
            builder.append("->");
            builder.append(Epoch.clock(write));
            builder.append(">, reads: <");
            builder.append(Arrays.toString(Epoch.toClock(reads)));
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

        public RR_R (int epoch) {
            int tid = Epoch.tid(epoch);
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
            builder.append(Arrays.toString(Epoch.toClock(reads)));
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
