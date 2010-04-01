package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.CorrelationDistance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract super class for correlation based distance functions. Provides the
 * correlation distance for real valued vectors.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector used
 * @param <P> the type of Preprocessor used
 * @param <D> the type of CorrelationDistance used
 */
public abstract class AbstractCorrelationDistanceFunction<V extends FeatureVector<V, ?>, P extends Preprocessor<V, ?>, D extends CorrelationDistance<D>> extends AbstractPreprocessorBasedDistanceFunction<V, P, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   * @param distance Distance Factory
   */
  public AbstractCorrelationDistanceFunction(Parameterization config, D distance) {
    super(config, distance);
  }

  /**
   * Provides the Correlation distance between the given two vectors by calling
   * {@link #correlationDistance correlationDistance(v1, v2)}.
   * 
   * @return the Correlation distance between the given two vectors as an
   *         instance of {@link CorrelationDistance CorrelationDistance}.
   */
  public final D distance(V v1, V v2) {
    return correlationDistance(v1, v2);
  }

  /**
   * Computes the correlation distance between the two specified vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return the correlation distance between the two specified vectors
   */
  protected abstract D correlationDistance(V v1, V v2);
}