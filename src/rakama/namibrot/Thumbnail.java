package rakama.namibrot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import rakama.namibrot.Namibrot.Mode;
import rakama.namibrot.fractal.Fractal;
import rakama.namibrot.theme.Theme;


public class Thumbnail extends Theme
{
    Namibrot nami, thumbnail;
    RenderContext thumbnailContext;
    BufferedImage buffer;
    
    public Thumbnail(Namibrot nami)
    {
        super();
                
        for(int i=0;i<7;i++)
        {
            r[i] = g[i] = b[i] = (byte)0;
            a[i] = (byte)128;
        }

        for(int i=7;i<=15;i++)
        {
            r[i] = g[i] = b[i] = (byte)128;
            a[i] = (byte)128;
        }

        r[0] = g[0] = b[0] = a[0] = (byte)255;
        r[255] = a[255] = (byte)255;
        g[255] = b[255] = (byte)0;

        setSpeed(0);
        setAlpha(true);
        
        buffer = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);        
        this.nami = nami;
        thumbnail = new Namibrot(new ThumbnailCamera(), 128, 128);
        thumbnailContext = new RenderContext(128*128);
        thumbnail.setTheme(this);
        redraw();        
    }
    
    public void paint(Graphics2D g2)
    {
        g2.drawImage(buffer, 0, 0, null);
    }
    
    public void paintBackground(Graphics2D g2, Namibrot nami)
    {
        
    }
    
    public Mode getMode()
    {
        return thumbnail.getMode();
    }
    
    public void redraw()
    {
        if(nami.getMode() == Mode.Julia)
        {
            thumbnail.setMode(Mode.Mandelbrot);
            thumbnail.setImaginary(0);
            thumbnail.setReal(-0.75);
            thumbnail.setZoom(-1);
        }
        else
        {
            thumbnail.setMode(Mode.Julia);    
            thumbnail.setImaginary(0);
            thumbnail.setReal(0);
            thumbnail.setJuliaImaginary(nami.getImaginary());
            thumbnail.setJuliaReal(nami.getReal());
            thumbnail.setZoom(-1);        
        }         
        
        thumbnail.interrupt();
        thumbnail.forceRefresh();
        thumbnail.applyChanges();
        thumbnail.resume();
        new Thread(){public void run(){redrawThreaded();}}.start();
    }
    
    public int getColor(Fractal fractal)
    {              
        int iter = fractal.getIterations();
        
        if(thumbnail.getMode() == Mode.Mandelbrot)
        {        
            if(iter < 5)
                return 1;
            else if(iter > 15)
                return 254;
            else
                return iter;
        }
        else
        {
            if(iter < 5)
                return 1;
            else if(iter > 12)
                return 254;
            else
                return iter;
        }
    }
    
    protected synchronized void redrawThreaded()
    {        
        thumbnail.draw(thumbnailContext, 1, 1, 0, 0, 1, 15);
        
        int[] data = ((DataBufferInt)buffer.getRaster().getDataBuffer()).getData();
        Arrays.fill(data, 0);
        Graphics2D bg = (Graphics2D)buffer.getGraphics();
        bg.setClip(0, 0, 96, 96);
        super.paint(bg, thumbnail);
        
        if(thumbnail.getMode() == Mode.Mandelbrot)
        {
            int x = (int)(((nami.getJuliaReal() + 2.25 + 0.75) / 4.5) * 96);
            int y = (int)(((nami.getJuliaImaginary() + 2.25) / 4.5) * 96);
            bg.setColor(Color.RED);
            bg.fillRect(x, y, 2, 2);
        }
    }
}

class ThumbnailCamera implements ActiveCamera
{
    public boolean isZooming(){return false;}        
    public int getZoomX(){return 0;}     
    public int getZoomY(){return 0;}     
    public int getZoomMagnitude(){return 0;}
    public void clearZoom(){};
    public boolean isDragging(){return false;} 
    public int getDragX(){return 0;}
    public int getDragY(){return 0;}
    public void clearDrag(){}
}