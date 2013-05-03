package rakama.namibrot;

import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

import javax.swing.JFrame;

import rakama.namibrot.fractal.Fractal;
import rakama.namibrot.theme.Autumn;
import rakama.namibrot.theme.Theme;
import rakama.namibrot.util.CircularBufferDouble;
import rakama.namibrot.util.CircularBufferInt;


public class Namibrot
{
    // TODO: do "fill with black" if # of updated pixels per cycle drops
    // below a particular threshold
    // OR when we start drawing over a point that was black before
    // TODO: save "high range" buffer for fast slope switching
    // TODO: separate Namibrot from its JPanel

    enum Mode {Julia, Mandelbrot};
    
    int zoom, zmTemp, zjTemp, width, height, resizeWidth, resizeHeight, maxIter, fastIter;
    private double zoomRatio, rOffset, iOffset, rJulia, iJulia, rjTemp, ijTemp, xRatio;
    private boolean antialiasing, isInterrupted, sizeChanged, themeChanged;    
    private BufferedImage front, back;
    private boolean[] mask, cached;

    private ActiveCamera camera;
    private Theme theme, oldTheme;
    private Mode mode;
    
    public static void main(String[] args)
    {
        int width = 800;
        int height = 600;
        
        final JFrame frame = new JFrame("Namibrot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final NamiGUI nami = new NamiGUI(frame, width, height);
        frame.getContentPane().add(nami);

        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                nami.getNamibrot().requestResize(frame.getWidth(), frame.getHeight());}});
        
        nami.init();
    }

    public Namibrot(ActiveCamera cam, int w, int h)
    {  
        camera = cam;
        mode = Mode.Mandelbrot;
        zoomRatio = 1;
        rOffset = -0.75;
        theme = new Autumn();
        maxIter = 1 << 17;
        fastIter = 1 << 12;
        
        requestResize(w, h);
        applyResize();        
    }
    
    public void paint(Graphics2D g2)
    {
        getTheme().paint(g2, this);
    }
    
    protected void draw(RenderContext context)
    {
        draw(context, 1, 1, 0, 0, 1, 0);
    }

    protected void draw(RenderContext context, int skipx, int skipy, int offx, int offy, int scale, int bail)
    {
        draw(context, 0, 0, width, height, skipx, skipy, offx, offy, scale, bail);
    }
    
    protected void draw(RenderContext context, int x0, int y0, int w0, int h0, int skipx, int skipy, 
                     int offx, int offy, int scale, int bail)
    {
        if(context == null || context.rendering)
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
        
        int samples = (int)(width * height / 1000.0);
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

        final boolean julia = mode == Mode.Julia;
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
                final double xz = zoomRatio * 3 * xRatio * (x / (double)width - 0.5) + rOffset;
                final double yz = zoomRatio * 3 * (y / (double)height - 0.5) + iOffset;
                
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
                    
                    if(julia)
                    {
                        if(hasPartial)
                            iteration = fractal.getValueOpt(r0, i0, rJulia, iJulia, previter, iter);
                        else
                            iteration = fractal.getValueOpt(xz, yz, rJulia, iJulia, 0, iter);
                    }
                    else
                    {
                        if(hasPartial)
                            iteration = fractal.getValueOpt(r0, i0, xz, yz, previter, iter);
                        else
                            iteration = fractal.getValueOpt(0, 0, xz, yz, 0, iter);
                    }
                    
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

        oldTheme.onDeactivate(this);        
        theme.onActivate(this);
        
        return true;
    }
    
    protected boolean applyZoom()
    {
        if(!camera.isZooming())
            return false;
        
        double x = camera.getZoomX();
        double y = camera.getZoomY();
        double z = camera.getZoomMagnitude();
        int dest_x, dest_y;
        double zscale;
        
        // TODO: needs cleanup
        
        if(z > 0)
        {
            if(antialiasing)
            {
                rOffset += zoomRatio * 3 * xRatio * (x / width - 0.5);
                iOffset += zoomRatio * 3 * (y / height - 0.5);
            }
            else
            {
                rOffset += (zoomRatio * 3 * xRatio * (x / width - 0.5)) / 2;
                iOffset += (zoomRatio * 3 * (y / height - 0.5)) / 2;
            }
            
            zoom += z;
            zoomRatio = Math.pow(2, -zoom);        
            zscale = Math.pow(2, -z);
            
            if(antialiasing)
            {
                dest_x = (int)(x - width * zscale / 2);
                dest_y = (int)(y - height * zscale / 2);
            }
            else                
            {
                dest_x = (int)(x * zscale);
                dest_y = (int)(y * zscale);
            }
        }
        else
        {
            zoom += z;
            zoomRatio = Math.pow(2, -zoom);        
            zscale = Math.pow(2, -z);

            if(antialiasing)
            {
                rOffset -= zoomRatio * 3 * xRatio * (x / width - 0.5);
                iOffset -= zoomRatio * 3 * (y / height - 0.5);     
            }
            else
            {
                rOffset -= (zoomRatio * 3 * xRatio * (x / width - 0.5)) / 2;
                iOffset -= (zoomRatio * 3 * (y / height - 0.5)) / 2;    
            }
            
            if(antialiasing)
            {
                double fx = x - (width / 2) / zscale;
                double fy = y - (height / 2) / zscale;

                double to_x = (fx * zscale) - fx;
                double to_y = (fy * zscale) - fy;

                dest_x = -(int)(to_x * zscale);
                dest_y = -(int)(to_y * zscale);
            }
            else
            {
                double fx = x - (width / 2) / zscale;
                double fy = y - (height / 2) / zscale;

                double to_x = (fx * zscale) - fx;
                double to_y = (fy * zscale) - fy;

                dest_x = -((((int)(to_x * zscale) - (width/2)) / 2) + (width/2));
                dest_y = -((((int)(to_y * zscale) - (height/2)) / 2) + (height/2));
            }
        }
        
        int dest_w = (int)(width / zscale);
        int dest_h = (int)(height / zscale);
        
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
        
        camera.clearZoom();
        return true;
    }
        
    protected boolean applyTranslate()
    {
        if(!camera.isDragging())
            return false;
        
        int scale = 1;
        if(antialiasing)
            scale = 2;
        
        if(mode == Mode.Mandelbrot)
        {
            rjTemp = 0;
            ijTemp = 0;
            zjTemp = 0;
        }

        double deltax = -camera.getDragX() * scale;
        double deltay = -camera.getDragY() * scale;
                
        rOffset += zoomRatio * 3 * xRatio * (deltax / width);
        iOffset += zoomRatio * 3 * (deltay / height);

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
        
        camera.clearDrag();
        
        return true;
    }

    public synchronized boolean applyChanges()
    {
        boolean modified = false;

        if(getActiveCamera().isZooming())
            modified |= applyZoom();

        if(getActiveCamera().isDragging())
        {
            synchronized(getActiveCamera())
            {
                modified |= applyTranslate();
            }
        }
        
        if(isResized())
            modified |= applyResize();
        
        if(isThemeChanged())
            modified |= applyThemeChange();

        return modified;
    }
    
    public ActiveCamera getActiveCamera()
    {
        return camera;
    }
    
    public void setReal(double r)
    {
        rOffset = r;
        forceRefresh();
    }
    
    public double getReal()
    {
        return rOffset;
    }

    public double getJuliaReal()
    {
        return rJulia;
    }

    public void setJuliaReal(double r)
    {
        rJulia = r;
    }

    public void setImaginary(double i)
    {
        iOffset = i;
        forceRefresh();
    }
    
    public double getImaginary()
    {
        return iOffset;
    }

    public double getJuliaImaginary()
    {
        return iJulia;
    }

    public void setJuliaImaginary(double i)
    {
        iJulia = i;
    }

    public void setZoom(int z)
    {
        zoom = z;
        zoomRatio = Math.pow(2, -zoom);
        forceRefresh();
    }    
    
    public int getZoom()
    {
        return zoom;
    }    
    
    public double getMagnification()
    {
        return 1 / zoomRatio;
    }
    
    public void setMode(Mode mode)
    {
        if(this.mode == mode)
            return;
        
        if(mode == Mode.Julia && this.mode == Mode.Mandelbrot)
        {
            this.mode = mode;
            rJulia = rOffset;
            iJulia = iOffset;
            rOffset = rjTemp;
            iOffset = ijTemp;
            zmTemp = zoom;
            setZoom(zjTemp);
        }
        else if(this.mode == Mode.Julia && mode == Mode.Mandelbrot)
        {
            this.mode = mode;
            rjTemp = rOffset;
            ijTemp = iOffset;
            zjTemp = zoom;
            rOffset = rJulia;
            iOffset = iJulia;
            setZoom(zmTemp);
        }
        
        this.mode = mode;
    }
    
    public Mode getMode()
    {
        return mode;
    }
        
    public void setAntialiasing(boolean enabled)
    {
        if(antialiasing == enabled)
            return;
        
        antialiasing = enabled;
        forceRefresh();
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
        
        oldTheme = this.theme;
        this.theme = theme;
        themeChanged = true;
        isInterrupted = true;
    }
        
    public Theme getTheme()
    {
        return theme;
    }
    
    public BufferedImage getImage()
    {
        return front;
    }
    
    public int getImageWidth()
    {
        return width;
    }
    
    public int getImageHeight()
    {
        return height;
    }
    
    public double getRatioX()
    {
        return xRatio;
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

    public void forceRefresh()
    {
        sizeChanged = true;
        isInterrupted = true;
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