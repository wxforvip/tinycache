
package cn.mybatis.tinycache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

public class FifoCache implements Cache
{

	private final Cache kernelCache;
	private final Deque<Object> keyList;
	private int size;

	public FifoCache(Cache cache)
	{
		this.kernelCache = cache;
		this.keyList = new LinkedList<Object>();
		this.size = 1024;
	}

	@Override
	public String getId()
	{
		return kernelCache.getId();
	}

	@Override
	public int getSize()
	{
		return kernelCache.getSize();
	}

	public void setSize(int size)
	{
		this.size = size;
	}

	@Override
	public void putObject(Object key, Object value)
	{
		this.cycleKeyList(key);
		kernelCache.putObject(key, value);
	}

	@Override
	public Object getObject(Object key)
	{
		return kernelCache.getObject(key);
	}

	@Override
	public Object removeObject(Object key)
	{
		keyList.remove(key);
		return kernelCache.removeObject(key);
	}

	@Override
	public void clear()
	{
		kernelCache.clear();
		keyList.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock()
	{
		return null;
	}

	private void cycleKeyList(Object key)
	{
		keyList.addLast(key);
		if (keyList.size() > size)
		{
			Object oldestKey = keyList.removeFirst();
			kernelCache.removeObject(oldestKey);
		}
	}

}
