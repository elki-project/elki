package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.PatternParameter;

/**
 * Wrapper class for COPAC algorithm. Performs an attribute wise normalization on
 * the database objects, partitions the database according to the correlation dimension of
 * its objects and then performs the algorithm DBSCAN over the partitions.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPACWrapper extends NormalizationWrapper {

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
    COPACWrapper wrapper = new COPACWrapper();
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
  public COPACWrapper() {
    super();
    // parameter epsilon
    PatternParameter eps = new PatternParameter(DBSCAN.EPSILON_P,EPSILON_D);
	//TODO constraint mit distance function
	optionHandler.put(DBSCAN.EPSILON_P, eps);
    
    // parameter min points
    optionHandler.put(DBSCAN.MINPTS_P, new IntParameter(DBSCAN.MINPTS_P,OPTICS.MINPTS_D,new GreaterConstraint(0)));
    
    // parameter k
    // TODO default value
    optionHandler.put(KnnQueryBasedHiCOPreprocessor.K_P,new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P,K_D,new GreaterConstraint(0)));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm COPAC
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(COPAC.class.getName());

    // partition algorithm DBSCAN
    parameters.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    parameters.add(DBSCAN.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(minpts);

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
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
    epsilon = optionHandler.getOptionValue(DBSCAN.EPSILON_P);
    minpts = optionHandler.getOptionValue(DBSCAN.MINPTS_P);
    // k
    if (optionHandler.isSet(KnnQueryBasedHiCOPreprocessor.K_P)) {
      k = optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P);
    }
    else {
      k = minpts;
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(DBSCAN.EPSILON_P, epsilon);
    mySettings.addSetting(DBSCAN.MINPTS_P, minpts);
    mySettings.addSetting(KnnQueryBasedHiCOPreprocessor.K_P, k);
    return settings;
  }

}
