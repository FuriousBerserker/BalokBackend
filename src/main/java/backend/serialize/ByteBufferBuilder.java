package backend.serialize;

import java.nio.ByteBuffer;

public class ByteBufferBuilder {

    private static final int defaultCapacity = 100;

    private ByteBuffer buf;

    public ByteBufferBuilder(int initialCapacity) {
        this.buf = ByteBuffer.allocateDirect(initialCapacity);
    }

    public ByteBufferBuilder() {
        this(defaultCapacity);
    }

    public void clear() {
        this.buf = ByteBuffer.allocateDirect(buf.capacity());
    }

    public void ensureCapacity(int capacity) {
        if (buf.capacity() < capacity) {
            // allocate more to avoid frequent reallocation
            ByteBuffer newBuf = ByteBuffer.allocateDirect(capacity * 2);
            newBuf.put(buf);
        }
    }
}
