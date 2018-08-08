package com.manager.configration;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.ibatis.reflection.ExceptionUtil;

public class ProxyBean<T> implements InvocationHandler {
	
	private final Class<T> mapperInterface;
	
	public ProxyBean(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		 try {
		      if (Object.class.equals(method.getDeclaringClass())) {
		        return method.invoke(this, args);
		      } else if (isDefaultMethod(method)) {
		        return invokeDefaultMethod(proxy, method, args);
		      }
		    } catch (Throwable t) {
		      throw ExceptionUtil.unwrapThrowable(t);
		    }
//		    final MapperMethod mapperMethod = cachedMapperMethod(method);
//		    return mapperMethod.execute(sqlSession, args);
		 return null;
	}

	/**
	 * Backport of java.lang.reflect.Method#isDefault()
	 */
	private boolean isDefaultMethod(Method method) {
		return (method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
				&& method.getDeclaringClass().isInterface();
	}

	private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
		final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
				.getDeclaredConstructor(Class.class, int.class);
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}
		final Class<?> declaringClass = method.getDeclaringClass();
		return constructor
				.newInstance(declaringClass,
						MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE
								| MethodHandles.Lookup.PUBLIC)
				.unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
	}

}
