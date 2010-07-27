package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreferenceVectorPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Distance function used in the DiSH algorithm.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public class DiSHDistanceFunction extends AbstractPreferenceVectorBasedCorrelationDistanceFunction<NumberVector<?, ?>, PreferenceVectorPreprocessor<NumberVector<?, ?>>> {
  /**
   * Logger for debug.
   */
  static Logging logger = Logging.getLogger(DiSHDistanceFunction.class);

  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public DiSHDistanceFunction(Parameterization config) {
    super(config);
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link DiSHPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return DiSHPreprocessor.class;
  }

  @Override
  public Class<? super NumberVector<?, ?>> getInputDatatype() {
    return NumberVector.class;
  }

  @Override
  public <T extends NumberVector<?, ?>> Instance<T> instantiate(Database<T> database) {
    return new Instance<T>(database, getPreprocessor().instantiate(database), getEpsilon(), this);
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<V extends NumberVector<?, ?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction.Instance<V, PreferenceVectorPreprocessor.Instance<V>> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param epsilon Epsilon
     * @param distanceFunction parent distance function
     */
    public Instance(Database<V> database, PreferenceVectorPreprocessor.Instance<V> preprocessor, double epsilon, DiSHDistanceFunction distanceFunction) {
      super(database, preprocessor, epsilon, distanceFunction);
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
      if(commonPreferenceVector.equals(pv1) || commonPreferenceVector.equals(pv2)) {
        double d = weightedDistance(v1, v2, commonPreferenceVector);
        if(d > 2 * epsilon) {
          subspaceDim++;
          if(logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append("d ").append(d);
            msg.append("\nv1 ").append(database.getObjectLabel(v1.getID()));
            msg.append("\nv2 ").append(database.getObjectLabel(v2.getID()));
            msg.append("\nsubspaceDim ").append(subspaceDim);
            msg.append("\ncommon pv ").append(FormatUtil.format(dim, commonPreferenceVector));
            logger.debugFine(msg.toString());
          }
        }
      }

      // flip commonPreferenceVector for distance computation in common subspace
      BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
      inverseCommonPreferenceVector.flip(0, dim);

      return new PreferenceVectorBasedCorrelationDistance(database.dimensionality(), subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
    }
  }

  /**
   * Get the minpts value.
   * 
   * @return
   */
  public int getMinpts() {
    // FIXME: get rid of this cast.
    return ((DiSHPreprocessor)getPreprocessor()).getMinpts();
  }
}