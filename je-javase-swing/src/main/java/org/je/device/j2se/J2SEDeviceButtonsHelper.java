package org.je.device.j2se;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.je.device.Device;
import org.je.device.DeviceFactory;
import org.je.device.impl.ButtonName;
import org.je.device.impl.SoftButton;
import org.je.log.Logger;

/**
 * Maps keyboard and mouse events to Buttons
 * 
 * @author vlads
 * 
 */
public class J2SEDeviceButtonsHelper {

	private static Map devices = new WeakHashMap();

	private static class DeviceInformation {

		Map keyboardKeyCodes = new HashMap();

		Map keyboardCharCodes = new HashMap();

		Map functions = new HashMap();
	}

	public static SoftButton getSoftButton(MouseEvent ev) {
		Iterator it = DeviceFactory.getDevice().getSoftButtons().iterator();
		while (it.hasNext()) {
			SoftButton button = (SoftButton) it.next();
			if (button.isVisible()) {
				org.je.device.impl.Rectangle pb = button.getPaintable();
				if (pb != null && pb.contains(ev.getX(), ev.getY())) {
					return button;
				}
			}
		}
		return null;
	}

	public static J2SEButton getSkinButton(MouseEvent ev) {
		for (Enumeration en = DeviceFactory.getDevice().getButtons().elements(); en.hasMoreElements();) {
			J2SEButton button = (J2SEButton) en.nextElement();
			if (button.getShape() != null) {
				if (button.getShape().contains(ev.getX(), ev.getY())) {
					return button;
				}
			}
		}
		return null;
	}

	public static J2SEButton getButton(KeyEvent ev) {
		DeviceInformation inf = getDeviceInformation();
		J2SEButton button = (J2SEButton) inf.keyboardCharCodes.get(new Integer(ev.getKeyChar()));
		if (button != null) {
			return button;
		}
		return (J2SEButton) inf.keyboardKeyCodes.get(new Integer(ev.getKeyCode()));
	}

	public static J2SEButton getButton(ButtonName functionalName) {
		DeviceInformation inf = getDeviceInformation();
		return (J2SEButton) inf.functions.get(functionalName);
	}

	private static DeviceInformation getDeviceInformation() {
		Device dev = DeviceFactory.getDevice();
		DeviceInformation inf;
		synchronized (J2SEDeviceButtonsHelper.class) {
			inf = (DeviceInformation) devices.get(dev);
			if (inf == null) {
				inf = createDeviceInformation(dev);
			}
		}
		return inf;
	}

	private static DeviceInformation createDeviceInformation(Device dev) {
		DeviceInformation inf = new DeviceInformation();
		boolean hasModeChange = false;
		for (Enumeration en = dev.getButtons().elements(); en.hasMoreElements();) {
			J2SEButton button = (J2SEButton) en.nextElement();
			int keyCodes[] = button.getKeyboardKeyCodes();
			for (int i = 0; i < keyCodes.length; i++) {
				inf.keyboardKeyCodes.put(new Integer(keyCodes[i]), button);
			}
			char charCodes[] = button.getKeyboardCharCodes();
			for (int i = 0; i < charCodes.length; i++) {
				inf.keyboardCharCodes.put(new Integer(charCodes[i]), button);
			}
			inf.functions.put(button.getFunctionalName(), button);
			if (button.isModeChange()) {
				hasModeChange = true;
			}
		}

		// Correction to missing code in xml
		if (!hasModeChange) {
			J2SEButton button = (J2SEButton) inf.functions.get(ButtonName.KEY_POUND);
			if (button != null) {
				button.setModeChange();
			} else {
				Logger.warn("Device has no ModeChange and POUND buttons");
			}
		}

		if (inf.functions.get(ButtonName.DELETE) == null) {
			dev.getButtons().add(new J2SEButton(ButtonName.DELETE));
		}
		if (inf.functions.get(ButtonName.BACK_SPACE) == null) {
			dev.getButtons().add(new J2SEButton(ButtonName.BACK_SPACE));
		}
		return inf;
	}
}
