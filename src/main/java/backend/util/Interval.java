package backend.util;

/**
 * Defines a half-open <a href="https://en.wikipedia.org/wiki/Interval_(mathematics)">interval</a> (that is left-closed and right-open).
 */
public final class Interval implements Comparable<Interval> {
    public enum IntervalMatch {
        BEHIND {
            @Override
            public int compareResult() {
                return 1;
            }
        },
        AHEAD {
            @Override
            public int compareResult() {
                return -1;
            }
        },
        CONNECT_BEHIND {
            @Override
            public int compareResult() {
                return 0;
            }
        },
        CONNECT_AHEAD {
            @Override
            public int compareResult() {
                return 0;
            }
        };
        public abstract int compareResult();
    }

    private int lowEndpoint;
    private int highEndpoint;

    public Interval() {
        this(0);
    }

    public Interval(int value) {
        this(value, value + 1);
    }

    public Interval(int lowEndpoint, int highEndpoint) {
        if (lowEndpoint >= highEndpoint) {
            throw new IllegalArgumentException(lowEndpoint + " >= " + highEndpoint);
        }
        this.lowEndpoint = lowEndpoint;
        this.highEndpoint = highEndpoint;
    }

    /**
     * Inclusive start of the range.
     * @return
     */
    public int getLowEndpoint() {
        return lowEndpoint;
    }

    /**
     * Exclusive start of the range.
     * @return
     */
    public int getHighEndpoint() {
        return highEndpoint;
    }

    public boolean succeeds(Interval other) {
        return other.highEndpoint == this.lowEndpoint;
    }

    public IntervalMatch match(Interval other) {
        if (succeeds(other)) {
            return IntervalMatch.CONNECT_BEHIND;
        } else if (other.succeeds(this)) {
            return IntervalMatch.CONNECT_AHEAD;
        } else if (other.highEndpoint < this.lowEndpoint) {
            return IntervalMatch.BEHIND;
        } else if (other.lowEndpoint > this.highEndpoint) {
            return IntervalMatch.AHEAD;
        }
        throw new IllegalStateException(this + " MATCH " + other);
    }

    @Override
    public int compareTo(Interval o) {
        if (equals(o)) { return 0; }
        return match(o).compareResult();
    }

    public void add(Interval other) {
        if (this.succeeds(other)) {
            this.lowEndpoint = other.lowEndpoint;
        } else if (other.succeeds(this)) {
            this.highEndpoint = other.highEndpoint;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || ! (obj instanceof Interval)) {
            return false;
        }
        Interval other = (Interval) obj;
        return this.getLowEndpoint() == other.getLowEndpoint() &&
                this.getHighEndpoint() == other.getHighEndpoint();
    }

    @Override
    public String toString() {
        return String.format("[%d,%d)", getLowEndpoint() , getHighEndpoint());
    }
}
