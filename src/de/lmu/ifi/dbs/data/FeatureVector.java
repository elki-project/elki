package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

/**
 * Interface FeatureVector defines the methods that should be implemented by any
 * Object that is element of a real vector space.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface FeatureVector<T extends Number> extends MetricalObject<FeatureVector<T>>
{
    
    /**
     * Returns a new FeatureVector of T for the given values.
     * 
     * 
     * @param values the values of the featureVector
     * @return a new FeatureVector of T for the given values
     * @throws NoSuchMethodException 
     * @throws SecurityException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws IllegalArgumentException 
     */
    FeatureVector<T> newInstance(T[] values) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException;
    
    /**
     * Returns a FeatureVector of T with random values.
     * 
     * 
     * @return a FeatureVector of T with random values
     */
    FeatureVector<T> randomInstance(Random random);
    
    /**
     * Returns a FeatureVector of T with random values between min and max.
     * 
     * 
     * @return a FeatureVector of T with random values between min and max
     */
    FeatureVector<T> randomInstance(T min, T max, Random random);
    
    
    /**
     * The dimensionality of the vector space whereof this RealVector is an
     * element.
     * 
     * @return the number of dimensions of this RealVector
     */
    int getDimensionality();

    /**
     * Returns the value in the specified dimension.
     * 
     * @param dimension
     *            the desired dimension, where 1 &le; dimension &le;
     *            <code>this.getDimensionality()</code>
     * @return the value in the specified dimension
     */
    T getValue(int dimension);

    /**
     * Returns a clone of the values of this FeatureVector.
     * 
     * @return a clone of the values of this FeatureVector
     */
    T[] getValues();

    /**
     * Returns a Matrix representing in one column and
     * <code>getDimensionality()</code> rows the values of this FeatureVector.
     * 
     * @return a Matrix representing in one column and
     *         <code>getDimensionality()</code> rows the values of this
     *         FeatureVector
     */
    Matrix getVector();

    /**
     * Returns a new FeatureVector that is the sum of this FeatureVector and the
     * given FeatureVector.
     * 
     * @param fv
     *            a FeatureVector to be added to this Featurevector
     * @return a new FeatureVector that is the sum of this FeatureVector and the
     *         given FeatureVector
     */
    FeatureVector<T> plus(FeatureVector<T> fv);

    /**
     * Provides a null vector of the same Feature Vector Space as this
     * FeatureVector (that is, of the same dimensionality).
     * 
     * @return a null vector of the same Feature Vector Space as this
     *         FeatureVector (that is, of the same dimensionality)
     */
    FeatureVector<T> nullVector();

    /**
     * Returns the additive inverse to this FeatureVector.
     * 
     * @return the additive inverse to this FeatureVector
     */
    FeatureVector<T> negativeVector();

    /**
     * Returns a new FeatureVector that is the result of a scalar multiplication
     * with the given scalar.
     * 
     * @param k
     *            a scalar to multiply this FeatureVector with
     * @return a new FeatureVector that is the result of a scalar multiplication
     *         with the given scalar
     */
    FeatureVector<T> multiplicate(double k);

    /**
     * Returns a String representation of the FeatureVector as a line that is
     * suitable to be printed in a sequential file.
     * 
     * @return a String representation of the FeatureVector
     */
    String toString();
    
    
}
