package org.je.device.impl;

public abstract class Shape implements Cloneable {
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public abstract Rectangle getBounds();
	
	public abstract boolean contains(int x, int y);
	
}
