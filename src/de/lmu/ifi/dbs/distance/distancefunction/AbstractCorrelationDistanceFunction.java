package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.CorrelationDistance;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract super class for correlation based distance functions. Provides the
 * Correlation distance for real valued vectors. All subclasses must implement a
 * method to process the preprocessing step in terms of doing the PCA for each
 * object of the database.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractCorrelationDistanceFunction<O extends RealVector, D extends CorrelationDistance>
    extends AbstractDistanceFunction<O, D> {

  /**
   * Indicates a separator.
   */
  public static final Pattern SEPARATOR = Pattern.compile("x");

  /**
   * The handler class for the preprocessor.
   */
  private final PreprocessorHandler<O> preprocessorHandler;

  /**
   * Provides a CorrelationDistanceFunction with a pattern defined to accept
   * Strings that define an Integer followed by a separator followed by a
   * Double.
   */
  public AbstractCorrelationDistanceFunction() {
    super(Pattern.compile("\\d+" + AbstractCorrelationDistanceFunction.SEPARATOR.pattern() + "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));

    preprocessorHandler = new PreprocessorHandler<O>(optionHandler,
                                                     getPreprocessorClassDescription(),
                                                     getPreprocessorSuperClassName(),
                                                     getDefaultPreprocessorClassName(),
                                                     getAssociationID());
  }

  /**
   * Provides the Correlation distance between the given two vectors.
   *
   * @return the Correlation distance between the given two vectors as an
   *         instance of {@link CorrelationDistance CorrelationDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject,
   *      de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public D distance(O rv1, O rv2) {
    return correlationDistance(rv1, rv2);
  }

  /**
   * Returns a description of the class and the required parameters.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Correlation distance for NumberVectors. Pattern for defining a range: \""
                                           + requiredInputPattern() + "\".", false));
    description.append('\n');
    description.append("Preprocessors available within this framework for distance function ");
    description.append(this.getClass().getName());
    description.append(":\n");
    description.append(Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(getPreprocessorSuperClassName()));
    description.append('\n');
    return description.toString();

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
   * Sets the values for the parameters delta and preprocessor if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

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
    preprocessorHandler.addAttributeSettings(result);
    return result;
  }

  /**
   * Returns the preprocessor of this distance function.
   *
   * @return the preprocessor of this distance function
   */
  public Preprocessor<O> getPreprocessor() {
    return preprocessorHandler.getPreprocessor();
  }

  /**
   * Computes the correlation distance between the two specified vectors.
   *
   * @param dv1 first RealVector
   * @param dv2 second RealVector
   * @return the correlation distance between the two specified vectors
   */
  abstract D correlationDistance(O dv1, O dv2);

  /**
   * Returns the name of the default preprocessor.
   */
  abstract String getDefaultPreprocessorClassName();

  /**
   * Returns the description for parameter preprocessor.
   */
  abstract String getPreprocessorClassDescription();

  /**
   * Returns the super class for the preprocessor.
   */
  abstract Class getPreprocessorSuperClassName();

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  abstract AssociationID getAssociationID();
}
