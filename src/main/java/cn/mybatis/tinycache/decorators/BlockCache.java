
package cn.mybatis.tinycache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import cn.mybatis.tinycache.core.Cache;
import cn.mybatis.tinycache.exceptions.CacheException;

/**
 * BlockCache 是阻塞版本的缓存器，它保证只有一个线程到数据库查找指定key对应的数据。
  * 假如线程A在BlockCache中未查找到key对应的缓存项，线程A会获取key对应的锁，这样后续线程在查找key是会发生阻塞。
  * 然后，线程A从数据库中查找到key对应的结果，并将结果放入到BlockCache中，此时线程A会释放key对应的锁，唤醒阻塞在该锁上的其他线程，
  * 其它线程可以从缓存中获取数据，而不是再次访问数据库。
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
	 * put与putIfAbsent区别:
	 * put在放入数据时，如果放入数据的key已经存在与Map中，最后放入的数据会覆盖之前存在的数据，
	  *  而putIfAbsent在放入数据时，如果存在key，那么putIfAbsent不会放入值，只返回存在的value，如果不存在，就添加key和value，返回null	
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
				 * 在ReentrantLock 中，lock()方法是一个无条件的锁，与synchronize意思差不多，
				 * 但是另一个方法 tryLock()方法只有在成功获取了锁的情况下才会返回true，如果别的线程当前正持有锁，则会立即返回false！
				 * 如果为这个方法加上timeout参数，则会在等待timeout的时间才会返回false或者在获取到锁的时候返回true。
				 */
				boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
				if (!acquired)
				{
					throw new CacheException("Couldn't get a lock in " + timeout + " for the key " + key
							+ " at the cache " + kernelCache.getId());
				}
			}
			catch (InterruptedException e)
			{//规定时间内没有获取到锁，则被打断，此处的异常是信号机制，人畜无害的信号机制，不是出现问题
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