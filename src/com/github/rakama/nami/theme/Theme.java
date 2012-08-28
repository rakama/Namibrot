package com.github.rakama.nami.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

import com.github.rakama.nami.Namibrot;
import com.github.rakama.nami.fractal.Fractal;

public abstract class Theme
{
    protected byte[] r, g, b, a;
    private double updateRemaining;
    private double multiplier, normalizer, invNormalizer, speed;
    private boolean alpha, dithering;
        
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
        
        setSpeed(1);
        setMultiplier(1);
        setNormalizer(0);
        setDithering(true);
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
    
    public void setDithering(boolean enabled)
    {
        dithering = enabled;
    }
    
    public boolean hasDithering()
    {
        return dithering;
    }

    public void setAlpha(boolean enabled)
    {
        alpha = enabled;
    }

    public boolean hasAlpha()
    {
        return alpha;
    }
    
    public void setSpeed(double val)
    {
        speed = val;
    }
    
    public double getSpeed()
    {
        return speed;
    }

    public void setMultiplier(double val)
    {
        multiplier = val;
    }
    
    public double getMultiplier()
    {
        return multiplier;
    }

    public void setNormalizer(double val)
    {
        normalizer = val;
        invNormalizer = Math.max(0, Math.min(1, getNormalizer()));
        invNormalizer = 1 / (1 - invNormalizer);
    }
    
    public double getNormalizer()
    {
        return normalizer;
    }
    
    public void paintBackground(Graphics2D g2, Namibrot nami)
    {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, nami.getWidth(), nami.getHeight());        
    }

    public void paintFractal(Graphics2D g2, Namibrot nami)
    {
        AffineTransform identity = g2.getTransform();    
        g2.translate(nami.getGUI().getDragX(), nami.getGUI().getDragY());
        
        if(nami.getAntialiasing())
        {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, 
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
                    RenderingHints.VALUE_RENDER_SPEED);
            g2.scale(0.5, 0.5);
            g2.translate(nami.getWidth() >> 1, nami.getHeight() >> 1);
        }

        IndexColorModel icm = new IndexColorModel(8, 256, r, g, b);
        BufferedImage current1 = new BufferedImage(icm, nami.getImage().getRaster(), true, null);
        g2.drawImage(current1, nami.getImageX(), nami.getImageY(), null);        
        g2.setTransform(identity);
    }
    
    public void paintForeground(Graphics2D g2, Namibrot nami)
    {
        
    }
    
    public int getColor(Fractal fractal)
    {              
        double val = getNormalizedValue(fractal);      
        return getColorFromValue(Math.abs((val * multiplier) % 1));        
    }
    
    protected final int getColorFromValue(double val)
    {        
        double cfloat = val * 255;        
        int color = (int)Math.floor(cfloat);        
        if(dithering && cfloat - color > Math.random())
            color = (color + 1) % 255;   
        if(multiplier > 0)
            return color + 1;
        else
            return 255 - color;
    }

    private final double getNormalizedValue(Fractal fractal)
    {
        double val = fractal.getIterations() + fractal.getSmooth();
        return Math.log(val) / invNormalizer;
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
    
    public String getName()
    {
        return this.getClass().getName();
    }
}