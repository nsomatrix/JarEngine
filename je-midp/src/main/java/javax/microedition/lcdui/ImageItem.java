package javax.microedition.lcdui;

import org.je.device.DeviceFactory;
import org.je.device.ui.ImageStringItemUI;

public class ImageItem extends Item {

	public static final int LAYOUT_DEFAULT = 0;

	public static final int LAYOUT_LEFT = 1;

	public static final int LAYOUT_RIGHT = 2;

	public static final int LAYOUT_CENTER = 3;

	public static final int LAYOUT_NEWLINE_BEFORE = 0x100;

	public static final int LAYOUT_NEWLINE_AFTER = 0x200;

	Image img;

	String altText;

	private int appearanceMode;

	public ImageItem(String label, Image img, int layout, String altText) {
		this(label, img, layout, altText, ImageItem.PLAIN);
	}

	public ImageItem(String label, Image img, int layout, String altText, int appearanceMode) {
		super(label);
		this.altText = altText;
		this.appearanceMode = appearanceMode;
		super.setUI(DeviceFactory.getDevice().getUIFactory().createImageStringItemUI(this));

		// may throw IllegalArgumentException
		// (that is the intentended behaviour)
		setLayout(layout);
		if (appearanceMode != ImageItem.PLAIN && appearanceMode != ImageItem.HYPERLINK
				&& appearanceMode != ImageItem.BUTTON) {
			throw new IllegalArgumentException();
		}

		setImage(img);
	}

	public String getAltText() {
		return altText;
	}

	public int getAppearanceMode() {
		return this.appearanceMode;
	}

	public Image getImage() {
		return img;
	}

	public int getLayout() {
		return super.getLayout();
	}

	public void setAltText(String text) {
		altText = text;
	}

	public void setImage(Image img) {
		if (img != null && img.isMutable()) {
			img = Image.createImage(img);
		}
		this.img = img;
		repaint();
		
		((ImageStringItemUI) ui).setImage(img);
	}

	public void setLayout(int layout) {
		super.setLayout(layout);
	}

	int getHeight() {
		if (img == null) {
			return super.getHeight();
		} else {
			return super.getHeight() + img.getHeight();
		}
	}

	int paint(Graphics g) {
		super.paintContent(g);

		if (img != null) {
			g.translate(0, super.getHeight());
			if (layout == LAYOUT_DEFAULT || layout == LAYOUT_LEFT) {
				g.drawImage(img, 0, 0, Graphics.LEFT | Graphics.TOP);
			} else if (layout == LAYOUT_RIGHT) {
				g.drawImage(img, owner.getWidth(), 0, Graphics.RIGHT | Graphics.TOP);
			} else if (layout == LAYOUT_CENTER) {
				g.drawImage(img, owner.getWidth() / 2, 0, Graphics.HCENTER | Graphics.TOP);
			} else {
				g.drawImage(img, 0, 0, Graphics.LEFT | Graphics.TOP);
			}
			g.translate(0, -super.getHeight());
		}

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
