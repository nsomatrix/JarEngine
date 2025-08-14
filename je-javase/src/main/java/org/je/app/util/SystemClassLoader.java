package org.je.app.util;

import java.net.URL;

/**
 * 
 * @deprecated use MIDletClassLoader 
 */
public class SystemClassLoader /*extends ClassLoader*/ {
	
//	private static MIDletClassLoader childClassLoader = null; 
//	
//	public SystemClassLoader(ClassLoader parent) {
//		super(parent);
//		
//		if (this instanceof MIDletClassLoader) {
//			childClassLoader = (MIDletClassLoader) this;
//		}
//	}
//
//	protected URL findResource(String name) {
//		URL result = null;
//		
//		if (childClassLoader != null) {
//			result = childClassLoader.findResource(name);
//		}
//		
//		return result;
//	}
//	
}
