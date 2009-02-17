package de.lmu.ifi.dbs.elki.varianceanalysis.ica;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualStringConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Implementation of the FastICA algorithm.
 *
 * @author Elke Achtert
 */
public class FastICA<V extends RealVector<V, ?>> extends AbstractParameterizable {
    /**
     * The number format for debugging purposes.
     */
    private static NumberFormat NF = NumberFormat.getInstance(Locale.US);

    static {
        NF.setMinimumFractionDigits(4);
        NF.setMaximumFractionDigits(4);
    }

    /**
     * The approach.
     */
    public enum Approach {
        SYMMETRIC, DEFLATION
    }

    /**
     * OptionID for {@link #IC_PARAM}
     */
    public static final OptionID IC_ID = OptionID.getOrCreateOptionID("fastica.ic",
        "the maximum number of independent components (ics) to be found. "
        + "The number of ics to be found must be less to or equal than "
        + "the dimensionality of the feature space.");

    /**
     * Parameter for the number of independent components.
     */
    private final IntParameter IC_PARAM = new IntParameter(IC_ID, new GreaterConstraint(0), true);
    
    /**
     * OptionID for {@link #UNIT_PARAM}
     */
    public static final OptionID UNIT_ID = OptionID.getOrCreateOptionID("fastica.unit",
        "Flag that indicates that the unit matrix "
        + "is used as initial weight matrix. If this flag "
        + "is not set, the initial weight matrix will be "
        + "generated randomly.");

    /**
     * Flag for initial unit matrix
     */
    private final Flag UNIT_FLAG = new Flag(UNIT_ID);
    
    /**
     * The default value for parameter maxIter.
     */
    public static final int DEFAULT_MAX_ITERATIONS = 1000;

    /**
     * OptionID for {@link #MAX_ITERATIONS_PARAM}
     */
    public static final OptionID MAX_ITERATIONS_ID = OptionID.getOrCreateOptionID("fastica.maxIter",
        "the number of maximum iterations. " + "Default: " + DEFAULT_MAX_ITERATIONS);

    /**
     * Parameter for the number of independent components.
     */
    private final IntParameter MAX_ITERATIONS_PARAM = new IntParameter(MAX_ITERATIONS_ID,
        new GreaterConstraint(0), DEFAULT_MAX_ITERATIONS);
    
    /**
     * The default approach.
     */
    public static final Approach DEFAULT_APPROACH = Approach.DEFLATION;

    /**
     * OptionID for {@link #APPROACH_PARAM}
     */
    public static final OptionID APPROACH_ID = OptionID.getOrCreateOptionID("fastica.app",
        "the approach to be used, available approaches are: [" + Approach.DEFLATION + "| "
        + Approach.SYMMETRIC + "]" + ". Default: " + DEFAULT_APPROACH + ")");

    /**
     * Parameter for approach
     */
    private final PatternParameter APPROACH_PARAM = new PatternParameter(APPROACH_ID,
        new EqualStringConstraint(new String[]{
            Approach.DEFLATION.toString(),
            Approach.SYMMETRIC.toString()}),
            DEFAULT_APPROACH.getClass().getName());

    /**
     * The default g.
     */
    public static final String DEFAULT_G = KurtosisBasedContrastFunction.class.getName();

    /**
     * OptionID for {@link #G_PARAM}
     */
    public static final OptionID G_ID = OptionID.getOrCreateOptionID("fastica.g",
        "the contrast function to be used to estimate negentropy "
        + Properties.ELKI_PROPERTIES.restrictionString(ContrastFunction.class) + ". Default: " + DEFAULT_G);

    /**
     * Parameter for contrast function g
     */
    private final ClassParameter<ContrastFunction> G_PARAM = new ClassParameter<ContrastFunction>(G_ID,
        ContrastFunction.class, DEFAULT_G);
    
    /**
     * The default epsilon.
     */
    public static final double DEFAULT_EPSILON = 0.001;

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("fastica.epsilon",
        "a positive value defining the criterion for convergence of weight vector w_p: "
        + "if the difference of the values of w_p after two iterations " + "is less than or equal to epsilon. " + "Default: "
        + DEFAULT_EPSILON);

