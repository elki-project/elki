package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * Provides a DistanceFunction that is based on DoubleDistance.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class DoubleDistanceFunction<T extends MetricalObject> extends AbstractDistanceFunction<T>
{

    /**
     * Provides a DoubleDistanceFunction with a pattern defined to accept
     * Strings that define a non-negative Double.
     */
    protected DoubleDistanceFunction()
    {
        super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
    }

    /**
     * An infinite DoubleDistance is based on
     * {@link Double#POSITIVE_INFINITY Double.POSITIVE_INFINITY}.
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#infiniteDistance()
     */
    public Distance infiniteDistance()
    {
        return new DoubleDistance(1.0 / 0.0);
    }

    /**
     * A null DoubleDistance is based on 0.
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#nullDistance()
     */
    public Distance nullDistance()
    {
        return new DoubleDistance(0);
    }

    /**
     * An undefined DoubleDistance is based on {@link Double#NaN Double.NaN}.
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#undefinedDistance()
     */
    public Distance undefinedDistance()
    {
        return new DoubleDistance(0.0 / 0.0);
    }

    /**
     * As pattern is required a String defining a Double.
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#valueOf(java.lang.String)
     */
    public Distance valueOf(String pattern) throws IllegalArgumentException
    {
        if(matches(pattern))
        {
            return new DoubleDistance(Double.parseDouble(pattern));
        }
        else
        {
            throw new IllegalArgumentException("Given pattern \"" + pattern + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
        }
    }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getParameterSettings() {
    return new ArrayList<AttributeSettings>();
  }

}
