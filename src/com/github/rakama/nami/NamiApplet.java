package com.github.rakama.nami;

import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JApplet;

@SuppressWarnings("serial")
public class NamiApplet extends JApplet
{
    Namibrot nami;
    ComponentListener resize;
    
    public void init()
    {
        if(nami != null)
            return;

        nami = new Namibrot(this, this.getWidth(), this.getHeight());        
        getContentPane().add(nami);
        
        resize = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                nami.requestResize(getWidth(), getHeight());}};        
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
}
