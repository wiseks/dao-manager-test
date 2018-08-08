package com.manager.configration;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ClassResolverUtil<T> {

	private ClassLoader classloader;

	private Set<Class<? extends T>> matches = new HashSet<Class<? extends T>>();
	
	public Set<Class<? extends T>> getMatches() {
		return matches;
	}

	public ClassResolverUtil<T> find(String packageName) {
		String path = getPackagePath(packageName);

		try {
			List<String> children = this.list(path);
			for (String child : children) {
				if (child.endsWith(".class")) {
					addIfMatching(child);
				}
			}
		} catch (IOException ioe) {
			System.out.println("Could not read package: " + packageName + "," + ioe);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	protected void addIfMatching(String fqn) {
		try {
			String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
			ClassLoader loader = getClassLoader();
			// if (log.isDebugEnabled()) {
			// log.debug("Checking to see if class " + externalName + " matches
			// criteria [" + test + "]");
			// }

			Class<?> type = loader.loadClass(externalName);
			if (type != null && Object.class.isAssignableFrom(type)) {
				matches.add((Class<T>) type);
			}
		} catch (Throwable t) {
			System.out.println("Could not examine class '" + fqn + "'" + " due to a " + t.getClass().getName()
					+ " with message: " + t.getMessage());
		}
	}

	public ClassLoader getClassLoader() {
		return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
	}

	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}

	public List<String> list(String path) throws IOException {
		List<String> names = new ArrayList<String>();
		for (URL url : getResources(path)) {
			names.addAll(list(url, path));
		}
		return names;
	}

	protected static List<URL> getResources(String path) throws IOException {
		return Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
	}

	protected List<String> list(URL url, String path) throws IOException {
		ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		Resource[] resources = resourceResolver.getResources("classpath*:" + path + "/**/*.class");
		List<String> resourcePaths = new ArrayList<String>();
		for (Resource resource : resources) {
			resourcePaths.add(preserveSubpackageName(resource.getURI(), path));
		}
		return resourcePaths;
	}

	private static String preserveSubpackageName(final URI uri, final String rootPath) {
		final String uriStr = uri.toString();
		final int start = uriStr.indexOf(rootPath);
		return uriStr.substring(start);
	}
}
