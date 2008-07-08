package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.varianceanalysis.LinearLocalPCA;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dependency derivator computes quantitativly linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA.
 * <p>Reference:
 * <br>E. Achtert, C. Boehm, H.-P. Kriegel, P. Kroeger, A. Zimek:
 * Deriving Quantitative Dependencies for Correlation Clusters.
 * <br>In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06), Philadelphia, PA 2006.
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public class DependencyDerivator<V extends RealVector<V, ?>, D extends Distance<D>> extends DistanceBasedAlgorithm<V, D> {

    /**
     * OptionID for {@link #OUTPUT_ACCURACY_PARAM}
     */
    public static final OptionID OUTPUT_ACCURACY_ID = OptionID.getOrCreateOptionID(
        "derivator.accuracy",
        "Threshold for output accuracy fraction digits."
    );

    /**
     * Parameter to specify the threshold for output accuracy fraction digits,
     * must be an integer equal to or greater than 0.
     * <p>Default value: {@code 4} </p>
     * <p>Key: {@code -derivator.accuracy} </p>
     */
    private final IntParameter OUTPUT_ACCURACY_PARAM = new IntParameter(
        OUTPUT_ACCURACY_ID,
        new GreaterEqualConstraint(0), 4);

    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#SAMPLE_SIZE_PARAM}
     */
    public static final OptionID SAMPLE_SIZE_ID = OptionID.getOrCreateOptionID(
        "derivator.sampleSize",
        "Threshold for the size of the random sample to use. " +
            "Default value is size of the complete dataset."
    );

    /**
     * Optional parameter to specify the treshold for the size of the random sample to use,
     * must be an integer greater than 0.
     * <p>Default value: the size of the complete dataset </p>
     * <p>Key: {@code -derivator.sampleSize} </p>
     */
    private final IntParameter SAMPLE_SIZE_PARAM = new IntParameter(
        SAMPLE_SIZE_ID, new GreaterConstraint(0), true);

    /**
     * Holds the value of {@link #SAMPLE_SIZE_PARAM}.
     */
    private Integer sampleSize;

    /**
     * Flag to use random sample (use knn query around centroid, if flag is not set).
     * <p>Key: {@code -derivator.randomSample} </p>
     */
    private final Flag RANDOM_SAMPLE_FLAG = new Flag(OptionID.DEPENDENCY_DERIVATOR_RANDOM_SAMPLE);

    /**
     * Holds the object performing the pca.
     */
    private LinearLocalPCA<V> pca;

    /**
     * Holds the solution.
     */
    private CorrelationAnalysisSolution<V> solution;

    /**
     * Number format for output of solution.
     */
    public final NumberFormat NF = NumberFormat.getInstance(Locale.US);

    /**
     * Provides a dependency derivator,
     * adding parameters
     * {@link DependencyDerivator#OUTPUT_ACCURACY_PARAM},
     * {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#SAMPLE_SIZE_PARAM}, and
     * flag {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#RANDOM_SAMPLE_FLAG}
     * to the option handler additionally to parameters of super class.
     */
    public DependencyDerivator() {
        super();

        // parameter output accuracy
        addOption(OUTPUT_ACCURACY_PARAM);

        // parameter sample size
        addOption(SAMPLE_SIZE_PARAM);

        // random sample
        addOption(RANDOM_SAMPLE_FLAG);
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("DependencyDerivator",
            "Deriving numerical inter-dependencies on data",
            "Derives an equality-system describing dependencies between attributes in a correlation-cluster",
            "E. Achtert, C. Boehm, H.-P. Kriegel, P. Kroeger, A. Zimek: " +
            "Deriving Quantitative Dependencies for Correlation Clusters. " +
            "In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06), Philadelphia, PA 2006.");
    }

    /**
     * Runs the pca.
     *
     * @param db the database
     * @see AbstractAlgorithm#runInTime(Database)
     */
    @Override
    public void runInTime(Database<V> db) throws IllegalStateException {
        if (isVerbose()) {
            verbose("retrieving database objects...");
        }
        Set<Integer> dbIDs = new HashSet<Integer>();
        for (Iterator<Integer> idIter = db.iterator(); idIter.hasNext();) {
            dbIDs.add(idIter.next());
        }
        V centroidDV = Util.centroid(db, dbIDs);
        Set<Integer> ids;
        if (this.sampleSize != null) {
            if (isSet(RANDOM_SAMPLE_FLAG)) {
                ids = db.randomSample(this.sampleSize, 1);
            }
            else {
                List<QueryResult<D>> queryResults = db.kNNQueryForObject(centroidDV, this.sampleSize, this.getDistanceFunction());
                ids = new HashSet<Integer>(this.sampleSize);
                for (QueryResult<D> qr : queryResults) {
                    ids.add(qr.getID());
                }
            }
        }
        else {
            ids = dbIDs;
        }

        if (isVerbose()) {
            verbose("PCA...");
        }

        pca.run(ids, db);
        Matrix weakEigenvectors = pca.getEigenvectors().times(pca.selectionMatrixOfWeakEigenvectors());

        Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
        if (this.debug) {
            StringBuilder log = new StringBuilder();
            log.append("strong Eigenvectors:\n");
            log.append(pca.getEigenvectors().times(pca.selectionMatrixOfStrongEigenvectors()).toString(NF));
            log.append('\n');
            log.append("transposed weak Eigenvectors:\n");
            log.append(transposedWeakEigenvectors.toString(NF));
            log.append('\n');
            log.append("Eigenvalues:\n");
            log.append(Util.format(pca.getEigenvalues(), " , ", 2));
            log.append('\n');
            debugFine(log.toString());
        }
        Vector centroid = centroidDV.getColumnVector();
        Matrix B = transposedWeakEigenvectors.times(centroid);
        if (this.debug) {
            StringBuilder log = new StringBuilder();
            log.append("Centroid:\n");
            log.append(centroid);
            log.append('\n');
            log.append("tEV * Centroid\n");
            log.append(B);
            log.append('\n');
            debugFine(log.toString());
        }

        Matrix gaussJordan = new Matrix(transposedWeakEigenvectors.getRowDimensionality(), transposedWeakEigenvectors.getColumnDimensionality() + B.getColumnDimensionality());
        gaussJordan.setMatrix(0, transposedWeakEigenvectors.getRowDimensionality() - 1, 0, transposedWeakEigenvectors.getColumnDimensionality() - 1, transposedWeakEigenvectors);
        gaussJordan.setMatrix(0, gaussJordan.getRowDimensionality() - 1, transposedWeakEigenvectors.getColumnDimensionality(), gaussJordan.getColumnDimensionality() - 1, B);

        if (isVerbose()) {
            verbose("Gauss-Jordan-Elimination of " + gaussJordan.toString(NF));
        }

        double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
        double[][] we = transposedWeakEigenvectors.getArray();
        double[] b = B.getColumn(0).getRowPackedCopy();
        System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());

        LinearEquationSystem lq = new LinearEquationSystem(a, b);
        lq.solveByTotalPivotSearch();

        Matrix strongEigenvectors = pca.getEigenvectors().times(pca.selectionMatrixOfStrongEigenvectors());
        this.solution = new CorrelationAnalysisSolution<V>(lq, db, strongEigenvectors, pca.getWeakEigenvectors(), pca.similarityMatrix(), centroid, NF);

        if (isVerbose()) {
            StringBuilder log = new StringBuilder();
            log.append("Solution:");
            log.append('\n');
            log.append("Standard deviation ").append(this.solution.getStandardDeviation());
            log.append('\n');
            log.append(lq.equationsToString(NF.getMaximumFractionDigits()));
            log.append('\n');
            verbose(log.toString());
        }
    }

    /**
     * @see Algorithm#getResult()
     */
    public CorrelationAnalysisSolution<V> getResult() {
        return solution;
    }

    /**
     * Calls {@link DistanceBasedAlgorithm#setParameters(String[]) DistanceBasedAlgorithm#setParameters(args)}
     * and sets additionally the values of the parameters
     * {@link #OUTPUT_ACCURACY_PARAM} and {@link #SAMPLE_SIZE_PARAM}.
     * The remaining parameters are passed to the {@link #pca}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // accuracy
        int accuracy = getParameterValue(OUTPUT_ACCURACY_PARAM);
        NF.setMaximumFractionDigits(accuracy);
        NF.setMinimumFractionDigits(accuracy);

        // sample size
        if (isSet(SAMPLE_SIZE_PARAM)) {
            sampleSize = getParameterValue(SAMPLE_SIZE_PARAM);
        }

        // pca
        pca = new LinearLocalPCA<V>();
        remainingParameters = pca.setParameters(remainingParameters);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Calls {@link DistanceBasedAlgorithm#getAttributeSettings()}
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #pca}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(pca.getAttributeSettings());
        return attributeSettings;
    }
}