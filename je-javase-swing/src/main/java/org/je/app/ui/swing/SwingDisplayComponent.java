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
 *  @version $Id$
 */

package org.je.app.ui.swing;



import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Screen;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.je.DisplayAccess;
import org.je.DisplayComponent;
import org.je.MIDletAccess;
import org.je.MIDletBridge;
import org.je.app.Common;
import org.je.app.ui.DisplayRepaintListener;
import org.je.device.Device;
import org.je.device.DeviceDisplay;
import org.je.device.DeviceFactory;
import org.je.device.impl.ButtonName;
import org.je.device.impl.InputMethodImpl;
import org.je.device.impl.SoftButton;
import org.je.device.impl.ui.CommandManager;
import org.je.device.j2se.J2SEButton;
import org.je.device.j2se.J2SEDeviceDisplay;
import org.je.device.j2se.J2SEGraphicsSurface;
import org.je.performance.PerformanceManager;
import org.je.device.j2se.J2SEInputMethod;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;

public class SwingDisplayComponent extends JComponent implements DisplayComponent {
	private static final long serialVersionUID = 1L;

	private SwingDeviceComponent deviceComponent;

	private J2SEGraphicsSurface graphicsSurface;

	private SoftButton initialPressedSoftButton;

	private DisplayRepaintListener displayRepaintListener;

	private boolean showMouseCoordinates = false;

	private Point pressedPoint = new Point();
	


