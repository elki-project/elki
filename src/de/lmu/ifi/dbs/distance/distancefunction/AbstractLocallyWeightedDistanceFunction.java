package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * Abstract super class for locally weighted distance functions using a preprocessor
 * to compute the local weight matrix.
 *
 * @author Elke Achtert
 */
public abstract class AbstractLocallyWeightedDistanceFunction<O extends RealVector<O,?>,P extends Preprocessor<O>>
    extends AbstractDoubleDistanceFunction<O> {

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
  private final PreprocessorHandler<O,P> preprocessorHandler;

  /**
   * Provides an abstract locally weighted distance function.
   */
  protected AbstractLocallyWeightedDistanceFunction() {
    super();
    preprocessorHandler = new PreprocessorHandler(optionHandler,
                                                     PREPROCESSOR_CLASS_D,
                                                     Preprocessor.class,
                                                     DEFAULT_PREPROCESSOR_CLASS,
                                                     getAssociationID());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    preprocessorHandler.runPreprocessor(database, verbose, time);
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    remainingParameters = preprocessorHandler.setParameters(optionHandler, remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();
    preprocessorHandler.addAttributeSettings(result);
    return result;
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
    description.append('\n' + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Preprocessor.class));
    description.append('\n');
    return description.toString();
  }

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  abstract AssociationID getAssociationID();

}
