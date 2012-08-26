package com.github.rakama.nami.theme;

public class Bubblegum extends Theme
{
    final double[] rDelta, gDelta, bDelta;
    final double[] rFloat, gFloat, bFloat;
    int offset;
    
    public Bubblegum()
    {
        super();

        float colorSpeed = 0.5f;
        
        rFloat = new double[16];
        gFloat = new double[16];
        bFloat = new double[16];
        rDelta = new double[16];
        gDelta = new double[16];
        bDelta = new double[16];
        
        for(int i=0; i<16; i++)
        {
            rFloat[i] = Math.random()*256;
            gFloat[i] = Math.random()*256;
            bFloat[i] = Math.random()*256;
            rDelta[i] = Math.random() > 0.5 ? colorSpeed : -colorSpeed;
            gDelta[i] = Math.random() > 0.5 ? colorSpeed : -colorSpeed;
            bDelta[i] = Math.random() > 0.5 ? colorSpeed : -colorSpeed;
        }
        
        for(int i=1;i<256;i++)
        {
            r[i] = (byte)rFloat[i>>4];
            g[i] = (byte)gFloat[i>>4];
            b[i] = (byte)bFloat[i>>4];
        }
        
        r[0] = g[0] = b[0] = 0;
    }
    
    protected void cycleColors()
    {  
        offset--;
        if(offset < 0)
            offset = 255;

        final int minbrightness = 0;
        final int maxbrightness = 255;
        final float switchprob = 0.005f;

        for(int j=0; j<16; j++)
        {
            rDelta[j] = Math.random() > switchprob ? rDelta[j] : -rDelta[j];
            gDelta[j] = Math.random() > switchprob ? gDelta[j] : -gDelta[j];
            bDelta[j] = Math.random() > switchprob ? bDelta[j] : -bDelta[j];
            
            rFloat[j] += rDelta[j]; 
            gFloat[j] += gDelta[j]; 
            bFloat[j] += bDelta[j];
            
            if(rFloat[j] < minbrightness)
            {
                rFloat[j] = minbrightness;
                rDelta[j] = Math.abs(rDelta[j]);
            }
            
            if(rFloat[j] > maxbrightness)
            {
                rFloat[j] = maxbrightness;
                rDelta[j] = -Math.abs(rDelta[j]);
            }

            if(gFloat[j] < minbrightness)
            {
                gFloat[j] = minbrightness;
                gDelta[j] = Math.abs(gDelta[j]);
            }
            
            if(gFloat[j] > maxbrightness)
            {
                gFloat[j] = maxbrightness;
                gDelta[j] = -Math.abs(gDelta[j]);
            }

            if(bFloat[j] < minbrightness)
            {
                bFloat[j] = minbrightness;
                bDelta[j] = Math.abs(bDelta[j]);
            }
            
            if(bFloat[j] > maxbrightness)
            {
                bFloat[j] = maxbrightness;
                bDelta[j] = -Math.abs(bDelta[j]);
            }
        }
    
        for(int j=1;j<256;j++)
        {
            int k = (j + offset) % 255;
            
            r[j] = (byte)rFloat[k>>4];
            g[j] = (byte)gFloat[k>>4];
            b[j] = (byte)bFloat[k>>4];
        }
    }
    
    public double getSpeed()
    {
        return 1;
    }

    public boolean hasDithering()
    {
        return false;
    }
    
    public boolean hasAlpha()
    {
        return false;
    }

    public double getNormalizer()
    {
        return 0.5;
    }
}