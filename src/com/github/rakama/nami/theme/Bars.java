package com.github.rakama.nami.theme;

import com.github.rakama.nami.fractal.Fractal;

public class Bars extends Zebra
{
    Theme parent;
    
    public int getColor(Fractal fractal)
    {
        if(fractal.getIterations() % 2 == 0)
            return super.getColorFromValue(fractal.getDecomposition());
        else
            return super.getColorFromValue(1 - fractal.getDecomposition());
    }
}