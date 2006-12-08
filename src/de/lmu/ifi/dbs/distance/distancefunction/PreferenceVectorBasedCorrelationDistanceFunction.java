package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * XXX unify CorrelationDistanceFunction and VarianceDistanceFunction
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class PreferenceVectorBasedCorrelationDistanceFunction extends AbstractCorrelationDistanceFunction<PreferenceVectorBasedCorrelationDistance> {

  /**
   * The default value for epsilon.
   */
  public static final double DEFAULT_EPSILON = 0.001;

  /**
   * Option string for parameter epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static String EPSILON_D = "a double specifying the "
                                   + "maximum distance between two vectors "
                                   + "with equal preference vectors before considering them "
                                   + "as parallel (default is " + DEFAULT_EPSILON
                                   + ").";

  /**
   * Holds the value of epsilon parameter.
   */
  private double epsilon;


  /**
   * Provides a preference vector based CorrelationDistanceFunction.
   */
  public PreferenceVectorBasedCorrelationDistanceFunction() {
    super();

    // parameter epsilon
    ArrayList<ParameterConstraint> cons = new ArrayList<ParameterConstraint>();
    cons.add(new GreaterEqualConstraint(0));
    DoubleParameter eps = new DoubleParameter(EPSILON_P, EPSILON_D, cons);
    eps.setDefaultValue(DEFAULT_EPSILON);
    optionHandler.put(EPSILON_P, eps);
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
      String[] values = AbstractCorrelationDistanceFunction.SEPARATOR.split(pattern);
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
   * @see de.lmu.ifi.dbs.distance.distancefunction.AbstractCorrelationDistanceFunction#correlationDistance(de.lmu.ifi.dbs.data.RealVector,de.lmu.ifi.dbs.data.RealVector)
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
  public abstract PreferenceVectorBasedCorrelationDistance correlationDistance(RealVector v1, RealVector v2, BitSet pv1, BitSet pv2);

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

  /**
   * Sets the values for the parameters delta and preprocessor if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon
    if (optionHandler.isSet(EPSILON_P)) {
      String epsString = optionHandler.getOptionValue(EPSILON_P);
      try {
        epsilon = Double.parseDouble(epsString);
        if (epsilon < 0) {
          throw new WrongParameterValueException(EPSILON_P, epsString, EPSILON_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(EPSILON_P, epsString, EPSILON_D, e);
      }
    }
    else {
      epsilon = DEFAULT_EPSILON;
    }
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();

    AttributeSettings mySetting = settings.get(0);
    mySetting.addSetting(EPSILON_P, Double.toString(epsilon));

    return settings;
  }

  /**
   * Returns epsilon.
   * @return epsilon
   */
  public double getEpsilon() {
    return epsilon;
  }
}
