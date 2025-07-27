/*
 *  MicroEmulator
 *  Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  Contributor(s):
 *    daniel(at)angrymachine.com.ar
 */

package org.je.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.swing.Timer;

import org.je.DisplayComponent;
import org.je.MIDletBridge;
import org.je.MIDletContext;
import org.je.MicroEmulator;
import org.je.RecordStoreManager;
import org.je.app.launcher.Launcher;
import org.je.app.ui.swing.SwingDeviceComponent;
import org.je.app.util.MIDletResourceLoader;
import org.je.app.util.MIDletSystemProperties;
import org.je.device.DeviceDisplay;
import org.je.device.DeviceFactory;
import org.je.device.EmulatorContext;
import org.je.device.FontManager;
import org.je.device.InputMethod;
import org.je.device.impl.DeviceDisplayImpl;
import org.je.device.impl.DeviceImpl;
import org.je.device.j2se.J2SEDevice;
import org.je.device.j2se.J2SEDeviceDisplay;
import org.je.device.j2se.J2SEFontManager;
import org.je.device.j2se.J2SEInputMethod;
import org.je.device.ui.EventDispatcher;
import org.je.log.Logger;
import org.je.util.JadMidletEntry;
import org.je.util.JadProperties;
import org.je.util.MemoryRecordStoreManager;

public class Main extends Applet implements MicroEmulator {

	private static final long serialVersionUID = 1L;

	private MIDlet midlet = null;

	private RecordStoreManager recordStoreManager;

	private JadProperties manifest = new JadProperties();

	private SwingDeviceComponent devicePanel;

	/**
	 * Host name accessible by MIDlet
	 */
	private String accessibleHost;

	private EmulatorContext emulatorContext = new EmulatorContext() 
	{
		private InputMethod inputMethod = new J2SEInputMethod();

		private DeviceDisplay deviceDisplay = new J2SEDeviceDisplay(this);

		private FontManager fontManager = new J2SEFontManager();

		public DisplayComponent getDisplayComponent() {
			return devicePanel.getDisplayComponent();
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
            return getClass().getResourceAsStream(name);
		}
		
		public boolean platformRequest(String url) {
			try {
				getAppletContext().showDocument(new URL(url), "mini");
			} catch (Exception e) {
			}
			return false;
		}
	};

	public Main() {
		devicePanel = new SwingDeviceComponent();
		devicePanel.addKeyListener(devicePanel);
	}

	public void init() {
		if (midlet != null) {
			return;
		}

		MIDletSystemProperties.applyToJavaSystemProperties = false;
		MIDletBridge.setMicroEmulator(this);

		URL baseURL = getCodeBase();
		if (baseURL != null) {
			accessibleHost = baseURL.getHost();
		}

		recordStoreManager = new MemoryRecordStoreManager();

		setLayout(new BorderLayout());
		add(devicePanel, "Center");

		// Always use resizable device
		DeviceImpl device;
		try {
			device = DeviceImpl.create(emulatorContext, Main.class.getClassLoader(), DeviceImpl.RESIZABLE_LOCATION,
					J2SEDevice.class);
			DeviceFactory.setDevice(device);
		} catch (IOException ex) {
			Logger.error(ex);
			return;
		}

		devicePanel.init();

		manifest.clear();
		try {
			URL url = getClass().getClassLoader().getResource("META-INF/MANIFEST.MF");
			if (url != null) {
				manifest.read(url.openStream());
				if (manifest.getProperty("MIDlet-Name") == null) {
					manifest.clear();
				}
			}
		} catch (IOException e) {
			Logger.error(e);
		}

		// load jad
		String midletClassName = null;
		String jadFile = getParameter("jad");
		if (jadFile != null) {
			InputStream jadInputStream = null;
			try {
				URL jad = new URL(getCodeBase(), jadFile);
				jadInputStream = jad.openStream();
				manifest.read(jadInputStream);
				Vector entries = manifest.getMidletEntries();
				// only load the first (no midlet suite support anyway)
				if (entries.size() > 0) {
					JadMidletEntry entry = (JadMidletEntry) entries.elementAt(0);
					midletClassName = entry.getClassName();
				}
			} catch (IOException e) {
			} finally {
				if (jadInputStream != null) {
					try {
						jadInputStream.close();
					} catch (IOException e1) {
					}
				}
			}
		}

		if (midletClassName == null) {
			midletClassName = getParameter("midlet");
			if (midletClassName == null) {
				Logger.debug("There is no midlet parameter");
				return;
			}
		}
		
		String maxFps = getParameter("maxfps");
		if (maxFps != null) {
			try {
				EventDispatcher.maxFps = Integer.parseInt(maxFps);
			} catch (NumberFormatException ex) {
			}
		}

		// Applet is using only one classLoader
		MIDletResourceLoader.classLoader = this.getClass().getClassLoader();
		Class midletClass;
		try {
			midletClass = Class.forName(midletClassName);
		} catch (ClassNotFoundException ex) {
			Logger.error("Cannot find " + midletClassName + " MIDlet class");
			return;
		}

		try {
			midlet = (MIDlet) midletClass.newInstance();
		} catch (Exception ex) {
			Logger.error("Cannot initialize " + midletClass + " MIDlet class", ex);
			return;
		}

		if (((DeviceDisplayImpl) device.getDeviceDisplay()).isResizable()) {
			resize(device.getDeviceDisplay().getFullWidth(), device.getDeviceDisplay().getFullHeight());
		} else {
			resize(device.getNormalImage().getWidth(), device.getNormalImage().getHeight());
		}

		return;
	}

