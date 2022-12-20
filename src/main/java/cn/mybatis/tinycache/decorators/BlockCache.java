
package cn.mybatis.tinycache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import cn.mybatis.tinycache.core.Cache;
import cn.mybatis.tinycache.exceptions.CacheException;

/**
 * BlockCache �������汾�Ļ�����������ֻ֤��һ���̵߳����ݿ����ָ��key��Ӧ�����ݡ�
  * �����߳�A��BlockCache��δ���ҵ�key��Ӧ�Ļ�����߳�A���ȡkey��Ӧ���������������߳��ڲ���key�ǻᷢ��������
  * Ȼ���߳�A�����ݿ��в��ҵ�key��Ӧ�Ľ��������������뵽BlockCache�У���ʱ�߳�A���ͷ�key��Ӧ���������������ڸ����ϵ������̣߳�
  * �����߳̿��Դӻ����л�ȡ���ݣ��������ٴη������ݿ⡣
 *
 */
public class BlockCache implements Cache
{

	private long timeout;
	private final Cache kernelCache;
	private final ConcurrentHashMap<Object, ReentrantLock> locks;

	public BlockCache(Cache cache)
	{
		this.kernelCache = cache;
		this.locks = new ConcurrentHashMap<Object, ReentrantLock>();
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

	@Override
	public void putObject(Object key, Object value)
	{
		try
		{
			kernelCache.putObject(key, value);
		}
		finally
		{
			this.releaseLock(key);
		}
	}

	@Override
	public Object getObject(Object key)
	{
		this.acquireLock(key);
		Object value = kernelCache.getObject(key);
		if (value != null)
		{
			this.releaseLock(key);
		}
		return value;
	}

	@Override
	public Object removeObject(Object key)
	{
		kernelCache.removeObject(key);
		this.releaseLock(key);
		return null;
	}

	@Override
	public void clear()
	{
		kernelCache.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock()
	{
		return null;
	}

	/**
	 * 
	 * put��putIfAbsent����:
	 * put�ڷ�������ʱ������������ݵ�key�Ѿ�������Map�У�����������ݻḲ��֮ǰ���ڵ����ݣ�
	  *  ��putIfAbsent�ڷ�������ʱ���������key����ôputIfAbsent�������ֵ��ֻ���ش��ڵ�value����������ڣ������key��value������null	
	 *
	 */
	private ReentrantLock getLockForKey(Object key)
	{
		ReentrantLock lock = new ReentrantLock();
		ReentrantLock previous = locks.putIfAbsent(key, lock);
		return previous == null ? lock : previous;
	}

	private void acquireLock(Object key)
	{
		Lock lock = this.getLockForKey(key);
		if (timeout > 0)
		{
			try
			{
				/**
				 * ��ReentrantLock �У�lock()������һ����������������synchronize��˼��࣬
				 * ������һ������ tryLock()����ֻ���ڳɹ���ȡ����������²Ż᷵��true���������̵߳�ǰ���������������������false��
				 * ���Ϊ�����������timeout����������ڵȴ�timeout��ʱ��Ż᷵��false�����ڻ�ȡ������ʱ�򷵻�true��
				 */
				boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
				if (!acquired)
				{
					throw new CacheException("Couldn't get a lock in " + timeout + " for the key " + key
							+ " at the cache " + kernelCache.getId());
				}
			}
			catch (InterruptedException e)
			{//�涨ʱ����û�л�ȡ�������򱻴�ϣ��˴����쳣���źŻ��ƣ������޺����źŻ��ƣ����ǳ�������
				throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
			}
		}
		else
		{
			lock.lock();
		}
	}

	private void releaseLock(Object key)
	{
		ReentrantLock lock = locks.get(key);
		if (lock.isHeldByCurrentThread())
		{
			lock.unlock();
		}
	}

	public long getTimeout()
	{
		return timeout;
	}

	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}
}