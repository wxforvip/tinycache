package cn.mybatis.tinycache.core;

import java.util.concurrent.locks.ReadWriteLock;

/**
  *  ºËÐÄ½Ó¿Ú
 * @author www.mybatis.cn
 *
 */
public interface Cache
{

	String getId();

	void putObject(Object key, Object value);

	Object getObject(Object key);

	Object removeObject(Object key);

	void clear();

	int getSize();

	ReadWriteLock getReadWriteLock();

}