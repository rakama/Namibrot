package rakama.namibrot;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JApplet;

import rakama.namibrot.Namibrot.Mode;
import rakama.namibrot.theme.Theme;


@SuppressWarnings("serial")
public class NamiApplet extends JApplet
{
    NamiGUI gui;
    Namibrot nami;
    ComponentListener resize;
    String url;
    
    public void init()
    {
        if(gui != null)
            return;

        gui = new NamiGUI(this, this.getWidth(), this.getHeight());        
        nami = gui.getNamibrot();
        getContentPane().add(gui);
        
        resize = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                nami.requestResize(getWidth(), getHeight());}};

        double r = getValue(getParameter("r"));
        double i = getValue(getParameter("i"));
        double z = getValue(getParameter("z"));
        double rj = getValue(getParameter("rj"));
        double ij = getValue(getParameter("ij"));
        Theme t = gui.getTheme(getParameter("t"));
        url = getParameter("url");
        if(url != null)
            url = url.split("[\\?\\#]+")[0];
        
        if(!Double.isNaN(rj))
        {
            nami.setMode(Mode.Julia);
            nami.setJuliaReal(rj);
        }

        if(!Double.isNaN(ij))
        {
            nami.setMode(Mode.Julia);
            nami.setJuliaImaginary(ij);
        }
        
        if(!Double.isNaN(r))
            nami.setReal(r);

        if(!Double.isNaN(i))
            nami.setImaginary(i);

        if(!Double.isNaN(z))
            nami.setZoom((int)z);

        if(t != null)
            nami.setTheme(t);
    }
        
    protected double getValue(String str)
    {
        try
        {
            return Double.parseDouble(str);
        }
        catch(Exception e)
        {
            return Double.NaN;
        }
    }

    public void start()
    {        
        if(nami == null)
            return;
        
        gui.init();        
        addComponentListener(resize);
        nami.forceRefresh();     
    }
    
    public void stop()
    {
        if(nami == null)
            return;

        gui.stop();
        nami.interrupt();   
        removeComponentListener(resize);
    }

    public void destroy()
    {
        if(nami == null)
            return;
        
        gui.stop();
        nami.interrupt();
        nami = null;
    }
        
    public boolean hasURL()
    {
        return url != null && !url.isEmpty();
    }
    
    public void copyURLToClipboard()
    {
        if(!hasURL())
            return;

        StringBuilder builder = new StringBuilder();
        
        builder.append(url);
        builder.append("?");
        
        if(nami.getMode() == Mode.Julia)
        {
            builder.append("rj=");
            builder.append(nami.getJuliaReal());
            
            builder.append("&");

            builder.append("ij=");
            builder.append(nami.getJuliaImaginary());
            
            builder.append("&");
        }
        
        builder.append("r=");
        builder.append(nami.getReal());
        
        builder.append("&");

        builder.append("i=");
        builder.append(nami.getImaginary());
        
        builder.append("&");

        builder.append("z=");
        builder.append(nami.getZoom());

        builder.append("&");

        builder.append("t=");
        builder.append(nami.getTheme().getName().toLowerCase());
        
        StringSelection str = new StringSelection(builder.toString());
        
        ClipboardService cs = getClipboardService();        
        if(cs == null)
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(str, null);
        else
            cs.setContents(str);
    }
    
    public ClipboardService getClipboardService()
    {
        try
        {
            return (ClipboardService)ServiceManager.lookup("javax.jnlp.ClipboardService");
        }
        catch(UnavailableServiceException e)
        {
            return null;
        }
    }
}
