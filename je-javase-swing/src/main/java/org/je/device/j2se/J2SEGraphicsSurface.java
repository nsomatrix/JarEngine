package org.je.device.j2se;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

public class J2SEGraphicsSurface {
	
    private static final DirectColorModel ALPHA_COLOR_MODEL = 
        new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);

    private static final DirectColorModel NO_ALPHA_COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
    
    private int[] imageData;
	
	private BufferedImage image;
	
	private Graphics2D graphics;

	public J2SEGraphicsSurface(int width, int height, boolean withAlpha, int fillColor) {
        this.imageData = new int[width * height];
        DataBuffer dataBuffer = new DataBufferInt(this.imageData, width * height);            
        if (withAlpha) {
            SampleModel sampleModel = new SinglePixelPackedSampleModel(
                    DataBuffer.TYPE_INT, width, height, new int[] { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 });             
            WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0,0));  
            this.image = new BufferedImage(ALPHA_COLOR_MODEL, raster, true, new Hashtable());
        } else {
            SampleModel sampleModel = new SinglePixelPackedSampleModel(
                    DataBuffer.TYPE_INT, width, height, new int[] { 0xff0000, 0x00ff00, 0x0000ff });             
            WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0,0));  
            this.image = new BufferedImage(NO_ALPHA_COLOR_MODEL, raster, false, new Hashtable());
        }
		this.graphics = this.image.createGraphics();
		this.graphics.setColor(new Color(fillColor));
		this.graphics.fillRect(0, 0, width, height);
	}

	public Graphics2D getGraphics() {
		return graphics;
	}

	public BufferedImage getImage() {
		return image;
	}

	public int[] getImageData() {
		return imageData;
	}

}
