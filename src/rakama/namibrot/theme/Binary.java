package rakama.namibrot.theme;

import rakama.namibrot.fractal.Fractal;

public class Binary extends Theme
{
    public boolean update(long milliseconds)
    {
        return true;
    }

    public int getColor(Fractal fractal)
    {
        if(fractal.getImaginary() > 0)
            return 255;
        else
            return 0;
    }
}