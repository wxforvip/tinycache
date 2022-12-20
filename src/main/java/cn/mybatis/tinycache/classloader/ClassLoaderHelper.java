
package cn.mybatis.tinycache.classloader;

/**
 * Java�ж�������������䱾�ʵĲ�ͬ��������Ͻ�ķ�Χ��ͬ��
  *  ��Ϊclass�ļ��Ĵ�ŵط���ͬ������Ҫ�������������������ż��ش�class�ļ�
 *  
 * @author www.mybatis.cn
 *
 */
public class ClassLoaderHelper
{

	ClassLoader defaultClassLoader;
	ClassLoader systemClassLoader;

	public ClassLoaderHelper()
	{
		try
		{
			systemClassLoader = ClassLoader.getSystemClassLoader();
		}
		catch (SecurityException e)
		{

		}
	}

	public Class<?> loadClassByName(String name) throws ClassNotFoundException
	{
		return this.loadClassByName(name, getClassLoaders(null));
	}

	public Class<?> loadClassByName(String name, ClassLoader classLoader) throws ClassNotFoundException
	{
		return this.loadClassByName(name, getClassLoaders(classLoader));
	}

	public Class<?> loadClassByName(String name, ClassLoader[] classLoader) throws ClassNotFoundException
	{

		for (ClassLoader classloader : classLoader)
		{

			if (classloader != null)
			{

				try
				{

					Class<?> c = Class.forName(name, true, classloader);

					if (c != null)
					{
						return c;
					}

				}
				catch (ClassNotFoundException e)
				{

				}

			}

		}

		throw new ClassNotFoundException("Cannot find class: " + name);

	}

	public ClassLoader[] getClassLoaders(ClassLoader classLoader)
	{
		ClassLoader[] classloaders =
		{ classLoader, defaultClassLoader, Thread.currentThread().getContextClassLoader(),
				this.getClass().getClassLoader(), systemClassLoader };
		return classloaders;
	}

}
