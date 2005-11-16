package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyDescription;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

/**
 * Provides a locally weighted distance function.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LocallyWeightedDistanceFunction extends DoubleDistanceFunction<DoubleVector> {
  /**
   * Prefix for properties related to this class.
   */
  public static final String PREFIX = "LOCALLY_WEIGHTED_DISTANCE_FUNCTION_";

  /**
   * Property suffix preprocessor.
   */
  public static final String PROPERTY_PREPROCESSOR = "PREPROCESSOR";

  /**
   * The association id to associate a pca to an object.
   */
  public static final AssociationID ASSOCIATION_ID_PCA = CorrelationDimensionPreprocessor.ASSOCIATION_ID_PCA;

  /**
   * The default preprocessor class name.
   */
  public static final String DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedCorrelationDimensionPreprocessor.class.getName();

  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_P = "preprocessor";

  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_D = "<classname>the preprocessor to determine the correlation dimensions of the objects - must implement " + CorrelationDimensionPreprocessor.class.getName() + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS + ").";

  /**
   * Flag for force of preprocessing.
   */
  public static final String FORCE_PREPROCESSING_F = "forcePreprocessing";

  /**
   * Description for flag for force of preprocessing.
   */
  public static final String FORCE_PREPROCESSING_D = "flag to force preprocessing regardless whether for each object a PCA already has been associated.";

  /**
   * Whether preprocessing is forced.
   */
  private boolean force;

  /**
   * The preprocessor to determine the correlation dimensions of the objects.
   */
  private CorrelationDimensionPreprocessor preprocessor;

  /**
   * Provides a locally weighted distance function.
   */
  public LocallyWeightedDistanceFunction() {
    super();

    parameterToDescription.put(PREPROCESSOR_CLASS_P + OptionHandler.EXPECTS_VALUE, PREPROCESSOR_CLASS_D);
    parameterToDescription.put(FORCE_PREPROCESSING_F, FORCE_PREPROCESSING_D);

    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see DistanceFunction#distance(T, T)
   */
  public DoubleDistance distance(DoubleVector rv1, DoubleVector rv2) {
    noDistanceComputations++;
    CorrelationPCA pca1 = (CorrelationPCA) getDatabase().getAssociation(ASSOCIATION_ID_PCA, rv1.getID());
    CorrelationPCA pca2 = (CorrelationPCA) getDatabase().getAssociation(ASSOCIATION_ID_PCA, rv2.getID());

    Matrix m1 = pca1.getSimilarityMatrix();
    Matrix m2 = pca2.getSimilarityMatrix();

    Matrix rv1Mrv2 = rv1.plus(rv2.negativeVector()).getColumnVector();
    Matrix rv2Mrv1 = rv2.plus(rv1.negativeVector()).getColumnVector();

    double dist1 = rv1Mrv2.transpose().times(m1).times(rv1Mrv2).get(0, 0);
    double dist2 = rv2Mrv1.transpose().times(m2).times(rv2Mrv1).get(0, 0);

    return new DoubleDistance(Math.max(Math.sqrt(dist1), Math.sqrt(dist2)));
  }

  /**
   * @see DistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database,
   *      boolean)
   */
  public void setDatabase(Database<DoubleVector> database, boolean verbose) {
    super.setDatabase(database, verbose);

    if (force || !database.isSet(ASSOCIATION_ID_PCA)) {
      preprocessor.run(getDatabase(), verbose);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Locally weighted distance function. Pattern for defining a range: \"" + requiredInputPattern() + "\".", false));
    description.append('\n');
    description.append("Preprocessors available within this framework for distance function ");
    description.append(this.getClass().getName());
    description.append(":");
    description.append('\n');
    for (PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix() + PROPERTY_PREPROCESSOR))) {
      description.append(pd.getEntry());
      description.append('\n');
      description.append(pd.getDescription());
      description.append('\n');
    }
    description.append('\n');
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    if (optionHandler.isSet(PREPROCESSOR_CLASS_P)) {
      preprocessor = Util.instantiate(CorrelationDimensionPreprocessor.class, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P));
    }
    else {
      preprocessor = Util.instantiate(CorrelationDimensionPreprocessor.class, DEFAULT_PREPROCESSOR_CLASS);
    }
    force = optionHandler.isSet(FORCE_PREPROCESSING_F);

    return preprocessor.setParameters(remainingParameters);
  }

  /**
   * Returns the prefix for properties concerning this class. Extending
   * classes requiring other properties should overwrite this method to
   * provide another prefix.
   */
  protected String propertyPrefix() {
    return PREFIX;
  }
}
