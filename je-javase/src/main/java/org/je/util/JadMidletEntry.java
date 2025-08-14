package org.je.util;


public class JadMidletEntry
{
  
  String name;
  String icon;
  String className;
  
  
  JadMidletEntry(String name, String icon, String className)
  {
    this.name = name;
    this.icon = icon;
    this.className = className;
  }
  
  
  public String getClassName()
  {
    return className;
  }
  
  
  public String getName()
  {
    return name;
  }
  
  
  // remove it later
  public String toString()
  {
    return name +"+"+ icon +"+"+ className;
  }
  
}
