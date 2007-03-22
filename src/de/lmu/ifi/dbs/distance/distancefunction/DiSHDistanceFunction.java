package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.BitSet;

/**
 * Distance function used in the DiSH algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSHDistanceFunction<V extends RealVector<V,?>,P extends Preprocessor<V>>
    extends PreferenceVectorBasedCorrelationDistanceFunction<V,P> {
  /**
   * The default preprocessor class name.
   */
  public static final String DEFAULT_PREPROCESSOR_CLASS = DiSHPreprocessor.class.getName();


  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_D = "<class>the preprocessor to determine the preference vectors of the objects "
                                                    + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(PREPROCESSOR_SUPER_CLASS)
                                                    + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS;

  /**
   * Computes the correlation distance between the two specified vectors
   * according to the specified preference vectors.
   *
   * @param v1  first RealVector
   * @param v2  second RealVector
   * @param pv1 the first preference vector
   * @param pv2 the second preference vector
   * @return the correlation distance between the two specified vectors
   */
  public PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2) {
    BitSet commonPreferenceVector = (BitSet) pv1.clone();
    commonPreferenceVector.and(pv2);
    int dim = v1.getDimensionality();

    // number of zero values in commonPreferenceVector
    Integer subspaceDim = dim - commonPreferenceVector.cardinality();

    // special case: v1 and v2 are in parallel subspaces
    if (commonPreferenceVector.equals(pv1) || commonPreferenceVector.equals(pv2)) {
      double d = weightedDistance(v1, v2, commonPreferenceVector);
      if (d > 2 * getEpsilon()) {
        subspaceDim++;
        if (this.debug) {
          StringBuffer msg = new StringBuffer();
          msg.append("\n");
          msg.append("\nd " + d);
          msg.append("\nv1 " + getDatabase().getAssociation(AssociationID.LABEL, v1.getID()));
          msg.append("\nv2 " + getDatabase().getAssociation(AssociationID.LABEL, v2.getID()));
          msg.append("\nsubspaceDim " + subspaceDim);
          msg.append("\ncommon pv " + Util.format(dim, commonPreferenceVector));
          debugFine(msg.toString());
        }
      }
    }

    // flip commonPreferenceVector for distance computation in common subspace
    BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
    inverseCommonPreferenceVector.flip(0, dim);

    return new PreferenceVectorBasedCorrelationDistance(subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
  }

  /**
   * Returns the name of the default preprocessor.
   */
  String getDefaultPreprocessorClassName() {
    return DEFAULT_PREPROCESSOR_CLASS;
  }

  /**
   * Returns the description for parameter preprocessor.
   */
  String getPreprocessorClassDescription() {
    return PREPROCESSOR_CLASS_D;
  }
}
