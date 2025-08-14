package org.je.app.util;

import java.io.InputStream;

import org.je.Injected;
import org.je.log.Logger;
import org.je.util.ThreadUtils;

/**
 * @author vlads
 * 
 * Use MIDletResourceLoader to load resources. To solve resource resource
 * loading paterns commonly used in MIDlet and not aceptable in Java SE
 * application when System class is called to load resource
 * 
 * j2me example:
 * 
 * String.class.getResourceAsStream(resourceName)
 * 
 */
public class MIDletResourceLoader {

	// TODO make this configurable

	public static boolean traceResourceLoading = false;

	/**
	 * @deprecated find better solution to share variable
	 */
	public static ClassLoader classLoader;

	private static final String FQCN = Injected.class.getName();

	public static InputStream getResourceAsStream(Class origClass, String resourceName) {
		if (traceResourceLoading) {
			Logger.debug("Loading MIDlet resource", resourceName);
		}
		if (classLoader != origClass.getClassLoader()) {
			// showWarning
			String callLocation = ThreadUtils.getCallLocation(FQCN);
			if (traceResourceLoading && callLocation != null) {
				Logger.warn("attempt to load resource [" + resourceName + "] using System ClasslLoader from "
						+ callLocation);
			}
		}
		resourceName = resolveName(origClass, resourceName);

		InputStream is = classLoader.getResourceAsStream(resourceName);
		if (is == null) {
			Logger.debug("Resource not found ", resourceName);
			return null;
		} else {
			return new MIDletResourceInputStream(is);
		}
	}

	private static String resolveName(Class origClass, String name) {
		if (name == null) {
			return name;
		}
		if (!name.startsWith("/")) {
			while (origClass.isArray()) {
				origClass = origClass.getComponentType();
			}
			String baseName = origClass.getName();
			int index = baseName.lastIndexOf('.');
			if (index != -1) {
				name = baseName.substring(0, index).replace('.', '/') + "/" + name;
			}
		} else {
			name = name.substring(1);
		}
		return name;
	}
}
