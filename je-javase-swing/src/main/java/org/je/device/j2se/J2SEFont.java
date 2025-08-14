package org.je.device.j2se;

import java.awt.Font;

public interface J2SEFont extends org.je.device.impl.Font {

	Font getFont();
	
	void setAntialiasing(boolean antialiasing);
	
}
