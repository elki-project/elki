package de.lmu.ifi.dbs.elki.data.synthetic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.wrapper.StandAloneWrapper;

/**
 * Provides automatic generation of axes parallel hyperplanes of arbitrary
 * correlation dimensionalities, where the the dependent and independent
 * variables can be specified.
 *
 * @author Elke Achtert
 * todo parameter
 */
public class AxesParallelCorrelationGenerator extends StandAloneWrapper {
    protected final static String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * A pattern defining a comma.
     */
    public static final Pattern COMMA_SPLIT = Pattern.compile(",");

    /**
     * A pattern defining a :.
     */
    public static final Pattern VECTOR_SPLIT = Pattern.compile(":");

    /**
     * OptionID for {@link #DIM_PARAM}
     */
    public static final OptionID DIM_ID = OptionID.getOrCreateOptionID(
        "apcg.dim", "the dimensionality of the feature space.");

    /**
     * OptionID for {@link #CORRDIM_PARAM}
     */
    public static final OptionID CORRDIM_ID = OptionID.getOrCreateOptionID(
        "apcg.corrdim", "the correlation dimensionality of the correlation hyperplane.");

    /**
     * OptionID for {@link #DEPENDENT_VALUES_PARAM}
     */
    public static final OptionID DEPENDENT_VALUES_ID = OptionID.getOrCreateOptionID(
        "apcg.dep", "a vector specifying " +
        "the dependent and independent variables of the correlation hyperplane, " +
        "where d denotes the dimensionality of the feature space. " +
        "p_i = 0 specifies an independent variable, any other value of p_i " +
        "specifies the value of the dependent variable. " +
        "The number of zero values has to " + "correspond with the specified correlation dimensionality. " +
        "The values of the dependent variables have to correspond with the " +
        "specified main and max values. " +
        "If no vector is specified, the " +
        "first dataDim - corrDim variables are the dependent variables " +
        "(the values will be randomized), " +
        "the last corrDim variables are the independent variables.");

    /**
     * OptionID for {@link #NUMBER_PARAM}
     */
    public static final OptionID NUMBER_ID = OptionID.getOrCreateOptionID(
        "apcg.number", "the (positive) number of points in the correlation hyperplane.");

    /**
     * OptionID for {@link #LABEL_PARAM}
     */
    public static final OptionID LABEL_ID = OptionID.getOrCreateOptionID(
        "apcg.label", "a label specifiying the correlation hyperplane, " + "default is no label.");

    /**
     * The default value for minima.
     */
    public static final double MIN_DEFAULT = 0;

    /**
     * OptionID for {@link #MIN_PARAM}
     */
    public static final OptionID MIN_ID = OptionID.getOrCreateOptionID(
        "apcg.minima", "a comma separated list of the coordinates of the minimum "
        + "value in each dimension, default is " + MIN_DEFAULT + " in each dimension");

    /**
     * The default value for maxima.
     */
    public static final double MAX_DEFAULT = 1;

    /**
     * OptionID for {@link #MAX_PARAM}
     */
    public static final OptionID MAX_ID = OptionID.getOrCreateOptionID(
        "apcg.maxima", "a comma separated list of the coordinates of the maximum "
        + "value in each dimension, default is " + MAX_DEFAULT + " in each dimension");

    /**
     * The default value for jitter.
     */
    public static final double JITTER_DEFAULT = 0;

    /**
     * OptionID for {@link #JITTER_PARAM}
     */
    public static final OptionID JITTER_ID = OptionID.getOrCreateOptionID(
        "apcg.jitter", "maximum percentage [0..1] of jitter in each dimension, " + "default is " + JITTER_DEFAULT + ".");

    /**
     * The parameter dim.
     */
    protected final IntParameter CORRDIM_PARAM = new IntParameter(CORRDIM_ID, new GreaterConstraint(0));

    /**
     * The dimensionality of the correlation to be generated.
     */
    int corrDim;

    /**
     * The parameter dim.
     */
    protected final IntParameter DIM_PARAM = new IntParameter(DIM_ID, new GreaterConstraint(0));

    /**
     * The dimensionality of the data points to be generated.
     */
    int dataDim;

    /**
     * Parameter minima.
     */
    protected final DoubleListParameter MIN_PARAM = new DoubleListParameter(MIN_ID, null, true, null);

    /**
     * The minimum value in each dimension.
     */
    double[] min;

