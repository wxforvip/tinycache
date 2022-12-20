
package cn.mybatis.tinycache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

public class SoftCache implements Cache
{

	/**
	 * ReferenceQueue��������һ�����У���ʵ���ڲ�������ʵ�ʵĴ洢�ṹ�����Ĵ洢���������ڲ��ڵ�֮��Ĺ�ϵ����
	 * ReferenceQueue������һ����������洢��ǰ��head�ڵ㣬������Ľڵ��ϵ��ÿ��Reference�ڵ�����ͨ��next�����֡�
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
	 * SoftReference���ڲ��ṩ2�����캯����һ����Queue��һ������Queue��
	  *  ����Queue���������ڣ����ǿ������ⲿ�����Queue���м�أ�����ж��󼴽������գ���ô��Ӧ��SoftReference����ͻᱻ�ŵ����Queue�
	  *  �����õ�SoftReference���Ϳ�������һЩ����
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