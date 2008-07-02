package de.lmu.ifi.dbs.elki.distance;

/**
 * Provides a Distance for a number-valued distance.
 * 
 * @author Elke Achtert 
 */
public abstract class NumberDistance<D extends NumberDistance<D>> extends
        AbstractDistance<D>
{

    /**
     * @see Distance
     */
    public String description()
    {
        return "distance";
    }

    /**
     * Returns the double value of this distance.
     * 
     * @return the double value of this distance
     */
    public abstract double getDoubleValue();
}
