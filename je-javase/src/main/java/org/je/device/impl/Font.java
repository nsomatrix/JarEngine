package org.je.device.impl;

public interface Font {

	int charWidth(char ch);
	
	int charsWidth(char[] ch, int offset, int length);
	
	int getBaselinePosition();
	
	int getHeight();
	
	int stringWidth(String str);
	
}
