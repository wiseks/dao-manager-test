package com.manager.configration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.binding.BindingException;

public class ProxyBeanCreator {

	private static final Map<Class<?>, ProxyFactory<?>> knownMappers = new HashMap<Class<?>, ProxyFactory<?>>();

	public ProxyBeanCreator(String packageName) {
		ClassResolverUtil<Class<?>> util = new ClassResolverUtil<>();
		Set<Class<? extends Class<?>>> set = util.find(packageName).getMatches();
		for (Class<?> mapperClass : set) {
			addMapper(mapperClass);
		}
	}

	public <T> void addMapper(Class<T> type) {
		if (type.isInterface()) {
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
			}
			boolean loadCompleted = false;
			try {
				knownMappers.put(type, new ProxyFactory<T>(type));
				// It's important that the type is added before the parser is
				// run
				// otherwise the binding may automatically be attempted by the
				// mapper parser. If the type is already known, it won't try.
				// MapperAnnotationBuilder parser = new
				// MapperAnnotationBuilder(config, type);
				// parser.parse();
				loadCompleted = true;
			} finally {
				if (!loadCompleted) {
					knownMappers.remove(type);
				}
			}
		}
	}

	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getMapper(Class<T> type) {
		final ProxyFactory<T> mapperProxyFactory = (ProxyFactory<T>) knownMappers.get(type);
		if (mapperProxyFactory == null) {
			throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			return mapperProxyFactory.newInstance();
		} catch (Exception e) {
			throw new BindingException("Error getting mapper instance. Cause: " + e, e);
		}
	}
}
