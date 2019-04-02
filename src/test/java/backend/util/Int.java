package backend.util;

public class Int implements Comparable<Int> {

    final int i;

    public Int(int i) {
        this.i = i;
    }


    @Override
    public int compareTo(Int other) {
        return Integer.compare(this.i, other.i);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof Int) {
            Int other = (Int) obj;
            return this.i == other.i;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return i;
    }

    @Override
    public String toString() {
        return Integer.toString(i);
    }
}
