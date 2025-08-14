package org.je.device;

import java.util.Map;
import java.util.Vector;

import javax.microedition.lcdui.Image;

import org.je.device.ui.UIFactory;

public interface Device {

	void init();

	void destroy();

	String getName();

	InputMethod getInputMethod();

	FontManager getFontManager();

	DeviceDisplay getDeviceDisplay();
	
	UIFactory getUIFactory();

	Image getNormalImage();

	Image getOverImage();

	Image getPressedImage();

	Vector getSoftButtons();

	Vector getButtons();

	boolean hasPointerEvents();

	boolean hasPointerMotionEvents();

	boolean hasRepeatEvents();

	boolean vibrate(int duration);

	Map getSystemProperties();

}