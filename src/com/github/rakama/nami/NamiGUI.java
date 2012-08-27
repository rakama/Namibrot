package com.github.rakama.nami;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.github.rakama.nami.theme.Theme;

public class NamiGUI implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
    Namibrot nami;    
    Theme[] themes;
    int themeid;

    long lastTranslate, dragTime = 200;    
    boolean showStatusBar;
    int prevMouseX, prevMouseY;
    int dragX, dragY, dragDeltaX, dragDeltaY;
    int xZoom, yZoom, magZoom;
    int maxZoom, minZoom;
    double minX, minY, maxX, maxY;
    double updateRemaining;    
    
    public NamiGUI(Namibrot nami)
    {
        this.nami = nami;
        
        maxZoom = 35;
        minZoom = -1;
        minX = -4;
        minY = -3;
        maxX = 4;
        maxY = 3;
        
        showStatusBar = true;        
        themes = new Theme[0];
    }
    
    public int addTheme(Theme theme)
    {
        int index = themes.length;
        Theme[] array = new Theme[index + 1];
        System.arraycopy(themes, 0, array, 0, index);
        array[index] = theme;
        themes = array;
        return index;
    }
    
    public Theme[] getThemes()
    {
        return themes;
    }
    
    public void paint(Graphics2D g2)
    {   
        if(showStatusBar)
            paintStatusBar(g2);
    }

    protected void paintStatusBar(Graphics2D g2)
    {
        int width = nami.getWidth();
        int height = nami.getHeight();
        
        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(0, height - 18, width, height);
        
        double mag = nami.getMagnification();

        String xStr = "r = " + Float.toString((float)nami.getCenterX());
        String yStr = "i = " + Float.toString((float)nami.getCenterY());
        String zStr;
        
        if(mag > 1 << 13)
            zStr = "z = 2^" + nami.getZoom() + "x";
        else if(mag >= 1)
            zStr = "z = " + (int)(mag) + "x";
        else
            zStr = "z = 1/" + (int)(1 / (mag)) + "x";
        
        g2.setColor(Color.WHITE);
        g2.drawString(xStr, 4, height - 4);
        g2.drawString(yStr, 154, height - 4);
        g2.drawString(zStr, 304, height - 4);
        
        if(nami.getRenderManager().isRendering())
        {
           String str = "Rendering";
           int w = g2.getFontMetrics().stringWidth(str + "...");
           
           long delta = System.currentTimeMillis() % 2000;
           
           if(delta > 1500)
               str += "...";          
           else if(delta > 1000)
               str += "..";
           else if(delta > 500)
               str += ".";
               
           if(340 + w < width)
               g2.drawString(str, width - w - 5, height - 4);            
        }
    }
    
    public boolean isZooming()
    {
        return magZoom != 0;
    }

    public int getZoomX()
    {
        return xZoom;
    }
    
    public int getZoomY()
    {
        return yZoom;
    }
    
    public int getZoomMagnitude()
    {
        return magZoom;
    }

    public void clearZoom()
    {
        synchronized(nami)
        {
            xZoom = 0;
            yZoom = 0;
            magZoom = 0;
        }
    }
    
    public boolean update(long milliseconds)
    {
        updateRemaining += milliseconds;
        
        while(updateRemaining > 33.333)
        {
            dragCamera(dragDeltaX, dragDeltaY);
            updateRemaining -= 33.333;
        }
        
        return false;
    }
    
    public boolean isDragging()
    {
        return dragX != 0 || dragY != 0;
    }

    public int getDragX()
    {
        return dragX;
    }
    
    public int getDragY()
    {
        return dragY;
    }
    
    public void clearDrag()
    {
        dragX = 0;
        dragY = 0;
    }

    public void zoomCamera(int mag)
    {
        zoomCamera(mag, 0, 0);
    }
    
    public void zoomCamera(int mag, int x, int y)
    {
        // TODO: generalize for |mag| > 1
        
        if(mag == 0)
            return;
        
        if(mag > 0 && nami.getZoom() >= maxZoom)
            return;
        
        if(mag < 0 && nami.getZoom() <= minZoom)
            return;
                     
        synchronized(nami)
        {
            xZoom = x + (nami.getImageWidth() >> 1);
            yZoom = y + (nami.getImageHeight() >> 1);
            magZoom = (int)Math.signum(mag);
            nami.interrupt();
        }
    }
    
    protected void setDeltaX(int x)
    {
        dragDeltaX = x;

        synchronized(nami)
        {
            if(dragDeltaY == 0 && dragDeltaX == 0 && isDragging())
                nami.interrupt();
        }
    }

    protected void setDeltaY(int y)
    {
        dragDeltaY = y;

        synchronized(nami)
        {
            if(dragDeltaY == 0 && dragDeltaX == 0 && isDragging())
                nami.interrupt();
        }
    }
    
    public void dragCamera(int x, int y)
    {
        if(x == 0 && y == 0)
            return;
            
//        synchronized(nami)
//        {
            dragX += x;
            dragY += y;
            
            long curtime = System.currentTimeMillis();        
            if(curtime - lastTranslate < dragTime)
                return;
            
            lastTranslate = curtime;        
            nami.interrupt();
//        }
    }
    
    public void mouseWheelMoved(MouseWheelEvent e)
    {     
        zoomCamera(-e.getWheelRotation(), 
                   e.getX() - (nami.getWidth() >> 1),
                   e.getY() - (nami.getHeight() >> 1));
    }
    
    public void mouseDragged(MouseEvent e)
    {
        dragCamera(e.getX() - prevMouseX,
                   e.getY() - prevMouseY);
        
        prevMouseX = e.getX();
        prevMouseY = e.getY();
    }

    public void mouseMoved(MouseEvent e)
    {
    }
    
    public void mouseExited(MouseEvent e)
    {
        synchronized(nami)
        {
            if(isDragging())
                nami.interrupt();
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        synchronized(nami)
        {
            if(isDragging())
                nami.interrupt();
        }
    }    
    
    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
        synchronized(nami)
        {
            if(isDragging())
                nami.interrupt();
        }
    }

    public void mousePressed(MouseEvent e)
    {
        prevMouseX = e.getX();
        prevMouseY = e.getY();
    }

    public void keyPressed(KeyEvent e)
    {
        switch(e.getKeyCode())
        {
        case KeyEvent.VK_UP:
            setDeltaY(5);
            return;
        case KeyEvent.VK_DOWN:
            setDeltaY(-5);
            return;
        case KeyEvent.VK_LEFT:
            setDeltaX(5);
            return;
        case KeyEvent.VK_RIGHT:
            setDeltaX(-5);
            return;
        }

        switch(e.getKeyChar())
        {
        case 'w':
        case 'W':
            setDeltaY(5);
            return;
        case 's':
        case 'S':
            setDeltaY(-5);
            return;
        case 'a':
        case 'A':
            setDeltaX(5);
            return;
        case 'd':
        case 'D':
            setDeltaX(-5);
            return;
        }
    }

    public void keyReleased(KeyEvent e)
    {
        char key = e.getKeyChar();
        
        if(key >= '0' && key <= '9')
        {
            int num;
            
            if(key == '0')
                num = 10;
            else
                num = key - '1';
            
            if(num < themes.length)
                nami.setTheme(themes[num]);
            
            return;
        }

        switch(key)
        {
        case '+':
        case 'z':
        case 'Z':
            zoomCamera(1);
            return;
        case '-':
        case 'x':
        case 'X':
            zoomCamera(-1);
            return;
        case 'w':
        case 'W':
        case 's':
        case 'S':
            setDeltaY(0);
            return;
        case 'a':
        case 'A':
        case 'd':
        case 'D':
            setDeltaX(0);
            return;
        }

        switch(e.getKeyCode())
        {
        case KeyEvent.VK_F1: 
            nami.setFullscreen(!nami.isFullscreen()); 
            return;
        case KeyEvent.VK_F2: 
            showStatusBar = !showStatusBar; 
            return;
        case KeyEvent.VK_ESCAPE:
            nami.setFullscreen(false);
            return;
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
            setDeltaY(0);
            return;
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
            setDeltaX(0);
            return;
        }        
    }

    public void keyTyped(KeyEvent e)
    {
    }
}