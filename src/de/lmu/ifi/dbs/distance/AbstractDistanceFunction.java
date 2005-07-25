package de.lmu.ifi.dbs.distance;

import java.util.regex.Pattern;

/**
 * Abstract Distance Function provides some methods valid for any extending class.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDistanceFunction implements DistanceFunction
{
    /**
     * A pattern to define the required input format.
     */
    protected Pattern pattern;
    
    /**
     * Provides an abstract DistanceFunction based on the given Pattern.
     * 
     * @param pattern a pattern to define the required input format
     */
    protected AbstractDistanceFunction(Pattern pattern)
    {
        this.pattern = pattern;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#isInfiniteDistance(de.lmu.ifi.dbs.distance.Distance)
     */
    public boolean isInfiniteDistance(Distance distance)
    {
        return distance.equals(infiniteDistance());
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#isNullDistance(de.lmu.ifi.dbs.distance.Distance)
     */
    public boolean isNullDistance(Distance distance)
    {
        return distance.equals(nullDistance());
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#isUndefinedDistance(de.lmu.ifi.dbs.distance.Distance)
     */
    public boolean isUndefinedDistance(Distance distance)
    {
        return distance.equals(undefinedDistance());
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#requiredInputPattern()
     */
    public String requiredInputPattern()
    {
        return this.pattern.pattern();
    }
}
