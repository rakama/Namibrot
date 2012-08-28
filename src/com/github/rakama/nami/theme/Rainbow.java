package com.github.rakama.nami.theme;

public class Rainbow extends Theme
{
    public Rainbow()
    {
        for(int i=0; i<43; i++) // red to orange
        {
            int j = 1 + i;
            r[j] = (byte)255;
            g[j] = (byte)Math.round(255 * (i / 43.0));
            b[j] = 0;
        }

        for(int i=0; i<42; i++) // orange to green
        {
            int j = 43 + i;
            r[j] = (byte)Math.round(255 * ((42 - i) / 42.0));
            g[j] = (byte)255;
            b[j] = 0;
        }

        for(int i=0; i<43; i++) // green to cyan
        {
            int j = 85 + i;
            r[j] = 0;
            g[j] = (byte)255;
            b[j] = (byte)Math.round(255 * (i / 43.0));
        }

        for(int i=0; i<42; i++) // cyan to blue
        {
            int j = 128 + i;
            r[j] = 0;
            g[j] = (byte)Math.round(255 * ((42 - i) / 42.0));
            b[j] = (byte)255;
        }

        for(int i=0; i<43; i++) // blue to magenta
        {
            int j = 170 + i;
            r[j] = (byte)Math.round(255 * (i / 43.0));
            g[j] = 0;
            b[j] = (byte)255;
        }

        for(int i=0; i<43; i++) // magenta to red
        {
            int j = 213 + i;
            r[j] = (byte)255;
            g[j] = 0;
            b[j] = (byte)Math.round(255 * ((42 - i) / 42.0));
        }
        
        setSpeed(-2);
        setDithering(true);
    }    
}
