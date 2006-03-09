package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * Wrapper class for COPAC algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPACWrapper extends COPAAWrapper {
  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public COPACWrapper() {
    super();
  }

  public static void main(String[] args) {
    COPACWrapper wrapper = new COPACWrapper();
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
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    List<String> params = super.initParameters(remainingParameters);

    // algorithm COPAC
    int index = params.indexOf(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.remove(index);
    params.remove(index);
    params.add(0, OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(1, COPAC.class.getName());

    // partition algorithm DBSCAN
    index = params.indexOf(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    params.remove(index);
    params.remove(index);
    params.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    params.add(DBSCAN.class.getName());

    // epsilon
    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    params.add(epsilon);

    // minpts
    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    params.add(Integer.toString(minpts));

    // distance function
    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
    params.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    params.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    params.add(OptionHandler.OPTION_PREFIX + COPAC.PREPROCESSOR_P);
    params.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k
    params.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
    params.add(Integer.toString(k));

    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(AttributeWiseRealVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return params;
  }
}
