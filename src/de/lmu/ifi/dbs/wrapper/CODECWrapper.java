package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.CoDeC;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper class for the CoDeC algorithm. Performs an attribute wise
 * normalization on the database objects, partitions the database according to
 * the correlation dimension of its objects, performs the algorithm DBSCAN over
 * the partitions and then determines the correlation dependencies in each
 * cluster of each partition.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CODECWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>the maximum radius of the neighborhood to"
                                         + "be considerd, must be suitable to "
                                         + LocallyWeightedDistanceFunction.class.getName();

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<int>a positive integer specifying the number of "
                                   + "nearest neighbors considered in the PCA. "
                                   + "If this value is not defined, k ist set to minpts";

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    CODECWrapper wrapper = new CODECWrapper();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);

    }
  }

  /**
   * Sets the parameter epsilon, minpts and k in the parameter map
   * additionally to the parameters provided by super-classes.
   */
  public CODECWrapper() {
    super();
    parameterToDescription.put(DBSCAN.EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(DBSCAN.MINPTS_P + OptionHandler.EXPECTS_VALUE, OPTICS.MINPTS_D);
    parameterToDescription.put(KnnQueryBasedHiCOPreprocessor.K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // algorithm CoDeC
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(CoDeC.class.getName());

    // clustering algorithm COPAC
    parameters.add(OptionHandler.OPTION_PREFIX
                   + CoDeC.CLUSTERING_ALGORITHM_P);
    parameters.add(COPAC.class.getName());

    // partition algorithm
    parameters.add(OptionHandler.OPTION_PREFIX
                   + COPAC.PARTITION_ALGORITHM_P);
    parameters.add(DBSCAN.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.EPSILON_P));

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

    // distance function
    parameters
    .add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    parameters.add(OptionHandler.OPTION_PREFIX
                   + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedHiCOPreprocessor.class
    .getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX
                   + KnnQueryBasedHiCOPreprocessor.K_P);
    if (optionHandler
    .isSet(KnnQueryBasedHiCOPreprocessor.K_P)) {
      parameters
      .add(optionHandler
      .getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P));
    }
    else {
      parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));
    }

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX
                   + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }

  /**
   * Initailizes the parametrs for the algorithm to apply.
   *
   * @param parameters the parametrs array
   */
  protected void initParametersForAlgorithm(List<String> parameters) {

  }
}
