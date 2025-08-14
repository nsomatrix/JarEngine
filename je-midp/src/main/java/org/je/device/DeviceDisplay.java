package org.je.device;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

public interface DeviceDisplay 
{
	
	boolean flashBacklight(int duration);
	
	int getWidth();

	int getHeight();

	int getFullWidth();

	int getFullHeight();

	boolean isColor();
	
	boolean isFullScreenMode();

    int numAlphaLevels();

    int numColors();

	void repaint(int x, int y, int width, int height);

	void setScrollDown(boolean state);

	void setScrollUp(boolean state);
	
	Image createImage(int width, int height, boolean withAlpha, int fillColor);

	Image createImage(String name) throws IOException;

	Image createImage(Image source);

	Image createImage(byte[] imageData, int imageOffset, int imageLength);

	Image createImage(InputStream is) throws IOException;

	Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha);

	Image createImage(Image image, int x, int y, int width, int height, int transform);
	
	Graphics getGraphics(GameCanvas gameCanvas);
	
	void flushGraphics(GameCanvas gameCanvas, int x, int y, int width, int height);
	
}