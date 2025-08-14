package javax.microedition.lcdui;

import org.je.device.DeviceFactory;
import org.je.device.ui.AlertUI;


public class Alert extends Screen
{
	public static final int FOREVER = -2;

	ImageStringItem alertContent;
	AlertType type;
	// XXX actually the label for this should be an empty String
	// but the implementation is free to show a label
	// so the label should be set to "" and on the Command render
	// we should check if it was the dismiss command and
	// display a predefined label...
	public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);
	int time;
	Gauge indicator;

	// this is for alertListener
	static Displayable nextDisplayable;
	static CommandListener defaultListener = new CommandListener()
	{
		public void commandAction(Command cmd, Displayable d)
		{
			// XXX if nextDisplayable == null
			// then it means that this Alert was
			// setted current when there was not a previous
			// Displayable (ie immediately after MIDlet start)
			// in that particular case the initial state should 
			// be restored
			((Alert) d).currentDisplay.setCurrent(nextDisplayable);
			nextDisplayable = null;
		}
	};
	
	public Alert(String title)
	{
		this(title, null, null, null);
	}


	public Alert(String title, String alertText, Image alertImage, AlertType alertType)
	{
		super(title);
		super.setUI(DeviceFactory.getDevice().getUIFactory().createAlertUI(this));
		
		setTimeout(getDefaultTimeout());
		setString(alertText);
		setImage(alertImage);
		setType(alertType);
		super.addCommand(Alert.DISMISS_COMMAND);
		super.setCommandListener(defaultListener);
	}


	public void addCommand(Command cmd)
	{
		if (cmd == Alert.DISMISS_COMMAND) {
			return;
		} else {
			super.addCommand(cmd);
			super.removeCommand(Alert.DISMISS_COMMAND);
		}
	}

	public void removeCommand(Command cmd) {
		if (cmd == Alert.DISMISS_COMMAND) {
			return;
		} else {
			super.removeCommand(cmd);
			if (getCommands().size() == 0) {
				super.addCommand(Alert.DISMISS_COMMAND);
			}
		}
	}

	public int getDefaultTimeout()
	{
		return Alert.FOREVER;
	}


	public String getString()
	{
		return alertContent.getText();
	}


	public int getTimeout()
	{
		return time;
	}


  public AlertType getType()
  {
    return type;
  }
  
  
  public void setType(AlertType type)
	{
		this.type = type;
		repaint();
	}


	public void setCommandListener(CommandListener l)
	{
		if (l == null)
			l = defaultListener;
		super.setCommandListener(l);
	}


	public Image getImage()
	{
		return alertContent.getImage();
	}


	public void setImage(Image img)
	{
		if (alertContent == null) {
			alertContent = new ImageStringItem(null, img, null);
		}

		if (img != null && img.isMutable()) {
	      img = Image.createImage(img);
	    }
	    alertContent.setImage(img);
	    repaint();
	}
	
	public Gauge getIndicator() {
		return indicator;
	}
	
	public void setIndicator(Gauge indicator) {
		if (indicator == null) {
			if (this.indicator != null)
				this.indicator.setOwner(null);
			this.indicator = null;
			repaint();
			return;
		}
		
		// validate the gauge against the restrictrions
		if (indicator.getLayout() != 0 || 
					indicator.getLabel() != null ||
					indicator.prefHeight != -1 ||
					indicator.prefWidth != -1 ||
					indicator.commandListener != null ||
					indicator.isInteractive() ||
					indicator.getOwner() != null ||
					!indicator.commands.isEmpty()) {
			// if the command vector is empty then
			// there is no default command
			throw new IllegalArgumentException(
					"This gauge cannot be added to an Alert");
		}
		indicator.setOwner(this);
		this.indicator = indicator;
		repaint();
	}


	public void setString(String str)
	{
		if (ui.getClass().getName().equals("org.je.android.device.ui.AndroidAlertUI")) {
			((AlertUI) ui).setString(str);
		}
		
		if (alertContent == null) {
			alertContent = new ImageStringItem(null, null, str);
		}

		alertContent.setText(str);
		repaint();
	}


	public void setTimeout(int time)
	{
	    if (time != FOREVER && time <= 0) {
	      throw new IllegalArgumentException();
	    }
	    // XXX stop timeout thread!
		if (time != FOREVER && getCommands().size() > 1)
			time = FOREVER;
	    
	    this.time = time;
	}


	private int getContentHeight()
	{
		return alertContent.getHeight();
	}


	int paintContent(Graphics g)
	{
		return alertContent.paint(g);
	}


	void showNotify()
	{
		super.showNotify();
		viewPortY = 0;
	}


	int traverse(int gameKeyCode, int top, int bottom)
	{
		Font f = Font.getDefaultFont();

		if (gameKeyCode == 1 && top != 0) {
			return -f.getHeight();
		}
		if (gameKeyCode == 6 && bottom < getContentHeight()) {
			return f.getHeight();
		}

		return 0;
	}

}
