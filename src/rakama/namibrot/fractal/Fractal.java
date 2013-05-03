package rakama.namibrot.fractal;

public final class Fractal
{
    private final double radius = 10;
    private final double bailout = radius * radius;
    
    private int iteration, maxIterations;
    private double r, i, rr, ii, x, y, theta;
        
    public final int getValueOpt(double r0, double i0, double x0, double y0, int offset, int max)
    {
        maxIterations = max; 
        iteration = offset;
        x = x0;
        y = y0;
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
        x = x0;
        y = y0;
        r = r0;
        i = i0;
        rr = r*r;
        ii = i*i;
        
        while(rr + ii < bailout && iteration < max)
        {
            double temp = rr - ii + x0;
            double ri = r*i;
            i = ri + ri + y0;
            r = temp;            
            rr = r*r;
            ii = i*i;
            iteration++;
        }

        return iteration;
    }

//    public int getValueBot(double r0, double i0, double x0, double y0, int offset, int max)
//    {
//        maxIterations = max;
//        iteration = offset;
//        x = x0;
//        y = y0;
//        r = r0;
//        i = i0;
//        rr = r*r;
//        ii = i*i;
//
//        theta = Math.atan2(i, r);
//        
//        while(rr + ii < bailout && iteration < max)
//        {
//            double temp = rr - ii + x0;
//            double ri = r*i;
//            i = ri + ri + y0;
//            r = temp;            
//            rr = r*r;
//            ii = i*i;
//            
//            double min_r = r - x0;
//            double min_i = i - y0;
//            double arg1 = Math.atan2(i, r);
//            double arg2 = Math.atan2(min_i, min_r);
//            
//            if(arg1 - arg2 > Math.PI)
//                arg2 += Math.PI * 2;
//            else if(arg2 - arg1 > Math.PI)
//                arg1 += Math.PI * 2;
//                                    
//            double div = ((arg1 - arg2) % Math.PI);
//            theta += Math.pow(0.5, iteration) * div;
//                        
//            iteration++;
//        }
//
//        return iteration;
//    }

//    public int getValueWeird(double r0, double i0, double x0, double y0, int offset, int max)
//    {
//        maxIterations = max;
//        iteration = offset;
//        x = x0;
//        y = y0;
//        r = r0;
//        i = i0;
//        rr = r*r;
//        ii = i*i;
//
//        theta = 0;//Math.atan2(i, r);
//        
//        while(rr + ii < bailout && iteration < max)
//        {
//            double temp = rr - ii + x0;
//            double ri = r*i;
//            i = ri + ri + y0;
//            r = temp;            
//            rr = r*r;
//            ii = i*i;
//            
//            double min_r = r - x0;
//            double min_i = i - y0;
//            double div_r = (r*min_r + i*min_i)/(min_r*min_r + min_i*min_i);
//            double div_i = (i*min_r - r*min_i)/(min_r*min_r + min_i*min_i);
//            if(!Double.isNaN(Math.atan2(div_i, div_r)))
//                theta += Math.pow(0.5, iteration) * Math.atan2(div_r, div_i);
//            
//            iteration++;
//        }
//        
//        return iteration;
//    }

    public double getX()
    {
        return x;
    }
    
    public double getY()
    {
        return y;
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
        
        if(value < maxIterations)
            value = smooth(rr + ii, bailout);
        
        return value;
    }
    
    public double getDecomposition()
    {
        double ang = Math.atan2(getImaginary(), getReal());
        return (ang + Math.PI) / (Math.PI * 2);
    }

    public double getDecompositionAlt()
    {
        double ang = Math.atan2(getReal(), getImaginary());
        return (ang + Math.PI) / (Math.PI * 2);
    }
    
    public double getTheta()
    {
        return theta;
    }
    
//    private static double bottcher(double r, double i, double x0, double y0)
//    {   
//        double v = arg(r, i);
//        
//        for(int n=0; n<10000; n++)
//        {
//            // div (r, i) / (r,i - c)
//            double div_r = 0;
//            double div_i = 0;
//            v += Math.pow(0.5, n) * arg(div_r, div_i);
//        }
//        
//        return v;
//    }
    
    private static double smooth(double magnitude, double radius)
    {
        return 1 - Math.log(Math.log(magnitude) / Math.log(radius)) / Math.log(2);
    }
}