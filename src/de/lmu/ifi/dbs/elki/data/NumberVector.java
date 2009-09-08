package de.lmu.ifi.dbs.elki.data;

import java.util.Random;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialObject;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

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
public interface NumberVector<V extends NumberVector<V, N>, N extends Number> extends FeatureVector<V,N>, SpatialObject {
  /**
   * Returns a FeatureVector of V with uniformly distributed (0-1) random
   * values.
   * 
   * @param random a Random instance
   * @return a FeatureVector of V with random values
   */
  V randomInstance(Random random);

  /**
   * Returns a FeatureVector of V with random values between min and max.
   * 
   * @param min minimum of random value
   * @param max maximum of random value
   * @param random a random instance
   * @return a FeatureVector of V with random values between min and max
   */
  V randomInstance(N min, N max, Random random);

  /**
   * Returns a FeatureVector of V with random values between min and max.
   * 
   * @param min minimum of random value for each axis
   * @param max maximum of random value for each axis
   * @param random a random instance
   * @return a FeatureVector of V with random values between min and max
   */
  V randomInstance(V min, V max, Random random);

  /**
   * Returns a Vector representing in one column and
   * <code>getDimensionality()</code> rows the values of this FeatureVector of V.
   * 
   * @return a Matrix representing in one column and
   *         <code>getDimensionality()</code> rows the values of this
   *         FeatureVector of V
   */
  Vector getColumnVector();

  /**
   * Returns a Matrix representing in one row and
   * <code>getDimensionality()</code> columns the values of this FeatureVector of V.
   * 
   * @return a Matrix representing in one row and
   *         <code>getDimensionality()</code> columns the values of this
   *         FeatureVector of V
   */
  Matrix getRowVector();

  /**
   * Returns a new FeatureVector of V that is the sum of this FeatureVector of V and the
   * given FeatureVector of V.
   * 
   * @param fv a FeatureVector of V to be added to this FeatureVector of V
   * @return a new FeatureVector of V that is the sum of this FeatureVector of V and the
   *         given FeatureVector of V
   */
  V plus(V fv);

  /**
   * Provides a null vector of the same Feature Vector Space as this
   * FeatureVector of V (that is, of the same dimensionality).
   * 
   * @return a null vector of the same Feature Vector Space as this
   *         FeatureVector of V (that is, of the same dimensionality)
   */
  V nullVector();

  /**
   * Returns the additive inverse to this FeatureVector of V.
   * 
   * @return the additive inverse to this FeatureVector of V
   */
  V negativeVector();

  /**
   * Returns a new FeatureVector of V that is the result of a scalar multiplication
   * with the given scalar.
   * 
   * @param k a scalar to multiply this FeatureVector of V with
   * @return a new FeatureVector of V that is the result of a scalar multiplication
   *         with the given scalar
   */
  V multiplicate(double k);
}
