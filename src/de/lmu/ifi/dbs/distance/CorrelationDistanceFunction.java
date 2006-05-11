package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract super class for correlation based distance functions.
 * Provides the Correlation distance for real valued
 * vectors. All subclasses must implement a method to process the preprocessing
 * step in terms of doing the PCA for each object of the database.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class CorrelationDistanceFunction extends AbstractDistanceFunction<RealVector, CorrelationDistance> {
  /**
   * Indicates a separator.
   */
  public static final Pattern SEPARATOR = Pattern.compile("x");

  /**
   * The default preprocessor class name.
   */
  public static String DEFAULT_PREPROCESSOR_CLASS;

  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_P = "preprocessor";

  /**
   * The Assocoiation ID for the association to be set by the preprocessor.
   */
  public static AssociationID ASSOCIATION_ID;

  /**
   * Description for parameter preprocessor.
   */
  public static String PREPROCESSOR_CLASS_D;

  /**
   * The super class for the preprocessor.
   */
  public static Class PREPROCESSOR_SUPER_CLASS;

  /**
   * Flag for omission of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_F = "omitPreprocessing";

  /**
   * Description for flag for force of preprocessing.
   */
  public static final String OMIT_PREPROCESSING_D = "flag to omit (a new) preprocessing if for each object a matrix already has been associated.";

  /**
   * True, if preprocessing is omitted, false otherwise.
   */
  private boolean omit;

  /**
   * The preprocessor to run the variance analysis of the objects.
   */
  Preprocessor preprocessor;

  /**
   * Provides a CorrelationDistanceFunction with a pattern defined to accept
   * Strings that define an Integer followed by a separator followed by a
   * Double.
   */
  public CorrelationDistanceFunction() {
    super(Pattern.compile("\\d+" + CorrelationDistanceFunction.SEPARATOR.pattern()
                          + "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));

    parameterToDescription.put(CorrelationDistanceFunction.OMIT_PREPROCESSING_F, CorrelationDistanceFunction.OMIT_PREPROCESSING_D);
    parameterToDescription.put(CorrelationDistanceFunction.PREPROCESSOR_CLASS_P + OptionHandler.EXPECTS_VALUE, CorrelationDistanceFunction.PREPROCESSOR_CLASS_D);

    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Provides the Correlation distance between the given two vectors.
   *
   * @return the Correlation distance between the given two vectors as an
   *         instance of {@link CorrelationDistance CorrelationDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject,
   *      de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public CorrelationDistance distance(RealVector rv1, RealVector rv2) {
    noDistanceComputations++;
    return correlationDistance(rv1, rv2);
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
  public CorrelationDistance valueOf(String pattern)
  throws IllegalArgumentException {
    if (pattern.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if (matches(pattern)) {
      String[] values = CorrelationDistanceFunction.SEPARATOR.split(pattern);
      return new CorrelationDistance(Integer.parseInt(values[0]), Double.parseDouble(values[1]));
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
  public CorrelationDistance infiniteDistance() {
    return new CorrelationDistance(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  public CorrelationDistance nullDistance() {
    return new CorrelationDistance(0, 0);
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  public CorrelationDistance undefinedDistance() {
    return new CorrelationDistance(-1, Double.NaN);
  }

  /**
   * Returns a description of the class and the required parameters.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Correlation distance for NumberVectors. Pattern for defining a range: \"" + requiredInputPattern() + "\".", false));
    description.append('\n');
    description.append("Preprocessors available within this framework for distance function ");
    description.append(this.getClass().getName());
    description.append(":\n");
    description.append(Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(PREPROCESSOR_SUPER_CLASS));
    description.append('\n');
    return description.toString();

  }

  /**
   * Computes the necessary PCA associations for each object of the database.
   * Afterwards the database is set to get later on the PCA associations
   * needed for distance computing.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   * @param time     flag to request output of performance time
   */
  public void setDatabase(Database<RealVector> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    if (!omit || !database.isSet(ASSOCIATION_ID)) {
      preprocessor.run(database, verbose, time);
    }
  }

  /**
   * Sets the values for the parameters delta and preprocessor if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // preprocessor
    if (optionHandler.isSet(PREPROCESSOR_CLASS_P)) {
      try {
        //noinspection unchecked
        preprocessor = (Preprocessor) Util.instantiate(PREPROCESSOR_SUPER_CLASS, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P));
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, optionHandler.getOptionValue(PREPROCESSOR_CLASS_P), PREPROCESSOR_CLASS_D, e);
      }
    }
    else {
      try {
        //noinspection unchecked
        preprocessor = (Preprocessor) Util.instantiate(PREPROCESSOR_SUPER_CLASS, DEFAULT_PREPROCESSOR_CLASS);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PREPROCESSOR_CLASS_P, optionHandler.getOptionValue(DEFAULT_PREPROCESSOR_CLASS), PREPROCESSOR_CLASS_D, e);
      }
    }

    // omit
    omit = optionHandler.isSet(CorrelationDistanceFunction.OMIT_PREPROCESSING_F);

    remainingParameters = preprocessor.setParameters(remainingParameters);
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
    attributeSettings.addSetting(OMIT_PREPROCESSING_F, Boolean.toString(omit));
    attributeSettings.addSetting(PREPROCESSOR_CLASS_P, preprocessor.getClass().getName());

    result.addAll(preprocessor.getAttributeSettings());

    return result;
  }

  /**
   * Computes the correlation distance between the two specified vectors.
   *
   * @param dv1 first RealVector
   * @param dv2 second RealVector
   * @return the correlation distance between the two specified vectors
   */
  abstract CorrelationDistance correlationDistance(RealVector dv1, RealVector dv2);
}
