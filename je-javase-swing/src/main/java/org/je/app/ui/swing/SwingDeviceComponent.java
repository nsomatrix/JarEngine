package org.je.app.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Command;
import javax.swing.JPanel;

import org.je.DisplayAccess;
import org.je.DisplayComponent;
import org.je.MIDletAccess;
import org.je.MIDletBridge;
import org.je.app.Common;
import org.je.device.Device;
import org.je.device.DeviceFactory;
import org.je.device.impl.DeviceDisplayImpl;
import org.je.device.impl.Rectangle;
import org.je.device.impl.SoftButton;
import org.je.device.impl.ui.CommandManager;
import org.je.device.j2se.J2SEButton;
import org.je.device.j2se.J2SEDeviceButtonsHelper;
import org.je.device.j2se.J2SEDeviceDisplay;
import org.je.device.j2se.J2SEImmutableImage;
import org.je.device.j2se.J2SEInputMethod;
import org.je.log.Logger;

public class SwingDeviceComponent extends JPanel implements KeyListener, InputMethodListener, InputMethodRequests {

	private static final long serialVersionUID = 1L;

	SwingDisplayComponent dc;

	J2SEButton prevOverButton;

	J2SEButton overButton;

	J2SEButton pressedButton;

	private boolean mouseButtonDown = false;

	private static class MouseRepeatedTimerTask extends TimerTask {

		private static final int DELAY = 100;

		Timer timer;

		Component source;

		J2SEButton button;

		J2SEInputMethod inputMethod;

		static MouseRepeatedTimerTask task;

		static void schedule(Component source, J2SEButton button, J2SEInputMethod inputMethod) {
			if (task != null) {
				task.cancel();
			}
			task = new MouseRepeatedTimerTask();
			task.source = source;
			task.button = button;
			task.inputMethod = inputMethod;
			task.timer = new Timer();
			task.timer.scheduleAtFixedRate(task, 5 * DELAY, DELAY);
		}

		static void stop() {
			if (task != null) {
				task.inputMethod = null;
				if (task.timer != null) {
					task.timer.cancel();
				}
				task.cancel();
				task = null;
			}
		}

		public static void mouseReleased() {
			if ((task != null) && (task.inputMethod != null)) {
				task.inputMethod.buttonReleased(task.button, '\0');
				stop();
			}

		}

		public void run() {
			if (inputMethod != null) {
				inputMethod.buttonPressed(button, '\0');
			}
		}

	}

	private MouseAdapter mouseListener = new MouseAdapter() {

		public void mousePressed(MouseEvent e) {
			requestFocus();
			mouseButtonDown = true;

			MouseRepeatedTimerTask.stop();
			if (MIDletBridge.getCurrentMIDlet() == null) {
				return;
			}

			Device device = DeviceFactory.getDevice();
			J2SEInputMethod inputMethod = (J2SEInputMethod) device.getInputMethod();
			// if the displayable is in full screen mode, we should not
			// invoke any associated commands, but send the raw key codes
			// instead
			boolean fullScreenMode = device.getDeviceDisplay().isFullScreenMode();

			pressedButton = J2SEDeviceButtonsHelper.getSkinButton(e);
			if (pressedButton != null) {
				if (pressedButton instanceof SoftButton && !fullScreenMode) {
					Command cmd = ((SoftButton) pressedButton).getCommand();
					if (cmd != null) {
						MIDletAccess ma = MIDletBridge.getMIDletAccess();
						if (ma == null) {
							return;
						}
						DisplayAccess da = ma.getDisplayAccess();
						if (da == null) {
							return;
						}
						if (cmd.equals(CommandManager.CMD_MENU)) {
							CommandManager.getInstance().commandAction(cmd);
						} else {
							da.commandAction(cmd, da.getCurrent());
						}
					}
				} else {
					inputMethod.buttonPressed(pressedButton, '\0');
					MouseRepeatedTimerTask.schedule(SwingDeviceComponent.this, pressedButton, inputMethod);
				}
				// optimize for some video cards.
				repaint(pressedButton.getShape().getBounds());
			}
		}

		public void mouseReleased(MouseEvent e) {
			mouseButtonDown = false;
			MouseRepeatedTimerTask.stop();

			if (MIDletBridge.getCurrentMIDlet() == null) {
				return;
			}

			Device device = DeviceFactory.getDevice();
			J2SEInputMethod inputMethod = (J2SEInputMethod) device.getInputMethod();
			if (pressedButton != null) {
				inputMethod.buttonReleased(pressedButton, '\0');
			}
			// optimize for some video cards.
			if (pressedButton != null) {
				repaint(pressedButton.getShape().getBounds());
			} else {
				repaint();
			}
			prevOverButton = pressedButton;
			pressedButton = null;
		}

	};

