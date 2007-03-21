package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract super class for distance functions needing a preprocessor.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractPreprocessorBasedDistanceFunction<O extends DatabaseObject, D extends Distance>
    extends AbstractDistanceFunction<O, D> {

  /**
   * The handler class for the preprocessor.
   */
  private final PreprocessorHandler<O> preprocessorHandler;

  /**
   * Provides a super class for distance functions needing a preprocessor
   *
   * @param pattern a pattern to define the required input format
   */
  public AbstractPreprocessorBasedDistanceFunction(Pattern pattern) {
    super(pattern);
    preprocessorHandler = new PreprocessorHandler<O>(optionHandler,
                                                     getPreprocessorClassDescription(),
                                                     getPreprocessorSuperClassName(),
                                                     getDefaultPreprocessorClassName(),
                                                     getAssociationID());
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
