package de.lmu.ifi.dbs.data;

/**
 * An interface to define requirements for a number
 * to perform arithmetic operations. The operations
 * are to be done on the Number, i.e., a Number is
 * changed by an arithmetic operation. 
 *  
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Arithmetic<N extends Number> extends Comparable<N>
{
    /**
     * Adds the given number to this number.
     * This number is supposed to be the result of
     * arithmetic addition with the given number
     * after calling this method.
     * 
     * @param number the number to add to this number.
     */
    public void plus(N number);
    
    /**
     * Multiplies this number with the given number.
     * This number is supposed to be the result of
     * arithmetic multiplication with the given number
     * after calling this method.
     * 
     * @param number the number to multiply this number with
     */
    public void times(N number);
    
    /**
     * Subtracts the given number from this number.
     * This number is supposed to be the result of
     * arithmetic subtraction with the given number
     * after calling this method.
     * 
     * @param number the number to subtract from this number
     */
    public void minus(N number);
    
    /**
     * Divides this number by the given number.
     * This number is supposed to be the result of
     * arithmetic division with the given number
     * after calling this method.
     * 
     * @param number the number to divide this number by
     */
    public void divided(N number);
}