    /**
     * Parameter maxima.
     */
    protected final DoubleListParameter MAX_PARAM = new DoubleListParameter(MAX_ID, null, true, null);

    /**
     * The maximum value in each dimension.
     */
    double[] max;

    /**
     * Parameter dep.
     */
    protected final DoubleListParameter DEPENDENT_VALUES_PARAM = new DoubleListParameter(DEPENDENT_VALUES_ID, null, true, null);

    /**
     * Specifies dependent and independent variables.
     */
    double[] dependentValues;

    /**
     * Parameter number.
     */
    protected final IntParameter NUMBER_PARAM = new IntParameter(NUMBER_ID, new GreaterConstraint(0));

    /**
     * The number of points to be generated.
     */
    int number;

    /**
     * Parameter jitter.
     */
    protected final DoubleParameter JITTER_PARAM = new DoubleParameter(JITTER_ID,
        new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 1.0, IntervalConstraint.IntervalBoundary.CLOSE),
        JITTER_DEFAULT);

    /**
     * The maximum percentage of jitter in each dimension.
     */
    double jitter;

    /**
     * Parameter label.
     */
    protected final PatternParameter LABEL_PARAM = new PatternParameter(LABEL_ID, "");

    /**
     * Label for output.
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
        // data dimension
        addOption(DIM_PARAM);
        // correlation dimension
        addOption(CORRDIM_PARAM);

        // minima
        addOption(MIN_PARAM);
        // TODO default value
//    minParameter.setDefaultValue(MIN_DEFAULT);

        // maxima
        addOption(MAX_PARAM);
        // TODO default value
//    maxParameter.setDefaultValue(MAX_DEFAULT);

        // dependent values
        addOption(DEPENDENT_VALUES_PARAM);
        // TODO default value

        // parameter number
        addOption(NUMBER_PARAM);

        // parameter jitter
        addOption(JITTER_PARAM);

        // parameter label
        addOption(LABEL_PARAM);

        // global constraints
        optionHandler.setGlobalParameterConstraint(new LessEqualGlobalConstraint<Integer>(CORRDIM_PARAM, DIM_PARAM));
        optionHandler.setGlobalParameterConstraint(new GlobalListSizeConstraint(MIN_PARAM, DIM_PARAM));
        optionHandler.setGlobalParameterConstraint(new GlobalListSizeConstraint(MAX_PARAM, DIM_PARAM));
        optionHandler.setGlobalParameterConstraint(new GlobalListSizeConstraint(DEPENDENT_VALUES_PARAM, DIM_PARAM));
        // todo global constraint fuer min < max
    }

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        LoggingConfiguration.assertConfigured();
        AxesParallelCorrelationGenerator wrapper = new AxesParallelCorrelationGenerator();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggingUtil.exception(OptionUtil.describeParameterizable(wrapper), cause);
        }
        catch (Exception e) {
          LoggingUtil.exception(OptionUtil.describeParameterizable(wrapper), e);
        }
    }

    /**
     * Runs the wrapper with the specified arguments.
     */
    public void run() throws UnableToComplyException {
        try {
            File outputFile = getOutput();
            if (outputFile.exists()) {
                if (logger.isVerbose()) {
                  logger.verbose("The file " + outputFile + " already exists, " + "the generator result will be appended.");
                }
            }

            OutputStreamWriter outStream = new FileWriter(outputFile, true);
            generateCorrelation(outStream);

            outStream.flush();
            outStream.close();
        }
        catch (FileNotFoundException e) {
            throw new UnableToComplyException(e.getMessage(), e);
        }
        catch (IOException e) {
            throw new UnableToComplyException(e.getMessage(), e);
        }
    }

    /**
     * Generates an axes parallel dependency. The first dataDim - corrDim
     * variables are the dependent variables, the last corrDim variables are the
     * independent variables. The generated data points are in each dimension in
     * the range of [start, start+1].
     *
     * @param outStream the output stream to write to
     * @throws java.io.IOException if an I/O exception occurs during writing
     */
    void generateCorrelation(OutputStreamWriter outStream) throws IOException {
        outStream.write("########################################################" + LINE_SEPARATOR);
        outStream.write("### corrDim " + corrDim + LINE_SEPARATOR);
        outStream.write("########################################################" + LINE_SEPARATOR);

        // generate the feature vectors
        FiniteProgress progress_1 = new FiniteProgress("Generate the feature vectors", number);
        if (logger.isVerbose()) {
          logger.verbose("corrDim " + corrDim);
          logger.verbose("Generate the feature vectors");
        }
        double[][] featureVectors = new double[number][dataDim];
        for (int n = 0; n < number; n++) {
            for (int d = 0; d < dataDim; d++) {
                if (dependentValues[d] != 0)
                    featureVectors[n][d] = dependentValues[d];
                else
                    featureVectors[n][d] = RANDOM.nextDouble() * (max[d] - min[d]) + min[d];
            }

            if (logger.isVerbose()) {
                progress_1.setProcessed(n);
                logger.verbose("\r" + progress_1.toString());
            }
        }

        // jitter the feature vectors
        if (jitter != 0) {
            FiniteProgress progress_2 = new FiniteProgress("Jitter the feature vectors", number);
            if (logger.isVerbose()) {
              logger.verbose("Jitter the feature vectors");
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
                if (logger.isVerbose()) {
                    progress_2.setProcessed(n);
                    logger.verbose("\r" + progress_2.toString());
                }
            }
        }

        // print the feature vectors
        FiniteProgress progress_3 = new FiniteProgress("Print the feature vectors", number);
        if (logger.isVerbose()) {
          logger.verbose("Print the feature vectors");
        }
        for (int n = 0; n < number; n++) {
            for (int d = 0; d < dataDim; d++) {
                outStream.write(featureVectors[n][d] + " ");
            }
            outStream.write(label + LINE_SEPARATOR);
            if (logger.isVerbose()) {
                progress_3.setProcessed(n);
                logger.verbose("\r" + progress_3.toString());
            }
        }
    }

    // todo comment
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // dataDim
        dataDim = DIM_PARAM.getValue();

        // corrDim
        corrDim = CORRDIM_PARAM.getValue();

        // minima
        min = new double[dataDim];
        if (MIN_PARAM.isSet()) {
            List<Double> min_list = MIN_PARAM.getValue();
            for (int i = 0; i < dataDim; i++) {
                min[i] = (min_list.get(i));
            }
        }
        else {
            Arrays.fill(min, MIN_DEFAULT);
        }

        // maxima
        max = new double[dataDim];
        if (MAX_PARAM.isSet()) {
            List<Double> max_list = MAX_PARAM.getValue();
            for (int i = 0; i < dataDim; i++) {
                max[i] = max_list.get(i);
            }
        }
        else {
            Arrays.fill(max, MAX_DEFAULT);
        }

        // min < max?
        for (int i = 0; i < dataDim; i++) {
            if (min[i] >= max[i]) {
                throw new WrongParameterValueException("Parameter " + MIN_PARAM.getName() + " > " + MAX_PARAM.getName() + "!");
            }
        }

        // dependent values
        if (DEPENDENT_VALUES_PARAM.isSet()) {
            List<Double> dep_list = DEPENDENT_VALUES_PARAM.getValue();

            double[] dv = new double[dataDim];
            int c = 0;
            for (int d = 0; d < dataDim; d++) {
                try {
                    dv[d] = dep_list.get(d);
                    if (dv[d] == 0) {
                        c++;
                    }
                    else if (dv[d] < min[d] || dv[d] > max[d]) {
                        throw new WrongParameterValueException(DEPENDENT_VALUES_PARAM, dep_list.toString(), null);
                    }
                }
                catch (NumberFormatException e) {
                    throw new WrongParameterValueException(DEPENDENT_VALUES_PARAM, dep_list.toString(), e);
                }
            }
            if (c != corrDim) {
                throw new WrongParameterValueException("Value of parameter " + DEPENDENT_VALUES_ID.getName()
                    + " does not correspond with the specified correlation dimensionality  " + "Numbber of zero values " + c + " != "
                    + corrDim);
            }
            dependentValues = dv;
        }
        else {
            dependentValues = new double[dataDim];
            for (int i = 0; i < dataDim - corrDim; i++) {
                dependentValues[i] = RANDOM.nextDouble() * (max[i] - min[i]) + min[i];
            }
        }

        // number of points
        number = NUMBER_PARAM.getValue();

        // jitter
        jitter = JITTER_PARAM.getValue();

        // label
        label = LABEL_PARAM.getValue();

        return remainingParameters;
    }

    /**
     * Returns the description for the output parameter. Subclasses may
     * need to overwrite this method.
     *
     * @return the description for the output parameter
     */
    @Override
    public String getOutputDescription() {
        return "the file to write the generated correlation hyperplane in, "
            + "if the file already exists, the generated points will be appended to this file.";
    }
}
