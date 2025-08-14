package org.je.app.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.StringTokenizer;

import org.je.app.util.IOUtils;
import org.je.log.Logger;

/**
 * 
 * Class loader for device and other Extensions
 * 
 * @author vlads
 *
 */
public class ExtensionsClassLoader extends URLClassLoader {

	private final static boolean debug = false;
	
	/* The context to be used when loading classes and resources */
    private AccessControlContext acc;
    
	public ExtensionsClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		acc = AccessController.getContext();
	}
	
	public void addURL(URL url) {
		super.addURL(url);
	}
	
	public void addClasspath(String classpath) {
		StringTokenizer st = new StringTokenizer(classpath, ";");
		while (st.hasMoreTokens()) {
			try {
				String path = st.nextToken();
				if (path.startsWith("file:")) {
					addURL(new URL(path));	
				} else {
					addURL(new URL(IOUtils.getCanonicalFileURL(new File(path))));
				}
			} catch (MalformedURLException e) {
				throw new Error(e);
			}
		}
	}
	 
	
	/**
	 * Finds the resource with the given name. A resource is some data (images,
	 * audio, text, etc) that can be accessed by class code in a way that is
	 * independent of the location of the code.
	 * 
	 * <p>
	 * The name of a resource is a '<tt>/</tt>'-separated path name that
	 * identifies the resource.
	 * 
	 * <p>
	 * Search order is reverse to standard implemenation
	 * </p>
	 * 
	 * <p>
	 * This method will first use {@link #findResource(String)} to find the
	 * resource. That failing, this method will NOT invoke the parent class
	 * loader.
	 * </p>
	 * 
	 * @param name
	 *            The resource name
	 * 
	 * @return A <tt>URL</tt> object for reading the resource, or
	 *         <tt>null</tt> if the resource could not be found or the invoker
	 *         doesn't have adequate privileges to get the resource.
	 * 
	 */
	public URL getResource(final String name) {
		try {
			URL url = (URL) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() {
					return findResource(name);
				}
			}, acc);
			if (url != null) {
				return url;
			}
		} catch (PrivilegedActionException e) {
			if (debug) {
				Logger.error("Unable to find resource " + name + " ", e);
			}
		}
		return super.getResource(name);
	}

}
