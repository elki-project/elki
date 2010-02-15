package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.AbstractPreferenceVectorBasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreferenceVectorPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Distance function used in the HiSC algorithm.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public class HiSCDistanceFunction<V extends NumberVector<V,?>, P extends PreferenceVectorPreprocessor<V>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction<V, P> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public HiSCDistanceFunction(Parameterization config) {
    super(config);
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

    if(Math.max(dist1, dist2) > getEpsilon()) {
      subspaceDim++;
      if(logger.isVerbose()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\ndist1 " + dist1);
        msg.append("\ndist2 " + dist2);
        msg.append("\nv1 " + getDatabase().getAssociation(AssociationID.LABEL, v1.getID()));
        msg.append("\nv2 " + getDatabase().getAssociation(AssociationID.LABEL, v2.getID()));
        msg.append("\nsubspaceDim " + subspaceDim);
        msg.append("\ncommon pv " + FormatUtil.format(dim, commonPreferenceVector));
        logger.verbose(msg.toString());
      }
    }

    // flip commonPreferenceVector for distance computation in common subspace
    BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
    inverseCommonPreferenceVector.flip(0, dim);

    return new PreferenceVectorBasedCorrelationDistance(getDatabase().dimensionality(), subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link HiSCPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return HiSCPreprocessor.class;
  }
}
