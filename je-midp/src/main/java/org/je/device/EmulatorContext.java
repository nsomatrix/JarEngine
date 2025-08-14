package org.je.device;

import java.io.InputStream;

import javax.microedition.io.ConnectionNotFoundException;

import org.je.DisplayComponent;
import org.je.device.DeviceDisplay;
import org.je.device.FontManager;
import org.je.device.InputMethod;

public interface EmulatorContext {

	DisplayComponent getDisplayComponent();

	InputMethod getDeviceInputMethod();

	DeviceDisplay getDeviceDisplay();

	FontManager getDeviceFontManager();
	
	InputStream getResourceAsStream(Class origClass, String name);

	boolean platformRequest(final String URL) throws ConnectionNotFoundException;
	
}
