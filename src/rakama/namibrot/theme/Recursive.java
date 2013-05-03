package rakama.namibrot.theme;

import rakama.namibrot.fractal.Fractal;

public class Recursive extends Rainbow
{
    public Recursive()
    {
        super();
        setSpeed(2);
    }
        
    public int getColor(Fractal fractal)
    {
        double psmooth = fractal.getIterations() + fractal.getSmooth();
        double pvalue = getNormalizedValue(fractal);    
        
      double y = (fractal.getDecompositionAlt() - 0.5) * 6;
      double x = (fractal.getSmooth() - 0.5) * 3;
      fractal.getValueOpt(x, y, fractal.getX(), fractal.getY(), 0, 128);

//        double y = (fractal.getDecompositionAlt() - 0.5) * 9;
//        double x = -(fractal.getSmooth() - 0.5) * 4 - 0.75;
//        fractal.getValueOpt(0, 0, x, y, 0, 128);
        
        double value;
        
        if(fractal.getIterations() >= 128)
        {
            return 0;
        }
        else
        {
            double child = psmooth + fractal.getIterations() + fractal.getSmooth();
            value = child / 16.0;
        }

        double p = 1;

        if(fractal.getIterations() < 3)
            p = 0;
        else if(fractal.getIterations() < 7)
            p = (fractal.getIterations() + fractal.getSmooth() - 3) / 5.0;
        
        value = p * value + (pvalue * (1 - p));
        
        return super.getColorFromValue(value);
    }
}