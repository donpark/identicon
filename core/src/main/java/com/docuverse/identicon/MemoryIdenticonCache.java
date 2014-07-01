package com.docuverse.identicon;

import org.cache2k.Cache;
import org.cache2k.CacheBuilder;

public class MemoryIdenticonCache implements IdenticonCache {

    private Cache<String, byte[]> cache;

    public MemoryIdenticonCache() {
        cache = CacheBuilder.newCache(String.class, byte[].class).build();
    }

    public byte[] get(String key) {
        if (cache != null)
            return cache.peek(key);
        else
            return null;
    }

    public void add(String key, byte[] imageData) {
        if (cache != null)
            cache.put(key, imageData);
    }

    public void remove(String key) {
        if (cache != null)
            cache.remove(key);
    }

    public void removeAll() {
        if (cache != null)
            cache.clear();
    }

}
