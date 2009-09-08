package de.lmu.ifi.dbs.elki.data;

/**
 * RealVector is an abstract super class for all feature vectors having real numbers as values.
 *
 * @param <V> the concrete type of this RealVector
 * @param <N> the type of number, this RealVector consists of (i.e., a RealVector {@code v} of type {@code V}
 *  and dimensionality {@code d} is an element of {@code N}<sup><code>d</code></sup>)
 * 
 * @author Elke Achtert
 */
public interface RealVector<V extends RealVector<V,N>,N extends Number> extends NumberVector<V,N> {
  /**
   * Returns a new RealVector of N for the given values.
   *
   * @param values the values of the featureVector
   * @return a new FeatureVector of T for the given values
   */
  public abstract V newInstance(double[] values);

}
