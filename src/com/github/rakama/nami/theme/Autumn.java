package com.github.rakama.nami.theme;

public class Autumn extends Theme
{
    public Autumn()
    {
        super();
        
        for(int i=1;i<256;i++)
        {
            r[i] = (byte)(Math.abs(128 - ((i + 0) % 256)) * 1.9);  
            g[i] = (byte)(Math.abs(128 - ((i + 32) % 256)) * 1.9);  
            b[i] = (byte)(Math.abs(128 - ((i + 64) % 256)) * 1.9);
        }
    }
    
    public double getSpeed()
    {
        return 2;
    }
    
    public boolean hasDithering()
    {
        return true;
    }
    
    public boolean hasAlpha()
    {
        return false;
    }
}