package backend.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestSegment {

    Segment acc1;
    Segment acc2;
    Segment acc3;
    Segment acc4;

    @Before
    public void setUp() {
        acc1 = Segment.make(true, new int[]{1}, 0);
        acc2 = Segment.make(false, new int[]{0, 2}, 1);
        acc3 = Segment.make(false, new int[]{0, 0 ,3}, 2);
        acc4 = Segment.make(true, new int[]{0, 0, 0, 4}, 3);
    }

    @Test
    public void testAddWR() {
        acc1.accept(acc2);
        assertEquals(Segment.make(true, new int[]{1}, 0).getPending(), acc1.getPending());
        SegmentEntryR.WR_R wr = (SegmentEntryR.WR_R) (Segment.make(true, new int[]{1}, 0).getCurrent());
        wr.reads = new int[]{0, 2};
        assertEquals(wr, acc1.getCurrent());
    }

    @Test
    public void testAddRW() {
        acc3.accept(acc4);
        assertEquals(Segment.make(true, new int[]{0, 0, 0, 4}, 3).getCurrent(), acc3.getCurrent());
        SegmentEntryL.RW_L rw = (SegmentEntryL.RW_L) (Segment.make(true, new int[]{0, 0, 0, 4}, 3).getPending());
        rw.addRead(new int[]{0, 0 ,3});
        assertEquals(rw, acc3.getPending());
    }

    @Test
    public void testAddRR() {
        acc2.accept(acc3);
        Segment expect = Segment.make(false, new int[]{0, 2}, 1);
        expect.getPending().addRead(new int[]{0, 0, 3});
        SegmentEntryR.RR_R rr = (SegmentEntryR.RR_R) expect.getCurrent();
        rr.reads = new int[]{0, 2, 3};
        assertEquals(expect, acc2);
    }

    @Test
    public void testAddWW() {
        acc1.accept(acc4);
        assertEquals(Segment.make(true, new int[]{1}, 0).getPending(), acc1.getPending());
        assertEquals(Segment.make(true, new int[]{0, 0, 0, 4}, 3).getCurrent(), acc4.getCurrent());
    }

    @Test
    public void raceWR() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc1 = Segment.make(true, new int[]{2}, 0);
        acc2 = Segment.make(false, new int[]{0, 1}, 1);
        try {
            acc1.accept(acc2);
            fail();
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void raceRW() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc1 = Segment.make(true, new int[]{2}, 0);
        acc2 = Segment.make(false, new int[]{0, 1}, 1);
        try {
            acc2.accept(acc1);
            fail();
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void raceWW() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc1 = Segment.make(true, new int[]{0, 1, 3}, 2);
        acc2 = Segment.make(true, new int[]{2, 2, 1}, 1);
        try {
            acc1.accept(acc2);
            fail();
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void raceRR() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc1 = Segment.make(false, new int[]{0, 1, 3}, 2);
        acc2 = Segment.make(false, new int[]{2, 2, 1}, 1);
        try {
            acc1.accept(acc2);
        } catch (IllegalStateException e) {
            fail();
        }
    }

    @Test
    public void happyWR() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc1 = Segment.make(true, new int[]{0, 1}, 1);
        acc2 = Segment.make(false, new int[]{0, 2}, 1);
        try {
            acc1.accept(acc2);
        } catch (IllegalStateException e) {
            fail();
        }

        acc1 = Segment.make(true, new int[]{0, 1, 2}, 1);
        acc2 = Segment.make(false, new int[]{0, 2, 3}, 2);
        try {
            acc1.accept(acc2);
        } catch (IllegalStateException e) {
            fail();
        }
    }

    @Test
    public void happyRW() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc3 = Segment.make(false, new int[]{0, 0 ,3, 4}, 3);
        acc4 = Segment.make(true, new int[]{0, 0, 5, 4}, 2);
        try {
            acc3.accept(acc4);
        } catch (IllegalStateException e) {
            fail();
        }
    }

    @Test
    public void happyWW() {
        SegmentEntryR.THROW_EXCEPTION = true;
        acc1 = Segment.make(true, new int[]{0, 1}, 1);
        acc2 = Segment.make(true, new int[]{0, 2}, 1);
        try {
            acc1.accept(acc2);
        } catch (IllegalStateException e) {
            fail();
        }

        acc1 = Segment.make(true, new int[]{0, 1, 2}, 1);
        acc2 = Segment.make(true, new int[]{0, 2, 3}, 2);
        try {
            acc1.accept(acc2);
        } catch (IllegalStateException e) {
            fail();
        }
    }
}