    /**
     * Parameter for epsilon
     */
    private final DoubleParameter EPSILON_PARAM = new DoubleParameter(EPSILON_ID,
        new GreaterConstraint(0), DEFAULT_EPSILON);
    
//    /**
//     * OptionID for {@link #ALPHA_PARAM}
//     */
//    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("fastica.alpha",
//        "a double between 0 and 1 specifying "
//        + "the threshold for strong eigenvectors of the pca "
//        + "performed as a preprocessing step: "
//        + "the strong eigenvectors explain a "
//        + "portion of at least alpha of the total variance "
//        + "Default: " + PercentageEigenPairFilter.DEFAULT_ALPHA + ")");
//
//    /**
//     * Parameter for epsilon
//     */
//    private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID,
//        new IntervalConstraint(0,IntervalBoundary.CLOSE,1,IntervalBoundary.OPEN), PercentageEigenPairFilter.DEFAULT_ALPHA);
    
    /**
     * The input data.
     */
    private Matrix inputData;

    /**
     * The centered pca data.
     */
    private Matrix centeredData;

    /**
     * The whitened centered data.
     */
    private Matrix whitenedData;

    /**
     * The centroid of the input data.
     */
    private Vector centroid;

    /**
     * The whitening matrix.
     */
    private Matrix whiteningMatrix;

    /**
     * The dewhitening matrix.
     */
    private Matrix dewhiteningMatrix;

    /**
     * The mixing matrix of the whitened data.
     */
    private Matrix mixingMatrix;

    /**
     * The separating matrix of the whitened data.
     */
    private Matrix separatingMatrix;

    /**
     * The number of independent components to be found.
     */
    private Integer numICs;

    /**
     * The (current) weight matrix.
     */
    private Matrix weightMatrix;

    /**
     * The independent components
     */
    private Matrix ics;

    /**
     * The maximum number of iterations to be performed.
     */
    private int maximumIterations;

    /**
     * The approach to be used.
     */
    private Approach approach;

    /**
     * The contrast function;
     */
    private ContrastFunction contrastFunction;

    /**
     * The convergence criterion.
     */
    private double epsilon;

    /**
     * True, if the initial weight matrix is a unit matrix, false if the initial
     * weight matrix is generated randomly.
     */
    private boolean initialUnitWeightMatrix;

//    /**
//     * The alpha parameter for the preprocessing pca.
//     * todo: necessary?
//     */
//    private double alpha;

    /**
     * Provides the fast ica algorithm.
     */
    public FastICA() {
        super();
        addOption(UNIT_FLAG);

        // parameter ics
        addOption(IC_PARAM);

        // parameter max iteration
        addOption(MAX_ITERATIONS_PARAM);

        // parameter approach
        addOption(APPROACH_PARAM);

        // parameter contrast function
        addOption(G_PARAM);

        // parameter epsilon
        addOption(EPSILON_PARAM);

//        // parameter alpha
//        addOption(ALPHA_PARAM);

        this.debug = true;
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // initial mixing matrix
        initialUnitWeightMatrix = UNIT_FLAG.isSet();

        // ics
        if (IC_PARAM.isSet()) {
            numICs =IC_PARAM.getValue();
        }

        // maximum iterations
        maximumIterations = MAX_ITERATIONS_PARAM.getValue();

        // approach

        String approachString = APPROACH_PARAM.getValue();
        if (approachString.equals(Approach.DEFLATION.toString())) {
            approach = Approach.DEFLATION;
        }
        else if (approachString.equals(Approach.SYMMETRIC.toString())) {
            approach = Approach.SYMMETRIC;
        }
        else
            throw new WrongParameterValueException(APPROACH_PARAM, approachString, null);

        // contrast function
        contrastFunction = G_PARAM.instantiateClass();
        remainingParameters = contrastFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        // epsilon
        epsilon = EPSILON_PARAM.getValue();

//        // alpha
//        alpha = ALPHA_PARAM.getValue();

        return remainingParameters;
    }

