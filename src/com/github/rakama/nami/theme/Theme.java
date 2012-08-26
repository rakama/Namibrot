package com.github.rakama.nami.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

import com.github.rakama.nami.Namibrot;
import com.github.rakama.nami.fractal.Fractal;

public abstract class Theme
{
    protected byte[] r, g, b, a;
    private double updateRemaining;
    private double multiplier, normalizer;
        
    public Theme()
    {
        r = new byte[256];
        g = new byte[256];
        b = new byte[256];
        a = new byte[256];

        Arrays.fill(r, (byte)255);
        Arrays.fill(g, (byte)255);
        Arrays.fill(b, (byte)255);
        Arrays.fill(a, (byte)255);

        r[0] = 0;
        g[0] = 0;
        b[0] = 0;
        a[0] = 0;

        multiplier = getMultiplier();
        normalizer = Math.max(0, Math.min(1, getNormalizer()));
        normalizer = 1 / (1 - normalizer);
    }

    public void onActivate(Namibrot nami)
    {
        
    }
    
    public void onDeactivate(Namibrot nami)
    {

    }
    
    public boolean update(long milliseconds)
    {
        boolean updated = false;
        updateRemaining += milliseconds;
        double speed = getSpeed();
        
        if(speed == 0)
            return false;
        
        double step = 33.333 / Math.abs(speed);
        
        while(updateRemaining > step)
        {
            if(speed > 0)
                cycleColors();
            else
                cycleColorsReverse();
            
            updateRemaining -= step;
            updated = true;
        }
        
        return updated;
    }

    protected void cycleColors()
    {
        byte rtemp = r[255];
        byte gtemp = g[255];
        byte btemp = b[255];
        byte atemp = a[255];
        
        System.arraycopy(r, 1, r, 2, r.length-2);
        System.arraycopy(g, 1, g, 2, g.length-2);
        System.arraycopy(b, 1, b, 2, b.length-2);
        System.arraycopy(a, 1, a, 2, a.length-2);

        r[1] = rtemp;
        g[1] = gtemp;
        b[1] = btemp;
        a[1] = atemp;
    }
    
    protected void cycleColorsReverse()
    {        
        byte rtemp = r[1];
        byte gtemp = g[1];
        byte btemp = b[1];
        byte atemp = a[1];
        
        System.arraycopy(r, 2, r, 1, r.length-2);
        System.arraycopy(g, 2, g, 1, g.length-2);
        System.arraycopy(b, 2, b, 1, b.length-2);
        System.arraycopy(a, 2, a, 1, a.length-2);

        r[255] = rtemp;
        g[255] = gtemp;
        b[255] = btemp;
        a[255] = atemp;
    }
    
    public abstract double getSpeed();
    public abstract boolean hasDithering();
    public abstract boolean hasAlpha();
    
    public void paintBackground(Graphics2D g, Namibrot nami)
    {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, nami.getWidth(), nami.getHeight());        
    }

    public void paintForeground(Graphics2D g, Namibrot nami)
    {
        
    }
    
    public double getMultiplier()
    {
        return 1;
    }
    
    public double getNormalizer()
    {
        return 0;
    }
    
    public int getColor(Fractal fractal)
    {              
        double val = getNormalizedValue(fractal);      
        double cfloat = Math.abs((val * multiplier) % 1) * 255;        
        int color = (int)Math.floor(cfloat);        
        if(hasDithering() && cfloat - color > Math.random())
            color = (color + 1) % 255;   
        if(multiplier > 0)
            return color + 1;
        else
            return 255 - color;
    }

    private final double getNormalizedValue(Fractal fractal)
    {
        double val = fractal.getIterations() + fractal.getSmooth();
        return Math.log(val) / normalizer;
    }
       
    public byte[] getRed()
    {
        return r;
    }

    public byte[] getGreen()
    {
        return g;
    }

    public byte[] getBlue()
    {
        return b;
    }

    public byte[] getAlpha()
    {
        return a;
    }
}