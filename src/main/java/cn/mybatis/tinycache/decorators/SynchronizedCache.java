
package cn.mybatis.tinycache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

public class SynchronizedCache implements Cache
{

	private final Cache kernelCache;

	public SynchronizedCache(Cache cache)
	{
		this.kernelCache = cache;
	}

	@Override
	public String getId()
	{
		return kernelCache.getId();
	}

	@Override
	public synchronized int getSize()
	{
		return kernelCache.getSize();
	}

	@Override
	public synchronized void putObject(Object key, Object object)
	{
		kernelCache.putObject(key, object);
	}

	@Override
	public synchronized Object getObject(Object key)
	{
		return kernelCache.getObject(key);
	}

	@Override
	public synchronized Object removeObject(Object key)
	{
		return kernelCache.removeObject(key);
	}

	@Override
	public synchronized void clear()
	{
		kernelCache.clear();
	}

	@Override
	public int hashCode()
	{
		return kernelCache.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return kernelCache.equals(obj);
	}

	@Override
	public ReadWriteLock getReadWriteLock()
	{
		return null;
	}

}
