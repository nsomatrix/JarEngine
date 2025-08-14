package org.je.device.impl;

import javax.microedition.lcdui.Image;


public class PositionedImage
{
    private Image image;

    private Rectangle rectangle;

    public PositionedImage(Image img, Rectangle arectangle)
    {
        image = img;
        rectangle = arectangle;
    }

    public Image getImage()
    {
        return image;
    }

    public Rectangle getRectangle()
    {
        return rectangle;
    }

}
