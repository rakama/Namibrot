package com.github.rakama.nami;

import com.github.rakama.nami.fractal.Fractal;
import com.github.rakama.nami.util.CircularBufferDouble;
import com.github.rakama.nami.util.CircularBufferInt;

public class RenderContext
{
    final CircularBufferInt queue, pending;
    final CircularBufferDouble partial;
    final Fractal fractal;
    boolean rendering;
    
    public RenderContext(int queueSize)
    {
        queue = new CircularBufferInt(queueSize);
        pending = new CircularBufferInt(queueSize);
        partial = new CircularBufferDouble(queueSize/4);
        fractal = new Fractal();
        rendering = false;
    }
}