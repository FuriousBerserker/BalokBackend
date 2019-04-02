package backend.util;

import com.google.common.collect.Collections2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class TestIntervalTree {
    private static class I implements Consumer<I> {
        public I(int value) {
            this.value = value;
        }

        int value;
        @Override
        public void accept(I other) {
            this.value = Math.max(this.value, other.value);
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            I i = (I) o;
            return value == i.value;
        }

        @Override
        public int hashCode() {

            return Objects.hash(value);
        }
    }

    @Test
    public void constructor() {
        I i10 = new I(10);
        IntervalTree<I> rng1 = new IntervalTree<>(i10, new Interval(1));
        assertSame(i10, rng1.getValue());
        assertSame(1, rng1.getInterval().getLowEndpoint());
        assertSame(2, rng1.getInterval().getHighEndpoint());
        assertNull(rng1.getBiggerRange());
        assertNull(rng1.getSmallerRange());
    }

    @Test
    public void happyPath() {
        IntervalTree<I> rng1 = new IntervalTree<>(new I(10), new Interval(1));
        IntervalTree<I> rng2 = new IntervalTree<>(new I(20), new Interval(2));
        rng1.add(rng2);
        assertEquals(20, rng1.getValue().value);
        assertEquals(new Interval(1, 3), rng1.getInterval());
        assertNull(rng1.getBiggerRange());
        assertNull(rng1.getSmallerRange());
    }

    private static void runPerms(int count, Consumer<IntervalTree[]> cb) {
        ArrayList<Integer> elems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elems.add(i);
        }
        // Try all the permutations of adding the tree events
        for (List<Integer> perm : Collections2.orderedPermutations(elems)) {
            IntervalTree[] ranges = new IntervalTree[count];
            for (Integer i : perm) {
                ranges[i] = new IntervalTree<>(new I((i + 1)*10), new Interval(i + 1));
            }
            cb.accept(ranges);
        }
    }

    @Test
    public void add4() {
        runPerms(4, rng -> {
            // Two groups of intervals connecting with each other:
            rng[0].add(rng[1]);
            rng[2].add(rng[3]);
            rng[0].add(rng[2]);
            assertEquals(rng[0].toString(), new IntervalTree(new I(40), new Interval(1, 5)), rng[0]);
            assertNull(rng[0].getSmallerRange());
            assertNull(rng[0].getBiggerRange());
        });
    }
    @Test
    public void addN() {
        // merge all elements into one, check all possible permutations
        for (int i = 1; i < 7; i++) {
            final int count = i;
            runPerms(count, rng -> {
                IntervalTree r = rng[0];
                for (int j = 1; j < count; j++) {
                    r.add(rng[j]);
                }
                assertEquals(rng[0].toString(), new IntervalTree(new I(count * 10), new Interval(1, count + 1)), rng[0]);
                assertNull(rng[0].getSmallerRange());
                assertNull(rng[0].getBiggerRange());
            });
        }
    }

    @Test
    public void addConnectBehind() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> left = new IntervalTree<>(new I(-1), new Interval(-1));
        root.add(left);
        assertEquals(new IntervalTree<>(new I(0), new Interval(-1, 1)), root);
        assertNull(root.getSmallerRange());
        assertNull(root.getBiggerRange());
    }

    @Test
    public void addConnectAhead() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> left = new IntervalTree<>(new I(-1), new Interval(-1));
        left.add(root);
        assertEquals(new IntervalTree<>(new I(0), new Interval(-1, 1)), left);
        assertNull(left.getSmallerRange());
        assertNull(left.getBiggerRange());
    }

    @Test
    public void addBehind() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> left = new IntervalTree<>(new I(-2), new Interval(-2));
        root.add(left);
        assertSame(left, root.getSmallerRange());
        assertNull(root.getBiggerRange());
    }

    @Test
    public void addAhead() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> right = new IntervalTree<>(new I(2), new Interval(2));
        root.add(right);
        assertSame(right, root.getBiggerRange());
        assertNull(root.getSmallerRange());
    }

    @Test
    public void testRemove() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> left = new IntervalTree<>(new I(-2), new Interval(-2));
        IntervalTree<I> right = new IntervalTree<>(new I(4), new Interval(4));
        IntervalTree<I> rightRight = new IntervalTree<>(new I(8), new Interval(8));
        IntervalTree<I> rightLeft = new IntervalTree<>(new I(2), new Interval(2));
        right.add(rightLeft);
        right.add(rightRight);
        root.add(left);
        root.add(right);
        right.removeChild(root, rightLeft);
        assertNull(root.getBiggerRange().getSmallerRange());
        right.removeChild(root, rightRight);
        assertNull(root.getBiggerRange().getBiggerRange());
        root.add(rightLeft);
        root.add(rightRight);
        IntervalTree<I> newRoot = new IntervalTree<>(new I(-3), new Interval(-3));
        IntervalTree<I> newLeft = new IntervalTree<>(new I(-5), new Interval(-5));
        newRoot.add(root);
        newRoot.add(newLeft);
        root.removeChild(newRoot, right);

        IntervalTree<I> newRoot2 = new IntervalTree<>(new I(-3), new Interval(-3));
        IntervalTree<I> newLeft2 = new IntervalTree<>(new I(-5), new Interval(-5));
        IntervalTree<I> root2 = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> left2 = new IntervalTree<>(new I(-2), new Interval(-2));
        IntervalTree<I> rightRight2 = new IntervalTree<>(new I(8), new Interval(8));
        IntervalTree<I> rightLeft2 = new IntervalTree<>(new I(2), new Interval(2));
        root2.add(left2);
        rightRight2.add(rightLeft2);
        root2.add(rightRight2);
        newRoot2.add(newLeft2);
        newRoot2.add(root2);
        assertEquals(newRoot2, newRoot);

    }

    @Test
    public void testUpdateLowChild() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> right = new IntervalTree<>(new I(4), new Interval(4));
        IntervalTree<I> left = new IntervalTree<>(new I(-4), new Interval(-4));
        IntervalTree<I> leftRight = new IntervalTree<>(new I(-1), new Interval(-1));
        IntervalTree<I> leftLeft = new IntervalTree<>(new I(-8), new Interval(-8));
        left.add(leftLeft);
        left.add(leftRight);
        root.add(left);
        root.add(right);
        root.updateLowChild();
        assertEquals(new Interval(-1, 1), root.getInterval());
        assertEquals(new I(0), root.getValue());
        assertNull(root.getSmallerRange().getBiggerRange());

    }

    @Test
    public void testUpdateHighChild() {
        IntervalTree<I> root = new IntervalTree<>(new I(0), new Interval(0));
        IntervalTree<I> left = new IntervalTree<>(new I(-2), new Interval(-2));
        IntervalTree<I> right = new IntervalTree<>(new I(4), new Interval(4));
        IntervalTree<I> rightRight = new IntervalTree<>(new I(8), new Interval(8));
        IntervalTree<I> rightLeft = new IntervalTree<>(new I(1), new Interval(1));
        right.add(rightLeft);
        right.add(rightRight);
        root.add(left);
        root.add(right);
        root.updateHighChild();
        assertEquals(new Interval(0, 2), root.getInterval());
        assertEquals(new I(1), root.getValue());
        assertNull(root.getBiggerRange().getSmallerRange());
    }

}
