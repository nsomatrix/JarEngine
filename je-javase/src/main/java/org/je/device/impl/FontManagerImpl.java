package org.je.device.impl;

import java.net.URL;

import org.je.device.FontManager;

public interface FontManagerImpl extends FontManager {

	void setAntialiasing(boolean antialiasing);

	void setFont(String face, String style, String size, Font font);

	Font createSystemFont(String defName, String defStyle, int defSize, boolean antialiasing);
	
	Font createTrueTypeFont(URL defUrl, String defStyle, int defSize, boolean antialiasing);
	
}
