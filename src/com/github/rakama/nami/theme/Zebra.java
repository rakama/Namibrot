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
        
        setSpeed(4);
        setDithering(false);
        setMultiplier(32);
        setNormalizer(0.9);
    }
}