	public void start() {
		devicePanel.requestFocus();

		new Thread("midlet_starter") {
			public void run() {
				try {
					MIDletBridge.getMIDletAccess(midlet).startApp();
				} catch (MIDletStateChangeException ex) {
					System.err.println(ex);
				}
			}
		}.start();

		Timer timer = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				devicePanel.requestFocus();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	public void stop() {
		MIDletBridge.getMIDletAccess(midlet).pauseApp();
	}

	public void destroy() {
		try {
			MIDletBridge.getMIDletAccess(midlet).destroyApp(true);
		} catch (MIDletStateChangeException ex) {
			System.err.println(ex);
		}
		// TODO handle this through ImplementationInitialization.notifyMIDletDestroyed()
		try {
            Class managerClass = Class.forName("javax.microedition.media.Manager");
            Method cleanupMethod = managerClass.getMethod("cleanupMedia", (Class[]) null);
            cleanupMethod.invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	public RecordStoreManager getRecordStoreManager() {
		return recordStoreManager;
	}

	public String getAppProperty(String key) {
		if (key.equals("applet")) {
			return "yes";
		}

		String value = null;
		if (key.equals("microedition.platform")) {
			value = "JE";
		} else if (key.equals("microedition.profiles")) {
			value = "MIDP-2.0";
		} else if (key.equals("microedition.configuration")) {
			value = "CLDC-1.0";
		} else if (key.equals("microedition.locale")) {
			value = Locale.getDefault().getLanguage();
		} else if (key.equals("microedition.encoding")) {
			value = System.getProperty("file.encoding");
		} else if (key.equals("je.applet")) {
			value = "true";
		} else if (key.equals("je.accessible.host")) {
			value = accessibleHost;
		} else if (getParameter(key) != null) {
			value = getParameter(key);
		} else {
			value = manifest.getProperty(key);
		}

		return value;
	}
	
	public String getSuiteName() {
		return null;
	}

	public InputStream getResourceAsStream(Class origClass, String name) {
		return emulatorContext.getResourceAsStream(origClass, name);
	}
	
	public int checkPermission(String permission) {
		// TODO
		
		return 0;
	}

	public boolean platformRequest(String url) throws ConnectionNotFoundException {
		return emulatorContext.platformRequest(url);
	}

	public void notifyDestroyed(MIDletContext midletContext) {
	}

	public void destroyMIDletContext(MIDletContext midletContext) {

	}

	public Launcher getLauncher() {
		return null;
	}

	public String getAppletInfo() {
		return "Title: JarEngine \nAuthor: Bartek Teodorczyk, 2001-2010\nForked and rebranded as JarEngine";
	}

	public String[][] getParameterInfo() {
		String[][] info = { { "midlet", "MIDlet class name", "The MIDlet class name. This field is mandatory." }, };

		return info;
	}

}
