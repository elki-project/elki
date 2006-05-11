package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;

import java.util.BitSet;

/**
 * XXX unify CorrelationDistanceFunction and VarianceDistanceFunction
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PreferenceVectorBasedCorrelationDistanceFunction extends CorrelationBasedDistanceFunction {
  static {
    ASSOCIATION_ID = AssociationID.PREFERENCE_VECTOR;
    PREPROCESSOR_SUPER_CLASS = HiSCPreprocessor.class;
    DEFAULT_PREPROCESSOR_CLASS = HiSCPreprocessor.class.getName();
    PREPROCESSOR_CLASS_D = "<class>the preprocessor to determine the preference vectors of the objects "
                           + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(PREPROCESSOR_SUPER_CLASS)
                           + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS;
  }


  /**
   * @see CorrelationBasedDistanceFunction#correlationDistance(de.lmu.ifi.dbs.data.RealVector, de.lmu.ifi.dbs.data.RealVector)
   */
  protected CorrelationDistance correlationDistance(RealVector dv1, RealVector dv2) {
    BitSet preferenceVector1 = (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, dv1.getID());
    BitSet preferenceVector2 = (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, dv2.getID());
    BitSet commonPreferenceVector = (BitSet) preferenceVector1.clone();
    commonPreferenceVector.and(preferenceVector2);
    int dim = dv1.getDimensionality();
    Integer lambda = dim - commonPreferenceVector.cardinality();
    if (weightedDistance(dv1, dv2, preferenceVector1) > ((HiSCPreprocessor) preprocessor).getAlpha()) {
      lambda++;
    }
    commonPreferenceVector.flip(0, dim);

    return new CorrelationDistance(lambda, weightedDistance(dv1, dv2, commonPreferenceVector));
  }

  /**
   * Computes the weighted distance between the two specified vectors according to the given preference vector.
   *
   * @param dv1          the first vector
   * @param dv2          the second vector
   * @param weightVector the preference vector
   * @return the weighted distance between the two specified vectors according to the given preference vector
   */
  private double weightedDistance(RealVector dv1, RealVector dv2, BitSet weightVector) {
    if (dv1.getDimensionality() != dv2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  first argument: " + dv1.toString() + "\n  second argument: " + dv2.toString());
    }

    double sqrDist = 0;
    for (int i = 1; i <= dv1.getDimensionality(); i++) {
      if (weightVector.get(i - 1)) {
        double manhattanI = dv1.getValue(i).doubleValue() - dv2.getValue(i).doubleValue();
        sqrDist += manhattanI * manhattanI;
      }
    }
    return Math.sqrt(sqrDist);
  }
}
