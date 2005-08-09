package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.RealVector;

import java.util.regex.Pattern;

/**
 * Intentionally this is the superclass for any distance function that works in
 * a real vector space. Thus, any algorithm specified for real vector space
 * operations is to use as distance function any Class that extends this class.
 * 
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class RealVectorDistanceFunction extends AbstractDistanceFunction
{

    /**
     * @see de.lmu.ifi.dbs.distance.AbstractDistanceFunction#AbstractDistanceFunction(Pattern)
     */
    public RealVectorDistanceFunction(Pattern pattern)
    {
        super(pattern);
    }
    
    /**
     * The distance between two metrical objects can only be computed
     * if they are RealVectors. The returned distance is then
     * the same as
     * {@link #distance(RealVector, RealVector) distance((RealVector) mo1, (RealVector) mo2)}.
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#distance(de.lmu.ifi.dbs.data.MetricalObject, de.lmu.ifi.dbs.data.MetricalObject)
     */
    public final Distance distance(MetricalObject mo1, MetricalObject mo2)
    {
        try
        {
            RealVector rv1 = (RealVector) mo1;
            RealVector rv2 = (RealVector) mo2;
            return distance(rv1, rv2);
        }
        catch(ClassCastException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    /**
     * Computes the distance between two RealVectors according
     * to this DistanceFunction.
     * 
     * 
     * @param rv1 first RealVector
     * @param rv2 second RealVector
     * @return the distance between two RealVectors according
     * to this DistanceFunction
     */
    public abstract Distance distance(RealVector rv1, RealVector rv2);

}
