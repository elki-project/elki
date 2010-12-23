package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Distance function used in the HiSC algorithm.
 * 
 * @author Elke Achtert
 * 
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class HiSCDistanceFunction<V extends NumberVector<?, ?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction<V, HiSCPreferenceVectorIndex<V>> {
  /**
   * Logger for debug.
   */
  static Logging logger = Logging.getLogger(DiSHDistanceFunction.class);

  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public HiSCDistanceFunction(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  protected Class<?> getIndexFactoryRestriction() {
    return HiSCPreferenceVectorIndex.Factory.class;
  }

  @Override
  protected Class<?> getIndexFactoryDefaultClass() {
    return HiSCPreferenceVectorIndex.Factory.class;
  }

  @Override
  public Class<? super V> getInputDatatype() {
    return NumberVector.class;
  }

  @Override
  public <T extends V> Instance<T> instantiate(Database<T> database) {
    // We can't really avoid these warnings, due to a limitation in Java
    // Generics (AFAICT)
    @SuppressWarnings("unchecked")
    HiSCPreferenceVectorIndex<T> indexinst = (HiSCPreferenceVectorIndex<T>) index.instantiate((Database<V>) database);
    return new Instance<T>(database, indexinst, getEpsilon(), this);
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   * 
   * @param <V> the type of NumberVector to compute the distances in between
   */
  public static class Instance<V extends NumberVector<?, ?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction.Instance<V, HiSCPreferenceVectorIndex<V>> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Preprocessed index
     * @param epsilon Epsilon
     * @param distanceFunction parent distance function
     */
    public Instance(Database<V> database, HiSCPreferenceVectorIndex<V> index, double epsilon, HiSCDistanceFunction<? super V> distanceFunction) {
      super(database, index, epsilon, distanceFunction);
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
    @Override
    public PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2) {
      BitSet commonPreferenceVector = (BitSet) pv1.clone();
      commonPreferenceVector.and(pv2);
      int dim = v1.getDimensionality();

      // number of zero values in commonPreferenceVector
      Integer subspaceDim = dim - commonPreferenceVector.cardinality();

      // special case: v1 and v2 are in parallel subspaces
      double dist1 = weightedDistance(v1, v2, pv1);
      double dist2 = weightedDistance(v1, v2, pv2);

      if(Math.max(dist1, dist2) > epsilon) {
        subspaceDim++;
        if(logger.isDebugging()) {
          StringBuffer msg = new StringBuffer();
          msg.append("\ndist1 " + dist1);
          msg.append("\ndist2 " + dist2);
          msg.append("\nv1 " + database.getObjectLabel(v1.getID()));
          msg.append("\nv2 " + database.getObjectLabel(v2.getID()));
          msg.append("\nsubspaceDim " + subspaceDim);
          msg.append("\ncommon pv " + FormatUtil.format(dim, commonPreferenceVector));
          logger.debugFine(msg.toString());
        }
      }

      // flip commonPreferenceVector for distance computation in common subspace
      BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
      inverseCommonPreferenceVector.flip(0, dim);

      return new PreferenceVectorBasedCorrelationDistance(DatabaseUtil.dimensionality(database), subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
    }
  }
}