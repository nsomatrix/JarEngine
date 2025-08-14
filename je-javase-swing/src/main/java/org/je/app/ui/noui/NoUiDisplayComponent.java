package org.je.app.ui.noui;

import javax.microedition.lcdui.Displayable;

import org.je.DisplayAccess;
import org.je.DisplayComponent;
import org.je.MIDletAccess;
import org.je.MIDletBridge;
import org.je.app.ui.DisplayRepaintListener;
import org.je.device.Device;
import org.je.device.DeviceFactory;
import org.je.device.j2se.J2SEDeviceDisplay;
import org.je.device.j2se.J2SEGraphicsSurface;

public class NoUiDisplayComponent implements DisplayComponent {

	private J2SEGraphicsSurface graphicsSurface;

	private DisplayRepaintListener displayRepaintListener;
	
	public void addDisplayRepaintListener(DisplayRepaintListener l) {
		displayRepaintListener = l;
	}

	public void removeDisplayRepaintListener(DisplayRepaintListener l) {
		if (displayRepaintListener == l) {
			displayRepaintListener = null;
		}
	}

	public void repaintRequest(int x, int y, int width, int height) 
	{
		MIDletAccess ma = MIDletBridge.getMIDletAccess();
		if (ma == null) {
			return;
		}
		DisplayAccess da = ma.getDisplayAccess();
		if (da == null) {
			return;
		}
		Displayable current = da.getCurrent();
		if (current == null) {
			return;
		}

		Device device = DeviceFactory.getDevice();

		if (device != null) {
			if (graphicsSurface == null) {
				graphicsSurface = new J2SEGraphicsSurface(
						device.getDeviceDisplay().getFullWidth(), device.getDeviceDisplay().getFullHeight(), false, 0x000000);
			}
					
			J2SEDeviceDisplay deviceDisplay = (J2SEDeviceDisplay) device.getDeviceDisplay();
			synchronized (graphicsSurface) {
				deviceDisplay.paintDisplayable(graphicsSurface, x, y, width, height);
				if (!deviceDisplay.isFullScreenMode()) {
					deviceDisplay.paintControls(graphicsSurface.getGraphics());
				}
			}

			fireDisplayRepaint(graphicsSurface);
		}	
	}


	private void fireDisplayRepaint(J2SEGraphicsSurface graphicsSurface)
	{
		if (displayRepaintListener != null) {
			displayRepaintListener.repaintInvoked(graphicsSurface);
		}
	}

}