	private MouseAdapter mouseListener = new MouseAdapter() {

		public void mousePressed(MouseEvent e) {
			deviceComponent.requestFocus();
			pressedPoint = e.getPoint();

			if (MIDletBridge.getCurrentMIDlet() == null) {
				return;
			}

			if (SwingUtilities.isMiddleMouseButton(e)) {
				// fire
				KeyEvent event = new KeyEvent(deviceComponent, 0, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER,
						KeyEvent.CHAR_UNDEFINED);
				deviceComponent.keyPressed(event);
				deviceComponent.keyReleased(event);
				return;
			}

			Device device = DeviceFactory.getDevice();
			J2SEInputMethod inputMethod = (J2SEInputMethod) device.getInputMethod();
			// if the displayable is in full screen mode, we should not
			// invoke any associated commands, but send the raw key codes
			// instead
			boolean fullScreenMode = device.getDeviceDisplay().isFullScreenMode();

			if (device.hasPointerEvents()) {
				if (!fullScreenMode) {
					Iterator it = device.getSoftButtons().iterator();
					while (it.hasNext()) {
						SoftButton button = (SoftButton) it.next();
						if (button.isVisible()) {
							org.je.device.impl.Rectangle pb = button.getPaintable();
							if (pb != null) {
								Point mapped = mapToDeviceCoordinates(e.getX(), e.getY());
								if (pb.contains(mapped.x, mapped.y)) {
									initialPressedSoftButton = button;
									button.setPressed(true);
									repaintRequest(pb.x, pb.y, pb.width, pb.height);
									break;
								}
							}
						}
					}
				}
				// Map coordinates
				Point mapped = mapToDeviceCoordinates(e.getX(), e.getY());
				MIDletAccess ma = MIDletBridge.getMIDletAccess();
				if (ma != null && ma.getDisplayAccess() != null) {
					inputMethod.pointerPressed(mapped.x, mapped.y);
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (MIDletBridge.getCurrentMIDlet() == null) {
				return;
			}

			Device device = DeviceFactory.getDevice();
			J2SEInputMethod inputMethod = (J2SEInputMethod) device.getInputMethod();
			boolean fullScreenMode = device.getDeviceDisplay().isFullScreenMode();
			if (device.hasPointerEvents()) {
				if (!fullScreenMode) {
					if (initialPressedSoftButton != null && initialPressedSoftButton.isPressed()) {
						initialPressedSoftButton.setPressed(false);
						org.je.device.impl.Rectangle pb = initialPressedSoftButton.getPaintable();
						if (pb != null) {
							repaintRequest(pb.x, pb.y, pb.width, pb.height);
							Point mapped = mapToDeviceCoordinates(e.getX(), e.getY());
							if (pb.contains(mapped.x, mapped.y)) {
								MIDletAccess ma = MIDletBridge.getMIDletAccess();
								if (ma == null) {
									return;
								}
								DisplayAccess da = ma.getDisplayAccess();
								if (da == null) {
									return;
								}
								Displayable d = da.getCurrent();
								Command cmd = initialPressedSoftButton.getCommand();
								if (cmd != null) {
									if (cmd.equals(CommandManager.CMD_MENU)) {
										CommandManager.getInstance().commandAction(cmd);
									} else {
										da.commandAction(cmd, d);
									}
								} else {
									if (d != null && d instanceof Screen) {
										if (initialPressedSoftButton.getName().equals("up")) {
											da.keyPressed(getButtonByButtonName(ButtonName.UP).getKeyCode());
										} else if (initialPressedSoftButton.getName().equals("down")) {
											da.keyPressed(getButtonByButtonName(ButtonName.DOWN).getKeyCode());
										}
									}
								}
							}
						}
					}
					initialPressedSoftButton = null;
				}
				// Map coordinates
				Point mapped = mapToDeviceCoordinates(e.getX(), e.getY());
				inputMethod.pointerReleased(mapped.x, mapped.y);
			}
		}

	};

	private MouseMotionListener mouseMotionListener = new MouseMotionListener() {

		public void mouseDragged(MouseEvent e) {
			// Input throttling
			if (PerformanceManager.shouldThrottlePointerDrag()) {
				return;
			}
			if (showMouseCoordinates) {
				StringBuffer buf = new StringBuffer();
				int width = e.getX() - pressedPoint.x;
				int height = e.getY() - pressedPoint.y;
				Point p = deviceCoordinate(DeviceFactory.getDevice().getDeviceDisplay(), pressedPoint);
				buf.append(p.x).append(",").append(p.y).append(" ").append(width).append("x").append(height);
				Common.setStatusBar(buf.toString());
			}

			Device device = DeviceFactory.getDevice();
			InputMethodImpl inputMethod = (InputMethodImpl) device.getInputMethod();
			boolean fullScreenMode = device.getDeviceDisplay().isFullScreenMode();
			if (device.hasPointerMotionEvents()) {
				if (!fullScreenMode) {
					if (initialPressedSoftButton != null) {
						org.je.device.impl.Rectangle pb = initialPressedSoftButton.getPaintable();
						if (pb != null) {
							Point mapped = mapToDeviceCoordinates(e.getX(), e.getY());
							if (pb.contains(mapped.x, mapped.y)) {
								if (!initialPressedSoftButton.isPressed()) {
									initialPressedSoftButton.setPressed(true);
									repaintRequest(pb.x, pb.y, pb.width, pb.height);
								}
							} else {
								if (initialPressedSoftButton.isPressed()) {
									initialPressedSoftButton.setPressed(false);
									repaintRequest(pb.x, pb.y, pb.width, pb.height);
								}
							}
						}
					}
				}
				// Map coordinates
				Point mapped = mapToDeviceCoordinates(e.getX(), e.getY());
				inputMethod.pointerDragged(mapped.x, mapped.y);
			}
		}

		public void mouseMoved(MouseEvent e) {
			if (showMouseCoordinates) {
				StringBuffer buf = new StringBuffer();
				Point p = deviceCoordinate(DeviceFactory.getDevice().getDeviceDisplay(), e.getPoint());
				buf.append(p.x).append(",").append(p.y);
				Common.setStatusBar(buf.toString());
			}
		}

	};

	private MouseWheelListener mouseWheelListener = new MouseWheelListener() {

		public void mouseWheelMoved(MouseWheelEvent ev) {
			if (ev.getWheelRotation() > 0) {
				// down
				KeyEvent event = new KeyEvent(deviceComponent, 0, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN,
						KeyEvent.CHAR_UNDEFINED);
				deviceComponent.keyPressed(event);
				deviceComponent.keyReleased(event);
			} else {
				// up
				KeyEvent event = new KeyEvent(deviceComponent, 0, System.currentTimeMillis(), 0, KeyEvent.VK_UP,
						KeyEvent.CHAR_UNDEFINED);
				deviceComponent.keyPressed(event);
				deviceComponent.keyReleased(event);
			}
		}

	};

	SwingDisplayComponent(SwingDeviceComponent deviceComponent) {
		this.deviceComponent = deviceComponent;

		setFocusable(false);

		addMouseListener(mouseListener);
		addMouseMotionListener(mouseMotionListener);
		addMouseWheelListener(mouseWheelListener);

		// Request focus after resize/maximize to ensure input works
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				requestFocusInWindow();
			}
		});
	}

	public void init() {
		synchronized (this) {
			graphicsSurface = null;
			initialPressedSoftButton = null;
		}
	}

	public void addDisplayRepaintListener(DisplayRepaintListener l) {
		displayRepaintListener = l;
	}

	public void removeDisplayRepaintListener(DisplayRepaintListener l) {
		if (displayRepaintListener == l) {
			displayRepaintListener = null;
		}
	}

    @Override
    public Dimension getPreferredSize() {
		Device device = DeviceFactory.getDevice();
		if (device == null) {
			return new Dimension(0, 0); // Allow free shrinking when no device
		}

        // Return device size but do not enforce as minimum
        return new Dimension(device.getDeviceDisplay().getFullWidth(), device.getDeviceDisplay().getFullHeight());
	}

    @Override
    public Dimension getMinimumSize() {
		// Never block layout from showing the status bar
		return new Dimension(0, 0);
	}

    @Override
    protected void paintComponent(Graphics g) {
		// Increment frame counter for FPS calculation
		org.je.app.tools.FPSTool.incrementFrameCount();

		// Apply double buffering preference dynamically
		if (!PerformanceManager.isDoubleBuffering() && isDoubleBuffered()) {
			setDoubleBuffered(false);
		} else if (PerformanceManager.isDoubleBuffering() && !isDoubleBuffered()) {
			setDoubleBuffered(true);
		}
		
		if (graphicsSurface != null && graphicsSurface.getImage() != null) {
			synchronized (graphicsSurface) {
				int compW = getWidth();
				int compH = getHeight();
				int imgW = graphicsSurface.getImage().getWidth(null);
				int imgH = graphicsSurface.getImage().getHeight(null);

				if (compW > 0 && compH > 0 && imgW > 0 && imgH > 0) {
					Graphics2D g2 = (Graphics2D) g;
					if (PerformanceManager.isAntiAliasing()) {
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					}
					Object interp = PerformanceManager.isTextureFiltering() ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
					g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp);
					g2.drawImage(graphicsSurface.getImage(), 0, 0, compW, compH, 0, 0, imgW, imgH, null);
				}
			}
		} else {
			// Fallback: draw a black background when no graphics surface is available
			g.setColor(java.awt.Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		
        // Draw FPS overlay if enabled
        if (org.je.app.tools.FPSTool.fpsOverlayEnabled) {
			drawFpsOverlay(g);
		}
	}
	
	private void drawFpsOverlay(Graphics g) {
		// Get current FPS from FPSTool
		double currentFps = org.je.app.tools.FPSTool.currentFps;
		int targetFps = org.je.app.tools.FPSTool.targetFps;
		
		// Set up graphics for overlay
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Position overlay in top-left corner
		int overlayX = 10;
		int overlayY = 10;
		int overlayWidth = 90;
		int overlayHeight = 30;
		
		// Draw semi-transparent background
		g2d.setColor(new Color(0, 0, 0, 140));
		g2d.fillRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 8, 8);
		
		// Set font and color for text
		g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
		// Draw shadow for better readability
		g2d.setColor(new Color(0, 0, 0, 180));
		g2d.drawString(String.format("%.1f FPS", currentFps), overlayX + 9, overlayY + 21);
		g2d.setColor(Color.WHITE);
		g2d.drawString(String.format("%.1f FPS", currentFps), overlayX + 8, overlayY + 20);
		
		g2d.dispose();
	}


	public void repaintRequest(int x, int y, int width, int height) {

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
			J2SEDeviceDisplay deviceDisplay = (J2SEDeviceDisplay) device.getDeviceDisplay();
			int deviceWidth = device.getDeviceDisplay().getFullWidth();
			int deviceHeight = device.getDeviceDisplay().getFullHeight();

			synchronized (this) {
				// Only recreate graphicsSurface if it doesn't exist or has wrong size
				boolean needsNewSurface = graphicsSurface == null
					|| graphicsSurface.getImage().getWidth(null) != deviceWidth
					|| graphicsSurface.getImage().getHeight(null) != deviceHeight;
				if (needsNewSurface) {
					graphicsSurface = new J2SEGraphicsSurface(deviceWidth, deviceHeight, false, 0x000000);
				}
				synchronized (graphicsSurface) {
					deviceDisplay.paintDisplayable(graphicsSurface, x, y, width, height);
					if (!deviceDisplay.isFullScreenMode()) {
						deviceDisplay.paintControls(graphicsSurface.getGraphics());
					}
				}
			}

			if (deviceDisplay.isFullScreenMode()) {
				fireDisplayRepaint(
						graphicsSurface, x, y, width, height);
			} else {
				fireDisplayRepaint(
						graphicsSurface, 0, 0, graphicsSurface.getImage().getWidth(), graphicsSurface.getImage().getHeight());
			}
			repaint();
		}
	}

	public void fireDisplayRepaint(J2SEGraphicsSurface graphicsSurface, int x, int y, int width, int height) {
		if (displayRepaintListener != null) {
			displayRepaintListener.repaintInvoked(graphicsSurface);
		}
		
		repaint(x, y, width, height);
	}

	Point deviceCoordinate(DeviceDisplay deviceDisplay, Point p) {
		if (deviceDisplay.isFullScreenMode()) {
			return p;
		} else {
			org.je.device.impl.Rectangle pb = ((J2SEDeviceDisplay) deviceDisplay).getDisplayPaintable();
			return new Point(p.x - pb.x, p.y - pb.y);
		}
	}



	public J2SEGraphicsSurface getGraphicsSurface() {
		return graphicsSurface;
}

	public MouseAdapter getMouseListener() {
		return mouseListener;
	}

	public MouseMotionListener getMouseMotionListener() {
		return mouseMotionListener;
	}

	public MouseWheelListener getMouseWheelListener() {
		return mouseWheelListener;
	}
	
	private J2SEButton getButtonByButtonName(ButtonName buttonName) {
		J2SEButton result;
		for (Enumeration e = DeviceFactory.getDevice().getButtons().elements(); e.hasMoreElements();) {
			result = (J2SEButton) e.nextElement();
			if (result.getFunctionalName() == buttonName) {
				return result;
			}
		}

		return null;
	}

    // Add a method to reset the graphics surface when device size changes
    public void resetGraphicsSurface() {
        synchronized (this) {
            graphicsSurface = null;
        }
    }



    // Utility to map component coordinates to device coordinates
    private Point mapToDeviceCoordinates(int x, int y) {
		Device device = DeviceFactory.getDevice();
		if (device == null || graphicsSurface == null || graphicsSurface.getImage() == null) {
			return new Point(x, y);
		}
		int deviceWidth = graphicsSurface.getImage().getWidth(null);
		int deviceHeight = graphicsSurface.getImage().getHeight(null);
		int compWidth = getWidth();
		int compHeight = getHeight();

		if (compWidth <= 0 || compHeight <= 0 || deviceWidth <= 0 || deviceHeight <= 0) {
			return new Point(x, y);
		}

		int mappedX = (int) Math.round(x * (deviceWidth / (double) compWidth));
		int mappedY = (int) Math.round(y * (deviceHeight / (double) compHeight));
		return new Point(mappedX, mappedY);
	}
}
