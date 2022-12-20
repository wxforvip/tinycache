
package cn.mybatis.tinycache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

public class WeakCache implements Cache
{
	private final ReferenceQueue<Object> garbageCollectedQueue;
	private final Cache kernelCache;

	public WeakCache(Cache cache)
	{
		this.kernelCache = cache;
		this.garbageCollectedQueue = new ReferenceQueue<Object>();
	}

	@Override
	public String getId()
	{
		return kernelCache.getId();
	}

	@Override
	public int getSize()
	{
		removeGCQueueItems();
		return kernelCache.getSize();
	}

	@Override
	public void putObject(Object key, Object value)
	{
		removeGCQueueItems();
		kernelCache.putObject(key, new WeakCacheEntry(key, value, garbageCollectedQueue));
	}

	@Override
	public Object getObject(Object key)
	{
		Object result = null;
		@SuppressWarnings("unchecked")
		WeakReference<Object> weakReference = (WeakReference<Object>) kernelCache.getObject(key);
		if (weakReference != null)
		{
			result = weakReference.get();
			if (result == null)
			{
				kernelCache.removeObject(key);
			}
		}
		return result;
	}

	@Override
	public Object removeObject(Object key)
	{
		removeGCQueueItems();
		return kernelCache.removeObject(key);
	}

	@Override
	public void clear()
	{
		removeGCQueueItems();
		kernelCache.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock()
	{
		return null;
	}

	private void removeGCQueueItems()
	{
		WeakCacheEntry weakCacheEntry;
		while ((weakCacheEntry = (WeakCacheEntry) garbageCollectedQueue.poll()) != null)
		{
			kernelCache.removeObject(weakCacheEntry.key);
		}
	}

	private static class WeakCacheEntry extends WeakReference<Object>
	{
		private final Object key;

		private WeakCacheEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue)
		{
			super(value, garbageCollectionQueue);
			this.key = key;
		}
	}

}
