package com.github.rakama.nami.fractal;

public final class Fractal
{
    private final double radius = 10;
    private final double bailout = radius * radius;
    
    private int iteration, maxIterations;
    private double r, i, rr, ii;
    
    public final int getValue(double x0, double y0, int max)
    {
       return getValue(0, 0, x0, y0, 0, max);
    }
    
    public final int getValue(double r0, double i0, double x0, double y0, int offset, int max)
    {
        maxIterations = max;
        iteration = offset;
        r = r0;
        i = i0;
        rr = r*r;
        ii = i*i;
        
        while(rr + ii < bailout && iteration < max)
        {
            {
                double temp = rr - ii + x0;
                double ri = r*i;
                i = ri + ri + y0;
                r = temp;
            }
            
            rr = r*r;
            ii = i*i;
            iteration++;
        }

        return iteration;
    }
    
    public double getReal()
    {
        return r;
    }
    
    public double getImaginary()
    {
        return i;
    }

    public int getMaxIterations()
    {
        return maxIterations;
    }
    
    public int getIterations()
    {
        return iteration;
    }
    
    public double getSmooth()
    {
        double value = 0;
        double magnitude = Math.sqrt(rr + ii);
        
        if(value < maxIterations)
            value = smooth(magnitude, radius);
        
        return value;
    }
    
    private double smooth(double magnitude, double radius)
    {
        return 1 - Math.log(Math.log(magnitude) / Math.log(radius)) / Math.log(2);
    }
}