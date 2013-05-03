package rakama.namibrot;

import rakama.namibrot.fractal.Fractal;
import rakama.namibrot.util.CircularBufferDouble;
import rakama.namibrot.util.CircularBufferInt;

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