package com.vivosys.test.persistence.ops;

import java.util.Collection;
import java.util.Map;

/**
 * A simple key-value store.
 */
public interface KeyValueStore<K, V> {

    /**
     * Stores the key-value pair.
     *
     * @param key   key of the entry to store
     * @param value value of the entry to store
     */
    void put(K key, V value);

    /**
     * Stores multiple entries. Implementation of this method can optimize the
     * store operation by storing all entries in one database connection for instance.
     *
     * @param map map of entries to store
     */
    void putAll(Map<K, V> map);

    /**
     * Gets the value with the given key.
     *
     * @param key the key to retrieve.
     */
    V get(K key);

    /**
     * Deletes the entry with a given key from the store.
     *
     * @param key key to delete from the store.
     */
    void remove(K key);

    /**
     * Deletes multiple entries from the store.
     *
     * @param keys keys of the entries to delete.
     */
    void removeAll(Collection<K> keys);

}
