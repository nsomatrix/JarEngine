package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;

/**
 *
 * @author Andres Navarro
 */

// i suppose this Class needs no comments 
public abstract class Layer {
    private int width;
    private int height;
    private int x;
    private int y;
    private boolean visible;
    
    Layer(int x, int y, int width, int height, boolean visible) {
        setSize(width, height);
        setPosition(x, y);
        setVisible(visible);
    }
    
    // package access to modify from Sprite
    void setSize(int width, int height) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException();

        this.width = width;
        this.height = height;
    }
    
    public final int getWidth() {
        return width;
    }
    
    public final int getHeight() {
        return height;
    }
    
    public final int getX() {
        return x;
    }
    
    public final int getY() {
        return y;
    }
    
    public final boolean isVisible() {
        return visible;
    }
    
    public void move(int dx, int dy) {
    	synchronized (this) {
	        x += dx;
	        y += dy;
    	}
    }
    
    public abstract void paint(Graphics g);
    
    public void setPosition(int x, int y) {
    	synchronized (this) {
	        this.x = x;
	        this.y = y;
    	}
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
