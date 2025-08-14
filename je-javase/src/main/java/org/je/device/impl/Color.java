package org.je.device.impl;

public class Color
{
    private int value;
    
    
    public Color(int value)
    {
        this.value = value;
    }
    

    public int getRed()
    {
        return (value >> 16) & 0xff;
    }
    
    
    public int getGreen()
    {
        return (value >> 8) & 0xff;
    }

    
    public int getBlue()
    {
        return value & 0xff;
    }
    
    
    public int getRGB()
    {
        return value;
    }
    
}