    /**
     * Runs the fast ica algorithm on the specified database.
     *
     * @param database the database containing the data vectors
     * @param verbose  flag that allows verbode messages
     */
    public void run(Database<V> database, boolean verbose) {
        if (verbose) {
            verbose("database size: " + database.size() + " x " + database.dimensionality());
            verbose("preprocessing and data whitening...");
        }
        preprocessAndWhitenData(database);

        // set number of independent components to be found
        int dim = whitenedData.getRowDimensionality();
        if (numICs == null || numICs > dim) {
            numICs = dim;
        }
        if (verbose) {
            verbose("\n numICs = " + numICs);
        }

        // initialize the weight matrix
        weightMatrix = new Matrix(dim, numICs);

        // determine the weights
        if (approach.equals(Approach.SYMMETRIC)) {
            symmetricOrthogonalization(verbose);
        }
        else if (approach.equals(Approach.DEFLATION)) {
            deflationaryOrthogonalization(verbose);
        }

        // compute mixing and separating matrix
        mixingMatrix = dewhiteningMatrix.times(weightMatrix);
        separatingMatrix = weightMatrix.transpose().times(whiteningMatrix);

        // compute ics
        ics = separatingMatrix.times(inputData);

        if (debug) {
            // StringBuffer msg = new StringBuffer();
            // msg.append("\nweight " + weightMatrix);
            // msg.append("\nmix " + mixingMatrix.toString(NF));
            // msg.append("\nsep " + separatingMatrix);
            // msg.append("\nics " + ics.transpose());
            // debugFine(msg.toString());
            generate(weightMatrix, Util.centroid(whitenedData).getColumnPackedCopy(), "w");
            Matrix ic = ics.transpose();
            for (int i = 0; i < ic.getRowDimensionality(); i++) {
                // output(ic.getRow(i), "ic"+i);

            }
            output(ics.transpose(), "ics");
            generate(mixingMatrix, Util.centroid(inputData).getColumnPackedCopy(), "a");
        }
    }

    /**
     * Returns the resulting independent components.
     *
     * @return the resulting independent components
     */
    public Matrix getICVectors() {
        return ics;
    }

    /**
     * Returns the assumed mixing matrix.
     *
     * @return the assumed mixing matrix
     */
    public Matrix getMixingMatrix() {
        return mixingMatrix;
    }

    /**
     * Returns the assumed seperating matrix.
     *
     * @return the assumed seperating matrix
     */
    public Matrix getSeparatingMatrix() {
        return separatingMatrix;
    }

    /**
     * Returns the weight matrix.
     *
     * @return the weight matrix
     */
    public Matrix getWeightMatrix() {
        return weightMatrix;
    }

    /**
     * Returns the matrix of the original input data.
     *
     * @return the matrix of the original input data
     */
    public Matrix getInputData() {
        return inputData;
    }

    /**
     * Returns the centered pca data.
     *
     * @return the centered pca data
     */
    public Matrix getCenteredData() {
        return centeredData;
    }

    /**
     * Returns the whitened data.
     *
     * @return the whitened data
     */
    public Matrix getWhitenedData() {
        return whitenedData;
    }

    /**
     * Performs the FastICA algorithm using deflationary orthogonalization.
     *
     * @param verbose flag indicating verbose messages
     */
    private void deflationaryOrthogonalization(boolean verbose) {
        Progress progress = new Progress("Deflationary Orthogonalization ", numICs);

        for (int p = 0; p < numICs; p++) {
            if (verbose) {
                progress.setProcessed(p);
                progress(progress);
            }

            int iterations = 0;
            boolean converged = false;

            // init w_p
            int dimensionality = whitenedData.getRowDimensionality();
            Vector w_p = initialUnitWeightMatrix ? Vector.unitVector(dimensionality, p) : Vector.randomNormalizedVector(dimensionality);

            while ((iterations < maximumIterations) && (!converged)) {
                // determine w_p
                Vector w_p_old = w_p.copy();
                w_p = updateWeight(w_p);

                // orthogonalize w_p
                Vector sum = new Vector(dimensionality);
                for (int j = 0; j < p; j++) {
                    Vector w_j = weightMatrix.getColumnVector(j);
                    sum = sum.plus(w_j.times(w_p.scalarProduct(w_j)));
                }
                w_p = w_p.minus(sum);
                w_p.normalize();

                // test if good approximation
                converged = isVectorConverged(w_p_old, w_p);
                iterations++;

                if (verbose) {
                    progress(new ProgressLogRecord(
                        Util.status(progress) + " - " + iterations,
                        progress.getTask(),
                        progress.status()));
                }

                if (debug) {
                    debugFine("\nw_" + p + " " + w_p + "\n");
                    generate(w_p, Util.centroid(whitenedData).getColumnPackedCopy(), "w_" + p + iterations);
                }
            }

            // write new vector to the matrix
            weightMatrix.setColumn(p, w_p);
        }
    }

