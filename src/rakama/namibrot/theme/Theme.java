package rakama.namibrot.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rakama.namibrot.ActiveCamera;
import rakama.namibrot.Namibrot;
import rakama.namibrot.fractal.Fractal;


public abstract class Theme
{
    protected List<String> aliases;
    protected byte[] r, g, b, a;
    private double updateRemaining;
    private double multiplier, normalizer, invNormalizer, speed;
    private boolean alpha, dithering;
        
    public Theme()
    {
        aliases = new ArrayList<String>();
        
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
            return true;
        
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
        cycleColors(r);
        cycleColors(g);
        cycleColors(b);
        cycleColors(a);
    }
    
    protected void cycleColorsReverse()
    {        
        cycleColorsReverse(r);
        cycleColorsReverse(g);
        cycleColorsReverse(b);
        cycleColorsReverse(a);
    }

    protected void cycleColors(byte[] c)
    {
        byte ctemp = c[255];
        System.arraycopy(c, 1, c, 2, c.length-2);
        c[1] = ctemp;
    }
    
    protected void cycleColorsReverse(byte[] c)
    {        
        byte ctemp = c[1];
        System.arraycopy(c, 2, c, 1, c.length-2);
        c[255] = ctemp;
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
    
    public void paint(Graphics2D g2, Namibrot nami)
    {
        paintBackground(g2, nami);
        paintFractal(g2, nami);
        paintForeground(g2, nami);
    }
    
    public void paintBackground(Graphics2D g2, Namibrot nami)
    {
        Rectangle bounds = g2.getClipBounds();   
        g2.setColor(new Color(0xFF & r[0], 0xFF & g[0], 0xFF & b[0]));
        g2.fill(bounds);    
    }

    public void paintFractal(Graphics2D g2, Namibrot nami)
    {
        Rectangle bounds = g2.getClipBounds();    
        
        int width = (int)bounds.getWidth();
        int height = (int)bounds.getHeight();
        
        ActiveCamera gui = nami.getActiveCamera();
        
        AffineTransform identity = g2.getTransform();    
        g2.translate(gui.getDragX(), gui.getDragY());
        
        if(nami.getAntialiasing())
        {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.scale(0.5, 0.5);
            g2.translate(width / 2, height / 2);
        }
        else
        {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
        
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
                RenderingHints.VALUE_RENDER_SPEED);

        IndexColorModel icm;
        
        if(alpha)
            icm = new IndexColorModel(8, 256, getRed(), getGreen(), getBlue(), getAlpha());
        else
            icm = new IndexColorModel(8, 256, getRed(), getGreen(), getBlue());

        int imageX = (width / 2) - (nami.getImageWidth() / 2);
        int imageY = (height / 2) - (nami.getImageHeight() / 2);
        
        BufferedImage img = nami.getImage();        
        BufferedImage current1 = new BufferedImage(icm, img.getRaster(), false, null);
        g2.drawImage(current1, imageX, imageY, null);        
        g2.setTransform(identity);
    }
    
    public void paintForeground(Graphics2D g2, Namibrot nami)
    {
        
    }
    
    public int getColor(Fractal fractal)
    {              
        double val = getNormalizedValue(fractal);      
        return getColorFromValue(Math.abs((val * multiplier)));        
    }
    
    protected final int getColorFromValue(double val)
    {        
        double cfloat = (val % 1) * 255;        
        int color = (int)Math.floor(cfloat);        
        if(dithering && cfloat - color > Math.random())
            color = (color + 1) % 255;   
        if(multiplier > 0)
            return color + 1;
        else
            return 255 - color;
    }

    protected final double getNormalizedValue(Fractal fractal)
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
        String[] split = this.getClass().getName().split("\\.");
        return split[split.length-1];
    }
    
    public List<String> getAliases()
    {
        return Collections.unmodifiableList(aliases);
    }
}