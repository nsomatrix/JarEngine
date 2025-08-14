package org.je.device.j2se;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;

import org.je.device.Device;
import org.je.device.DeviceFactory;
import org.je.device.impl.Rectangle;
import org.je.device.impl.Shape;
import org.je.device.impl.SoftButton;

public class J2SESoftButton extends J2SEButton implements SoftButton {

	public static int LEFT = 1;

	public static int RIGHT = 2;

	private int type;

	private Image normalImage;

	private Image pressedImage;

	private Vector commandTypes = new Vector();

	private Command command = null;

	private Rectangle paintable;

	private int alignment;

	private boolean visible;

	private boolean pressed;

	private Font font;

	/**
	 * @param name
	 * @param rectangle
	 * @param keyCode -
	 *            Integer.MIN_VALUE when unspecified
	 * @param keyName
	 * @param paintable
	 * @param alignmentName
	 * @param commands
	 * @param font
	 */
	public J2SESoftButton(int skinVersion, String name, Shape shape, int keyCode, String keyboardKeys,
			Rectangle paintable, String alignmentName, Vector commands, Font font) {
		super(skinVersion, name, shape, keyCode, keyboardKeys, null, new Hashtable(), false);

		this.type = TYPE_COMMAND;

		this.paintable = paintable;
		this.visible = true;
		this.pressed = false;
		this.font = font;

		if (alignmentName != null) {
			try {
				alignment = J2SESoftButton.class.getField(alignmentName).getInt(null);
			} catch (Exception ex) {
				System.err.println(ex);
			}
		}

		for (Enumeration e = commands.elements(); e.hasMoreElements();) {
			String tmp = (String) e.nextElement();
			try {
				addCommandType(Command.class.getField(tmp).getInt(null));
			} catch (Exception ex) {
				System.err.println("a3" + ex);
			}
		}
	}

	public J2SESoftButton(int skinVersion, String name, Rectangle paintable, Image normalImage, Image pressedImage) {
		super(skinVersion, name, null, Integer.MIN_VALUE, null, null, null, false);

		this.type = TYPE_ICON;

		this.paintable = paintable;
		this.normalImage = normalImage;
		this.pressedImage = pressedImage;

		this.visible = true;
		this.pressed = false;
	}

	public int getType() {
		return type;
	}

	/**
	 * Sets the command attribute of the SoftButton object
	 * 
	 * @param cmd
	 *            The new command value
	 */
	public void setCommand(Command cmd) {
		synchronized (this) {
			command = cmd;
		}
	}

	/**
	 * Gets the command attribute of the SoftButton object
	 * 
	 * @return The command value
	 */
	public Command getCommand() {
		return command;
	}

	public Rectangle getPaintable() {
		return paintable;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean state) {
		visible = state;
	}

	public boolean isPressed() {
		return pressed;
	}

	public void setPressed(boolean state) {
		pressed = state;
	}

	public void paint(Graphics g) {
		if (!visible || paintable == null) {
			return;
		}

		java.awt.Shape clip = g.getClip();

		g.setClip(paintable.x, paintable.y, paintable.width, paintable.height);
		if (type == TYPE_COMMAND) {
			int xoffset = 0;
			Device device = DeviceFactory.getDevice();
			J2SEDeviceDisplay deviceDisplay = (J2SEDeviceDisplay) device.getDeviceDisplay();
			
			// Get themed colors for soft buttons
			java.awt.Color buttonBgColor;
			java.awt.Color buttonFgColor;
			try {
				// Use proper FlatLaf theme colors for consistency
				if ("dark".equals(deviceDisplay.getCurrentTheme())) {
					buttonBgColor = new java.awt.Color(0x222222); // Same as LauncherCanvas dark theme
					buttonFgColor = new java.awt.Color(0xFFFFFF); // White text
				} else {
					// Use proper FlatLaf light theme colors
					buttonBgColor = new java.awt.Color(0xFFFFFF); // Pure white for light theme
					buttonFgColor = new java.awt.Color(0x000000); // Black text for light theme
				}
			} catch (Exception e) {
				buttonBgColor = deviceDisplay.backgroundColor;
				buttonFgColor = deviceDisplay.foregroundColor;
			}
			
			if (pressed) {
				g.setColor(buttonFgColor);
			} else {
				g.setColor(buttonBgColor);
			}
			g.fillRect(paintable.x, paintable.y, paintable.width, paintable.height);
			synchronized (this) {
				if (command != null) {
					if (font != null) {
						J2SEFontManager fontManager = (J2SEFontManager) device.getFontManager();
						J2SEFont buttonFont = (J2SEFont) fontManager.getFont(font);
						g.setFont(buttonFont.getFont());
					}
					FontMetrics metrics = g.getFontMetrics();
					if (alignment == RIGHT) {
						xoffset = paintable.width - metrics.stringWidth(command.getLabel()) - 1;
					}
					if (pressed) {
						g.setColor(buttonBgColor);
					} else {
						g.setColor(buttonFgColor);
					}
					g.drawString(command.getLabel(), paintable.x + xoffset, paintable.y + paintable.height
							- metrics.getDescent());
				}
			}
		} else if (type == TYPE_ICON) {
			if (pressed) {
				g.drawImage(((J2SEImmutableImage) pressedImage).getImage(), paintable.x, paintable.y, null);
			} else {
				g.drawImage(((J2SEImmutableImage) normalImage).getImage(), paintable.x, paintable.y, null);
			}
		}

		g.setClip(clip);
	}

	public boolean preferredCommandType(Command cmd) {
		for (Enumeration ct = commandTypes.elements(); ct.hasMoreElements();) {
			if (cmd.getCommandType() == ((Integer) ct.nextElement()).intValue()) {
				return true;
			}
		}
		return false;
	}

	public void addCommandType(int commandType) {
		commandTypes.addElement(new Integer(commandType));
	}

}