	private MouseMotionListener mouseMotionListener = new MouseMotionListener() {

		public void mouseDragged(MouseEvent e) {
			mouseMoved(e);
		}

		public void mouseMoved(MouseEvent e) {
			if (mouseButtonDown && pressedButton == null) {
				return;
			}

			prevOverButton = overButton;
			overButton = J2SEDeviceButtonsHelper.getSkinButton(e);
			if (overButton != prevOverButton) {
				// optimize for some video cards.
				if (prevOverButton != null) {
					MouseRepeatedTimerTask.mouseReleased();
					pressedButton = null;
					repaint(prevOverButton.getShape().getBounds());
				}
				if (overButton != null) {
					repaint(overButton.getShape().getBounds());
				}
			} else if (overButton == null) {
				MouseRepeatedTimerTask.mouseReleased();
				pressedButton = null;
				if (prevOverButton != null) {
					repaint(prevOverButton.getShape().getBounds());
				}
			}
		}

	};

	public SwingDeviceComponent() {
		dc = new SwingDisplayComponent(this);
		setLayout(new BorderLayout());

		addMouseListener(mouseListener);
		addMouseMotionListener(mouseMotionListener);
		
 		//Input methods support begin
 		enableInputMethods(true);
 		addInputMethodListener(this);
 		//End

 		// Add the display component to fill the parent
 		add(dc, BorderLayout.CENTER);
	}

	public DisplayComponent getDisplayComponent() {
		return dc;
	}

	public void init() {
		dc.init();

		remove(dc);
		add(dc, BorderLayout.CENTER);

		revalidate();
	}

	private void repaint(Rectangle r) {
		repaint(r.x, r.y, r.width, r.height);
	}

	// Mouse coordinates functionality removed

	
 	private static final AttributedCharacterIterator EMPTY_TEXT = new AttributedString("").getIterator();
 	
 	public void caretPositionChanged(InputMethodEvent event) {
 		repaint();
 	}
 	
 	public void inputMethodTextChanged(InputMethodEvent event) {
 		StringBuffer committedText = new StringBuffer();
 		AttributedCharacterIterator text = event.getText();
 		Device device = DeviceFactory.getDevice();
 		J2SEInputMethod inputMethod = (J2SEInputMethod)device.getInputMethod();
 		if (text != null) {
 			int toCopy = event.getCommittedCharacterCount();
 			char c = text.first();
 			while (toCopy-- > 0) {
 				committedText.append(c);
 				c = text.next();
 			}
 			if (committedText.length() > 0) {
 				inputMethod.clipboardPaste(committedText.toString());
 			}
 		}
 		repaint();
 	}
 	
 	public InputMethodRequests getInputMethodRequests() {
 		return this;
 	}
 	
 	public int getCommittedTextLength() {
 		return 0;
 	}
 	
 	public int getInsertPositionOffset() {
 		return getCommittedTextLength();
 	}
 	
