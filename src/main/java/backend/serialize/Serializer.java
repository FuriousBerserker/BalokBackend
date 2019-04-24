package backend.serialize;

import java.nio.ByteBuffer;

public interface Serializer<T> {

    public T readObject(ByteBuffer buffer, int len);

    public ByteBuffer writeObject(T obj);

}
