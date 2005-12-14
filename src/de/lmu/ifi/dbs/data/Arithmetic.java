package de.lmu.ifi.dbs.data;

/**
 * An interface to define requirements for a number
 * to perform arithmetic operations.
 *  
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Arithmetic<N extends Number> extends Comparable<N>
{
    /**
     * Adds the given number to this number.
     * 
     * @param number the number to add to this number.
     */
    public void plus(N number);
    
    /**
     * Multiplies this number with the given number.
     * 
     * @param number the number to multiply this number with
     */
    public void times(N number);
    
    /**
     * Subtracts the given number from this number.
     * 
     * @param number the number to subtract from this number
     */
    public void minus(N number);
    
    /**
     * Divides this number by the given number.
     * 
     * @param number the number to divide this number by
     */
    public void divided(N number);
}
