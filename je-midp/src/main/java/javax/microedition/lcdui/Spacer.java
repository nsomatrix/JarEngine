package javax.microedition.lcdui;

import org.je.CustomItemAccess;
import org.je.device.DeviceFactory;

public class Spacer extends Item {
	
	private int minWidth;
	
	private int minHeight;
	
	private SpacerCustomItem customItem;
	
	public Spacer(int minWidth, int minHeight) {
		super(null);
		this.customItem = new SpacerCustomItem();
		super.setUI(DeviceFactory.getDevice().getUIFactory().createCustomItemUI(new CustomItemAccess() {

			public CustomItem getCustomItem() {
				return customItem;
			}

			public int getPrefContentWidth(int height) {
				return customItem.getPrefContentWidth(height);
			}
			
			public int getPrefContentHeight(int width) {
				return customItem.getPrefContentHeight(width);
			}

			public void paint(Graphics g, int w, int h) {
				customItem.paint(g, w, h);
			}

		}));
		setMinimumSize(minWidth, minHeight);
	}

	public void setLabel(String label) {
		throw new IllegalStateException("Spacer items can't have labels");
	}

	public void addCommand(Command cmd) {
		throw new IllegalStateException("Spacer items can't have commands");
	}
	
	public void setDefaultCommand(Command cmd) {
		throw new IllegalStateException("Spacer items can't have commands");
	}
	
	public void setMinimumSize(int minWidth, int minHeight) {
		if (minWidth < 0 || minHeight < 0) {
			throw new IllegalArgumentException();
		}
		
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}
	
	// Item methods
	int paint(Graphics g) {
		return 0;
	}
	
	private class SpacerCustomItem extends CustomItem {

		protected SpacerCustomItem() {
			super(null);
		}

		protected int getMinContentWidth() {
			return minWidth;
		}

		protected int getMinContentHeight() {
			return minHeight;
		}

		protected int getPrefContentWidth(int height) {
			return minWidth;
		}

		protected int getPrefContentHeight(int width) {
			return minHeight;
		}

		protected void paint(Graphics g, int w, int h) {
		}
		
	}

}
