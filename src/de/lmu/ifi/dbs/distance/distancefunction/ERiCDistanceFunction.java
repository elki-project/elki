package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.Bit;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.BitDistance;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.List;

/**
 * Provides a distance function for building the hierarchiy in the ERiC algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ERiCDistanceFunction<V extends RealVector<V, ?>>
    extends AbstractPreprocessorBasedDistanceFunction<V, BitDistance> {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.1;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "a double specifying the threshold for approximate linear dependency:" +
                                       "the strong eigenvectors of q are approximately linear dependent " +
                                       "from the strong eigenvectors p if the following condition " +
                                       "holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p): " +
                                       "q_i' * M^check_p * q_i <= delta^2.  " +
                                       "(default is delta = " + DEFAULT_DELTA + ")";

  /**
   * The default value for delta.
   */
  public static final double DEFAULT_TAU = 0.1;

  /**
   * Option string for parameter delta.
   */
  public static final String TAU_P = "tau";

  /**
   * Description for parameter delta.
   */
  public static final String TAU_D = "a double specifying the " +
                                     "maximum distance between two " +
                                     "approximately linear dependent subspaces of two objects p and q " +
                                     "(lambda_q < lambda_p) " +
                                     "before considering them " +
                                     "as parallel (default is " + DEFAULT_TAU +
                                     ").";

  /**
   * The Assocoiation ID for the association to be set by the preprocessor.
   */
  public static final AssociationID ASSOCIATION_ID = AssociationID.LOCAL_PCA;

  /**
   * The super class for the preprocessor.
   */
  public static final Class PREPROCESSOR_SUPER_CLASS = Preprocessor.class;

  /**
   * The default preprocessor class name.
   */
  public static final String DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedHiCOPreprocessor.class.getName();

  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_D = "the preprocessor to determine the correlation dimensions of the objects " +
                                                    Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Preprocessor.class) +
                                                    ". Default: " + DEFAULT_PREPROCESSOR_CLASS;

  /**
   * The threshold for approximate linear dependency.
   */
  private double delta;

  /**
   * The threshold for parallel subspaces.
   */
  private double tau;

  /**
   * Provides a distance function for the ERiC algorithm.
   */
  public ERiCDistanceFunction() {
    super(Bit.BIT_PATTERN);
    // delta
    DoubleParameter delta = new DoubleParameter(DELTA_P, DELTA_D, new GreaterEqualConstraint(0));
    delta.setDefaultValue(DEFAULT_DELTA);
    optionHandler.put(delta);
    // tau
    DoubleParameter tau = new DoubleParameter(TAU_P, TAU_D, new GreaterEqualConstraint(0));
    tau.setDefaultValue(DEFAULT_TAU);
    optionHandler.put(tau);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // delta, tau
    delta = (Double) optionHandler.getOptionValue(DELTA_P);
    tau = (Double) optionHandler.getOptionValue(TAU_P);

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();

    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(DELTA_P, Double.toString(delta));
    mySettings.addSetting(TAU_P, Double.toString(tau));
    return settings;
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

  /**
   * Returns the super class for the preprocessor.
   */
  Class getPreprocessorSuperClassName() {
    return PREPROCESSOR_SUPER_CLASS;
  }

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  AssociationID getAssociationID() {
    return ASSOCIATION_ID;
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#valueOf(String)
   */
  public BitDistance valueOf(String pattern) throws IllegalArgumentException {
    if (matches(pattern)) {
      return new BitDistance(Bit.valueOf(pattern).bitValue());
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern
                                         + "\" does not match required pattern \""
                                         + requiredInputPattern() + "\"");
    }
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#infiniteDistance()
   */
  public BitDistance infiniteDistance() {
    return new BitDistance(true);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#nullDistance()
   */
  public BitDistance nullDistance() {
    return new BitDistance(false);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#undefinedDistance()
   */
  public BitDistance undefinedDistance() {
    throw new UnsupportedOperationException("Undefinded distance not supported!");
  }

  /**
   * Note, that the pca of o1 must have equal ore more strong
   * eigenvectors than the pca of o2.
   *
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public BitDistance distance(V o1, V o2) {
    LocalPCA pca1 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o1.getID());
    LocalPCA pca2 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o2.getID());
    return distance(o1, o2, pca1, pca2);
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function. Note, that the first pca must have equal or more strong
   * eigenvectors than the second pca.
   *
   * @param o1   first DatabaseObject
   * @param o2   second DatabaseObject
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public BitDistance distance(V o1, V o2, LocalPCA pca1, LocalPCA pca2) {
    if (pca1.getCorrelationDimension() < pca2.getCorrelationDimension()) {
      throw new IllegalStateException("pca1.getCorrelationDimension() < pca2.getCorrelationDimension(): " +
                                      pca1.getCorrelationDimension() + " < " + pca2.getCorrelationDimension());
    }

    boolean approximatelyLinearDependent;
    if (pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
      approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2) &&
                                     approximatelyLinearDependent(pca2, pca1);
    }
    else {
      approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2);
    }


    if (!approximatelyLinearDependent) {
      return new BitDistance(true);
    }

    else {
      double affineDistance;

      if (pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
        WeightedDistanceFunction<V> df1 = new WeightedDistanceFunction<V>(pca1.similarityMatrix());
        WeightedDistanceFunction<V> df2 = new WeightedDistanceFunction<V>(pca2.similarityMatrix());
        affineDistance = Math.max(df1.distance(o1, o2).getDoubleValue(),
                                  df2.distance(o1, o2).getDoubleValue());
      }
      else {
        WeightedDistanceFunction<V> df1 = new WeightedDistanceFunction<V>(pca1.similarityMatrix());
        affineDistance = df1.distance(o1, o2).getDoubleValue();
      }

      if (affineDistance > tau) {
        return new BitDistance(true);
      }

      return new BitDistance(false);
    }
  }

  /**
   * Returns true, if the strong eigenvectors of the two specified
   * pcas span up the same space. Note, that the first pca must have equal ore more strong
   * eigenvectors than the second pca.
   *
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return true, if the strong eigenvectors of the two specified
   *         pcas span up the same space
   */
  private boolean approximatelyLinearDependent(LocalPCA pca1, LocalPCA pca2) {
    Matrix m1_czech = pca1.dissimilarityMatrix();
    Matrix v2_strong = pca2.adapatedStrongEigenvectors();
    for (int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of pca_1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transpose().times(v2_i).get(0, 0) - v2_i.transpose().times(m1_czech).times(v2_i).get(0, 0));

      // if so, return false
      if (dist > delta) {
        return false;
      }
    }

    return true;
  }
}
