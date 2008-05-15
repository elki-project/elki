package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.ERiC;
import de.lmu.ifi.dbs.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for hierarchical COPAC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 */
public class ERiCWrapper extends NormalizationWrapper {

  /**
   * Description for parameter k.
   */
  public static final String K_D = "a positive integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set to minpts";

  /**
   * The value of the minpts parameter.
   */
  private int minpts;

  /**
   * The value of the k parameter.
   */
  private int k;

  /**
   * The value of the delta parameter.
   */
  private double delta;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    ERiCWrapper wrapper = new ERiCWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter epsilon, minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public ERiCWrapper() {
    super();

    // parameter min points
    IntParameter minPam = new IntParameter(DBSCAN.MINPTS_P, DBSCAN.MINPTS_D, new GreaterConstraint(0));
    optionHandler.put(minPam);

    // parameter k
    IntParameter kPam = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P, ERiCWrapper.K_D, new GreaterConstraint(0));
    kPam.setOptional(true);
    optionHandler.put(kPam);

    // parameter delta
    DoubleParameter deltaPam = new DoubleParameter(ERiCDistanceFunction.DELTA_P,
                                                   ERiCDistanceFunction.DELTA_D,
                                                   new GreaterConstraint(0));
    deltaPam.setDefaultValue(ERiCDistanceFunction.DEFAULT_DELTA);
    optionHandler.put(deltaPam);
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm ERiC
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(ERiC.class.getName());

    // partition algorithm DBSCAN
    parameters.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    parameters.add(DBSCAN.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_PARAM.getName());
    parameters.add("0");

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
    parameters.add(ERiCDistanceFunction.class.getName());

    // omit preprocessing
    parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedHiCOPreprocessor.class.getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
    parameters.add(Integer.toString(k));

    // delta
    parameters.add(OptionHandler.OPTION_PREFIX + ERiCDistanceFunction.DELTA_P);
    parameters.add(Double.toString(delta));

    // tau
    parameters.add(OptionHandler.OPTION_PREFIX + ERiCDistanceFunction.TAU_P);
    parameters.add(Double.toString(delta));

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // minpts
    minpts = (Integer) optionHandler.getOptionValue(DBSCAN.MINPTS_P);

    // k
    if (optionHandler.isSet(KnnQueryBasedHiCOPreprocessor.K_P)) {
      k = (Integer) optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P);
    }
    else {
      k = minpts;
    }

    // delta
    delta = (Double) optionHandler.getOptionValue(ERiCDistanceFunction.DELTA_P);

    return remainingParameters;
  }
}
