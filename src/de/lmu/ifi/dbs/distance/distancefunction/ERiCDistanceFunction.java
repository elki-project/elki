package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.Bit;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
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
 * Provides a distance function for the ERiC algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ERiCDistanceFunction<O extends RealVector>
    extends AbstractDistanceFunction<O, BitDistance> {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "a double specifying the threshold of a distance between a vector q and a given space that indicates that q adds a new dimension to the space (default is delta = " + DEFAULT_DELTA + ")";

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
   * The handler class for the preprocessor.
   */
  private final PreprocessorHandler<O> preprocessorHandler;

  /**
   * The threshold of a distance between a vector q and a given space that
   * indicates that q adds a new dimension to the space.
   */
  private double delta;

  /**
   * Provides a distance function for the ERiC algorithm.
   */
  public ERiCDistanceFunction() {
    super(Bit.BIT_PATTERN);
    DoubleParameter delta = new DoubleParameter(DELTA_P, DELTA_D, new GreaterEqualConstraint(0));
    delta.setDefaultValue(DEFAULT_DELTA);
    optionHandler.put(DELTA_P, delta);

    preprocessorHandler = new PreprocessorHandler<O>(optionHandler,
                                                     PREPROCESSOR_CLASS_D,
                                                     Preprocessor.class,
                                                     DEFAULT_PREPROCESSOR_CLASS,
                                                     AssociationID.LOCAL_PCA);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // delta
    delta = (Double) optionHandler.getOptionValue(DELTA_P);

    // preprocessor
    remainingParameters = preprocessorHandler.setParameters(optionHandler, remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();
    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(DELTA_P, Double.toString(delta));
    preprocessorHandler.addAttributeSettings(result);
    return result;
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
    throw new UnsupportedOperationException("Infinite distance not supported!");
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
  public BitDistance distance(O o1, O o2) {
    LocalPCA pca1 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o1.getID());
    LocalPCA pca2 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, o2.getID());
    return distance(o1, o2, pca1, pca2);
  }

  /**
   * Runs the preprocessor on the database.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   * @param time     flag to request output of performance time
   */
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    preprocessorHandler.runPreprocessor(database, verbose, time);
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function. Note, that the first pca must have equal ore more strong
   * eigenvectors than the second pca.
   *
   * @param o1   first DatabaseObject
   * @param o2   second DatabaseObject
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public BitDistance distance(O o1, O o2, LocalPCA pca1, LocalPCA pca2) {
    if (pca1.getCorrelationDimension() < pca2.getCorrelationDimension()) {
      throw new IllegalStateException("pca1.getCorrelationDimension() < pca2.getCorrelationDimension()");
    }

    if (!softlyLinearDependent(pca1, pca2)) {
      return new BitDistance(true);
    }
    else {
      WeightedDistanceFunction<O> weightedDistanceFunction = new WeightedDistanceFunction<O>(pca1.similarityMatrix());
      if (weightedDistanceFunction.distance(o1, o2).getDoubleValue() > delta)
        return new BitDistance(true);

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
  private boolean softlyLinearDependent(LocalPCA pca1, LocalPCA pca2) {
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
