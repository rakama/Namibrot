package com.github.rakama.nami;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JApplet;

import com.github.rakama.nami.theme.Theme;

@SuppressWarnings("serial")
public class NamiApplet extends JApplet
{
    Namibrot nami;
    ComponentListener resize;
    String url;
    
    public void init()
    {
        if(nami != null)
            return;

        nami = new Namibrot(this, this.getWidth(), this.getHeight());        
        getContentPane().add(nami);
        
        resize = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                nami.requestResize(getWidth(), getHeight());}};

        double r = getValue(getParameter("r"));
        double i = getValue(getParameter("i"));
        double z = getValue(getParameter("z"));
        Theme t = nami.getGUI().getTheme(getParameter("t"));
        url = getParameter("url");
        if(url != null)
            url = url.split("\\?")[0];

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
        
        nami.init();        
        addComponentListener(resize);
        nami.forceRefresh();     
    }
    
    public void stop()
    {
        if(nami == null)
            return;

        nami.stop();
        nami.interrupt();   
        removeComponentListener(resize);
    }

    public void destroy()
    {
        if(nami == null)
            return;
        
        nami.stop();
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
        
        builder.append("r=");
        builder.append(nami.getReal());
        
        builder.append("&");

        builder.append("i=");
        builder.append(nami.getImaginary());
        
        builder.append("&");

        builder.append("z=");
        builder.append(nami.getZoom());
        
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
