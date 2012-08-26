package com.github.rakama.nami.theme;

public class Zebra extends Theme
{
    public Zebra()
    {
        super();

        for(int i=0;i<128;i++)
            r[i] = g[i] = b[i] = (byte)0;
        
        for(int i=128;i<256;i++)
            r[i] = g[i] = b[i] = (byte)255;
    }

    public double getSpeed()
    {
        return 4;
    }
    
    public boolean hasDithering()
    {
        return false;
    }
    
    public boolean hasAlpha()
    {
        return false;
    }
    
    public double getMultiplier()
    {
        return 32;
    }
    
    public double getNormalizer()
    {
        return 0.9;
    }
}