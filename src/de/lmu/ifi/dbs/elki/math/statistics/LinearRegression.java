package de.lmu.ifi.dbs.elki.math.statistics;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.DoublePair;

import java.util.List;

/**
 * @author Arthur Zimek
 */
public class LinearRegression extends AbstractLoggable
{
    private double t;
    
    private double m;
    
    public LinearRegression(List<DoublePair> points)
    {
        super(LoggingConfiguration.DEBUG);
        double sumy = 0.0;
        double sumx = 0.0;
        double sumyy = 0.0;
        double sumxx = 0.0;
        double sumxy = 0.0;
        int gap = 0;
        for(DoublePair point : points)
        {
            sumy += point.getY();
            sumyy += point.getY()*point.getY();
            gap++;
            sumx += point.getX();
            sumxx += point.getX()*point.getX();
            sumxy += point.getX()*point.getY(); 
        }
        double Sxy = sumxy - sumx * sumy / gap;
        double Sxx = sumxx - sumx * sumx / gap;
        m = Sxy / Sxx;
        t = (sumy - m * sumx) / gap; 
    }

    public double getM()
    {
        return this.m;
    }

    public double getT()
    {
        return this.t;
    }
    
}
