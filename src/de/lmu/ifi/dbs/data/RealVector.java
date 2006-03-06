package de.lmu.ifi.dbs.data;

/**
 * RealVector is an abstract super class for all feature vectors having real numbers as values.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class RealVector<N extends Number> extends NumberVector<N> {
  /**
   * Returns a new RealVector of N for the given values.
   *
   * @param values the values of the featureVector
   * @return a new FeatureVector of T for the given values
   */
  public abstract RealVector<N> newInstance(double[] values);

}
