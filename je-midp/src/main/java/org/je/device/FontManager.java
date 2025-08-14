package org.je.device;

import javax.microedition.lcdui.Font;


public interface FontManager 
{
  void init();
  
  int charWidth(Font f, char ch);
  
  int charsWidth(Font f, char[] ch, int offset, int length);
  
  int getBaselinePosition(Font f);
  
  int getHeight(Font f);
  
  int stringWidth(Font f, String str);
  
  int substringWidth(Font f, String str, int offset, int len);
  
}
