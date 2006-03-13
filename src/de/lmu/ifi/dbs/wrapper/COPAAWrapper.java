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
 * Wrapper class for COPAA algorithm. Partitions a database according to the correlation dimension of
 * its objects and then performs the algorithm OPTICS over the partitions.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPAAWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = OPTICS.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon> the maximum radius of the neighborhood to" +
                                         "be considerd, must be suitable to " +
                                         LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = OPTICS.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = OPTICS.MINPTS_D;

  /**
   * Option string for parameter k.
   */
  public static final String K_P = KnnQueryBasedCorrelationDimensionPreprocessor.K_P;

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k> a positive integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set to minpts";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected String minpts;

  /**
   * k.
   */
  protected String k;

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
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public COPAAWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) {
    String[] remainingParameters = super.setParameters(args);

    // epsilon
    epsilon = optionHandler.getOptionValue(EPSILON_P);

    // minpts
    minpts = optionHandler.getOptionValue(MINPTS_P);

    // k
    if (optionHandler.isSet(K_P)) {
      k = optionHandler.getOptionValue(K_P);
    }
    else {
      k = minpts;
    }

    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#addParameters(java.util.List<java.lang.String>)
   */
  public final void addParameters(List<String> parameters) {
    // algorithm
    initParametersForAlgorithm(parameters);

    // partition algorithm
    initParametersForPartitionAlgorithm(parameters);

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
    parameters.add(k);

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);
  }

  /**
   * Initailizes the parametrs for the algorithm to apply.
   *
   * @param parameters the parametrs array
   */
  protected void initParametersForAlgorithm(List<String> parameters) {
    // algorithm COPAA
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(COPAA.class.getName());
  }

  /**
   * Initailizes the parameters for the partition algorithm to apply.
   *
   * @param parameters the parametrs array
   */
  protected void initParametersForPartitionAlgorithm(List<String> parameters) {
    // partition algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PARTITION_ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(minpts);

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    parameters.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);
  }
}