 	public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
 		return null;
 	}
 	
 	public java.awt.Rectangle getTextLocation(TextHitInfo offset) {
 		return null;
 	}
 	
 	public TextHitInfo getLocationOffset(int x, int y) {
 		return null;
 	}
 	
 	public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
 		return EMPTY_TEXT;
 	}
 	
 	public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
 		return null;
 	}
 	
 	//Input method support end

	public void keyTyped(KeyEvent ev) {
		if (MIDletBridge.getCurrentMIDlet() == null) {
			return;
		}

		J2SEInputMethod inputMethod = ((J2SEInputMethod) DeviceFactory.getDevice().getInputMethod());
		J2SEButton button = inputMethod.getButton(ev);
		if (button != null) {
			inputMethod.buttonTyped(button);
		}
	}

	public void keyPressed(KeyEvent ev) {
		if (MIDletBridge.getCurrentMIDlet() == null) {
			return;
		}

		Device device = DeviceFactory.getDevice();
		J2SEInputMethod inputMethod = (J2SEInputMethod) device.getInputMethod();

		if (ev.getKeyCode() == KeyEvent.VK_V && (ev.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = clipboard.getContents(null);
			if (transferable != null) {
				try {
					Object data = transferable.getTransferData(DataFlavor.stringFlavor);
					if (data instanceof String) {
						inputMethod.clipboardPaste((String) data);
					}
				} catch (UnsupportedFlavorException ex) {
					Logger.error(ex);
				} catch (IOException ex) {
					Logger.error(ex);
				}
			}
			return;
		}

		switch (ev.getKeyCode()) {
		case KeyEvent.VK_ALT:
		case KeyEvent.VK_CONTROL:
		case KeyEvent.VK_SHIFT:
			return;
		case 0:
			// Don't know what is the case was intended for but this may be
			// national keyboard letter, so let it work
			if (ev.getKeyChar() == '\0') {
				return;
			}
		}

		char keyChar = '\0';
		if (ev.getKeyChar() >= 32 && ev.getKeyChar() != 65535) {
			keyChar = ev.getKeyChar();
		}
		J2SEButton button = inputMethod.getButton(ev);
		if (button != null) {
			pressedButton = button;
			// numeric keypad functions as hot keys for buttons only
			if ((ev.getKeyCode() >= KeyEvent.VK_NUMPAD0) && (ev.getKeyCode() <= KeyEvent.VK_NUMPAD9)) {
				keyChar = '\0';
			}
			// soft buttons
			if ((ev.getKeyCode() >= KeyEvent.VK_F1) && (ev.getKeyCode() <= KeyEvent.VK_F12)) {
				keyChar = '\0';
			}
			org.je.device.impl.Shape shape = button.getShape();
			if (shape != null) {
				repaint(shape.getBounds());
			}
		} else {
			// Logger.debug0x("no button for KeyCode", ev.getKeyCode());
		}
		inputMethod.buttonPressed(button, keyChar);
	}

	public void keyReleased(KeyEvent ev) {
		if (MIDletBridge.getCurrentMIDlet() == null) {
			return;
		}

		switch (ev.getKeyCode()) {
		case KeyEvent.VK_ALT:
		case KeyEvent.VK_CONTROL:
		case KeyEvent.VK_SHIFT:
			return;
		case 0:
			// Don't know what is the case was intended for but this may be
			// national keyboard letter, so let it work
			if (ev.getKeyChar() == '\0') {
				return;
			}
		}

		Device device = DeviceFactory.getDevice();
		J2SEInputMethod inputMethod = (J2SEInputMethod) device.getInputMethod();

		char keyChar = '\0';
		if (ev.getKeyChar() >= 32 && ev.getKeyChar() != 65535) {
			keyChar = ev.getKeyChar();
		}
		// numeric keypad functions as hot keys for buttons only
		if ((ev.getKeyCode() >= KeyEvent.VK_NUMPAD0) && (ev.getKeyCode() <= KeyEvent.VK_NUMPAD9)) {
			keyChar = '\0';
		}
		// soft buttons
		if ((ev.getKeyCode() >= KeyEvent.VK_F1) && (ev.getKeyCode() <= KeyEvent.VK_F12)) {
			keyChar = '\0';
		}
		// Logger.debug0x("keyReleased [" + keyChar + "]", keyChar);
		inputMethod.buttonReleased(inputMethod.getButton(ev), keyChar);

		prevOverButton = pressedButton;
		pressedButton = null;
		if (prevOverButton != null) {
			org.je.device.impl.Shape shape = prevOverButton.getShape();
			if (shape != null) {
				repaint(shape.getBounds());
			}
		}
	}

	public MouseListener getDefaultMouseListener() {
		return mouseListener;
	}

	public MouseMotionListener getDefaultMouseMotionListener() {
		return mouseMotionListener;
	}

	protected void paintComponent(Graphics g) {
		Device device = DeviceFactory.getDevice();
		if (device == null) {
			g.setColor(java.awt.Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			return;
		}

		DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) device.getDeviceDisplay();
		if (deviceDisplay.isResizable()) {
			return; // Let the display component handle the painting
		}

		// Aspect-ratio preserving scaling for non-resizable devices
		javax.microedition.lcdui.Image normalImage = device.getNormalImage();
		if (normalImage != null) {
			Image gameImage = ((J2SEImmutableImage) normalImage).getImage();
			int imgWidth = gameImage.getWidth(null);
			int imgHeight = gameImage.getHeight(null);
			int panelWidth = getWidth();
			int panelHeight = getHeight();
			double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
			int drawWidth = (int)(imgWidth * scale);
			int drawHeight = (int)(imgHeight * scale);
			int x = (panelWidth - drawWidth) / 2;
			int y = (panelHeight - drawHeight) / 2;
			g.setColor(java.awt.Color.BLACK);
			g.fillRect(0, 0, panelWidth, panelHeight);
			g.drawImage(gameImage, x, y, drawWidth, drawHeight, null);
		} else {
			g.setColor(java.awt.Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		// Draw over/pressed buttons if they exist
		javax.microedition.lcdui.Image overImage = device.getOverImage();
		if (overImage != null) {
			Image gameImage = ((J2SEImmutableImage) overImage).getImage();
			g.drawImage(gameImage, 0, 0, null);
		}

		javax.microedition.lcdui.Image pressedImage = device.getPressedImage();
		if (pressedImage != null) {
			Image gameImage = ((J2SEImmutableImage) pressedImage).getImage();
			g.drawImage(gameImage, 0, 0, null);
		}
	}

    @Override
    public Dimension getPreferredSize() {
		Device device = DeviceFactory.getDevice();
		if (device == null) {
			return new Dimension(0, 0);
		}

		DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
		if (deviceDisplay.isResizable()) {
			return new Dimension(deviceDisplay.getFullWidth(), deviceDisplay.getFullHeight());
		} else {
			javax.microedition.lcdui.Image img = device.getNormalImage();
			if (img != null) {
				return new Dimension(img.getWidth(), img.getHeight());
			} else {
				return new Dimension(0, 0);
			}
		}
	}

    @Override
    public Dimension getMinimumSize() {
		// Ensure panel never forces status bar off-screen
		return new Dimension(0, 0);
	}

}
