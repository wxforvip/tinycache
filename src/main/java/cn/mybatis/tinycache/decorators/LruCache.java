
package cn.mybatis.tinycache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import cn.mybatis.tinycache.core.Cache;

/**
 * LinkedHashMap�洢����������ģ���Ϊ���֣�����˳��ͷ���˳��
 * 
 * @author www.mybatis.cn
 *
 */
public class LruCache implements Cache
{
	private final Cache kernelCache;
	private Map<Object, Object> keyMap;
	private Object eldestKey;

	public LruCache(Cache cache)
	{
		this.kernelCache = cache;
		this.setSize(1024);
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

	public void setSize(final int size)
	{
		keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest)
			{
				/**
				  * ������������remove���������ж϶��ѣ��ж�ʵ����putʱ��this.putObject->this.cycleKeyList->this.keyMap.put
				  *  ��������remove������removeEntryForKey
				 */
				boolean tooBig = size() > size;
				if (tooBig)
				{
					eldestKey = eldest.getKey();
				}
				return tooBig;
			}
		};
	}

	@Override
	public void putObject(Object key, Object value)
	{
		kernelCache.putObject(key, value);
		cycleKeyList(key);
	}

	@Override
	public Object getObject(Object key)
	{
		keyMap.get(key);
		return kernelCache.getObject(key);
	}

	@Override
	public Object removeObject(Object key)
	{
		keyMap.remove(key);
		return kernelCache.removeObject(key);
	}

	@Override
	public void clear()
	{
		kernelCache.clear();
		keyMap.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock()
	{
		return null;
	}

	private void cycleKeyList(Object key)
	{
		keyMap.put(key, key);
		if (eldestKey != null)
		{
			kernelCache.removeObject(eldestKey);
			eldestKey = null;
		}
	}

}
