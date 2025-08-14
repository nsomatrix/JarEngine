package javax.microedition.lcdui;

import org.je.DisplayAccess;
import org.je.MIDletBridge;


public class Ticker
{

  static int PAINT_TIMEOUT = 250;
  static int PAINT_MOVE = 5;
  static int PAINT_GAP = 10;
  
  Ticker instance = null;

  String text;
  int textPos = 0;
  int resetTextPosTo = -1;


  public Ticker(String str)
  {
    if (str == null) {
      throw new NullPointerException();
    }
    instance = this;
    
    text = str;
  }


  public String getString()
  {
    return text;
  }


  public void setString(String str)
  {
    if (str == null) {
      throw new NullPointerException();
    }
    text = str;
  }
  

  int getHeight()
  {
    return Font.getDefaultFont().getHeight();
  }
  
  
  int paintContent(Graphics g)
  {
		Font f = Font.getDefaultFont();
    
    synchronized (instance) {
      int stringWidth = f.stringWidth(text) + PAINT_GAP;
      g.drawString(text, textPos, 0, Graphics.LEFT | Graphics.TOP);
      int xPos = textPos + stringWidth;
      DisplayAccess da = MIDletBridge.getMIDletAccess().getDisplayAccess();
      while (xPos < da.getCurrent().getWidth()) {
        g.drawString(text, xPos, 0, Graphics.LEFT | Graphics.TOP);
        xPos += stringWidth;
      }
      if (textPos + stringWidth < 0) {
        resetTextPosTo = textPos + stringWidth;
      }
    }
    
    return f.getHeight();
  }
  
}