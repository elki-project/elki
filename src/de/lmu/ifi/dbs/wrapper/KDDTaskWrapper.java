package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * KDDTaskWrapper is an abstract super class for all wrapper classes running
 * algorithms in a kdd task.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class KDDTaskWrapper extends AbstractWrapper {

	/**
	 * The result of the kdd task.
	 */
	private Result result;

	/**
	 * The name of the output file.
	 */
	private String output;

	/**
	 * Time flag;
	 */
	private boolean time;

	/**
	 * Sets additionally to the parameters set by the super class the time flag
	 * and the parameter out in the parameter map. Any extending class should
	 * call this constructor, then add further parameters.
	 */
	protected KDDTaskWrapper() {
		super();
		FileParameter output = new FileParameter(KDDTask.OUTPUT_P,
				KDDTask.OUTPUT_D,FileParameter.FILE_OUT);
		output.setOptional(true);
		optionHandler.put(KDDTask.OUTPUT_P, output);

		optionHandler.put(AbstractAlgorithm.TIME_F, new Flag(
				AbstractAlgorithm.TIME_F, AbstractAlgorithm.TIME_D));
	}

	/**
	 * @see Wrapper#run()
	 */
	public final void run() throws UnableToComplyException {
		try {
			List<String> parameters = getKDDTaskParameters();
			KDDTask task = new KDDTask();
			task.setParameters(parameters
					.toArray(new String[parameters.size()]));
			result = task.run();
		} catch (ParameterException e) {
			e.printStackTrace();
			throw new UnableToComplyException(e);
		}
	}

	/**
	 * Returns the result of the kdd task.
	 * 
	 * @return the result of the kdd task
	 */
	public final Result getResult() {
		return result;
	}

	/**
	 * Returns the name of the output file.
	 * 
	 * @return the name of the output file
	 */
	public final String getOutput() {
		return output;
	}

	/**
	 * Returns the value of the time flag.
	 * 
	 * @return the value of the time flag.
	 */
	public final boolean isTime() {
		return time;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);
		// output
		if (optionHandler.isSet(KDDTask.OUTPUT_P)) {
			output = optionHandler.getOptionValue(KDDTask.OUTPUT_P);
		}
		// time
		time = optionHandler.isSet(AbstractAlgorithm.TIME_F);

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> settings = super.getAttributeSettings();
		AttributeSettings mySettings = settings.get(0);
		mySettings.addSetting(KDDTask.OUTPUT_P, output);
		mySettings.addSetting(AbstractAlgorithm.TIME_F, Boolean.toString(time));
		return settings;
	}

	/**
	 * Returns the parameters that are necessary to run the kdd task correctly.
	 * 
	 * @return the array containing the parametr setting that is necessary to
	 *         run the kdd task correctly
	 */
	public List<String> getKDDTaskParameters() {
		List<String> result = getRemainingParameters();

		// verbose
		if (isVerbose()) {
			result.add(OptionHandler.OPTION_PREFIX
					+ AbstractAlgorithm.VERBOSE_F);
		}
		// time
		if (isTime()) {
			result.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
		}
		// output
		if (output != null) {
			result.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
			result.add(getOutput());
		}

		return result;
	}
}
