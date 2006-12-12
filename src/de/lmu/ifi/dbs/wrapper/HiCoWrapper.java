package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * Wrapper class for HiCo algorithm. Performs an attribute wise normalization on
 * the database objects.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiCoWrapper extends NormalizationWrapper {

	/**
	 * Description for parameter k.
	 */
	public static final String K_D = "a positive integer specifying the number of " + "nearest neighbors considered in the PCA. "
			+ "If this value is not defined, k ist set to minpts";

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
	 * @param args
	 *            the arguments to run this wrapper
	 */
	public static void main(String[] args) {
		HiCoWrapper wrapper = new HiCoWrapper();
		try {
			wrapper.setParameters(args);
			wrapper.run();
		} catch (ParameterException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
		} catch (AbortException e) {
			wrapper.verbose(e.getMessage());
		} catch (Exception e) {

			wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
		}
	}

	/**
	 * Sets the parameter minpts and k in the parameter map additionally to the
	 * parameters provided by super-classes.
	 */
	public HiCoWrapper() {
		super();
		// parameter min points
		IntParameter minPam = new IntParameter(OPTICS.MINPTS_P, OPTICS.MINPTS_D, new GreaterConstraint(0));
		optionHandler.put(OPTICS.MINPTS_P, minPam);

		// parameter k
		IntParameter k = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P, K_D, new GreaterConstraint(0));
		k.setOptional(true);
		optionHandler.put(KnnQueryBasedHiCOPreprocessor.K_P, k);

		GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(k, minPam);
		optionHandler.setGlobalParameterConstraint(gpc);
	}

	/**
	 * @see KDDTaskWrapper#getKDDTaskParameters()
	 */
	public List<String> getKDDTaskParameters() {
		List<String> parameters = super.getKDDTaskParameters();

		// OPTICS algorithm
		parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
		parameters.add(OPTICS.class.getName());

		// distance function
		parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
		parameters.add(PCABasedCorrelationDistanceFunction.class.getName());

		// omit flag
		parameters.add(OptionHandler.OPTION_PREFIX + PCABasedCorrelationDistanceFunction.OMIT_PREPROCESSING_F);

		// epsilon for OPTICS
		parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
		parameters.add(PCABasedCorrelationDistanceFunction.INFINITY_PATTERN);

		// minpts for OPTICS
		parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
		parameters.add(minpts);

		// preprocessor
		parameters.add(OptionHandler.OPTION_PREFIX + PCABasedCorrelationDistanceFunction.PREPROCESSOR_CLASS_P);
		parameters.add(KnnQueryBasedHiCOPreprocessor.class.getName());

		// k for preprocessor
		parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
		parameters.add(k);

		return parameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);
		// minpts
		minpts = ((Integer) optionHandler.getOptionValue(OPTICS.MINPTS_P)).toString();
		
		// k
		k = ((Integer) optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P)).toString();

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> settings = super.getAttributeSettings();
		AttributeSettings mySettings = settings.get(0);
		mySettings.addSetting(OPTICS.MINPTS_P, minpts);
		mySettings.addSetting(KnnQueryBasedHiCOPreprocessor.K_P, k);
		return settings;
	}

}
