
package cn.mybatis.tinycache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

public class SoftCache implements Cache
{

	/**
	 * ReferenceQueue名义上是一个队列，但实际内部并非有实际的存储结构，它的存储是依赖于内部节点之间的关系来表达。
	 * ReferenceQueue本质是一个链表，其仅存储当前的head节点，而后面的节点关系由每个Reference节点自身通过next来保持。
	 */
	private final ReferenceQueue<Object> garbageCollectedQueue;
	private final Cache kernelCache;

	public SoftCache(Cache cache)
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
		kernelCache.putObject(key, new SoftCacheEntry(key, value, garbageCollectedQueue));
	}

	@Override
	public Object getObject(Object key)
	{
		Object result = null;
		@SuppressWarnings("unchecked")
		SoftReference<Object> softReference = (SoftReference<Object>) kernelCache.getObject(key);
		if (softReference != null)
		{
			result = softReference.get();
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
		SoftCacheEntry softCacheEntry;
		while ((softCacheEntry = (SoftCacheEntry) garbageCollectedQueue.poll()) != null)
		{
			kernelCache.removeObject(softCacheEntry.key);
		}
	}

	/**
	 * SoftReference其内部提供2个构造函数，一个带Queue，一个不带Queue。
	  *  其中Queue的意义在于，我们可以在外部对这个Queue进行监控，如果有对象即将被回收，那么相应的SoftReference对象就会被放到这个Queue里，
	  *  我们拿到SoftReference，就可以再作一些处理。
	 *
	 */
	private static class SoftCacheEntry extends SoftReference<Object>
	{
		private final Object key;

		SoftCacheEntry(Object key, Object value, ReferenceQueue<Object> queue)
		{
			super(value, queue);
			this.key = key;
		}
	}

}