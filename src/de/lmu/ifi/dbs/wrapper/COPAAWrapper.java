package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * Wrapper class for COPAA algorithm. Performs an attribute wise normalization on
 * the database objects, partitions the database according to the correlation dimension of
 * its objects and then performs the algorithm OPTICS over the partitions.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPAAWrapper extends NormalizationWrapper {

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "the maximum radius of the neighborhood to" +
                                         "be considerd, must be suitable to " +
                                         LocallyWeightedDistanceFunction.class.getName();

  /**
   * Description for parameter k.
   */
  public static final String K_D = "a positive integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set to minpts";

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * The value of the minpts parameter.
   */
  private String minpts;

  /**
   * The value of the k parameter.
   */
  private String k;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    COPAAWrapper wrapper = new COPAAWrapper();
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
    	wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter epsilon, minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public COPAAWrapper() {
    super();
    // parameter epsilon
    // TODO distance functio constraint
    optionHandler.put(OPTICS.EPSILON_P, new PatternParameter(OPTICS.EPSILON_P, EPSILON_D));
    
    // parameter min points
    IntParameter minPam = new IntParameter(OPTICS.MINPTS_P, OPTICS.MINPTS_D,new GreaterConstraint(0));
    optionHandler.put(OPTICS.MINPTS_P, minPam);
  
    // parameter k
    IntParameter kPam = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P,K_D, new GreaterConstraint(0));
    kPam.setOptional(true);
    optionHandler.put(KnnQueryBasedHiCOPreprocessor.K_P,kPam);
    
    GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(kPam, minPam);
    optionHandler.setGlobalParameterConstraint(gpc);
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm COPAA
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(COPAA.class.getName());

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

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedHiCOPreprocessor.class.getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
    parameters.add(k);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // epsilon, minpts
    epsilon = (String)optionHandler.getOptionValue(OPTICS.EPSILON_P);
    minpts = ((Integer)optionHandler.getOptionValue(OPTICS.MINPTS_P)).toString();
    // k
    k = ((Integer)optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P)).toString();

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(OPTICS.EPSILON_P, epsilon);
    mySettings.addSetting(OPTICS.MINPTS_P, minpts);
    mySettings.addSetting(KnnQueryBasedHiCOPreprocessor.K_P, k);
    return settings;
  }

}
