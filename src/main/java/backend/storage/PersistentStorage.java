package backend.storage;

public interface PersistentStorage<K, V> {

    public void put(K key, V value);

    public V get(K key);
}
