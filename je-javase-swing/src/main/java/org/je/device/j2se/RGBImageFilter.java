package org.je.device.j2se;


import org.je.device.DeviceFactory;
import org.je.device.impl.Color;



public class RGBImageFilter extends java.awt.image.RGBImageFilter
{

  private double Rr, Rg, Rb;
  private Color backgroundColor;
  private Color foregroundColor;
  

  public RGBImageFilter()
	{
    canFilterIndexColorModel = true;
    backgroundColor = 
        ((J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay()).getBackgroundColor();    
    foregroundColor = 
        ((J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay()).getForegroundColor();    
    Rr = foregroundColor.getRed() - backgroundColor.getRed();
    Rg = foregroundColor.getGreen() - backgroundColor.getGreen();
    Rb = foregroundColor.getBlue() - backgroundColor.getBlue();
  }


  public int filterRGB (int x, int y, int rgb)
	{
    int a = (rgb & 0xFF000000);
    int r = (rgb & 0x00FF0000) >>> 16;
    int g = (rgb & 0x0000FF00) >>> 8;
    int b = (rgb & 0x000000FF);

    if (Rr > 0) {
      r = (int) (r * Rr) / 255 + backgroundColor.getRed();
    } else {
      r = (int) (r * -Rr) / 255 + foregroundColor.getRed();
    }
    if (Rr > 0) {
      g = (int) (g * Rg) / 255 + backgroundColor.getGreen();
    } else {
      g = (int) (g * -Rg) / 255 + foregroundColor.getGreen();
    }
    if (Rr > 0) {
      b = (int) (b * Rb) / 255 + backgroundColor.getBlue();
    } else {
      b = (int) (b * -Rb) / 255 + foregroundColor.getBlue();
    }

    return a | (r << 16) | (g << 8) | b;
  }

}
