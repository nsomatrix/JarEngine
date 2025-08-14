package org.je.app.ui;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.je.log.Logger;

/**
 * 
 * This class is used to Show error message to user
 * 
 * @author vlads
 *
 */
public class Message {

	public static final int ERROR = 0;

    public static final int INFO = 1;

    public static final int WARN = 2;
    
    private static List listeners = new Vector();
    
    static {
    	Logger.addLogOrigin(Message.class);
    }
    
    /**
     * Show Error message to user
     * 
     * @param title Dialog title
     * @param text  Message
     */
    public static void error(String title, String text) {
        Logger.error("Message: " + title + ": " + text);
        callListeners(ERROR, title, text, null);
    }

    /**
     * Show Error message to user
     * 
     * @param text  Message
     */
    public static void error(String text) {
        Logger.error("Message: Error: " + text);
        callListeners(ERROR, "Error", text, null);
    }
    
    /**
     * Show Error message to user
     * 
     * @param title Dialog title
     * @param text  Message
     */
    public static void error(String title, String text, Throwable throwable) {
        Logger.error("Message: " + title + ": " + text, throwable);
        callListeners(ERROR, title, text, throwable);
    }

    public static void error(String text, Throwable throwable) {
        Logger.error("Message: Error : " + text, throwable);
        callListeners(ERROR, "Error", text, throwable);
    }
    
    /**
     * Show info message to user
     * 
     * @param text  Message
     */
    public static void info(String text) {
        Logger.info("Message: info: " + text);
        callListeners(INFO, "Info", text, null);
    }

    /**
     * Show info message to user
     * 
     * @param text  Message
     */
    public static void warn(String text) {
        Logger.warn("Message: warn: " + text);
        callListeners(INFO, "Warning", text, null);
    }
    
    /**
     * Here we can butify error message text.
     * @param throwable
     * @return
     */
    public static String getCauseMessage(Throwable throwable) {
		if (throwable.getCause() == null) {
			return throwable.toString();
		} else {
			return getCauseMessage(throwable.getCause());
		}
    }
    
    private static void callListeners(int level, String title, String text, Throwable throwable) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			MessageListener a = (MessageListener) iter.next();
			a.showMessage(level, title, text, throwable);
		};
	}
	
	public static void addListener(MessageListener newListener) {
		listeners.add(newListener);
	}
}
