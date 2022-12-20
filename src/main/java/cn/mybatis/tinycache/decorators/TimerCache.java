
package cn.mybatis.tinycache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

public class TimerCache implements Cache
{

	private final Cache kernelCache;
	protected long clearInterval;
	protected long lastClearTime;

	public TimerCache(Cache cache)
	{
		this.kernelCache = cache;
		this.clearInterval = 60 * 60 * 1000;
		this.lastClearTime = System.currentTimeMillis();
	}

	public void setClearInterval(long clearInterval)
	{
		this.clearInterval = clearInterval;
	}

	@Override
	public String getId()
	{
		return kernelCache.getId();
	}

	@Override
	public int getSize()
	{
		clearCache();
		return kernelCache.getSize();
	}

	@Override
	public void putObject(Object key, Object object)
	{
		clearCache();
		kernelCache.putObject(key, object);
	}

	@Override
	public Object getObject(Object key)
	{
		return clearCache() ? null : kernelCache.getObject(key);
	}

	@Override
	public Object removeObject(Object key)
	{
		clearCache();
		return kernelCache.removeObject(key);
	}

	@Override
	public void clear()
	{
		lastClearTime = System.currentTimeMillis();
		kernelCache.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock()
	{
		return null;
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

	private boolean clearCache()
	{
		if (System.currentTimeMillis() - lastClearTime > clearInterval)
		{
			clear();
			return true;
		}
		return false;
	}

}
