package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LRUCacheTest {

    LRUCache<Integer, Integer> cache = new LRUCache<>(4);

    @Test
    public void check_LRU_only_by_insert() {
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.put(4, 4);

        assertNull(cache.get(1));
        assertNotNull(cache.get(2));
        assertNotNull(cache.get(3));
        assertNotNull(cache.get(4));
    }

    @Test
    public void check_LRU_by_use() {
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.get(1);
        cache.put(4, 4);

        assertNull(cache.get(2));
        assertNotNull(cache.get(1));
        assertNotNull(cache.get(3));
        assertNotNull(cache.get(4));
    }
}
