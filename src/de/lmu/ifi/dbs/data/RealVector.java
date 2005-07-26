package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

/**
 * Interface RealVector defines the methods that should be implemented
 * by any Object that is element of a real vector space.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface RealVector extends MetricalObject
{
    /**
     * The dimensionality of the vector space
     * whereof this RealVector is an element. 
     * 
     * 
     * @return the number of dimensions of this RealVector
     */
    int getDimensionality();
    
    /**
     * Returns the value in the specified dimension.
     * 
     * 
     * @param dimension, where 1 &le; dimension &le; <code>this.getDimensionality()</code>
     * @return the value in the specified dimension
     */
    double getValue(int dimension);
    
    /**
     * Returns a Matrix representing in one column
     * and <code>getDimensionality()</code> rows the values
     * of this Realvector.
     * 
     * 
     * @return a Matrix representing in one column
     * and <code>getDimensionality()</code> rows the values
     * of this Realvector
     */
    Matrix getVector();
    
    /**
     * Returns a new Realvector that is the sum of this RealVector
     * and the given Realvector.
     * 
     * 
     * @param rv a RalVector to be added to this Realvector
     * @return a new Realvector that is the sum of this RealVector
     * and the given Realvector
     */
    RealVector plus(RealVector rv);
    
    /**
     * Provides a null vector of the same Real Vector Space
     * as this RealVector (that is, of the same dimensionality).
     * 
     * 
     * @return
     */
    RealVector nullVector();
    
    /**
     * Returns the additive inverse to this RealVector.
     * 
     * 
     * @return the additive inverse to this RealVector
     */
    RealVector negativeVector();
    
    /**
     * Returns a new RealVector that is the result
     * of a scalar multiplication with the given scalar.
     * 
     * 
     * @param k a scalar to multiply this RealVector with
     * @return a new RealVector that is the result
     * of a scalar multiplication with the given scalar
     */
    RealVector multiplicate(double k);
    
    /**
     * Returns a String representation of the RealVector
     * as a line that is suitable to be printed in a sequential file.
     * 
     * 
     * @return a String representation of the RealVector
     */
    String toString();
}
