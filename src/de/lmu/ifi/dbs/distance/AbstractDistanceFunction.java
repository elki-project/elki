package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.MetricalObject;

import java.util.regex.Pattern;

/**
 * Abstract Distance Function provides some methods valid for any extending
 * class.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDistanceFunction<T extends MetricalObject> implements DistanceFunction<T>
{
    /**
     * Indicates an infintiy pattern.
     */
    public static final String INFINITY_PATTERN = "inf";

    /**
     * A pattern to define the required input format.
     */
    private Pattern pattern;

    /**
     * Provides an abstract DistanceFunction based on the given Pattern.
     * 
     * @param pattern
     *            a pattern to define the required input format
     */
    protected AbstractDistanceFunction(Pattern pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#isInfiniteDistance(de.lmu.ifi.dbs.distance.Distance)
     */
    public boolean isInfiniteDistance(Distance distance)
    {
        return distance.equals(infiniteDistance());
    }

    /**
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#isNullDistance(de.lmu.ifi.dbs.distance.Distance)
     */
    public boolean isNullDistance(Distance distance)
    {
        return distance.equals(nullDistance());
    }

    /**
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#isUndefinedDistance(de.lmu.ifi.dbs.distance.Distance)
     */
    public boolean isUndefinedDistance(Distance distance)
    {
        return distance.equals(undefinedDistance());
    }

    /**
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#requiredInputPattern()
     */
    public String requiredInputPattern()
    {
        return this.pattern.pattern();
    }

    /**
     * Returns true if the given pattern matches the defined pattern, false
     * otherwise.
     * 
     * @param pattern
     *            the pattern to be matched woth the defined pattern
     * @return true if the given pattern matches the defined pattern, false
     *         otherwise
     */
    protected boolean matches(String pattern)
    {
        return this.pattern.matcher(pattern).matches();
    }

    // /**
    // *
    // * @see de.lmu.ifi.dbs.distance.DistanceFunction#infiniteDistance()
    // */
    // abstract public Distance infiniteDistance();
    //
    //
    // /**
    // *
    // * @see de.lmu.ifi.dbs.distance.DistanceFunction#nullDistance()
    // */
    // abstract public Distance nullDistance();
    //
    // /**
    // *
    // * @see de.lmu.ifi.dbs.distance.DistanceFunction#undefinedDistance()
    // */
    // abstract public Distance undefinedDistance();

}
