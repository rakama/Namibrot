package com.github.rakama.nami;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.github.rakama.nami.fractal.Fractal;
import com.github.rakama.nami.theme.Autumn;
import com.github.rakama.nami.theme.Bubblegum;
import com.github.rakama.nami.theme.Rainbow;
import com.github.rakama.nami.theme.Theme;
import com.github.rakama.nami.theme.Zebra;
import com.github.rakama.nami.util.CircularBufferDouble;
import com.github.rakama.nami.util.CircularBufferInt;

public class Namibrot extends JPanel
{
    // TODO: do "fill with black" if # of updated pixels per cycle drops
    // below a particular threshold
    // OR when we start drawing over a point that was black before
    // TODO: save "high range" buffer for fast slope switching
    
    private static final long serialVersionUID = 6327582443335433368L;
    
    int zoom, width, height, resizeWidth, resizeHeight, maxIter, fastIter;
    double zoomRatio, xOffset, yOffset, xRatio;
    private boolean antialiasing, isInterrupted, sizeChanged, themeChanged;    
    BufferedImage front, back;
    boolean[] mask, cached;

    Component parent;
    JFrame fullscreen;
    ThreadManager manager;
    NamiGUI gui;
    Theme theme;
    
    public static void main(String[] args)
    {
        int width = 800;
        int height = 600;
        
        final JFrame frame = new JFrame("Namibrot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final Namibrot mandel = new Namibrot(frame, width, height);
        frame.getContentPane().add(mandel);

        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                mandel.requestResize(frame.getWidth(), frame.getHeight());}});
        
        mandel.init();
    }

    public Namibrot(int w, int h)
    {  
        this(null, w, h);
    }
    
    public Namibrot(Component parent, int w, int h)
    {  
        this.parent = parent;
        
        gui = new NamiGUI(this);
        manager = new ThreadManager(this);

        gui.addTheme(new Rainbow());
        gui.addTheme(new Bubblegum());
        gui.addTheme(new Autumn());
        gui.addTheme(new Zebra());
        
        if(manager.getNumThreads() == 1)
            antialiasing = false;
        else
            antialiasing = true;

        zoomRatio = 1;
        xOffset = -0.75;
        theme = gui.getThemes()[1];
        maxIter = 1 << 17;
        fastIter = 1 << 10;
        
        requestResize(w, h);
        applyResize();      
    }    
    
    protected void init()
    {
        addMouseWheelListener(gui); 
        addMouseMotionListener(gui);   
        addMouseListener(gui);   
        
        if(parent != null)
            parent.addKeyListener(gui);   

        manager.startRepaintLoop();
        manager.startDrawLoop();
    }
    
    public void paint(Graphics gx)
    {
        int x = getImageX();
        int y = getImageY();

        Graphics2D g2 = (Graphics2D)gx;
        theme.paintBackground(g2, this);
        
        AffineTransform identity = g2.getTransform();    
        g2.translate(gui.getDragX(), gui.getDragY());
        
        if(antialiasing)
        {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, 
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
                    RenderingHints.VALUE_RENDER_SPEED);
            g2.scale(0.5, 0.5);
            g2.translate(getWidth() >> 1, getHeight() >> 1);
//            g2.translate(1, 1); // blur effect
        }

        byte[] r = theme.getRed();
        byte[] g = theme.getGreen();
        byte[] b = theme.getBlue();
        IndexColorModel icm = new IndexColorModel(8, 256, r, g, b); 

        BufferedImage current1 = new BufferedImage(icm, front.getRaster(), true, null);
        g2.drawImage(current1, x, y, null);
        
        g2.setTransform(identity);
        theme.paintForeground(g2, this);
        gui.paint(gx);
    }

    protected void draw(RenderContext context)
    {
        draw(context, 1, 1, 0, 0, 1, 0, 0);
    }

    protected void draw(RenderContext context, int skipx, int skipy, int offx, int offy, int scale, 
                    int t, int bail)
    {
        draw(context, 0, 0, width, height, skipx, skipy, offx, offy, scale, t, bail);
    }
    
    protected void draw(RenderContext context, int x0, int y0, int w0, int h0, int skipx, int skipy, 
                     int offx, int offy, int scale, int t, int bail)
    {
        if(context == null)
            return;
        
        context.rendering = true;
        
        
        CircularBufferInt queue = context.queue;
        CircularBufferInt pending = context.pending;
        
        final CircularBufferDouble partial = context.partial;
        final Fractal fractal = context.fractal;

        Arrays.fill(mask, false);
        
        queue.clear();
        pending.clear();
        partial.clear();

        skipx *= scale;
        skipy *= scale;
        offx *= scale;
        offy *= scale;
        
        final byte[] cfront = ((DataBufferByte)front.getRaster().getDataBuffer()).getData();
        
        int samples = (int)Math.sqrt(width * height);
        for(int i=0; i<samples; i++)
        {
            int init_x = x0 + (int)((Math.random() * (w0 - offx - 2)) / skipx) * skipx + offx;
            int init_y = y0 + (int)((Math.random() * (h0 - offy - 2)) / skipy) * skipy + offy; 
            int init_index = init_x + (init_y * width);
            
            if(mask[init_index])
                continue;
            
            pending.push(init_index);
            mask[init_index] = true;
        }

        double base = 1.25;
        int pow = 16;
        int previter = -1;
        int iter = (int)Math.ceil(Math.pow(base, pow));

        final boolean scaled = scale > 1;
        final int xmin = x0 + skipx;
        final int ymin = y0 + skipy;
        final int xmax = x0 + w0 - skipx;
        final int ymax = y0 + h0 - skipy;
        final int skipwidth = skipy * width;
        final int yieldtime = 100;
        
        long prevtime = System.currentTimeMillis() + (int)(Math.random() * yieldtime);
        
        while(!pending.isEmpty() && !isInterrupted)
        {
            CircularBufferInt swap = queue;
            queue = pending;
            pending = swap;
            
            while(!queue.isEmpty() && !isInterrupted)
            {
                final long curtime = System.currentTimeMillis();
                final long diff = curtime - prevtime;
                
                if(diff > yieldtime)
                {
                    Thread.yield();
                    prevtime = curtime;
                }
                
                int index = queue.poll();
                final boolean hasPartial = index < 0;
                index = Math.abs(index);
                
                final int x = index % width;
                final int y = index / width;
                final double xz = zoomRatio * 3 * xRatio * (x / (double)width - 0.5) + xOffset;
                final double yz = zoomRatio * 3 * (y / (double)height - 0.5) + yOffset;
                
                double r0, i0;
                if(hasPartial)
                {
                    r0 = partial.poll();
                    i0 = partial.poll();
                }
                else
                    r0 = i0 = 0;
                
                if(iter >= maxIter)
                {
                    cfront[index] = (byte)0;
                }
                else if(!cached[index])
                {
                    int iteration;
                    
                    if(hasPartial)
                        iteration = fractal.getValue(r0, i0, xz, yz, previter, iter);
                    else
                        iteration = fractal.getValue(xz, yz, iter);
                    
                    if(iteration >= iter && iter < maxIter)
                    {
                        if(partial.size() < partial.capacity() - 2)
                        {
                            pending.push(-index);
                            partial.push(fractal.getReal());
                            partial.push(fractal.getImaginary());
                        }
                        else
                            pending.push(index);
                        
                        continue;
                    }

                    final byte color = (byte)theme.getColor(fractal);
                    
                    if(scaled)
                    {
                        final int endi = Math.min(x0 + w0, x + scale);
                        final int endj = Math.min((y0 + h0) * width, (y + scale) * width);
                        for(int i=x; i<endi; i++)
                            for(int j=y*width; j<endj; j+=width)
                                if(!cached[i + j])
                                    cfront[i + j] = color;
                    }
                    else
                        cfront[index] = color;
                    
                    cached[index] = true;   
                }
                else if(scaled)
                {
                    final byte color = cfront[index];
                    if(color == 0)
                        continue;
                    
                    final int endi = Math.min(x0 + w0, x + scale);
                    final int endj = Math.min((y0 + h0) * width, (y + scale) * width);
                    for(int i=x; i<endi; i++)
                        for(int j=y*width; j<endj; j+=width)
                            if(!cached[i + j])
                                cfront[i + j] = color;
                }

                final int up = index - skipwidth;
                final int down = index + skipwidth;
                final int left = index - skipx;
                final int right = index + skipx;
    
                if(!(y <= ymin || mask[up]))
                {
                    queue.push(up);
                    mask[up] = true;
                } 

                if(!(y >= ymax || mask[down]))
                {
                    queue.push(down);
                    mask[down] = true;
                }                

                if(!(x >= xmax || mask[right]))
                {
                    queue.push(right);
                    mask[right] = true;
                }

                if(!(x <= xmin || mask[left]))
                {
                    queue.push(left);
                    mask[left] = true;
                } 
            }
            
            previter = iter;
            if(iter < maxIter)
                iter = (int)Math.ceil(Math.pow(base, pow++));
            
            if(bail > 0 && iter >= bail)
                break;
        }

        context.rendering = false;
    }

    protected boolean applyResize()
    {        
        width = resizeWidth;
        height = resizeHeight;
        sizeChanged = false;

        if(antialiasing)
        {
            width *= 2;
            height *= 2;
        }

        byte[] r = theme.getRed();
        byte[] g = theme.getGreen();
        byte[] b = theme.getBlue();
        IndexColorModel icm = new IndexColorModel(8, 256, r, g, b);        
        front = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, icm);
        back = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, icm);        
        mask = new boolean[width * height];
        cached = new boolean[width * height];
        xRatio = width / (double)height;
        Arrays.fill(cached, false);
        Arrays.fill(mask, false);
                
        return true;
    }
    
    protected boolean applyThemeChange()
    {
        if(!isThemeChanged())
            return false;

        Arrays.fill(cached, false);
        Arrays.fill(mask, false);
        themeChanged = false;
        
        return true;
    }
    
    protected boolean applyZoom()
    {        
        if(!gui.isZooming())
            return false;

        // TODO: figure it out
        double scale = 1;        
        if(antialiasing)
            scale = 1;
        
        double x = gui.getZoomX() * scale;
        double y = gui.getZoomY() * scale;
        double z = gui.getZoomMagnitude();
        
        xOffset += zoomRatio * 3 * xRatio * (x / width - 0.5);
        yOffset += zoomRatio * 3 * (y / height - 0.5);
        
        zoom += z;
        zoomRatio = Math.pow(2, -zoom);
        
        double zscale = Math.pow(2, -z);
        int dest_w = (int)(width / zscale);
        int dest_h = (int)(height / zscale);
        int dest_x = (int)(x - width * zscale / 2);
        int dest_y = (int)(y - height * zscale / 2);
        
        final boolean[] bfront = cached;
        final boolean[] bback = mask;      
        
        final byte[] cfront = ((DataBufferByte)front.getRaster().getDataBuffer()).getData();
        final byte[] cback = ((DataBufferByte)back.getRaster().getDataBuffer()).getData();   

        System.arraycopy(cfront, 0, cback, 0, cback.length); 

        if(zscale < 1)
        {
            // bleed uncolored pixels
            for(int yp=1; yp<height-1; yp++)
            {
                int yp_offset = yp*width;
                
                for(int xp=1; xp<width-1; xp++)
                {
                    int index = xp + yp_offset;
                    if((0xFF & cback[index-1]) == 0
                    || (0xFF & cback[index+1]) == 0
                    || (0xFF & cback[index-width]) == 0
                    || (0xFF & cback[index+width]) == 0)                
                        cfront[index] = 0;
                }
            }
        }
        
        double dx = width / (double)dest_w;
        double dy = height / (double)dest_h;
          
        if(zscale < 1)
        {
            for(int ydest=0; ydest<height; ydest++)
            {
                int ydest_offset = ydest*width;
                int ysrc = dest_y + (int)(dy * ydest); 
                
                if(ysrc < 0 || ysrc >= height)
                {
                    Arrays.fill(cback, ydest_offset, ydest_offset + width, (byte)0);
                    continue;
                }
            
                int ysrc_offset = ysrc*width;            
                for(int xdest=0; xdest<width; xdest++)
                {
                    int xsrc = dest_x + (int)(dx * xdest);
                    
                    if(xsrc < 0 || xsrc >= width)
                        cback[xdest + ydest_offset] = 0;
                    else 
                        cback[xdest + ydest_offset] = cfront[xsrc + ysrc_offset];      
                }
            }

            Arrays.fill(bback, false);
        }
        else
        {
            for(int ydest=0; ydest<height; ydest++)
            {
                int ydest_offset = ydest*width;
                int ysrc = dest_y + (int)(dy * ydest); 
                
                if(ysrc < 0 || ysrc >= height)
                {
                    Arrays.fill(cback, ydest_offset, ydest_offset + width, (byte)0);
                    Arrays.fill(bback, ydest_offset, ydest_offset + width, false);
                    continue;
                }
            
                int ysrc_offset = ysrc*width;            
                for(int xdest=0; xdest<width; xdest++)
                {
                    int xsrc = dest_x + (int)(dx * xdest);
                    
                    if(xsrc < 0 || xsrc >= width)
                    {
                        cback[xdest + ydest_offset] = 0;
                        bback[xdest + ydest_offset] = false;
                    }
                    else 
                    {
                        cback[xdest + ydest_offset] = cfront[xsrc + ysrc_offset];
                        bback[xdest + ydest_offset] = bfront[xsrc + ysrc_offset];
                    }
                }
            }
        }

        mask = bfront;
        cached = bback;
        
        Arrays.fill(mask, false);
        
        BufferedImage temp = back;
        back = front;
        front = temp;
        
        gui.clearZoom();
        return true;
    }
        
    protected boolean applyTranslate()
    {
        if(!gui.isDragging())
            return false;
        
        int scale = 1;        
        if(antialiasing)
            scale = 2;

        double deltax = -gui.getDragX() * scale;
        double deltay = -gui.getDragY() * scale;
        
        xOffset += zoomRatio * 3 * xRatio * (deltax / width);
        yOffset += zoomRatio * 3 * (deltay / height);

        final boolean[] bfront = cached;
        final boolean[] bback = mask;
        
        final byte[] cfront = ((DataBufferByte)front.getRaster().getDataBuffer()).getData();
        final byte[] cback = ((DataBufferByte)back.getRaster().getDataBuffer()).getData();

        int xshift = Math.abs((int)deltax);
        int yshift = Math.abs((int)deltay) * width;
        
        if(xshift >= width || yshift >= cfront.length)
        {
            Arrays.fill(cback, (byte)0);
            Arrays.fill(bback, false);
        }
        else
        {
            System.arraycopy(cfront, 0, cback, 0, cfront.length);
            System.arraycopy(bfront, 0, bback, 0, bfront.length);
            
            for(int i=0; i<cback.length; i+=width)
            {
                if(deltax > 0)
                {
                    System.arraycopy(cback, i + xshift, cback, i, width - xshift);
                    Arrays.fill(cback, i + width - xshift, i + width, (byte)0);
                    System.arraycopy(bback, i + xshift, bback, i, width - xshift);
                    Arrays.fill(bback, i + width - xshift, i + width, false);
                }
                else
                {
                    System.arraycopy(cback, i, cback, i + xshift, width - xshift);
                    Arrays.fill(cback, i, i + xshift, (byte)0);
                    System.arraycopy(bback, i, bback, i + xshift, width - xshift);
                    Arrays.fill(bback, i, i + xshift, false);
                }
            }
            
            if(deltay > 0)
            {
                System.arraycopy(cback, yshift, cback, 0, cback.length - yshift);
                Arrays.fill(cback, cback.length - yshift, cback.length, (byte)0);
                System.arraycopy(bback, yshift, bback, 0, bback.length - yshift);
                Arrays.fill(bback, cback.length - yshift, bback.length, false);
            }
            else
            {
                System.arraycopy(cback, 0, cback, yshift, cback.length - yshift);
                Arrays.fill(cback, 0, yshift, (byte)0);
                System.arraycopy(bback, 0, bback, yshift, bback.length - yshift);
                Arrays.fill(bback, 0, yshift, false);
            }
        }
        
        mask = bfront;
        cached = bback;
        
        Arrays.fill(mask, false);
        
        BufferedImage temp = back;
        back = front;
        front = temp;
        
        gui.clearDrag();
        
        return true;
    }
    
    public double getCenterX()
    {
        return xOffset;
    }

    public double getCenterY()
    {
        return yOffset;
    }

    public int getZoom()
    {
        return zoom;
    }    
    
    public double getMagnification()
    {
        return 1 / zoomRatio;
    }
    
    public ThreadManager getRenderManager()
    {
        return manager;
    }
    
    public NamiGUI getGUI()
    {
        return gui;
    }
    
    public void setAntialiasing(boolean enabled)
    {
        antialiasing = enabled;
    }
    
    public boolean getAntialiasing()
    {
        return antialiasing;
    }
    
    public void setTheme(Theme theme)
    {
        if(theme == null)
            throw new NullPointerException();
        
        if(this.theme == theme)
            return;
        
        this.theme.onDeactivate(this);
        this.theme = theme;
        themeChanged = true;
        isInterrupted = true;        
        theme.onActivate(this);
    }
        
    public Theme getTheme()
    {
        return theme;
    }

    public int getImageX()
    {
        return (getWidth() >> 1) - (width >> 1);
    }
    
    public int getImageY()
    {
        return (getHeight() >> 1) - (height >> 1);
    }
    
    public int getImageWidth()
    {
        return width;
    }
    
    public int getImageHeight()
    {
        return height;
    }
    
    public boolean isFullscreen()
    {
        return fullscreen != null;
    }
    
    public void setFullscreen(boolean enabled)
    {
        if(parent == null || !(parent instanceof JFrame))
            return;

        if(enabled && !isFullscreen())
        {
            JFrame pframe = (JFrame)parent;
            pframe.getContentPane().removeAll();
            pframe.setVisible(false);
            
            fullscreen = new JFrame();
            fullscreen.getContentPane().add(this);
            fullscreen.addWindowListener(new WindowAdapter(){
                public void windowClosed(WindowEvent e){setFullscreen(false);}
                public void windowDeactivated(WindowEvent e){setFullscreen(false);}
                public void windowIconified(WindowEvent arg0){setFullscreen(false);}});
            
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            setBounds(0, 0, size.width, size.height);
            fullscreen.setSize(size.width, size.height);
            fullscreen.setUndecorated(true);
            fullscreen.setVisible(true);
            fullscreen.addKeyListener(gui);

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if(fullscreen!=null)
                        fullscreen.toFront();}});
            
            requestResize(size.width, size.height);
        }
        else if(!enabled && isFullscreen())
        {
            fullscreen.getContentPane().removeAll();
            fullscreen.setVisible(false);
            fullscreen.removeKeyListener(gui);
            fullscreen = null;
            
            JFrame pframe = (JFrame)parent;
            pframe.getContentPane().add(this);
            pframe.setVisible(true);
            pframe.setState(JFrame.NORMAL);
            
            EventQueue.invokeLater(new Runnable() {
                public void run() {((JFrame)parent).toFront();}});
            
            requestResize(pframe.getWidth(), pframe.getHeight());
        }
    }
    
    protected void requestResize(int w, int h)
    {
        resizeWidth = w;
        resizeHeight = h;
        sizeChanged = true;
        isInterrupted = true;
    }

    public boolean isResized()
    {
        return sizeChanged;
    }

    public boolean isThemeChanged()
    {
        return themeChanged;
    }
    
    public boolean isInterrupted()
    {
        return isInterrupted;
    }

    public void interrupt()
    {
        isInterrupted = true;
    }

    public void resume()
    {
        isInterrupted = false;
    }
}