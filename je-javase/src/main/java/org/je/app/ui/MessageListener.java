package org.je.app.ui;

public interface MessageListener {

	public static final int ERROR = 0;

    public static final int INFO = 1;

    public static final int WARN = 2;
    
	public void showMessage(int level, String title, String text, Throwable throwable);
	
}
