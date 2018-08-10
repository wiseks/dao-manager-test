package com.manager.configration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.binding.BindingException;

public class ProxyBeanCreator {

	private static final Map<String, Class<?>> knownMappers = new HashMap<>();

	public ProxyBeanCreator(String packageName) {
		ClassResolverUtil<Class<?>> util = new ClassResolverUtil<>();
		Set<Class<? extends Class<?>>> set = util.find(packageName).getMatches();
		for (Class<?> mapperClass : set) {
			addMapper(mapperClass);
		}
	}

	private <T> void addMapper(Class<T> type) {
		if (type.isInterface()) {
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
			}
			boolean loadCompleted = false;
			try {
				knownMappers.put(type.getName(), type);
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
					knownMappers.remove(type.getName());
				}
			}
		}
	}

	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	public Class<?> getMapper(String beanName) {
		return knownMappers.get(beanName);
	}
}
