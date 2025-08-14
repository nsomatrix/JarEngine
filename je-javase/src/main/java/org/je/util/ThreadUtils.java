package org.je.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Timer;

public class ThreadUtils {

	private static boolean java13 = false;
	
	private static boolean java14 = false;
	
	/**
	 * Creates a new timer whose associated thread has the specified name in Java 1.5.
	 * 
	 * @param name the name of the associated thread
	 *
	 */
	public static Timer createTimer(String name) {
		try {
			Constructor c = Timer.class.getConstructor(new Class[] { String.class });
			return (Timer)c.newInstance(new Object[]{name});
		} catch (Throwable e) {
			// In cany case create new Timer
			return new Timer();
		}
	}

	public static String getCallLocation(String fqn) {
		if (!java13) {
			try {
				StackTraceElement[] ste = new Throwable().getStackTrace();
				for (int i = 0; i < ste.length - 1; i++) {
					if (fqn.equals(ste[i].getClassName())) {
						StackTraceElement callLocation = ste[i + 1];
						String nextClassName = callLocation.getClassName();
						if (nextClassName.equals(fqn)) {
							continue;
						}
						return callLocation.toString();
					}
				}
			} catch (Throwable e) {
				java13 = true;
			}
		}
		return null;
	}
	
	public static String getTreadStackTrace(Thread t) {
		if (java14) {
			return "";
		}
		try {
			// Java 1.5 thread.getStackTrace();
			Method m = t.getClass().getMethod("getStackTrace", (Class<?>[]) null);
			
			StackTraceElement[] trace = (StackTraceElement[])m.invoke(t, (Object[]) null);
			StringBuffer b = new StringBuffer();  
			for (int i=0; i < trace.length; i++) {
				b.append("\n\tat ").append(trace[i]);
			}
			return b.toString();
		} catch (Throwable e) {
			java14 = true;
			return "";
		}
	}
}
