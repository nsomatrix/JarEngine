package javax.microedition.lcdui;

import org.je.device.DeviceFactory;
import org.je.device.ui.ImageStringItemUI;

public class StringItem extends Item {

	private StringComponent stringComponent;
	private int appearanceMode;

	public StringItem(String label, String text) {
		this(label, text, PLAIN);
	}
	
	public StringItem(String label, String text, int appearanceMode) {
		super(label);
		this.appearanceMode = appearanceMode;
		super.setUI(DeviceFactory.getDevice().getUIFactory().createImageStringItemUI(this));
		
		stringComponent = new StringComponent();
		setText(text);
	}
	
	public int getAppearanceMode() {
		return appearanceMode;
	}
	
	public Font getFont() {
    	// TODO implement
		return Font.getDefaultFont();
	}
	
	public void setFont(Font font) {
    	// TODO implement
	}

	public void setPreferredSize(int width, int height) {
    	// TODO implement
	}
	
	public String getText() {
		return stringComponent.getText();
	}

	public void setText(String text) {
		if (ui.getClass().getName().equals("org.je.android.device.ui.AndroidImageStringItemUI")) {
			((ImageStringItemUI) ui).setText(text);
		}

		stringComponent.setText(text);
		repaint();
	}

	int getHeight() {
		return super.getHeight() + stringComponent.getHeight();
	}

	int paint(Graphics g) {
		super.paintContent(g);

		g.translate(0, super.getHeight());
		stringComponent.paint(g);
		g.translate(0, -super.getHeight());

		return getHeight();
	}

	int traverse(int gameKeyCode, int top, int bottom, boolean action) {
		Font f = Font.getDefaultFont();

		if (gameKeyCode == Canvas.UP) {
			if (top > 0) {
				if ((top % f.getHeight()) == 0) {
					return -f.getHeight();
				} else {
					return -(top % f.getHeight());
				}
			} else {
				return Item.OUTOFITEM;
			}
		}
		if (gameKeyCode == Canvas.DOWN) {
			if (bottom < getHeight()) {
				if (getHeight() - bottom < f.getHeight()) {
					return getHeight() - bottom;
				} else {
					return f.getHeight();
				}
			} else {
				return Item.OUTOFITEM;
			}
		}

		return 0;
	}

}
