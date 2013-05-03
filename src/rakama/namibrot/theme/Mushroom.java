package rakama.namibrot.theme;

import rakama.namibrot.fractal.Fractal;

public class Mushroom extends Rainbow
{
    public Mushroom()
    {
        super();
        setNormalizer(0.5);
        aliases.add("Spots");
    }
        
    public int getColor(Fractal fractal)
    {
        double x = (fractal.getDecompositionAlt() - 0.5) * 4;
        double y = (fractal.getSmooth() - 0.5) * 2;        
        if(x*x + y*y < 0.9)
            return ((240 + super.getColor(fractal)) % 254) + 1;   
        else
            return super.getColor(fractal);        
    }
}