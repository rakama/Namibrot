package rakama.namibrot.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import rakama.namibrot.ActiveCamera;
import rakama.namibrot.NamiGUI;
import rakama.namibrot.Namibrot;


public class Lightspeed extends Theme
{
    protected List<Point2D> stars, vecs;
    protected int numStars, band;
    
    public Lightspeed()
    {
        super();

        for(int i=0;i<=128;i++)
        {
            int rv = (int)Math.min(255, Math.max(0, 2 * (128 - i * 2)));
            int gv = (int)Math.min(255, Math.max(0, 2 * (128 - i * 1.5)));
            int av = (int)Math.min(255, Math.max(0, 2 * (128 - i)));
            r[i] = (byte)rv;
            g[i] = (byte)gv; 
            b[i] = (byte)255;
            a[i] = (byte)av;
            r[255 - i] = r[i];
            g[255 - i] = g[i]; 
            b[255 - i] = b[i];
            a[255 - i] = a[i];
        }

        r[0] = g[0] = b[0] = (byte)0;
        a[0] = (byte)255;
        
        setSpeed(0.5);
        setAlpha(true);
        setMultiplier(-0.5);
        
        numStars = 1400;
        stars = new ArrayList<Point2D>(numStars);
        vecs = new ArrayList<Point2D>(numStars);
    }

    public synchronized void onActivate(Namibrot nami)
    {        
        stars.clear();
        vecs.clear();

        for(int i=0; i<numStars; i++)
        {
            double x = Math.random() * 2000 - 1000;
            double y = Math.random() * 2000 - 1000;
            stars.add(new Point2D(x, y, 0));
            vecs.add(getRandomVector(x, y));
        }
        
        for(int i=50; i<100; i++)
            moveStars(1);
    }
    
    public synchronized void onDeactivate(Namibrot nami)
    {
        stars.clear();
        vecs.clear();
    }
    
    public void paint(Graphics2D g2, Namibrot nami)
    {
        synchronized(nami.getActiveCamera())
        {
            super.paint(g2, nami);
        }
    }
    
    public boolean update(long milliseconds)
    {
        super.update(milliseconds);
        moveStars(milliseconds / 28.0);        
        return true;
    }

    protected synchronized void moveStars(double s)
    {        
        for(int i=0; i<stars.size(); i++)
        {
            Point2D star = stars.get(i);
            Point2D vec = vecs.get(i);
            star.x += vec.x*s;
            star.y += vec.y*s;
            star.z = Math.min(1, star.z + vec.z * 0.015);
            
            if(star.x < -1000 || star.x > 1000
            || star.y < -1000 || star.y > 1000)
            {
                star.x = Math.random() * 44 - 22;
                star.y = Math.random() * 44 - 22;
                star.z = 0;
                randomizeVector(vec, star.x, star.y);
            }
        }
    }
    
    public void paintBackground(Graphics2D g2, Namibrot nami)
    {
        super.paintBackground(g2, nami);
                
        Rectangle bounds = g2.getClipBounds();   
        ActiveCamera cam = nami.getActiveCamera();
        
        int w = (int)bounds.getWidth();
        int h = (int)bounds.getHeight();
        int w2 = (int)bounds.getWidth() / 2;
        int h2 = (int)bounds.getHeight() / 2;
        double w3 = bounds.getWidth() / 3.0;
        double h3 = bounds.getHeight() / 3.0;
        double x0 = w3 * nami.getReal() * nami.getMagnification() / nami.getRatioX();
        double y0 = h3 * nami.getImaginary() * nami.getMagnification();        
        x0 = Math.max(0, Math.min(w, w2 - x0 + cam.getDragX()));
        y0 = Math.max(0, Math.min(h, h2 - y0 + cam.getDragY())); // check angle
        
        double ptScale = (bounds.getHeight() / 250.0);
        
        synchronized(this)
        {
            for(int i=0; i<stars.size(); i++)
            {
                Point2D star = stars.get(i);
                int c = (int)(star.z*255);
                g2.setColor(new Color(c, c, c));
                g2.fill(new Ellipse2D.Double(x0 + star.x * ptScale * 1.2, 
                        y0 + star.y * ptScale, 0.9 * ptScale, 0.9 * ptScale));            
            }
        }
    }
    
    protected Point2D getRandomVector(double x, double y)
    {
        Point2D vec = new Point2D(0, 0, 0);
        randomizeVector(vec, x, y);
        return vec;
    }

    protected void randomizeVector(Point2D vec, double x, double y)
    {
        double norm = Math.sqrt(x*x + y*y);
        double rand = Math.random() * 1.8 + 0.2;
        vec.x = rand * x / norm;
        vec.y = rand * y / norm;
        vec.z = Math.sqrt(vec.x*vec.x + vec.y*vec.y);
    }
    
    private class Point2D
    {
        public double x, y, z;
        
        public Point2D(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}