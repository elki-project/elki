package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;

import java.util.BitSet;

/**
 * XXX unify CorrelationDistanceFunction and VarianceDistanceFunction
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class VarianceDistanceFunction extends CorrelationDistanceFunction {
  static {
    ASSOCIATION_ID = AssociationID.PREFERENCE_VECTOR;
    PreprocessorClass = HiSCPreprocessor.class;
    DEFAULT_PREPROCESSOR_CLASS = HiSCPreprocessor.class.getName();
    PREPROCESSOR_CLASS_D = "<classname>the preprocessor to determine the preference vectors of the objects "
                           + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(HiSCPreprocessor.class)
                           + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS + ").";
  }

  /**
   * The preprocessor to determine the correlation dimensions of the objects.
   */
  private HiSCPreprocessor preprocessor;

  /**
   *
   */
  public VarianceDistanceFunction() {
    super();
  }

  @Override
  protected CorrelationDistance correlationDistance(RealVector dv1, RealVector dv2) {
    BitSet preferenceVector1 = (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, dv1.getID());
    BitSet preferenceVector2 = (BitSet) getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, dv2.getID());
    BitSet commonPreferenceVector = (BitSet) preferenceVector1.clone();
    commonPreferenceVector.and(preferenceVector2);
    int dim = dv1.getDimensionality();
    Integer lambda = dim - commonPreferenceVector.cardinality();
    if (//preferenceVector1.equals(preferenceVector2) &&
        weightedDistance(dv1, dv2, preferenceVector1) > getPreprocessor().getAlpha()) {
      lambda++;
    }
    commonPreferenceVector.flip(0, dim);

    return new CorrelationDistance(lambda, weightedDistance(dv1, dv2, commonPreferenceVector));
  }

  protected double weightedDistance(RealVector dv1, RealVector dv2, BitSet weightVector) {
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

  public HiSCPreprocessor getPreprocessor() {
    return preprocessor;
  }

  public void setPreprocessor(Preprocessor p) {
    this.preprocessor = (HiSCPreprocessor) p;
  }
}
