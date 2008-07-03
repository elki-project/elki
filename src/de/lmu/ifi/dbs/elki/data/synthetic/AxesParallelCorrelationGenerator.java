package de.lmu.ifi.dbs.elki.data.synthetic;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.*;
import de.lmu.ifi.dbs.elki.wrapper.StandAloneWrapper;

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
 * @author Elke Achtert
 * todo parameter
 */
public class AxesParallelCorrelationGenerator extends StandAloneWrapper {
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
     * Label for parameter dimensionality.
     */
    public static final String DIM_P = "dim";

    /**
     * Description for parameter dim.
     */
    public static final String DIM_D = "the dimensionality of the feature space.";

    /**
     * Label for parameter correlation dimensionality.
     */
    public static final String CORRDIM_P = "corrdim";

    /**
     * Description for parameter corrdim.
     */
    public static final String CORRDIM_D = "the correlation dimensionality of the correlation hyperplane.";

    /**
     * Label for parameter for dep.
     */
    public static final String DEPENDENT_VALUES_P = "dep";

    /**
     * Description for parameter dep.
     */
    public static final String DEPENDENT_VALUES_D = "<p_1,...,p_d>a vector specifying " +
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
        "the last corrDim variables are the independent variables.";

    /**
     * Label for parameter for number of points.
     */
    public static final String NUMBER_P = "number";

    /**
     * Description for parameter number.
     */
    public static final String NUMBER_D = "the (positive) number of points in the correlation hyperplane.";

    /**
     * Label for parameter for label.
     */
    public static final String LABEL_P = "label";

    /**
     * Description for parameter label.
     */
    public static final String LABEL_D = "a label specifiying the correlation hyperplane, " + "default is no label.";

    /**
     * Label for parameter minimum values.
     */
    public static final String MIN_P = "minima";

    /**
     * The default value for minima.
     */
    public static final double MIN_DEFAULT = 0;

    /**
     * Description for parameter minima.
     */
    public static final String MIN_D = "<min_1,...,min_d>a comma separated list of the coordinates of the minimum "
        + "value in each dimension, default is " + MIN_DEFAULT + " in each dimension";

    /**
     * Label for parameter maximum values.
     */
    public static final String MAX_P = "maxima";

    /**
     * The default value for maxima.
     */
    public static final double MAX_DEFAULT = 1;

    /**
     * Description for parameter maxima.
     */
    public static final String MAX_D = "<max_1,...,max_d>a comma separated list of the coordinates of the maximum "
        + "value in each dimension, default is " + MAX_DEFAULT + " in each dimension";

    /**
     * Label for parameter jitter.
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
     * The parameter dim.
     */
    IntParameter corrDimParameter;

    /**
     * The dimensionality of the correlation to be generated.
     */
    int corrDim;

    /**
     * The parameter dim.
     */
    IntParameter dimParameter;

    /**
     * The dimensionality of the data points to be generated.
     */
    int dataDim;

    /**
     * Parameter minima.
     */
    DoubleListParameter minParameter;

    /**
     * The minimum value in each dimension.
     */
    double[] min;

    /**
     * Parameter maxima.
     */
    DoubleListParameter maxParameter;

    /**
     * The maximum value in each dimension.
     */
    double[] max;

    /**
     * Parameter dep.
     */
    DoubleListParameter depParameter;

    /**
     * Specifies dependent and independent variables.
     */
    double[] dependentValues;

    /**
     * Parameter number.
     */
    IntParameter numberParameter;

    /**
     * The number of points to be generated.
     */
    int number;

    /**
     * Parameter jitter.
     */
    DoubleParameter jitterParameter;

    /**
     * The maximum percentage of jitter in each dimension.
     */
    double jitter;

    /**
     * Parameter label.
     */
    StringParameter labelParameter;

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
        // data dimension
        dimParameter = new IntParameter(DIM_P, DIM_D, new GreaterConstraint(0));
        optionHandler.put(dimParameter);

        // correlation dimension
        corrDimParameter = new IntParameter(CORRDIM_P, CORRDIM_D, new GreaterConstraint(0));
        optionHandler.put(corrDimParameter);

        // minima
        minParameter = new DoubleListParameter(MIN_P, MIN_D);
        minParameter.setOptional(true);
        // TODO default value
//    minParameter.setDefaultValue(MIN_DEFAULT);
        optionHandler.put(minParameter);