    /**
     * Performs the FastICA algorithm using symmetric orthogonalization.
     *
     * @param verbose flag indicating verbose messages
     */
    private void symmetricOrthogonalization(boolean verbose) {
        Progress progress = new Progress("Symmetric Orthogonalization ", numICs);

        // choose initial values for w_p
        int dimensionality = whitenedData.getRowDimensionality();
        for (int p = 0; p < numICs; ++p) {
            Vector w_p = initialUnitWeightMatrix ? Vector.unitVector(dimensionality, p) : Vector.randomNormalizedVector(dimensionality);

            weightMatrix.setColumn(p, w_p);
        }
        // ortogonalize weight matrix
        weightMatrix = symmetricOrthogonalizationOfWeightMatrix();
        generate(weightMatrix, Util.centroid(whitenedData).getColumnPackedCopy(), "w_0");

        int iterations = 0;
        boolean converged = false;
        while ((iterations < maximumIterations) && (!converged)) {
            Matrix w_old = weightMatrix.copy();
            for (int p = 0; p < numICs; p++) {
                Vector w_p = updateWeight(weightMatrix.getColumnVector(p));
                weightMatrix.setColumn(p, w_p);
            }
            // orthogonalize
            weightMatrix = symmetricOrthogonalizationOfWeightMatrix();

            // test if good approximation
            converged = isMatrixConverged(w_old, weightMatrix);
            iterations++;

            if (verbose) {
                progress(new ProgressLogRecord(
                    Util.status(progress) + " - " + iterations,
                    progress.getTask(), progress.status()));
            }

            if (debug) {
                generate(weightMatrix, Util.centroid(whitenedData).getColumnPackedCopy(), "w_" + (iterations + 1));
            }
        }
    }

    /**
     * Returns the weight matrix after symmetric orthogonalzation.
     *
     * @return the weight matrix after symmetric orthogonalzation
     */
    private Matrix symmetricOrthogonalizationOfWeightMatrix() {
        Matrix W = weightMatrix.transpose();
        EigenvalueDecomposition decomp = new EigenvalueDecomposition(W.times(W.transpose()));
        Matrix E = decomp.getV();
        Matrix D = decomp.getD();
        for (int i = 0; i < D.getRowDimensionality(); i++) {
            D.set(i, i, 1.0 / Math.sqrt(D.get(i, i)));
        }

        W = E.times(D).times(E.transpose()).times(W);
        return W.transpose();
    }

    /**
     * Updates the weight vector w_p according to the FastICA algorithm.
     *
     * @param w_p the weight vector to be updated
     * @return the new value of w_p
     */
    private Vector updateWeight(Vector w_p) {
        int n = whitenedData.getColumnDimensionality();
        int d = whitenedData.getRowDimensionality();

        // E(z*(g(w_p*z))
        Vector E_zg = new Vector(d);
        // E(g'(w_p*z))
        double E_gd = 0.0;

        for (int i = 0; i < n; i++) {
            Vector z = whitenedData.getColumnVector(i);
            // w_p * z
            double wz = w_p.scalarProduct(z);
            // g(w_p * z)
            double g = contrastFunction.function(wz);
            // g'(w_p * z)
            double gd = contrastFunction.derivative(wz);

            E_zg = E_zg.plus(z.times(g));
            E_gd += gd;
        }

        // E(z*(g(w_p*z))
        E_zg = E_zg.times(1.0 / n);
        // E(g'(w_p*z)) * w_p
        E_gd /= n;
        Vector E_gdw = w_p.times(E_gd);

        // w_p = E_zg - E_gd * w_p
        w_p = E_zg.minus(E_gdw);
        return w_p;
    }

    /**
     * Performs in a preprocessing step a pca on the data and afterwards the
     * data whitening.
     *
     * @param database the database storing the vector objects
     */
    private void preprocessAndWhitenData(Database<V> database) {
        // center data
        inputData = inputMatrix(database);
        centroid = Util.centroid(inputData);
        centeredData = new Matrix(inputData.getRowDimensionality(), inputData.getColumnDimensionality());
        for (int i = 0; i < inputData.getColumnDimensionality(); i++) {
            centeredData.setColumn(i, inputData.getColumnVector(i).minus(centroid));
        }

        // whiten data
        Matrix cov = Util.covarianceMatrix(centeredData);
        EigenvalueDecomposition evd = cov.eig();
        // eigen vectors
        Matrix E = evd.getV();
        // eigenvalues ^-0.5
        Matrix D_inv_sqrt = evd.getD().copy();
        for (int i = 0; i < D_inv_sqrt.getRowDimensionality(); i++) {
            D_inv_sqrt.set(i, i, 1.0 / Math.sqrt(D_inv_sqrt.get(i, i)));
        }
        // eigenvalue ^1/2
        Matrix D_sqrt = evd.getD().copy();
        for (int i = 0; i < D_sqrt.getRowDimensionality(); i++) {
            D_sqrt.set(i, i, Math.sqrt(D_sqrt.get(i, i)));
        }

        whiteningMatrix = D_inv_sqrt.times(E.transpose());
        dewhiteningMatrix = E.times(D_sqrt);

        // whiteningMatrix = E.times(D_inv_sqrt.times(E.transpose()));
        // dewhiteningMatrix = E.times(D_sqrt).times(E.transpose());

        whitenedData = whiteningMatrix.times(centeredData);

        if (debug) {
            StringBuffer msg = new StringBuffer();
            msg.append("\ninput data X: ").append(inputData.dimensionInfo());
            msg.append("\n").append(inputData);
            msg.append("\ncentered data: ").append(centeredData.dimensionInfo());
            msg.append("\n").append(centeredData);
            msg.append("\nWHITENING MATRIX: ").append(whiteningMatrix.dimensionInfo());
            msg.append("\n").append(whiteningMatrix.toString(NF));
            msg.append("\nDEWHITENING MATRIX: ").append(dewhiteningMatrix.dimensionInfo());
            msg.append("\n").append(dewhiteningMatrix.toString(NF));
            debugFine(msg.toString());
            output(inputData.transpose(), "x0");
            output(centeredData.transpose(), "x1");
            output(whitenedData.transpose(), "x2");
        }
    }

