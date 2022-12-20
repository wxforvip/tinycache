
package cn.mybatis.tinycache.classloader;

/**
 * Java有多种类加载器，其本质的不同在于所管辖的范围不同。
  *  因为class文件的存放地方不同，故需要多种类加载器逐个尝试着加载此class文件
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
