package de.lmu.ifi.dbs.elki.data;

import java.util.List;

/**
 * Generic FeatureVector class that can contain any type of data (i.e. numerical
 * or categorical attributes). See {@link NumberVector} for vectors that
 * actually store numerical features.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector class
 * @param <D> Data type
 */
public interface FeatureVector<V extends FeatureVector<V, D>, D> extends DatabaseObject {
  /**
   * Returns a new FeatureVector of V for the given values.
   * 
   * @param values the values of the featureVector
   * @return a new FeatureVector of V for the given values
   */
  V newInstance(D[] values);

  /**
   * Returns a new FeatureVector of V for the given values.
   * 
   * @param values the values of the featureVector
   * @return a new FeatureVector of V for the given values
   */
  V newInstance(List<D> values);

  /**
   * The dimensionality of the vector space where of this FeatureVector of V is
   * an element.
   * 
   * @return the number of dimensions of this FeatureVector of V
   */
  int getDimensionality();

  /**
   * Returns the value in the specified dimension.
   * 
   * @param dimension the desired dimension, where 1 &le; dimension &le;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  D getValue(int dimension);

  /**
   * Returns a String representation of the FeatureVector of V as a line that is
   * suitable to be printed in a sequential file.
   * 
   * @return a String representation of the FeatureVector of V
   */
  String toString();
}