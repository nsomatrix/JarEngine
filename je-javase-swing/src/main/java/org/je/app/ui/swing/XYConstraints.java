package org.je.app.ui.swing;

import java.awt.Rectangle;

public class XYConstraints implements Cloneable {
	int x;

	int y;

	int width; // <= 0 means use the components's preferred size

	int height; // <= 0 means use the components's preferred size

	public XYConstraints() {
		this(0, 0, 0, 0);
	}

	public XYConstraints(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public XYConstraints(Rectangle rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Returns the hashcode for this XYConstraints.
	 */
	public int hashCode() {
		return x ^ (y * 37) ^ (width * 43) ^ (height * 47);
	}

	/**
	 * Checks whether two XYConstraints are equal.
	 */
	public boolean equals(Object that) {
		if (that instanceof XYConstraints) {
			XYConstraints other = (XYConstraints) that;
			return other.x == x && other.y == y && other.width == width && other.height == height;
		}
		return false;
	}

	public Object clone() {
		return new XYConstraints(x, y, width, height);
	}

	public String toString() {
		return "XYConstraints[" + x + "," + y + "," + width + "," + height + "]";
	}
}