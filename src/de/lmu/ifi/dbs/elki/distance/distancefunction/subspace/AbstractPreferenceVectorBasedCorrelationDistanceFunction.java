package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractIndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.PreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Abstract super class for all preference vector based correlation distance
 * functions.
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public abstract class AbstractPreferenceVectorBasedCorrelationDistanceFunction<V extends NumberVector<?, ?>, P extends PreferenceVectorIndex<V>> extends AbstractIndexBasedDistanceFunction<V, P, PreferenceVectorBasedCorrelationDistance> {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("distancefunction.epsilon", "The maximum distance between two vectors with equal preference vectors before considering them as parallel.");

  /**
   * Parameter to specify the maximum distance between two vectors with equal
   * preference vectors before considering them as parallel, must be a double
   * equal to or greater than 0.
   * <p>
   * Default value: {@code 0.001}
   * </p>
   * <p>
   * Key: {@code -pvbasedcorrelationdf.epsilon}
   * </p>
   */
  private final DoubleParameter EPSILON_PARAM = new DoubleParameter(EPSILON_ID, new GreaterEqualConstraint(0), 0.001);

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  private double epsilon;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractPreferenceVectorBasedCorrelationDistanceFunction(Parameterization config) {
    super(config);
    config = config.descend(this);

    // parameter epsilon
    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }
  }

  @Override
  public PreferenceVectorBasedCorrelationDistance getDistanceFactory() {
    return PreferenceVectorBasedCorrelationDistance.FACTORY;
  }

  /**
   * Returns epsilon.
   * 
   * @return epsilon
   */
  public double getEpsilon() {
    return epsilon;
  }
  
  @Override
  protected Class<?> getIndexFactoryRestriction() {
    return PreferenceVectorIndex.Factory.class;
  }

  /**
   * Instance to compute the distances on an actual database.
   * 
   * @author Erich Schubert
   */
  abstract public static class Instance<V extends NumberVector<?, ?>, P extends PreferenceVectorIndex<V>> extends AbstractIndexBasedDistanceFunction.Instance<V, P, PreferenceVectorBasedCorrelationDistance, AbstractPreferenceVectorBasedCorrelationDistanceFunction<? super V, ?>> {
    /**
     * The epsilon value
     */
    final double epsilon;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param epsilon Epsilon
     * @param distanceFunction parent distance function
     */
    public Instance(Database<V> database, P preprocessor, double epsilon, AbstractPreferenceVectorBasedCorrelationDistanceFunction<? super V, ?> distanceFunction) {
      super(database, preprocessor, distanceFunction);
      this.epsilon = epsilon;
    }

    @Override
    public PreferenceVectorBasedCorrelationDistance distance(DBID id1, DBID id2) {
      BitSet preferenceVector1 = index.getPreferenceVector(id1);
      BitSet preferenceVector2 = index.getPreferenceVector(id2);
      V v1 = database.get(id1);
      V v2 = database.get(id2);
      return correlationDistance(v1, v2, preferenceVector1, preferenceVector2);
    }

    /**
     * Computes the correlation distance between the two specified vectors
     * according to the specified preference vectors.
     * 
     * @param v1 first vector
     * @param v2 second vector
     * @param pv1 the first preference vector
     * @param pv2 the second preference vector
     * @return the correlation distance between the two specified vectors
     */
    public abstract PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2);

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     * 
     * @param v1 the first vector
     * @param v2 the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according
     *         to the given preference vector
     */
    public double weightedDistance(V v1, V v2, BitSet weightVector) {
      if(v1.getDimensionality() != v2.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
      }

      double sqrDist = 0;
      for(int i = 1; i <= v1.getDimensionality(); i++) {
        if(weightVector.get(i - 1)) {
          double manhattanI = v1.doubleValue(i) - v2.doubleValue(i);
          sqrDist += manhattanI * manhattanI;
        }
      }
      return Math.sqrt(sqrDist);
    }

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     * 
     * @param id1 the id of the first vector
     * @param id2 the id of the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according
     *         to the given preference vector
     */
    public double weightedDistance(DBID id1, DBID id2, BitSet weightVector) {
      return weightedDistance(database.get(id1), database.get(id2), weightVector);
    }

    /**
     * Computes the weighted distance between the two specified data vectors
     * according to their preference vectors.
     * 
     * @param v1 the first vector
     * @param v2 the the second vector
     * @return the weighted distance between the two specified vectors according
     *         to the preference vector of the first data vector
     */
    public double weightedPrefereneceVectorDistance(V v1, V v2) {
      double d1 = weightedDistance(v1, v2, index.getPreferenceVector(v1.getID()));
      double d2 = weightedDistance(v2, v1, index.getPreferenceVector(v2.getID()));

      return Math.max(d1, d2);
    }

    /**
     * Computes the weighted distance between the two specified data vectors
     * according to their preference vectors.
     * 
     * @param id1 the id of the first vector
     * @param id2 the id of the second vector
     * @return the weighted distance between the two specified vectors according
     *         to the preference vector of the first data vector
     */
    public double weightedPrefereneceVectorDistance(DBID id1, DBID id2) {
      return weightedPrefereneceVectorDistance(database.get(id1), database.get(id2));
    }
  }
}