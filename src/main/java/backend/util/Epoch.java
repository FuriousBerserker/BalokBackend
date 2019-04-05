package backend.util;

import java.util.Arrays;

public class Epoch {

    public static int TID_BITS = 5;

    public static final int CLOCK_BITS = Integer.SIZE - TID_BITS;

    public static final int MAX_CLOCK = ( ((int)1) << CLOCK_BITS) - 1;

    public static int tid(int epoch) {
        return (int)(epoch >>> CLOCK_BITS);
    }

    public static int clock(int epoch) {
        return epoch & MAX_CLOCK;
    }

    public static boolean leq(int/*epoch*/ e1, int/*epoch*/ e2) {
        // Assert.assertTrue(tid(e1) == tid(e2));
        return clock(e1) <= clock(e2);
    }

    public static int/*epoch*/ make(int tid, int clock) {
        return (((int)tid) << CLOCK_BITS) | clock;
    }

    public static int[] toClock(int[] vc) {
        int[] clocks = new int[vc.length];
        for (int i = 0; i < vc.length; i++) {
            clocks[i] = Epoch.clock(vc[i]);
        }
        return clocks;
    }
}
