package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.preprocessing.PreferenceVectorPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * XXX unify CorrelationDistanceFunction and VarianceDistanceFunction
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PreferenceVectorBasedCorrelationDistanceFunction extends CorrelationDistanceFunction<PreferenceVectorBasedCorrelationDistance> {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"FieldCanBeLocal"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  static {
    ASSOCIATION_ID = AssociationID.PREFERENCE_VECTOR;
    PREPROCESSOR_SUPER_CLASS = PreferenceVectorPreprocessor.class;
    DEFAULT_PREPROCESSOR_CLASS = DiSHPreprocessor.class.getName();
    PREPROCESSOR_CLASS_D = "<class>the preprocessor to determine the preference vectors of the objects "
                           + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(PREPROCESSOR_SUPER_CLASS)
                           + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS;
  }

  /**
   * Provides a distance suitable to this DistanceFunction based on the given
   * pattern.
   *
   * @param pattern A pattern defining a distance suitable to this
   *                DistanceFunction
   * @return a distance suitable to this DistanceFunction based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this DistanceFunction
   */
  public PreferenceVectorBasedCorrelationDistance valueOf(String pattern)
  throws IllegalArgumentException {
    if (pattern.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if (matches(pattern)) {
      String[] values = CorrelationDistanceFunction.SEPARATOR.split(pattern);
      return new PreferenceVectorBasedCorrelationDistance(Integer.parseInt(values[0]), Double.parseDouble(values[1]), new BitSet());
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" +
                                         pattern +
                                         "\" does not match required pattern \"" +
                                         requiredInputPattern() + "\"");
    }
  }

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  public PreferenceVectorBasedCorrelationDistance infiniteDistance() {
    return new PreferenceVectorBasedCorrelationDistance(Integer.MAX_VALUE, Double.POSITIVE_INFINITY, new BitSet());
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  public PreferenceVectorBasedCorrelationDistance nullDistance() {
    return new PreferenceVectorBasedCorrelationDistance(0, 0, new BitSet());
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  public PreferenceVectorBasedCorrelationDistance undefinedDistance() {
    return new PreferenceVectorBasedCorrelationDistance(-1, Double.NaN, new BitSet());
  }


  /**
   * @see CorrelationDistanceFunction#correlationDistance(de.lmu.ifi.dbs.data.RealVector, de.lmu.ifi.dbs.data.RealVector)
   */
  protected PreferenceVectorBasedCorrelationDistance correlationDistance(RealVector v1, RealVector v2) {
    BitSet preferenceVector1 = (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, v1.getID());
    BitSet preferenceVector2 = (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, v2.getID());
    BitSet commonPreferenceVector = (BitSet) preferenceVector1.clone();
    commonPreferenceVector.and(preferenceVector2);
    int dim = v1.getDimensionality();

    // number of zero values in commonPreferenceVector
    Integer subspaceDim = dim - commonPreferenceVector.cardinality();

    // special case: v1 and v2 are in parallel subspaces 
    if (preferenceVector1.equals(preferenceVector2)) {
      double d1 = weightedDistance(v1, v2, preferenceVector1);
      double d2 = weightedDistance(v1, v2, preferenceVector2);
      if (Math.max(d1, d2) > 2 * ((DiSHPreprocessor) preprocessor).getEpsilon().getDoubleValue()) {
        subspaceDim++;
        if (DEBUG) {
          StringBuffer msg = new StringBuffer();
          msg.append("\nd1 " + d1);
          msg.append("\nd2 " + d2);
          msg.append("\nv1 " + getDatabase().getAssociation(AssociationID.LABEL, v1.getID()));
          msg.append("\nv2 " + getDatabase().getAssociation(AssociationID.LABEL, v2.getID()));
          msg.append("\nsubspaceDim " + subspaceDim);
          msg.append("\ncommon pv " + Util.format(dim, commonPreferenceVector));
          logger.info(msg.toString());
        }
      }
    }

    // flip commonPreferenceVector for distance computation in common subspace
    BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
    inverseCommonPreferenceVector.flip(0, dim);

    return new PreferenceVectorBasedCorrelationDistance(subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
  }

  /**
   * Computes the weighted distance between the two specified vectors
   * according to the given preference vector.
   *
   * @param dv1          the first vector
   * @param dv2          the second vector
   * @param weightVector the preference vector
   * @return the weighted distance between the two specified vectors according to the given preference vector
   */
  public double weightedDistance(RealVector dv1, RealVector dv2, BitSet weightVector) {
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
