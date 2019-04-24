package backend.serialize;

import backend.util.IntervalTree;
import backend.util.Segment;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class IntervalTreeSerializer implements Serializer<IntervalTree<Segment>> {


    @Override
    public IntervalTree<Segment> readObject(ByteBuffer buffer, int len) {
        return null;
    }

    @Override
    public ByteBuffer writeObject(IntervalTree<Segment> obj) {
        return null;
    }
}
