package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyDescription;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Provides a locally weighted distance function.
 * Computes the quadratic form distance between two vectors P and Q as follows:
 * result = max{dist<sub>P</sub>(P,Q), dist<sub>Q</sub>(Q,P)}
 * where dist<sub>X</sub>(X,Y) = (X-Y)*<b>M<sub>X</sub></b>*(X-Y)<b><sup>T</sup></b>
 * and <b>M<sub>X</sub></b> is the weight matrix of vector X.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LocallyWeightedDistanceFunction extends DoubleDistanceFunction<RealVector> {
  /**
   * Prefix for properties related to this class. TODO property
   */
  public static final String PREFIX = "LOCALLY_WEIGHTED_DISTANCE_FUNCTION_";

  /**
   * Property suffix preprocessor. TODO property
   */
  public static final String PROPERTY_PREPROCESSOR = "PREPROCESSOR";

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
   * Flag for omission of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_F = "omitPreprocessing";

  /**
   * Description for flag for force of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_D = "flag to omit (a new) preprocessing if for each object a matrix already has been associated.";

  /**
   * Whether preprocessing is omitted.
   */
  private boolean omit;

  /**
   * The preprocessor to determine the correlation dimensions of the objects.
   */
  private Preprocessor preprocessor;

  /**
   * Provides a locally weighted distance function.
   */
  public LocallyWeightedDistanceFunction() {
    super();

    parameterToDescription.put(PREPROCESSOR_CLASS_P + OptionHandler.EXPECTS_VALUE, PREPROCESSOR_CLASS_D);
    parameterToDescription.put(OMIT_PREPROCESSING_F, OMIT_PREPROCESSING_D);

    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Computes the distance between two given real vectors according to this
   * distance function.
   *
   * @param o1 first RealVector
   * @param o2 second RealVector
   * @return the distance between two given real vectors according to this
   *         distance function
   */
  public DoubleDistance distance(RealVector o1, RealVector o2) {
    noDistanceComputations++;
    Matrix m1 = (Matrix) getDatabase().getAssociation(AssociationID.LOCALLY_WEIGHTED_MATRIX, o1.getID());
    Matrix m2 = (Matrix) getDatabase().getAssociation(AssociationID.LOCALLY_WEIGHTED_MATRIX, o2.getID());

    //noinspection unchecked
    Matrix rv1Mrv2 = o1.plus(o2.negativeVector()).getColumnVector();
    //noinspection unchecked
    Matrix rv2Mrv1 = o2.plus(o1.negativeVector()).getColumnVector();

    double dist1 = rv1Mrv2.transpose().times(m1).times(rv1Mrv2).get(0, 0);
    double dist2 = rv2Mrv1.transpose().times(m2).times(rv2Mrv1).get(0, 0);

    return new DoubleDistance(Math.max(Math.sqrt(dist1), Math.sqrt(dist2)));
  }

  /**
   * @see DistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void setDatabase(Database<RealVector> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);

    if (! omit || !database.isSet(AssociationID.LOCALLY_WEIGHTED_MATRIX)) {
      preprocessor.run(getDatabase(), verbose, time);
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
    for (PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix() + PROPERTY_PREPROCESSOR)))
    {
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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // preprocessor
    if (optionHandler.isSet(PREPROCESSOR_CLASS_P)) {
      try {
        preprocessor = Util.instantiate(Preprocessor.class, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P));
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P), PREPROCESSOR_CLASS_D, e);  //To change body of catch statement use File | Settings | File Templates.
      }
    }
    else {
      try {
        preprocessor = Util.instantiate(Preprocessor.class, DEFAULT_PREPROCESSOR_CLASS);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, DEFAULT_PREPROCESSOR_CLASS, PREPROCESSOR_CLASS_D, e);  //To change body of catch statement use File | Settings | File Templates.
      }
    }

    // force flag
    omit = optionHandler.isSet(OMIT_PREPROCESSING_F);

    return preprocessor.setParameters(remainingParameters);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings settings = result.get(0);
    settings.addSetting(PREPROCESSOR_CLASS_P, preprocessor.getClass().getName());
    settings.addSetting(OMIT_PREPROCESSING_F, Boolean.toString(omit));

    result.addAll(preprocessor.getAttributeSettings());

    return result;
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
