package rakama.namibrot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.RootPaneContainer;

import rakama.namibrot.Namibrot.Mode;
import rakama.namibrot.theme.Autumn;
import rakama.namibrot.theme.Bubblegum;
import rakama.namibrot.theme.Lightspeed;
import rakama.namibrot.theme.Mushroom;
import rakama.namibrot.theme.Theme;
import rakama.namibrot.theme.Zebra;


@SuppressWarnings("serial")
public class NamiGUI extends JPanel implements MouseMotionListener, MouseListener, 
MouseWheelListener, KeyListener, ActiveCamera
{
    Component parent;
    JFrame fullscreen;
    ThreadManager manager;    
    Thumbnail thumbnail;
    Namibrot nami;
    List<Theme> themes;
    int themeid;
    
    int thumbX, thumbY, thumbWidth, thumbHeight;
    long lastTranslate, dragTime;    
    int prevMouseX, prevMouseY;
    int dragX, dragY, dragDeltaX, dragDeltaY;
    int xZoom, yZoom, magZoom;
    int maxZoom, minZoom;
    double minX, minY, maxX, maxY;
    double updateRemaining;
    boolean showUI, button1, button2;
    
    public NamiGUI(Component parent, int w, int h)
    {
        this.nami = new Namibrot(this, w, h);
        thumbnail = new Thumbnail(nami);

        thumbX = -72;
        thumbY = -90;
        thumbWidth = 64;
        thumbHeight = 64;
        maxZoom = 35;
        minZoom = -1;
        minX = -4;
        minY = -3;
        maxX = 4;
        maxY = 3;
        dragTime = 200;
        
        showUI = true;
        themes = new ArrayList<Theme>(); 
        
        this.parent = parent;
        
        setFocusable(true);
        
        manager = new ThreadManager(this);

        addTheme(new Autumn());
        addTheme(new Bubblegum());
        addTheme(new Mushroom());
        addTheme(new Zebra());
        addTheme(new Lightspeed());
//        addTheme(new Recursive());
//        gui.addTheme(new Lines());
        
        if(manager.getNumThreads() == 1 || (parent instanceof JApplet))
            nami.setAntialiasing(false);
        else
            nami.setAntialiasing(true);
    }
    
    protected void init()
    {
        manager.startRepaintLoop();
        manager.startDrawLoop();
        
        addMouseWheelListener(this); 
        addMouseMotionListener(this);   
        addMouseListener(this);
        addKeyListener(this);
        
        if(parent != null && parent instanceof JFrame)
            parent.addKeyListener(this);
    }
    
    protected void stop()
    {
        manager.stop();

        removeMouseWheelListener(this); 
        removeMouseMotionListener(this);   
        removeMouseListener(this);
        removeKeyListener(this);
        
        if(parent != null)
            parent.removeKeyListener(this);
    }
    
    public Component getParentComponent()
    {
        return parent;
    }
    
    public Namibrot getNamibrot()
    {
        return nami;
    }
    
    public void addTheme(Theme theme)
    {
        themes.add(theme);
    }
    
    public List<Theme> getThemes()
    {
        return themes;
    }
    
    public Theme getTheme(String theme)
    {
        if(theme == null || theme.isEmpty() || themes.isEmpty())
            return null;
        
        if(theme.toLowerCase().equals("default"))
            return themes.get(0);
        
        theme = theme.toLowerCase();
        
        // check names
        for(Theme t : themes)
        {
            String name = t.getName().toLowerCase();
            if(name.equals(theme))
                return t;
        }
        
        // check aliases
        for(Theme t : themes)
        {
            if(t.getAliases() == null)
                continue;
            
            for(String alias : t.getAliases())
                if(alias.toLowerCase().equals(theme))
                    return t;
        }
        
        return null;
    }

    public ThreadManager getThreadManager()
    {
        return manager;
    }
    
    public void paint(Graphics g)
    {     
        Graphics2D g2 = (Graphics2D)g;
        
        nami.paint(g2);        

        if(showUI)
        {
            paintThumbnail(g2);
            paintStatusBar(g2);
            paintZBar(g2);
        }
    }
    
    protected void paintThumbnail(Graphics2D g2)
    {        
        if(thumbnail.getMode() == nami.getMode())
            thumbnail.redraw();
        
        AffineTransform trans = g2.getTransform();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2.translate(getWidth() + thumbX, getHeight() + thumbY);
        g2.setClip(0, 0, thumbWidth, thumbHeight);
        g2.scale(0.75, 0.75);
        g2.translate(-6, -6);
        thumbnail.paint(g2);
        g2.setTransform(trans);
        g2.setClip(0, 0, getWidth(), getHeight());
    }

    protected void paintZBar(Graphics2D g2)
    {
        
    }
    
    protected void paintStatusBar(Graphics2D g2)
    {
        int width = getWidth();
        int height = getHeight();
        
        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(0, height - 18, width, height);
        
        double mag = nami.getMagnification();

        String xStr = "r = " + Float.toString((float)nami.getReal());
        String yStr = "i = " + Float.toString((float)nami.getImaginary());
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
        
        if(getThreadManager().isRendering())
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

        thumbnail.redraw();
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
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        thumbnail.redraw();
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
            
        synchronized(nami)
        {
            dragX += x;
            dragY += y;
            
            long curtime = System.currentTimeMillis();        
            if(curtime - lastTranslate < dragTime)
                return;
            
            lastTranslate = curtime;   
            
            if(isDragging())
                nami.interrupt();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e)
    {     
        zoomCamera(-e.getWheelRotation(), 
                   e.getX() - (getWidth() >> 1),
                   e.getY() - (getHeight() >> 1));
    }
    
    public void mouseDragged(MouseEvent e)
    {
        if(!(button1 || button2))
            return;
        
        dragCamera(e.getX() - prevMouseX,
                   e.getY() - prevMouseY);
        
        prevMouseX = e.getX();
        prevMouseY = e.getY();
        
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    public void mouseMoved(MouseEvent e)
    {
        if(isThumbnailClick(e))
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if(thumbnail.getMode() == Mode.Mandelbrot)
                setToolTipText("Mandelbrot Set");
            else
                setToolTipText("Julia Set");
        }
        else
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            setToolTipText(null);
        }
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
        if(e.getButton() == MouseEvent.BUTTON1)
            button1 = false;
        else if(e.getButton() == MouseEvent.BUTTON2)
            button2 = false;
        
        synchronized(nami)
        {
            if(isDragging())
                nami.interrupt();
        }
    }    
    
    public void mouseClicked(MouseEvent e)
    {        
        if(e.getClickCount() < 2)
            return;

        if(!isThumbnailClick(e))
        {
            dragCamera(-e.getX() + getWidth() / 2,
                       -e.getY() + getHeight() / 2);
            
            nami.interrupt();
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        if(e.getButton() == MouseEvent.BUTTON1)
        {
            if(isThumbnailClick(e))
            {
                if(nami.getMode() == Mode.Mandelbrot)
                    nami.setMode(Mode.Julia);
                else
                    nami.setMode(Mode.Mandelbrot);
                
                thumbnail.redraw();
            }
            else
            {
                prevMouseX = e.getX();
                prevMouseY = e.getY();
                button1 = true;
            }
        }
        else if(e.getButton() == MouseEvent.BUTTON2)
        {            
            prevMouseX = e.getX();
            prevMouseY = e.getY();
            button2 = true;
        }
        else if(e.getButton() == MouseEvent.BUTTON3)
        {
            doPopup(e);
        }
    }
    
    protected boolean isThumbnailClick(MouseEvent e)
    {
        return e.getX() > getWidth() + thumbX
        && e.getX() < getWidth() + thumbX + thumbWidth
        && e.getY() > getHeight() + thumbY
        && e.getY() < getHeight() + thumbY + thumbHeight;        
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
        case 'j':
        case 'J':
            if(nami.getMode() == Mode.Julia)
                nami.setMode(Mode.Mandelbrot);
            else
                nami.setMode(Mode.Julia);
            return;
        case 'm':
        case 'M':
            nami.setAntialiasing(!nami.getAntialiasing());
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
            
            if(num < themes.size())
                nami.setTheme(themes.get(num));
            
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
            setFullscreen(!isFullscreen()); 
            return;
        case KeyEvent.VK_F2: 
            showUI = !showUI; 
            return;
        case KeyEvent.VK_ESCAPE:
            setFullscreen(false);
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

    private void doPopup(MouseEvent e)
    {
        JPopupMenu menu = new ContextMenu();        
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    public void setFullscreen(boolean enabled)
    {
        if(parent == null || !(parent instanceof JFrame))
            return;

        if(enabled && !isFullscreen())
        {
            if(parent instanceof RootPaneContainer)
            {
                RootPaneContainer pframe = (RootPaneContainer)parent;
                pframe.getContentPane().removeAll();
                parent.setVisible(false);
            }
            
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
            fullscreen.addKeyListener(this);

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if(fullscreen!=null)
                        fullscreen.toFront();}});
            
            nami.requestResize(size.width, size.height);
        }
        else if(!enabled && isFullscreen())
        {
            fullscreen.getContentPane().removeAll();
            fullscreen.setVisible(false);
            fullscreen.removeKeyListener(this);
            fullscreen = null;
            
            RootPaneContainer pframe = (RootPaneContainer)parent;
            pframe.getContentPane().add(this);
            parent.setVisible(true);
            
            EventQueue.invokeLater(new Runnable() {
                public void run() {((JFrame)parent).toFront();}});
            
            nami.requestResize(parent.getWidth(), parent.getHeight());
        }
    }
    
    public boolean isFullscreen()
    {
        return fullscreen != null;
    }    
  
    class ContextMenu extends JPopupMenu implements ActionListener
    {
        NamiApplet applet;
        
        public ContextMenu()
        {
            if(parent instanceof NamiApplet)
                applet = (NamiApplet)parent;
            
            JCheckBoxMenuItem ui = new JCheckBoxMenuItem("Show UI");
            ui.addActionListener(this);
            ui.setState(showUI);
            add(ui);

            JCheckBoxMenuItem multisample = new JCheckBoxMenuItem("Multisampling");
            multisample.addActionListener(this);
            multisample.setState(nami.getAntialiasing());
            add(multisample);
            
            JMenu themeMenu = new JMenu("Theme");            
            for(Theme t : themes)
            {
                JCheckBoxMenuItem item;                
                if(t == themes.get(0))
                    item = new JCheckBoxMenuItem("Default");
                else
                    item = new JCheckBoxMenuItem(t.getName());
                item.setActionCommand("Theme > " + t.getName());
                item.addActionListener(this);                
                if(nami.getTheme() != null)
                    item.setState(nami.getTheme().getName().equals(t.getName()));
                themeMenu.add(item);
            }
            add(themeMenu);
              
            if(applet != null && applet.hasURL())
            {
                add(new JSeparator());;
                
                JMenuItem copyItem = new JMenuItem("Copy URL To Clipboard");
                add(copyItem);
                copyItem.addActionListener(this);
            }     
        }
    
        public void actionPerformed(ActionEvent e)
        {
            String command = e.getActionCommand();
            
            if(command.startsWith("Theme > "))
            {
                Theme theme = getTheme(command.substring(8));
                if(theme != null)
                    nami.setTheme(theme);
            }
            else if(command.startsWith("Copy URL"))
            {
                applet.copyURLToClipboard();
            }
            else if(command.startsWith("Show UI"))
            {
                showUI = !showUI;
            }
            else if(command.startsWith("Multisampling"))
            {
                nami.setAntialiasing(!nami.getAntialiasing());
            }
        }
    }
}