package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.wrapper.StandAloneWrapper;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Provides automatic generation of axes parallel hyperplanes of arbitrary
 * correlation dimensionalities, where the the dependent and independent
 * variables can be specified.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class AxesParallelCorrelationGenerator extends StandAloneWrapper {

	static {
		OUTPUT_D = "<filename>the file to write the generated correlation hyperplane in, "
				+ "if the file already exists, the generated points will be appended to this file.";
	}

	public final static String LINE_SEPARATOR = System.getProperty("line.separator");

	/**
	 * A pattern defining a comma.
	 */
	public static final Pattern COMMA_SPLIT = Pattern.compile(",");

	/**
	 * A pattern defining a :.
	 */
	public static final Pattern VECTOR_SPLIT = Pattern.compile(":");

	/**
	 * Parameter for dimensionality.
	 */
	public static final String DIM_P = "dim";

	/**
	 * Description for parameter dim.
	 */
	public static final String DIM_D = "the dimensionality of the feature space.";

	/**
	 * Parameter for correlation dimensionality.
	 */
	public static final String CORRDIM_P = "corrdim";

	/**
	 * Description for parameter corrdim.
	 */
	public static final String CORRDIM_D = "the correlation dimensionality of the correlation hyperplane.";

	/**
	 * Parameter for dep.
	 */
	public static final String DEPENDENT_VALUES_P = "dep";

	/**
	 * Description for parameter pref.
	 */
	public static final String DEPENDENT_VALUES_D = "<p_1,...,p_d>a vector specifying "
			+ "the dependent and independent variables of the correlation hyperplane, "
			+ "where d denotes the dimensionality of the feature space. "
			+ "p_i = 0 specifies an independent variable, any other value of p_i " + "specifies the value of the dependent variable. "
			+ "The number of zero values has to " + "correspond with the specified correlation dimensionality. The values of the "
			+ "dependent variables have to correspond with the specified main and max values. "
			+ "If no preference vector is specified, the " + "the first dataDim - corrDim variables are the dependent variables "
			+ "(the values will be randomized), " + "the last corrDim variables are the independent variables.";

	/**
	 * Parameter for number of points.
	 */
	public static final String NUMBER_P = "number";

	/**
	 * Description for parameter number.
	 */
	public static final String NUMBER_D = "the (positive) number of points in the correlation hyperplane.";

	/**
	 * Parameter for label.
	 */
	public static final String LABEL_P = "label";

	/**
	 * Description for parameter label.
	 */
	public static final String LABEL_D = "a label specifiying the correlation hyperplane, " + "default is no label.";

	/**
	 * Parameter for minimum value.
	 */
	public static final String MIN_P = "minima";

	/**
	 * The default value for min.
	 */
	public static final double MIN_DEFAULT = 0;

	/**
	 * Description for parameter min.
	 */
	public static final String MIN_D = "<min_1,...,min_d>a comma separated list of the coordinates of the minimum "
			+ "value in each dimension, default is " + MIN_DEFAULT + " in each dimension";

	/**
	 * Parameter for maximum value.
	 */
	public static final String MAX_P = "maxima";

	/**
	 * The default value for max.
	 */
	public static final double MAX_DEFAULT = 1;

	/**
	 * Description for parameter max.
	 */
	public static final String MAX_D = "<max_1,...,max_d>a comma separated list of the coordinates of the maximum "
			+ "value in each dimension, default is " + MAX_DEFAULT + " in each dimension";

	/**
	 * Parameter for jitter.
	 */
	public static final String JITTER_P = "jitter";

	/**
	 * The default value for jitter.
	 */
	public static final double JITTER_DEFAULT = 0;

	/**
	 * Description for parameter jitter.
	 */
	public static final String JITTER_D = "maximum percentage [0..1] of jitter in each dimension, " + "default is " + JITTER_DEFAULT + ".";

	/**
	 * The dimensionality of the correlation to be generated.
	 */
	int corrDim;

	/**
	 * The dimensionality of the data points to be generated.
	 */
	int dataDim;

	/**
	 * The minimum value in each dimension.
	 */
	double[] min;

	/**
	 * The maximum value in each dimension.
	 */
	double[] max;

	/**
	 * Specifies dependent and independent variables.
	 */
	double[] dependentValues;

	/**
	 * The number of points to be generated.
	 */
	int number;

	/**
	 * The maximum percentage of jitter in each dimension.
	 */
	double jitter;

	/**
	 * Label for outpout.
	 */
	String label;

	/**
	 * The random generator.
	 */
	static Random RANDOM = new Random();

	/**
	 * Creates a new correlation generator that provides automatic generation of
	 * axes parallel hyperplanes of arbitrary correlation dimensionalities.
	 */
	public AxesParallelCorrelationGenerator() {
		super();
		// parameter dimension
		IntParameter dim = new IntParameter(DIM_P, DIM_D, new GreaterConstraint(0));
		optionHandler.put(DIM_P, dim);

		// parameter correlation dim
		IntParameter corr = new IntParameter(CORRDIM_P, CORRDIM_D, new GreaterConstraint(0));
		optionHandler.put(CORRDIM_P, corr);

		// parameter minDim
		DoubleListParameter min = new DoubleListParameter(MIN_P, MIN_D);
		// TODO min.setDefaultValue(MIN_DEFAULT);
		optionHandler.put(MIN_P, min);

		// parameter maxDim
		DoubleListParameter max = new DoubleListParameter(MAX_P, MAX_D);
		// TODO max.setDefaultValue(MAX_DEFAULT);
		optionHandler.put(MAX_P, max);

		// parameter dependent values
		DoubleListParameter dep = new DoubleListParameter(DEPENDENT_VALUES_P, DEPENDENT_VALUES_D);
		// TODO default value
		optionHandler.put(DEPENDENT_VALUES_P, dep);

		// parameter number
		optionHandler.put(NUMBER_P, new IntParameter(NUMBER_P, NUMBER_D, new GreaterConstraint(0)));

		// parameter jitter
		ArrayList<ParameterConstraint<Number>> jitterCons = new ArrayList<ParameterConstraint<Number>>();
		jitterCons.add(new GreaterEqualConstraint(0));
		jitterCons.add(new LessEqualConstraint(1));
		DoubleParameter jitterPam = new DoubleParameter(JITTER_P, JITTER_D, jitterCons);
		jitterPam.setDefaultValue(JITTER_DEFAULT);
		optionHandler.put(JITTER_P, jitterPam);

		// parameter label
		optionHandler.put(LABEL_P, new StringParameter(LABEL_P, LABEL_D));

		// global constraints
		optionHandler.setGlobalParameterConstraint(new LessEqualGlobalConstraint(corr, dim));
		optionHandler.setGlobalParameterConstraint(new LengthGlobalConstraint(min, dim));
		optionHandler.setGlobalParameterConstraint(new LengthGlobalConstraint(max, dim));
		// TODO remaining global constraints
	}

	/**
	 * Main method to run this wrapper.
	 * 
	 * @param args
	 *            the arguments to run this wrapper
	 */
	public static void main(String[] args) {
		LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
		AxesParallelCorrelationGenerator wrapper = new AxesParallelCorrelationGenerator();
		try {
			wrapper.setParameters(args);
			wrapper.run();
		} catch (ParameterException e) {
			e.printStackTrace();
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
		} catch (Exception e) {
			e.printStackTrace();
			wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
		}
	}

	/**
	 * Runs the wrapper with the specified arguments.
	 */
	public void run() throws UnableToComplyException {
		try {
			File outputFile = getOutput();
			if (outputFile.exists()) {
				if (isVerbose()) {
					verbose("The file " + outputFile + " already exists, " + "the generator result will be appended.");
				}
			}

			OutputStreamWriter outStream = new FileWriter(outputFile, true);
			generateCorrelation(outStream);

			outStream.flush();
			outStream.close();
		} catch (FileNotFoundException e) {
			throw new UnableToComplyException(e.getMessage(), e);
		} catch (IOException e) {
			throw new UnableToComplyException(e.getMessage(), e);
		}
	}

	/**
	 * Generates an axes parallel dependency. The first dataDim - corrDim
	 * variables are the dependent variables, the last corrDim variables are the
	 * independent variables. The generated data points are in each dimension in
	 * the range of [start, start+1].
	 * 
	 * @param outStream
	 *            the output stream to write to
	 */
	void generateCorrelation(OutputStreamWriter outStream) throws IOException {
		outStream.write("########################################################" + LINE_SEPARATOR);
		outStream.write("### corrDim " + corrDim + LINE_SEPARATOR);
		outStream.write("########################################################" + LINE_SEPARATOR);

		// generate the feature vectors
		Progress progress_1 = new Progress("Generate the feature vectors", number);
		if (isVerbose()) {
			verbose(LINE_SEPARATOR + "corrDim " + corrDim + LINE_SEPARATOR);
			verbose("Generate the feature vectors" + LINE_SEPARATOR);
		}
		double[][] featureVectors = new double[number][dataDim];
		for (int n = 0; n < number; n++) {
			for (int d = 0; d < dataDim; d++) {
				if (dependentValues[d] != 0)
					featureVectors[n][d] = dependentValues[d];
				else
					featureVectors[n][d] = RANDOM.nextDouble() * (max[d] - min[d]) + min[d];
			}

			if (isVerbose()) {
				progress_1.setProcessed(n);
				verbose("\r" + progress_1.toString());
			}
		}

		// jitter the feature vectors
		if (jitter != 0) {
			Progress progress_2 = new Progress("Jitter the feature vectors", number);
			if (isVerbose()) {
				verbose(LINE_SEPARATOR + "Jitter the feature vectors" + LINE_SEPARATOR);
			}
			for (int n = 0; n < number; n++) {
				for (int d = 0; d < dataDim; d++) {
					double jitter = featureVectors[n][d] * RANDOM.nextDouble() * this.jitter / 100;
					boolean plus = RANDOM.nextBoolean();
					if (plus)
						featureVectors[n][d] += jitter;
					else
						featureVectors[n][d] -= jitter;
				}
				if (isVerbose()) {
					progress_2.setProcessed(n);
					verbose("\r" + progress_2.toString());
				}
			}
		}

		// print the feature vectors
		Progress progress_3 = new Progress("Print the feature vectors", number);
		if (isVerbose()) {
			verbose(LINE_SEPARATOR + "Print the feature vectors" + LINE_SEPARATOR);
		}
		for (int n = 0; n < number; n++) {
			for (int d = 0; d < dataDim; d++) {
				outStream.write(featureVectors[n][d] + " ");
			}
			outStream.write(label + LINE_SEPARATOR);
			if (isVerbose()) {
				progress_3.setProcessed(n);
				verbose("\r" + progress_3.toString());
			}
		}
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// dim
		dataDim = (Integer) optionHandler.getOptionValue(DIM_P);

		// corrDim;
		corrDim = (Integer) optionHandler.getOptionValue(CORRDIM_P);

		// corrDim < dim?
		if (corrDim > dataDim) {
			throw new WrongParameterValueException("Parameter " + CORRDIM_P + " > " + DIM_P + " (" + corrDim + " > " + dataDim + ")!");
		}

		// min
		min = new double[dataDim];
		if (optionHandler.isSet(MIN_P)) {
			List<Double> min_list = (List<Double>) optionHandler.getOptionValue(MIN_P);
			// String minString = optionHandler.getOptionValue(MIN_P);
			// String[] minima = COMMA_SPLIT.split(minString);
			if (min_list.size() != dataDim)
				throw new WrongParameterValueException("Value of parameter " + MIN_P + " has not the specified dimensionality  " + DIM_P
						+ " = " + dataDim);

			for (int i = 0; i < dataDim; i++) {
				try {
					min[i] = (min_list.get(i));
				} catch (NumberFormatException e) {
					throw new WrongParameterValueException(MIN_P, min_list.toString(), MIN_D);
				}
			}
		} else {
			Arrays.fill(min, MIN_DEFAULT);
		}

		// max
		max = new double[dataDim];
		if (optionHandler.isSet(MAX_P)) {
			List<Double> max_list = (List<Double>) optionHandler.getOptionValue(MAX_P);
			// String maxString = optionHandler.getOptionValue(MAX_P);
			// String[] maxima = COMMA_SPLIT.split(maxString);
			if (max_list.size() != dataDim)
				throw new WrongParameterValueException("Value of parameter " + MAX_P + " has not the specified dimensionality  " + DIM_P
						+ " = " + dataDim);

			for (int i = 0; i < dataDim; i++) {
				try {
					max[i] = max_list.get(i);
				} catch (NumberFormatException e) {
					throw new WrongParameterValueException(MAX_P, max_list.toString(), MAX_D);
				}
			}
		} else {
			Arrays.fill(max, MAX_DEFAULT);
		}

		// min < max?
		for (int i = 0; i < dataDim; i++) {
			if (min[i] >= max[i]) {
				throw new WrongParameterValueException("Parameter " + MIN_P + " > " + MAX_P + "!");
			}
		}

		// dependent values
		if (optionHandler.isSet(DEPENDENT_VALUES_P)) {
			List<Double> dep_list = (List<Double>) optionHandler.getOptionValue(DEPENDENT_VALUES_P);
			// String prefString = optionHandler
			// .getOptionValue(DEPENDENT_VALUES_P);
			// String[] prefVectorString = COMMA_SPLIT.split(prefString);
			if (dep_list.size() != dataDim) {
				throw new WrongParameterValueException("Value of parameter " + DEPENDENT_VALUES_P
						+ " has not the specified dimensionality  " + DIM_P + " = " + dataDim);
			}

			double[] dv = new double[dataDim];
			int c = 0;
			for (int d = 0; d < dataDim; d++) {
				try {
					dv[d] = dep_list.get(d);
					if (dv[d] == 0) {
						c++;
					} else if (dv[d] < min[d] || dv[d] > max[d]) {
						throw new WrongParameterValueException(DEPENDENT_VALUES_P, dep_list.toString(), DEPENDENT_VALUES_D);
					}
				} catch (NumberFormatException e) {
					throw new WrongParameterValueException(DEPENDENT_VALUES_P, dep_list.toString(), DEPENDENT_VALUES_D, e);
				}
			}
			if (c != corrDim) {
				throw new WrongParameterValueException("Value of parameter " + DEPENDENT_VALUES_P
						+ " does not correspond with the specified correlation dimensionality  " + "Numbber of zero values " + c + " != "
						+ corrDim);
			}
			dependentValues = dv;
		} else {
			dependentValues = new double[dataDim];
			for (int i = 0; i < dataDim - corrDim; i++) {
				dependentValues[i] = RANDOM.nextDouble() * (max[i] - min[i]) + min[i];
			}
		}

		// number of points
		number = (Integer) optionHandler.getOptionValue(NUMBER_P);

		// jitter
		jitter = (Double) optionHandler.getOptionValue(JITTER_P);

		// label
		if (optionHandler.isSet(LABEL_P)) {
			label = (String)optionHandler.getOptionValue(LABEL_P);
		} else
			label = "";

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> settings = super.getAttributeSettings();
		AttributeSettings mySettings = settings.get(0);
		mySettings.addSetting(DIM_P, Integer.toString(dataDim));
		mySettings.addSetting(CORRDIM_P, Integer.toString(corrDim));
		mySettings.addSetting(MIN_P, Util.format(min, ","));
		mySettings.addSetting(MAX_P, Util.format(max, ","));
		mySettings.addSetting(DEPENDENT_VALUES_P, Util.format(dependentValues, ","));
		mySettings.addSetting(NUMBER_P, Integer.toString(number));
		mySettings.addSetting(JITTER_P, Double.toString(jitter));
		mySettings.addSetting(LABEL_P, label);
		return settings;
	}

}