    /**
     * Determines the input matrix from the specified database, the objects are
     * columnvectors.
     *
     * @param database the database containing the objects
     * @return a matrix consisting of the objects of the specified database as
     *         column vectors
     */
    private Matrix inputMatrix(Database<V> database) {
        int dim = database.dimensionality();
        double[][] input = new double[database.size()][dim];

        int i = 0;
        for (Iterator<Integer> it = database.iterator(); it.hasNext(); i++) {
            V o = database.get(it.next());
            for (int d = 1; d <= dim; d++) {
                input[i][d - 1] = o.getValue(d).doubleValue();
            }
        }

        return new Matrix(input).transpose();
    }

    /**
     * Returns true, if the convergence criterion for weighting vector wp is
     * reached.
     *
     * @param wp_old the old value of wp
     * @param wp_new the new value of wp
     * @return true, if the scalar product between wp_old and wp_new is less
     *         than or equal to 1-epsilon
     */
    private boolean isVectorConverged(Vector wp_old, Vector wp_new) {
        double scalar = Math.abs(wp_old.scalarProduct(wp_new));
        return scalar >= (1 - epsilon) && scalar <= (1 + epsilon);
    }

    /**
     * Returns true, if the convergence criterion for weighting matrix w is
     * reached.
     *
     * @param w_old the old value of w
     * @param w_new the new value of w
     * @return true, if the convergence criterion for each column vector is
     *         reached.
     */
    private boolean isMatrixConverged(Matrix w_old, Matrix w_new) {
        for (int p = 0; p < w_old.getColumnDimensionality(); p++) {
            Vector wp_old = w_old.getColumnVector(p);
            Vector wp_new = w_new.getColumnVector(p);
            if (!isVectorConverged(wp_old, wp_new)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Output method for a matrix for debuging purposes.
     *
     * @param m    the matrix to be written
     * @param name the file name
     */
    private void output(Matrix m, String name) {
        try {
            PrintStream printStream = new PrintStream(new FileOutputStream(name));
            printStream.println("# " + m.dimensionInfo());
            for (int i = 0; i < m.getRowDimensionality(); i++) {
                for (int j = 0; j < m.getColumnDimensionality(); j++) {
                    printStream.print(m.get(i, j));
                    printStream.print(" ");
                }
                printStream.println("");
            }
            printStream.flush();
            printStream.close();
        }
        catch (FileNotFoundException e) {
            // todo exception handling
            e.printStackTrace();
        }
    }

    /**
     * Generates feature vectors belonging to a specified hyperplane for
     * debugging purposes and writes them to the specified file.
     *
     * @param m    the basis of the hyperplane
     * @param p    the model point of the hyperplane
     * @param name the file name
     */
    private void generate(Matrix m, double[] p, String name) {
        double[] min = new double[p.length];
        double[] max = new double[p.length];

        Arrays.fill(min, -100);
        Arrays.fill(max, 100);

        File file = new File(name);
        if (file.exists()) {
            file.delete();
        }
        for (int i = 0; i < m.getColumnDimensionality(); i++) {
            ICADataGenerator.runGenerator(100, p, new double[][]{m.getColumnVector(i).getColumnPackedCopy()}, name + i, min, max, 0,
                name);
        }
    }
}
