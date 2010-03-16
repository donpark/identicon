package com.docuverse.identicon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheException;
import com.whirlycott.cache.CacheManager;

public class MemoryIdenticonCache implements IdenticonCache {
	private static final Log log = LogFactory
			.getLog(MemoryIdenticonCache.class);

	private Cache cache;

	public MemoryIdenticonCache() {
		try {
			cache = CacheManager.getInstance().getCache("identicon");
		} catch (CacheException e) {
			log.error(e);
		}
	}

	public byte[] get(String key) {
		if (cache != null)
			return (byte[]) cache.retrieve(key);
		else
			return null;
	}

	public void add(String key, byte[] imageData) {
		if (cache != null)
			cache.store(key, imageData);
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
