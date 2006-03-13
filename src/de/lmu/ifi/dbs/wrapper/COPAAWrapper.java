package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Wrapper class for COPAA algorithm. Performs an attribute wise normalization on
 * the database objects, partitions the database according to the correlation dimension of
 * its objects and then performs the algorithm OPTICS over the partitions.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPAAWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon> the maximum radius of the neighborhood to" +
                                         "be considerd, must be suitable to " +
                                         LocallyWeightedDistanceFunction.class.getName();

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k> a positive integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set to minpts";

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    COPAAWrapper wrapper = new COPAAWrapper();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
  }

  /**
   * Sets the parameter epsilon, minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public COPAAWrapper() {
    super();
    parameterToDescription.put(OPTICS.EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(OPTICS.MINPTS_P + OptionHandler.EXPECTS_VALUE, OPTICS.MINPTS_D);
    parameterToDescription.put(KnnQueryBasedCorrelationDimensionPreprocessor.K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() {
    List<String> parameters = super.getParameters();

    // algorithm COPAA
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(COPAA.class.getName());

    // partition algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PARTITION_ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.EPSILON_P));

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    parameters.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
    if (optionHandler.isSet(KnnQueryBasedCorrelationDimensionPreprocessor.K_P)) {
      parameters.add(optionHandler.getOptionValue(KnnQueryBasedCorrelationDimensionPreprocessor.K_P));
    }
    else {
      parameters.add(optionHandler.getOptionValue(optionHandler.getOptionValue(OPTICS.MINPTS_P)));
    }

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }
}
