package com.github.rakama.nami.fractal;

public final class Fractal
{
    private final double radius = 10;
    private final double bailout = radius * radius;
    
    private int iteration, maxIterations;
    private double r, i, rr, ii;
        
    public final int getValueOpt(double r0, double i0, double x0, double y0, int offset, int max)
    {
        maxIterations = max;
        iteration = offset;
        r = r0;
        i = i0;
        rr = r*r;
        ii = i*i;

        if(rr + ii >= bailout || iteration >= max)
            return iteration;
        
        y0/=2;
        
        double i2 = (r*i + y0)*2;
        double r2 = (r - i)*(r + i) + x0; 
        i = (r2*i2 + y0)*2;
        r = (r2 - i2)*(r2 + i2) + x0;        
        iteration+=2;

        while(r*r + i*i < bailout && iteration < max)
        {
            i2 = (r*i + y0)*2;
            r2 = (r - i)*(r + i) + x0;
            i = (r2*i2 + y0)*2;
            r = (r2 - i2)*(r2 + i2) + x0;
            iteration+=2;
        }
        
        double rr2 = r2*r2;
        double ii2 = i2*i2;
        
        if(rr2 + ii2 >= bailout || iteration > max)
        {
            r = r2;
            i = i2;
            rr = rr2;
            ii = ii2;
            iteration--;
        }
        else
            rr = r*r;
            ii = i*i;
        
        return iteration;
    }
    
    public final int getValue(double r0, double i0, double x0, double y0, int offset, int max)
    {
        maxIterations = max;
        iteration = offset;
        r = r0;
        i = i0;
        rr = r*r;
        ii = i*i;
        
        y0/=2;        
        while(rr + ii < bailout && iteration < max)
        {
            i = (r*i + y0)*2;
            r = rr - ii + x0;
            rr = r*r;
            ii = i*i;
          
            iteration++;
        }
        
//        while(rr + ii < bailout && iteration < max)
//        {
//            double temp = rr - ii + x0;
//            double ri = r*i;
//            i = ri + ri + y0;
//            r = temp;            
//            rr = r*r;
//            ii = i*i;
//            iteration++;
//        }

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
    
    public double getDecomposition()
    {
        double ang = Math.atan2(getReal(), getImaginary());
        return (ang + Math.PI) / (Math.PI * 2);
    }
    
    private double smooth(double magnitude, double radius)
    {
        return 1 - Math.log(Math.log(magnitude) / Math.log(radius)) / Math.log(2);
    }
}