        // maxima
        maxParameter = new DoubleListParameter(MAX_P, MAX_D);
        maxParameter.setOptional(true);
        // TODO default value
//    maxParameter.setDefaultValue(MAX_DEFAULT);
        optionHandler.put(maxParameter);

        // dependent values
        depParameter = new DoubleListParameter(DEPENDENT_VALUES_P, DEPENDENT_VALUES_D);
        depParameter.setOptional(true);
        // TODO default value
        optionHandler.put(depParameter);

        // parameter number
        numberParameter = new IntParameter(NUMBER_P, NUMBER_D, new GreaterConstraint(0));
        optionHandler.put(numberParameter);

        // parameter jitter
        ArrayList<ParameterConstraint<Number>> jitterCons = new ArrayList<ParameterConstraint<Number>>();
        jitterCons.add(new GreaterEqualConstraint(0));
        jitterCons.add(new LessEqualConstraint(1));
        jitterParameter = new DoubleParameter(JITTER_P, JITTER_D, jitterCons);
        jitterParameter.setDefaultValue(JITTER_DEFAULT);
        optionHandler.put(jitterParameter);

        // parameter label
        labelParameter = new StringParameter(LABEL_P, LABEL_D);
        labelParameter.setDefaultValue("");
        optionHandler.put(labelParameter);

        // global constraints
        optionHandler.setGlobalParameterConstraint(new LessEqualGlobalConstraint<Integer>(corrDimParameter, dimParameter));
        optionHandler.setGlobalParameterConstraint(new GlobalListSizeConstraint(minParameter, dimParameter));
        optionHandler.setGlobalParameterConstraint(new GlobalListSizeConstraint(maxParameter, dimParameter));
        optionHandler.setGlobalParameterConstraint(new GlobalListSizeConstraint(depParameter, dimParameter));
        // todo global constraint fuer min < max
    }

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
        AxesParallelCorrelationGenerator wrapper = new AxesParallelCorrelationGenerator();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
        }
        catch (Exception e) {
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
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // dataDim
        dataDim = getParameterValue(dimParameter);

        // corrDim
        corrDim = getParameterValue(corrDimParameter);

        // minima
        min = new double[dataDim];
        if (optionHandler.isSet(MIN_P)) {
            List<Double> min_list = getParameterValue(minParameter);
            for (int i = 0; i < dataDim; i++) {
                min[i] = (min_list.get(i));
            }
        }
        else {
            Arrays.fill(min, MIN_DEFAULT);
        }

        // maxima
        max = new double[dataDim];
        if (optionHandler.isSet(MAX_P)) {
            List<Double> max_list = getParameterValue(maxParameter);
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
                throw new WrongParameterValueException("Parameter " + MIN_P + " > " + MAX_P + "!");
            }
        }

        // dependent values
        if (optionHandler.isSet(depParameter)) {
            List<Double> dep_list = getParameterValue(depParameter);

            double[] dv = new double[dataDim];
            int c = 0;
            for (int d = 0; d < dataDim; d++) {
                try {
                    dv[d] = dep_list.get(d);
                    if (dv[d] == 0) {
                        c++;
                    }
                    else if (dv[d] < min[d] || dv[d] > max[d]) {
                        throw new WrongParameterValueException(DEPENDENT_VALUES_P, dep_list.toString(), DEPENDENT_VALUES_D);
                    }
                }
                catch (NumberFormatException e) {
                    throw new WrongParameterValueException(DEPENDENT_VALUES_P, dep_list.toString(), DEPENDENT_VALUES_D, e);
                }
            }
            if (c != corrDim) {
                throw new WrongParameterValueException("Value of parameter " + DEPENDENT_VALUES_P
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
        number = (Integer) optionHandler.getOptionValue(NUMBER_P);

        // jitter
        jitter = (Double) optionHandler.getOptionValue(JITTER_P);

        // label

        label = (String) optionHandler.getOptionValue(LABEL_P);

        return remainingParameters;
    }

    /**
     * Returns the description for the output parameter. Subclasses may
     * need to overwrite this method.
     *
     * @return the description for the output parameter
     */
    public String getOutputDescription() {
        return "the file to write the generated correlation hyperplane in, "
            + "if the file already exists, the generated points will be appended to this file.";
    }
}
