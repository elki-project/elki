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
    return correlationDistance(v1, v2, preferenceVector1, preferenceVector2);
  }

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
  public PreferenceVectorBasedCorrelationDistance correlationDistance(RealVector v1, RealVector v2, BitSet pv1, BitSet pv2) {
    BitSet commonPreferenceVector = (BitSet) pv1.clone();
    commonPreferenceVector.and(pv2);
    int dim = v1.getDimensionality();

    // number of zero values in commonPreferenceVector
    Integer subspaceDim = dim - commonPreferenceVector.cardinality();

    if (v1.getID() != null && v2.getID() != null &&
        (v1.getID() == 509 && v2.getID() == 249 || v1.getID() == 249 && v2.getID() == 509)) {
      System.out.println("xxxxxxxxxxxxxxxxxxxxxx");
      System.out.println(v1.getID() + " " + v1 + " pv1 " + pv1);
      System.out.println(v2.getID() + " " + v2 + " pv2 " + pv2);
      System.out.println("commonPreferenceVector " + commonPreferenceVector);
      System.out.println("subspaceDim " + subspaceDim);
      System.out.println(commonPreferenceVector.equals(pv1));
      System.out.println(commonPreferenceVector.equals(pv2));
      System.out.println(" d = " + weightedDistance(v1, v2, commonPreferenceVector));
    }

    // special case: v1 and v2 are in parallel subspaces
    if (commonPreferenceVector.equals(pv1) || commonPreferenceVector.equals(pv2)) {
      double d = weightedDistance(v1, v2, commonPreferenceVector);
      if (d > 2 * ((DiSHPreprocessor) preprocessor).getEpsilon().getDoubleValue()) {
        subspaceDim++;
        if (DEBUG) {
          StringBuffer msg = new StringBuffer();
          msg.append("\n");
          msg.append("\nd " + d);
          msg.append("\nv1 " + getDatabase().getAssociation(AssociationID.LABEL, v1.getID()));
          msg.append("\nv2 " + getDatabase().getAssociation(AssociationID.LABEL, v2.getID()));
          msg.append("\nsubspaceDim " + subspaceDim);
          msg.append("\ncommon pv " + Util.format(dim, commonPreferenceVector));
          logger.info(msg.toString());
        }
      }
    }

    if (v1.getID() != null && v2.getID() != null &&
        (v1.getID() == 509 && v2.getID() == 249 || v1.getID() == 249 && v2.getID() == 509)) {
      System.out.println("xxxxxxxxxxxxxxxxxxxxxx");
      System.out.println(v1.getID() + " " + v1 + " pv1 " + pv1);
      System.out.println(v2.getID() + " " + v2 + " pv2 " + pv2);
      System.out.println("commonPreferenceVector " + commonPreferenceVector);
      System.out.println("subspaceDim " + subspaceDim);
      System.out.println(commonPreferenceVector.equals(pv1));
      System.out.println(commonPreferenceVector.equals(pv2));
      System.out.println(" d = " + weightedDistance(v1, v2, commonPreferenceVector));
    }

//    String l1 = (String) getDatabase().getAssociation(AssociationID.LABEL, v1.getID());
//    String l2 = (String) getDatabase().getAssociation(AssociationID.LABEL, v2.getID());
//    if ((l1.equals("e2") && l2.equals("e4")) || (l1.equals("e3") && l2.equals("e4")) ||
//        (l1.equals("e4") && l2.equals("e2")) || (l1.equals("e4") && l2.equals("e3"))) {
//      if (subspaceDim != 3) {
//        double d = weightedDistance(v1, v2, commonPreferenceVector);
//        StringBuffer msg = new StringBuffer();
//        msg.append("\n");
//        msg.append("\nd " + d);
//        msg.append("\nv1 " + getDatabase().getAssociation(AssociationID.LABEL, v1.getID()));
//        msg.append("\nv2 " + getDatabase().getAssociation(AssociationID.LABEL, v2.getID()));
//        msg.append("\nsubspaceDim " + subspaceDim);
//        msg.append("\ncommon pv " + Util.format(dim, commonPreferenceVector));
//        logger.info(msg.toString());
//      }
//    }

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

  /**
   * Computes the weighted distance between the two specified vectors
   * according to the given preference vector.
   *
   * @param id1          the id of the first vector
   * @param id2          the id of the second vector
   * @param weightVector the preference vector
   * @return the weighted distance between the two specified vectors according to the given preference vector
   */
  public double weightedDistance(Integer id1, Integer id2, BitSet weightVector) {
    return weightedDistance(getDatabase().get(id1), getDatabase().get(id2), weightVector);
  }

  /**
   * Computes the weighted distance between the two specified data vectors
   * according to their preference vectors.
   *
   * @param rv1 the first vector
   * @param rv2 the the second vector
   * @return the weighted distance between the two specified vectors
   *         according to the preference vector of the first data vector
   */
  public double weightedPrefereneceVectorDistance(RealVector rv1, RealVector rv2) {
    double d1 = weightedDistance(rv1, rv2, (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, rv1.getID()));
    double d2 = weightedDistance(rv2, rv1, (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, rv2.getID()));

    return Math.max(d1, d2);
  }

  /**
   * Computes the weighted distance between the two specified data vectors
   * according to their preference vectors.
   *
   * @param id1 the id of the first vector
   * @param id2 the id of the second vector
   * @return the weighted distance between the two specified vectors
   *         according to the preference vector of the first data vector
   */
  public double weightedPrefereneceVectorDistance(Integer id1, Integer id2) {
    return weightedPrefereneceVectorDistance(getDatabase().get(id1), getDatabase().get(id2));
  }
}
