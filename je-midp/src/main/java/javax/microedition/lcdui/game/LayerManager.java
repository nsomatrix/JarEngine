package javax.microedition.lcdui.game;

import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/**
 *
 * @author Andres Navarro
 */
/*
 * This class is deceptively simply, most of the
 * methods are calls to an underlying Vector
 */
public class LayerManager {
    private Vector layers;
    private int viewX, viewY, viewW, viewH;
    
    public LayerManager() {
        layers = new Vector();
        viewX = viewY = 0;
        viewW = viewH = Integer.MAX_VALUE;
    }
    
    public void append(Layer layer) {
    	synchronized (this) {
	        if (layer == null)
	            throw new NullPointerException();
	        layers.add(layer);
    	}
    }
            
    public Layer getLayerAt(int i) {
        // needs not be synchronized
        return (Layer) layers.get(i);
    }
    
    public int getSize() {
        // needs not be synchronized
        return layers.size();
    }
    
    public void insert(Layer layer, int i) {
    	synchronized (this) {
	        if (layer == null)
	            throw new NullPointerException();
	        layers.insertElementAt(layer, i);
    	}
    }
    
    public void remove(Layer layer) {
    	synchronized (this) {
	        if (layer == null)
	            throw new NullPointerException();
	        layers.remove(layer);
    	}
    }
    
    public void setViewWindow(int x, int y, int width, int height) {
    	synchronized (this) {
	        if (width < 0 || height < 0)
	            throw new IllegalArgumentException();
	        viewX = x;
	        viewY = y;
	        viewW = width;
	        viewH = height;
    	}
    }
    
    public void paint(Graphics g, int x, int y) {
		synchronized (this) {
			if (g == null)
				throw new NullPointerException();

			int clipX = g.getClipX();
			int clipY = g.getClipY();
			int clipW = g.getClipWidth();
			int clipH = g.getClipHeight();
			g.translate(x - viewX, y - viewY);
			g.clipRect(viewX, viewY, viewW, viewH);
			for (int i = getSize(); --i >= 0;) {
				Layer comp = getLayerAt(i);
				if (comp.isVisible()) {
					comp.paint(g);
				}
			}
			g.translate(-x + viewX, -y + viewY);
			g.setClip(clipX, clipY, clipW, clipH);
		}
	}
    
}
