package org.je.device.j2se;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.PixelGrabber;

import org.je.device.MutableImage;
import org.je.log.Logger;


public class J2SEMutableImage extends MutableImage
{
	private J2SEGraphicsSurface graphicsSurface;
	private PixelGrabber grabber = null;
	private int[] pixels;


	public J2SEMutableImage(int width, int height, boolean withAlpha, int fillColor)
	{
		graphicsSurface = new J2SEGraphicsSurface(width, height, withAlpha, fillColor);
	}


	public javax.microedition.lcdui.Graphics getGraphics()
	{
        Graphics2D g = graphicsSurface.getGraphics();
        g.setTransform(new AffineTransform());
        g.setClip(0, 0, getWidth(), getHeight());
        J2SEDisplayGraphics displayGraphics = new J2SEDisplayGraphics(graphicsSurface);
		displayGraphics.setColor(0x00000000);
		displayGraphics.translate(-displayGraphics.getTranslateX(), -displayGraphics.getTranslateY());
		
		return displayGraphics;
	}


	public boolean isMutable()
	{
		return true;
	}


	public int getHeight()
	{
		return graphicsSurface.getImage().getHeight();
	}


	public java.awt.Image getImage()
	{
		return graphicsSurface.getImage();
	}


	public int getWidth()
	{
		return graphicsSurface.getImage().getWidth();
	}


	public int[] getData()
	{
		if (grabber == null) {
			pixels = new int[getWidth() * getHeight()];
			grabber = new PixelGrabber(graphicsSurface.getImage(), 0, 0, getWidth(), getHeight(), pixels, 0, getWidth());
		}

		try {
			grabber.grabPixels();
		} catch (InterruptedException e) {
			Logger.error(e);
		}

		return pixels;
	}

    public void getRGB(int []argb, int offset, int scanlength,
            int x, int y, int width, int height) {

        if (width <= 0 || height <= 0)
            return;
        if (x < 0 || y < 0 || x + width > getWidth() || y + height > getHeight())
            throw new IllegalArgumentException("Specified area exceeds bounds of image");
        if ((scanlength < 0? -scanlength:scanlength) < width)
            throw new IllegalArgumentException("abs value of scanlength is less than width");
        if (argb == null)
            throw new NullPointerException("null rgbData");
        if (offset < 0 || offset + width > argb.length)
            throw new ArrayIndexOutOfBoundsException();
        if (scanlength < 0) {
            if (offset + scanlength*(height-1) < 0)
                throw new ArrayIndexOutOfBoundsException();
        } else {
            if (offset + scanlength*(height-1) + width > argb.length)
                throw new ArrayIndexOutOfBoundsException();
        }

        try {
            (new PixelGrabber(graphicsSurface.getImage(), x, y, width, height, argb, offset, scanlength)).grabPixels();
        } catch (InterruptedException e) {
            Logger.error(e);
        }
    }

}
