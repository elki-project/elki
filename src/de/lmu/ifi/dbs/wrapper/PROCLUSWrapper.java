package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PROCLUS;

import java.util.List;

/**
 * Wrapper class for PROCLUS algorithm. Performs an attribute wise normalization
 * on the database objects.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PROCLUSWrapper extends FileBasedDatabaseConnectionWrapper {

	/**
	 * The value of the k parameter.
	 */
	private String k;

	/**
	 * The value of the k_i parameter.
	 */
	private String k_i;

	/**
	 * The value of the dim parameter.
	 */
	private String dim;

	/**
	 * Main method to run this wrapper.
	 * 
	 * @param args
	 *            the arguments to run this wrapper
	 */
	public static void main(String[] args) {
		PROCLUSWrapper wrapper = new PROCLUSWrapper();
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
	 * Sets the parameters k, k_i and dim in the parameter map additionally to
	 * the parameters provided by super-classes.
	 */
	public PROCLUSWrapper() {
		super();
		optionHandler.put(PROCLUS.K_P, new IntParameter(PROCLUS.K_P, PROCLUS.K_D, new GreaterConstraint(0)));

		IntParameter ki = new IntParameter(PROCLUS.K_I_P, PROCLUS.K_I_D, new GreaterConstraint(0));
		ki.setDefaultValue(PROCLUS.K_I_DEFAULT);
		optionHandler.put(PROCLUS.K_I_P, ki);

		optionHandler.put(PROCLUS.L_P, new IntParameter(PROCLUS.L_P, PROCLUS.L_D, new GreaterConstraint(0)));
	}

	/**
	 * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
	 */
	public List<String> getKDDTaskParameters() {
		List<String> parameters = super.getKDDTaskParameters();

		// PROCLUS algorithm
		parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
		parameters.add(PROCLUS.class.getName());

		// dim
		parameters.add(OptionHandler.OPTION_PREFIX + PROCLUS.L_P);
		parameters.add(dim);

		// k
		parameters.add(OptionHandler.OPTION_PREFIX + PROCLUS.K_P);
		parameters.add(k);

		// k_i
		parameters.add(OptionHandler.OPTION_PREFIX + PROCLUS.K_I_P);
		parameters.add(k_i);

		return parameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// k, dim
		k = ((Integer) optionHandler.getOptionValue(PROCLUS.K_P)).toString();
		dim = ((Integer) optionHandler.getOptionValue(PROCLUS.L_P)).toString();

		// k_i
		k_i = ((Integer) optionHandler.getOptionValue(PROCLUS.K_I_P)).toString();

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> settings = super.getAttributeSettings();
		AttributeSettings mySettings = settings.get(0);
		mySettings.addSetting(PROCLUS.K_P, k);
		mySettings.addSetting(PROCLUS.K_I_P, k_i);
		mySettings.addSetting(PROCLUS.L_P, dim);
		return settings;
	}

}
