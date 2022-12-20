
package cn.mybatis.tinycache.decorators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.classloader.ClassLoaderHelper;
import cn.mybatis.tinycache.core.Cache;
import cn.mybatis.tinycache.exceptions.CacheException;

/**
 * ByteCache存储的都是序列化后的字节，对应于对象的句柄存储：SoftCache，WeakCache
 * 
 * @author www.mybatis.cn
 *
 */
public class ByteCache implements Cache
{

	private final Cache kernelCache;

	private static ClassLoaderHelper classLoaderHelper = new ClassLoaderHelper();

	public ByteCache(Cache cache)
	{
		this.kernelCache = cache;
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
	public void putObject(Object key, Object object)
	{
		if (object == null || object instanceof Serializable)
		{
			byte[] bytes = this.serialize((Serializable) object);
			kernelCache.putObject(key, bytes);
		}
		else
		{
			throw new CacheException("SharedCache failed to make a copy of a non-serializable object: " + object);
		}
	}

	@Override
	public Object getObject(Object key)
	{
		Object object = kernelCache.getObject(key);
		return object == null ? null : this.deserialize((byte[]) object);
	}

	@Override
	public Object removeObject(Object key)
	{
		return kernelCache.removeObject(key);
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

	private byte[] serialize(Serializable value)
	{
		try
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(value);
			oos.flush();
			oos.close();
			return bos.toByteArray();
		}
		catch (Exception e)
		{
			throw new CacheException("Error serializing object.  Cause: " + e, e);
		}
	}

	private Serializable deserialize(byte[] value)
	{
		Serializable result;
		try
		{
			ByteArrayInputStream bis = new ByteArrayInputStream(value);
			ObjectInputStream ois = new CustomObjectInputStream(bis);
			result = (Serializable) ois.readObject();
			ois.close();
		}
		catch (Exception e)
		{
			throw new CacheException("Error deserializing object.  Cause: " + e, e);
		}
		return result;
	}

	public static class CustomObjectInputStream extends ObjectInputStream
	{

		public CustomObjectInputStream(InputStream in) throws IOException
		{
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
		{
			return classLoaderHelper.loadClassByName(desc.getName());
		}

	}

}
