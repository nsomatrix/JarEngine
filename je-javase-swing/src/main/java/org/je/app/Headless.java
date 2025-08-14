package org.je.app;

import java.io.InputStream;
import java.util.ArrayList;

import org.je.DisplayComponent;
import org.je.MIDletBridge;
import org.je.app.ui.Message;
import org.je.app.ui.noui.NoUiDisplayComponent;
import org.je.app.util.DeviceEntry;
import org.je.device.DeviceDisplay;
import org.je.device.EmulatorContext;
import org.je.device.FontManager;
import org.je.device.InputMethod;
import org.je.device.impl.DeviceImpl;
import org.je.device.j2se.J2SEDevice;
import org.je.device.j2se.J2SEDeviceDisplay;
import org.je.device.j2se.J2SEFontManager;
import org.je.device.j2se.J2SEInputMethod;
import org.je.log.Logger;

public class Headless {

	private Common emulator;

	private EmulatorContext context = new EmulatorContext() {

		private DisplayComponent displayComponent = new NoUiDisplayComponent();

		private InputMethod inputMethod = new J2SEInputMethod();

		private DeviceDisplay deviceDisplay = new J2SEDeviceDisplay(this);

		private FontManager fontManager = new J2SEFontManager();

		public DisplayComponent getDisplayComponent() {
			return displayComponent;
		}

		public InputMethod getDeviceInputMethod() {
			return inputMethod;
		}

		public DeviceDisplay getDeviceDisplay() {
			return deviceDisplay;
		}

		public FontManager getDeviceFontManager() {
			return fontManager;
		}

		public InputStream getResourceAsStream(Class origClass, String name) {
            return MIDletBridge.getCurrentMIDlet().getClass().getResourceAsStream(name);
		}
		
		public boolean platformRequest(final String URL) {
			new Thread(new Runnable() {
				public void run() {
					Message.info("MIDlet requests that the device handle the following URL: " + URL);
				}
			}).start();

			return false;
		}
	};

	public Headless() {
		emulator = new Common(context);
	}

	public static void main(String[] args) {
		StringBuffer debugArgs = new StringBuffer();
		ArrayList params = new ArrayList();

		// Allow to override in command line
		// Non-persistent RMS
		params.add("--rms");
		params.add("memory");

		for (int i = 0; i < args.length; i++) {
			params.add(args[i]);
			if (debugArgs.length() != 0) {
				debugArgs.append(", ");
			}
			debugArgs.append("[").append(args[i]).append("]");
		}

		if (args.length > 0) {
			Logger.debug("headless arguments", debugArgs.toString());
		}

		Headless app = new Headless();

		app.emulator.initParams(params, null, J2SEDevice.class);
		app.emulator.initMIDlet(true);
	}

}
