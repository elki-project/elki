package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dependency derivator computes quantitativly linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA.
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * todo parameter
 */
public class DependencyDerivator<V extends RealVector<V, ?>, D extends Distance<D>> extends DistanceBasedAlgorithm<V, D> {

    /**
     * Parameter for output accuracy (number of fraction digits).
     */
    public static final String OUTPUT_ACCURACY_P = "accuracy";

    /**
     * Default value for output accuracy (number of fraction digits).
     */
    public static final int OUTPUT_ACCURACY_DEFAULT = 4;

    /**
     * Description for parameter output accuracy (number of fraction digits).
     */
    public static final String OUTPUT_ACCURACY_D = "output accuracy fraction digits (>0) (default: " + OUTPUT_ACCURACY_DEFAULT + ").";

    /**
     * Parameter for size of random sample.
     */
    public static final String SAMPLE_SIZE_P = "sampleSize";

    /**
     * Description for parameter for size of random sample.
     */
    public static final String SAMPLE_SIZE_D = "size (> 0) of random sample to use (default: use of complete dataset).";

    /**
     * Flag for use of random sample.
     */
    public static final String RANDOM_SAMPLE_F = "randomSample";

    /**
     * Description for flag for use of random sample.
     */
    public static final String RANDOM_SAMPLE_D = "flag to use random sample (use knn query around centroid, if flag is not set). Flag is ignored if no sample size is specified.";

    /**
     * Holds size of sample.
     */
    private Integer sampleSize;

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
     * Provides a dependency derivator, setting parameters alpha and output
     * accuracy additionally to parameters of super class.
     */
    public DependencyDerivator() {
        super();

        // parameter output accuracy
        IntParameter outputACC = new IntParameter(OUTPUT_ACCURACY_P, OUTPUT_ACCURACY_D, new GreaterEqualConstraint(0));
        outputACC.setDefaultValue(OUTPUT_ACCURACY_DEFAULT);
        addOption(outputACC);

        // parameter sample size
        IntParameter sampleSize = new IntParameter(SAMPLE_SIZE_P, SAMPLE_SIZE_D, new GreaterEqualConstraint(0));
        sampleSize.setOptional(true);
        addOption(sampleSize);

        addOption(new Flag(RANDOM_SAMPLE_F, RANDOM_SAMPLE_D));
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("DependencyDerivator", "Deriving numerical inter-dependencies on data", "Derives an equality-system describing dependencies between attributes in a correlation-cluster", "E. Achtert, C. Boehm, H.-P. Kriegel, P. Kroeger, A. Zimek: " + "Deriving Quantitative Dependencies for Correlation Clusters. " + "In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06), Philadelphia, PA 2006.");
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
            if (optionHandler.isSet(RANDOM_SAMPLE_F)) {
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
            log.append("Standard deviation " + this.solution.getStandardDeviation());
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
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // accuracy
        int accuracy = (Integer) optionHandler.getOptionValue(OUTPUT_ACCURACY_P);

        NF.setMaximumFractionDigits(accuracy);
        NF.setMinimumFractionDigits(accuracy);

        // sample size
        if (optionHandler.isSet(SAMPLE_SIZE_P)) {
            sampleSize = (Integer) optionHandler.getOptionValue(SAMPLE_SIZE_P);
        }

        // pca
        pca = new LinearLocalPCA<V>();
        remainingParameters = pca.setParameters(remainingParameters);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns the parameter setting of this algorithm.
     *
     * @return the parameter setting of this algorithm
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        AttributeSettings mySettings = attributeSettings.get(0);

        if (optionHandler.isSet(SAMPLE_SIZE_P)) {
            mySettings.addSetting(SAMPLE_SIZE_P, Integer.toString(sampleSize));
        }
        attributeSettings.addAll(pca.getAttributeSettings());
        return attributeSettings;
    }
}