package de.lmu.ifi.dbs.data;

/**
 * Provides a bit number.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
@SuppressWarnings("serial")
public class Bit extends Number
{
    /**
     * Internal representation of the bit value.
     */
    private boolean bit;

    /**
     * Provides a new bit according to the specified boolean value.
     */
    public Bit(boolean bit)
    {
        this.bit = bit;
    }

    /**
     * Provides a new bit according to the specified integer value.
     * The bit value is 1 for true and 0 for false.
     * 
     * @throws IllegalArgumentException if the specified value is neither 0 nor 1.
     */
    public Bit(int bit) throws IllegalArgumentException
    {
        this.bit = bit == 1;
    }

    
    /**
     * 
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue()
    {
        return bit ? 1 : 0;
    }

    /**
     * 
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue()
    {
        return intValue();
    }

    /**
     * 
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue()
    {
        return intValue();
    }

    /**
     * 
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue()
    {
        return intValue();
    }

    /**
     * Returns the bit value as a boolean.
     * 
     * @return the bit value
     */
    public boolean bitValue()
    {
        return this.bit;
    }
    
    /**
     * 
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return Integer.toString(intValue());
    }
}
