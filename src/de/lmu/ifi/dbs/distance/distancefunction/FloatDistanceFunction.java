package de.lmu.ifi.dbs.distance.distancefunction;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractDistanceFunction;
import de.lmu.ifi.dbs.distance.FloatDistance;

/**
 * Provides a DistanceFunction that is based on FloatDistance.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class FloatDistanceFunction<O extends DatabaseObject> extends
        AbstractDistanceFunction<O, FloatDistance>
{

    /**
     * Provides a FloatDistanceFunction with a pattern defined to accept Strings
     * that define a non-negative Float.
     */
    protected FloatDistanceFunction()
    {
        super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
    }

    /**
     * An infinite FloatDistance is based on
     * {@link Float#POSITIVE_INFINITY Float.POSITIVE_INFINITY}.
     * 
     * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#infiniteDistance()
     */
    public FloatDistance infiniteDistance()
    {
        return new FloatDistance(1.0F / 0.0F);
    }

    /**
     * A null FloatDistance is based on 0.
     * 
     * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#nullDistance()
     */
    public FloatDistance nullDistance()
    {
        return new FloatDistance(0);
    }

    /**
     * An undefined FloatDistance is based on {@link Float#NaN Float.NaN}.
     * 
     * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#undefinedDistance()
     */
    public FloatDistance undefinedDistance()
    {
        return new FloatDistance(0.0F / 0.0F);
    }

    /**
     * As pattern is required a String defining a Float.
     * 
     * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#valueOf(String)
     */
    public FloatDistance valueOf(String pattern)
            throws IllegalArgumentException
    {
        if (pattern.equals(INFINITY_PATTERN))
            return infiniteDistance();

        if (matches(pattern))
        {
            return new FloatDistance(Float.parseFloat(pattern));
        } else
        {
            throw new IllegalArgumentException("Given pattern \"" + pattern
                                               + "\" does not match required pattern \""
                                               + requiredInputPattern() + "\"");
        }
    }
}
