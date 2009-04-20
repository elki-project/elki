package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialObject;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

/**
 * Interface FeatureVector defines the methods that should be implemented by any
 * Object that is element of a real vector space of type N.
 * 
 * 
 * @param <V> the type of FeatureVector implemented by a subclass
 * @param <N> the type of the attribute values
 * 
 * @author Arthur Zimek 
 */
public interface FeatureVector<V extends FeatureVector<V,N>,N extends Number> extends DatabaseObject, SpatialObject
{

    /**
     * Returns a new FeatureVector of V for the given values.
     * 
     * @param values
     *            the values of the featureVector
     * @return a new FeatureVector of V for the given values
     */
    V newInstance(N[] values);


    /**
     * Returns a new FeatureVector of V for the given values.
     * 
     * @param values
     *            the values of the featureVector
     * @return a new FeatureVector of V for the given values
     */
    V newInstance(List<N> values);
    
    /**
     * Returns a FeatureVector of V with uniformly distributed (0-1) random values.
     * 
     * @param random
     *            a Random instance
     * @return a FeatureVector of V with random values
     */
    V randomInstance(Random random);

    /**
     * Returns a FeatureVector of V with random values between min and max.
     * 
     * @param min
     *            minimum of random value
     * @param max
     *            maximum of random value
     * @param random
     *            a random instance
     * @return a FeatureVector of V with random values between min and max
     */
    V randomInstance(N min, N max, Random random);
    
    /**
     * Returns a FeatureVector of V with random values between min and max.
     * 
     * @param min
     *            minimum of random value for each axis
     * @param max
     *            maximum of random value for each axis
     * @param random
     *            a random instance
     * @return a FeatureVector of V with random values between min and max
     */
    V randomInstance(V min, V max, Random random);


    /**
     * The dimensionality of the vector space whereof this FeatureVector is an
     * element.
     * 
     * @return the number of dimensions of this FeatureVector
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
    N getValue(int dimension);

    /**
     * Returns a Vector representing in one column and
     * <code>getDimensionality()</code> rows the values of this FeatureVector.
     * 
     * @return a Matrix representing in one column and
     *         <code>getDimensionality()</code> rows the values of this
     *         FeatureVector
     */
    Vector getColumnVector();

    /**
     * Returns a Matrix representing in one row and
     * <code>getDimensionality()</code> columns the values of this
     * FeatureVector.
     * 
     * @return a Matrix representing in one row and
     *         <code>getDimensionality()</code> columns the values of this
     *         FeatureVector
     */
    Matrix getRowVector();

    /**
     * Returns a new FeatureVector that is the sum of this FeatureVector and the
     * given FeatureVector.
     * 
     * @param fv
     *            a FeatureVector to be added to this Featurevector
     * @return a new FeatureVector that is the sum of this FeatureVector and the
     *         given FeatureVector
     */
    V plus(V fv);

    /**
     * Provides a null vector of the same Feature Vector Space as this
     * FeatureVector (that is, of the same dimensionality).
     * 
     * @return a null vector of the same Feature Vector Space as this
     *         FeatureVector (that is, of the same dimensionality)
     */
    V nullVector();

    /**
     * Returns the additive inverse to this FeatureVector.
     * 
     * @return the additive inverse to this FeatureVector
     */
    V negativeVector();

    /**
     * Returns a new FeatureVector that is the result of a scalar multiplication
     * with the given scalar.
     * 
     * @param k
     *            a scalar to multiply this FeatureVector with
     * @return a new FeatureVector that is the result of a scalar multiplication
     *         with the given scalar
     */
    V multiplicate(double k);

    /**
     * Returns a String representation of the FeatureVector as a line that is
     * suitable to be printed in a sequential file.
     * 
     * @return a String representation of the FeatureVector
     */
    String toString();

}
