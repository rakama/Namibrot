package com.github.rakama.nami.theme;

import com.github.rakama.nami.fractal.Fractal;

public class Spots extends Rainbow
{
    public Spots()
    {
        super();
        setNormalizer(0.5);
    }        
        
    public int getColor(Fractal fractal)
    {
        double x = (fractal.getDecomposition() - 0.5) * 4;
        double y = (fractal.getSmooth() - 0.5) * 2;
        
        if(x*x + y*y < 0.9)
            return ((32 + super.getColor(fractal)) % 254) + 1;   
        else
            return super.getColor(fractal);        
    }
}