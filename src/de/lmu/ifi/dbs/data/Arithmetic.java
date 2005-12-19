package de.lmu.ifi.dbs.data;

/**
 * An interface to define requirements for a number
 * to perform arithmetic operations. The Number
 * are supposed to remain unchanged by an arithmetic operation. 
 *  
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Arithmetic<N extends Number> extends Comparable<N>
{
    /**
     * Adds the given number to this number.
     * 
     * @param number the number to add to this number.
     * @return the result of
     * arithmetic addition of this Number with the given number
     */
    public N plus(N number);
    
    /**
     * Multiplies this number with the given number.
     * 
     * @param number the number to multiply this number with
     * @return the result of
     * arithmetic multiplication of this Number with the given number
     */
    public N times(N number);
    
    /**
     * Subtracts the given number from this number.
     * 
     * @param number the number to subtract from this number
     * @return the result of
     * arithmetic subtraction of the given number from this Number
     */
    public N minus(N number);
    
    /**
     * Divides this number by the given number.
     * 
     * @param number the number to divide this number by
     * @return the result of
     * arithmetic division of this Number by the given number
     */
    public N divided(N number);
}
