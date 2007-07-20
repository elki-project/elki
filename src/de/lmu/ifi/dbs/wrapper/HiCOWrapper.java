package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.PreprocessorHandler;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.*;
import de.lmu.ifi.dbs.varianceanalysis.PercentageEigenPairFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for HiCO algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert 
 */
public class HiCOWrapper extends NormalizationWrapper {

  /**
   * Description for parameter k.
   */
  public static final String K_D = "a positive integer specifying the number of nearest neighbors considered in the PCA. "
                                   + "If this value is not defined, k ist set to minpts";

  /**
   * The minpts parameter.
   */
  private IntParameter minpts;

  /**
   * The k parameter.
   */
  private IntParameter k;

  /**
   * The alpha parameter.
   */
  private DoubleParameter alpha;

  /**
   * The delta parameter.
   */
  private DoubleParameter delta;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    HiCOWrapper wrapper = new HiCOWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
//      e.printStackTrace();
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
//      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public HiCOWrapper() {
    super();
    // parameter minpts
    minpts = new IntParameter(OPTICS.MINPTS_P,
                              OPTICS.MINPTS_D,
                              new GreaterConstraint(0));
    optionHandler.put(minpts);

    // parameter k
    k = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P,
                         K_D,
                         new GreaterConstraint(0));
    k.setOptional(true);
    optionHandler.put(k);

    // parameter delta
    delta = new DoubleParameter(PCABasedCorrelationDistanceFunction.DELTA_P,
                                PCABasedCorrelationDistanceFunction.DELTA_D,
                                new GreaterEqualConstraint(0));
    delta.setDefaultValue(PCABasedCorrelationDistanceFunction.DEFAULT_DELTA);
    optionHandler.put(delta);

    // parameter alpha
    ArrayList<ParameterConstraint<Number>> alphaConstraints = new ArrayList<ParameterConstraint<Number>>();
    alphaConstraints.add(new GreaterConstraint(0));
    alphaConstraints.add(new LessConstraint(1));
    alpha = new DoubleParameter(PercentageEigenPairFilter.ALPHA_P,
                                PercentageEigenPairFilter.ALPHA_D,
                                alphaConstraints);
    alpha.setDefaultValue(PercentageEigenPairFilter.DEFAULT_ALPHA);
    optionHandler.put(PercentageEigenPairFilter.ALPHA_P, alpha);

    // global constraint for minpts and k
    GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(k, minpts);
    optionHandler.setGlobalParameterConstraint(gpc);
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // OPTICS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(PCABasedCorrelationDistanceFunction.class.getName());

    // omit flag
    parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(PCABasedCorrelationDistanceFunction.INFINITY_PATTERN);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + minpts.getName());
    parameters.add(Integer.toString(minpts.getValue()));

    // preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.PREPROCESSOR_CLASS_P);
    parameters.add(KnnQueryBasedHiCOPreprocessor.class.getName());

    // k for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
    parameters.add(Integer.toString(k.getValue()));

    // alpha
    parameters.add(OptionHandler.OPTION_PREFIX + alpha.getName());
    parameters.add(Double.toString(alpha.getValue()));

    // delta
    parameters.add(OptionHandler.OPTION_PREFIX + delta.getName());
    parameters.add(Double.toString(delta.getValue()));

    return parameters;
  }
}
