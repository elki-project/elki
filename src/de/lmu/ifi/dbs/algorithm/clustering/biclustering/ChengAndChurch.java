package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.biclustering.Bicluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.IntegerIntegerPair;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.LongParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * todo noemi/arthur comments
 * Provides a BiclusteringAlgorithm which deletes or inserts rows/columns
 * dependent on their score and finds biclusters with correlated values.
 *
 * @author Noemi Andor
 * @param <V> a certain subtype of RealVector - the data matrix is supposed to
 * consist of rows where each row relates to an object of type V and
 * the columns relate to the attribute values of these objects
 */

public class ChengAndChurch<V extends RealVector<V, Double>> extends
    AbstractBiclustering<V> {

    /**
     * Seed for initializing a random list for the masking values.
     * <p/>
     * Default value: 1
     * </p>
     * <p/>
     * Key: {@code -maskingSeed}
     * </p>
     */
    public static final LongParameter SEED_PARAM = new LongParameter(
        "maskingSeed",
        "seed for initializing random list for the masking values");

    /**
     * Parameter to indicate how many times the algorithm to add Nodes should be
     * performed for each iteration. A greater value will result in a more
     * accurate result but will require more time.
     * <p/>
     * Default value: 1
     * </p>
     * <p/>
     * Key: {@code -multipleAddition}
     * </p>
     */
    public static final IntParameter MULTIPLE_ADDITION_PARAM = new IntParameter(
        "multipleAddition",
        "indicates how many times the algorithm to add Nodes should be performed",
        new GreaterEqualConstraint(1));

    /**
     * Treshhold value to determine the maximal acceptable score of a bicluster.
     * <p/>
     * Key: {@code -sigma}
     * </p>
     */
    public static final DoubleParameter SIGMA_PARAM = new DoubleParameter(
        "sigma",
        "treshhold value to determine the maximal acceptable score of a bicluster",
        new GreaterEqualConstraint(0.0));

    /**
     * Parameter for multiple node deletion to accelerate the algorithm.
     * <p/>
     * Key: {@code -alpha}
     * </p>
     */
    public static final DoubleParameter ALPHA_PARAM = new DoubleParameter(
        "alpha",
        "parameter for multiple node deletion to accelerate the algorithm ",
        new GreaterEqualConstraint(1.0));

    /**
     * Number of biclusters to be found.
     * <p/>
     * Default value: 1
     * </p>
     * <p/>
     * Key: {@code -n}
     * </p>
     */
    public static final IntParameter N_PARAM = new IntParameter("n",
        "number of biclusters to be found ", new GreaterEqualConstraint(1));

    /**
     * Lower limit for maskingValues.
     * <p/>
     * Key: {@code -begin}
     * </p>
     */
    public static final IntParameter BEGIN_PARAM = new IntParameter("begin",
        "lower limit for maskingValues");

    /**
     * Upper limit for maskingValues.
     * <p/>
     * Key: {@code -end}
     * </p>
     */
    public static final IntParameter END_PARAM = new IntParameter("end",
        "upper limit for maskingValues");

    /**
     * Missing Value in database to be raplaced with maskingValues.
     * <p/>
     * Key: {@code -missing}
     * </p>
     */
    public static final IntParameter MISSING_PARAM = new IntParameter(
        "missing",
        "missing Value in database to be raplaced with maskingValues");

    /**
     * Sets the options for the ParameterValues maskingSeed, sigma, alpha, n,
     * missing, begin and end.
     */
    static {
        SEED_PARAM.setDefaultValue(1L);
        SEED_PARAM.setOptional(true);
        MULTIPLE_ADDITION_PARAM.setOptional(true);
        MULTIPLE_ADDITION_PARAM.setDefaultValue(1);
        SIGMA_PARAM.setOptional(false);
        ALPHA_PARAM.setOptional(false);
        N_PARAM.setDefaultValue(1);
        MISSING_PARAM.setOptional(true);
        BEGIN_PARAM.setOptional(false);
        END_PARAM.setOptional(false);
    }

    /**
     * Keeps the number of rows in the dataset.
     */
    private int rowDim;

    /**
     * Keeps the number of columns in the dataset.
     */
    private int colDim;

    /**
     * Keeps the position of inverted rows belonging to the current bicluster.
     */
    private BitSet invertedRows = new BitSet();

    /**
     * Keeps the position of the columns belonging to the current bicluster.
     */
    private BitSet cols;

    /**
     * Keeps the position of the rows belonging to the current bicluster.
     */
    private BitSet rows;

    /**
     * Rows with missing values for masking.
     */
    private BitSet missingRowsToMask;

    /**
     * Columns with missing values for masking.
     */
    private BitSet missingColsToMask;

    /**
     * Lower threshold for random maskedValues.
     */
    private int begin;

    /**
     * Upper threshold for random maskedValues.
     */
    private int end;

    /**
     * Keeps the rowMeans of all rows.
     */
    private Map<Integer, Double> rowMeans;

    /**
     * Keeps the columnMeans of all columns.
     */
    private Map<Integer, Double> columnMeans;

    /**
     * A list of all masked values mapped to their row and column.
     */
    private Map<IntegerIntegerPair, Double> maskedVals;

    /**
     * Mean of the current bicluster.
     */
    private double biclusterMean;

    /**
     * Threshold for bicluster score.
     */
    private double sigma;

    /**
     * The current bicluster score.
     */
    private double valueH;

    /**
     * A generator for masking found or missing rows/columns with random
     * numbers.
     */
    private Random r;

    /**
     * Parameter for multiple node deletion.
     */
    private double alpha;

    /**
     * Number of biclusters to be found.
     */
    private int n;

    /**
     * A value marking missing elements in the data matrix.
     */
    private int missing;

    /**
     * Information if some row/column has been removed from the current
     * bicluster.
     */
    private boolean removed;

    /**
     * Information if some row/column has been added to the current bicluster.
     */
    private boolean added;

    /**
     * Specifies that the current state of the iteration is in nodeAddition.
     */
    private boolean inNodeAddition;

    /**
     * Parameter to indicate how many times the algorithm to add Nodes should be
     * performed for each iteration.
     */
    private int multipleAddition;

    /**
     * Adds the parameterValues.
     */
    public ChengAndChurch() {
        this.addOption(SEED_PARAM);
        this.addOption(MULTIPLE_ADDITION_PARAM);
        this.addOption(SIGMA_PARAM);
        this.addOption(ALPHA_PARAM);
        this.addOption(N_PARAM);
        this.addOption(MISSING_PARAM);
        this.addOption(BEGIN_PARAM);
        this.addOption(END_PARAM);
    }

    /**
     * Calls
     * {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and sets additionally the parameters for r, sigma, alpha, n, begin, end
     *
     * @see AbstractAlgorithm#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        long seed = this.getParameterValue(SEED_PARAM);
        r = new Random(seed);
        sigma = this.getParameterValue(SIGMA_PARAM);
        alpha = this.getParameterValue(ALPHA_PARAM);
        n = this.getParameterValue(N_PARAM);
        begin = this.getParameterValue(BEGIN_PARAM);
        end = this.getParameterValue(END_PARAM) - begin;
        if (MISSING_PARAM.isSet()) {
            missing = this.getParameterValue(MISSING_PARAM);
        }
        if (MULTIPLE_ADDITION_PARAM.isSet()) {
            multipleAddition = this.getParameterValue(MULTIPLE_ADDITION_PARAM);
        }
        return remainingParameters;
    }

    /**
     * Calculates the score of the current bicluster, according to the rows and
     * columns set to true.
     *
     * @return score of the bicluster
     */
    public double getValueH() {
        double hValue = 0;
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double value;
                if (inNodeAddition && invertedRows.get(i)) {
                    value = -valueAt(i, j);
                }
                else if (inNodeAddition && !invertedRows.get(i)) {
                    value = valueAt(i, j);
                }
                else {
                    value = maskedValueAt(i, j);
                }
                hValue = hValue + meanSQR(value, i, j);
            }
        }
        hValue = hValue / (rows.cardinality() * cols.cardinality());
        return hValue;

    }

    /**
     * Calculates the score of a single value within the data matrix.
     *
     * @param val the value who`s score will be calculated
     * @param i   the row-key to the corresponding rowMean
     * @param j   the column-key to the corresponding columnMean
     * @return the score of the value in row i and column j
     */
    public double meanSQR(double val, int i, int j) {
        double rowMean = rowMeans.get(i);
        double columnMean = columnMeans.get(j);
        double biclusterM = biclusterMean;

        double residue = Math.pow(val - rowMean - columnMean + biclusterM, 2);
        return residue;
    }

    /**
     * Recalculates the rowMeans.
     */
    protected void initiateRowMeans() {
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            rowMeans.put(i, meanOfRow(i, cols));
        }
        if (inNodeAddition) {
            for (int i = rows.nextClearBit(0); i >= 0; i = rows
                .nextClearBit(i + 1)) {
                if (i >= rowDim) {
                    break;
                }
                rowMeans.put(i, meanOfRow(i, cols));
            }
        }
    }

    /**
     * Recalculates the columnMeans.
     */
    protected void initiateColMeans() {
        for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
            columnMeans.put(j, meanOfCol(rows, j));
        }
        if (inNodeAddition) {
            for (int j = cols.nextClearBit(0); j >= 0; j = cols
                .nextClearBit(j + 1)) {
                if (j >= colDim) {
                    break;
                }
                columnMeans.put(j, meanOfRow(j, cols));
            }
        }
    }

    /**
     * Overrides valueAt in AbstractBiclustering to replace missing values with
     * random numbers.
     */
    @Override
    protected double valueAt(int row, int col) {
        if (missingRowsToMask.get(row) && missingColsToMask.get(col)) {
            return maskedValueAt(row, col);
        }
        return super.valueAt(row, col);
    }

    /**
     * Adjusts the rowMean to the different requests of Algorithm3.
     */
    @Override
    protected double meanOfRow(int row, BitSet cols) {
        double sum = 0;
        if (inNodeAddition && invertedRows.get(row)) {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                sum += -valueAt(row, j);
            }
        }
        else if (inNodeAddition && !invertedRows.get(row)) {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                sum += valueAt(row, j);
            }
        }
        else {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                sum += maskedValueAt(row, j);
            }
        }
        return sum / cols.cardinality();
    }

    /**
     * Adjusts the columnMean to the different requests of the
     * Node-Addition-Algorithm.
     */
    @Override
    protected double meanOfCol(BitSet rows, int col) {
        double sum = 0;
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            if (inNodeAddition && invertedRows.get(i)) {
                sum += -valueAt(i, col);
            }
            else if (inNodeAddition && !invertedRows.get(i)) {
                sum += valueAt(i, col);
            }
            else {
                sum += maskedValueAt(i, col);
            }
        }
        return sum / rows.cardinality();

    }

    /**
     * Calculates the score of each row still belonging to the bicluster as a
     * potential candidate to be removed.
     *
     * @return a list of scores mapped to their row
     */
    protected Map<Integer, Double> ReductionValuesRows() {
        Map<Integer, Double> resultRows = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            temp = 0;
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double value = maskedValueAt(i, j);
                temp = temp + meanSQR(value, i, j);
            }
            resultRows.put(i, temp / cols.cardinality());
        }
        return resultRows;
    }

    /**
     * Calculates the score of each column still belonging to the bicluster as a
     * potential candidate to be removed.
     *
     * @return a list of scores mapped to their column
     */
    protected Map<Integer, Double> ReductionValuesCols() {
        Map<Integer, Double> resultCols = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
            temp = 0;
            for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
                double value = maskedValueAt(i, j);
                temp = temp + meanSQR(value, i, j);
            }
            resultCols.put(j, temp / rows.cardinality());
        }
        return resultCols;
    }

    /**
     * Recalculates the rowMean, the columnMean, the biclusterMean and the score
     * of the current bicluster.
     */
    protected void recomputeValues() {
        initiateRowMeans();
        initiateColMeans();
        biclusterMean = meanOfBicluster(rows, cols);
        valueH = getValueH();
    }

    /**
     * @param row the row of the requested value
     * @param col the column of the requested value
     * @return a random value if the row and the column already belong to a
     *         found bicluster, the value of the row and column in the dataset
     *         otherwise.
     */
    protected double maskedValueAt(int row, int col) {
        IntegerIntegerPair key = new IntegerIntegerPair(row, col);
        if (maskedVals.containsKey(key)) {
            return maskedVals.get(key);
        }
        return super.valueAt(row, col);
    }

    /**
     * Initiates the necessary structures for the following algorithm.
     */
    public void initiateChengAndChurch() {
        maskedVals = new LinkedHashMap<IntegerIntegerPair, Double>();
        this.rowDim = super.getRowDim();
        this.colDim = super.getColDim();
        missingRowsToMask = new BitSet();
        missingColsToMask = new BitSet();
        findMissingValues();
        aplyAlgorithms();
    }

    /**
     * Finds the missing values in the dataset if the missingParameter is
     * set.
     */
    private void findMissingValues() {
        if (!MISSING_PARAM.isSet()) {
            return;
        }
        for (int i = 0; i < rowDim; i++) {
            for (int j = 0; j < colDim; j++) {
                if (super.valueAt(i, j) == missing) {
                    missingRowsToMask.set(i);
                    missingColsToMask.set(j);
                    IntegerIntegerPair key = new IntegerIntegerPair(i, j);
                    maskedVals.put(key, (double) r.nextInt(end) + begin);
                }
            }
        }
    }

    /**
     * Masks all rows and columns previously belonging to some bicluster.
     */
    private void maskMatrix() {
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                IntegerIntegerPair key = new IntegerIntegerPair(i, j);
                maskedVals.put(key, (double) r.nextInt(end) + begin);
            }
        }
    }

    /**
     * Initiates the multipleNodeDeletion, the singleNodeDeletion and the
     * nodeAddition, masks the remaining rows and columns within the resulting
     * bicluster adds the found bicluster to the result.
     */
    public void aplyAlgorithms() {
        for (int i = 0; i < n; i++) {
            multipleNodeDeletion();
            singleNodeDeletion();
            nodeAddition();
            if (isVerbose()) {
                verbose("Score of bicluster" + (i + 1) + ": " + valueH);
                verbose("number of rows: " + rows.cardinality());
                verbose("number of columns: " + cols.cardinality());
                verbose("total number of masked values: " + maskedVals.size()
                        + "\n");
            }
            maskMatrix();
            Bicluster<V> bicluster = defineBicluster(rows, cols);
            addInvertedRows(bicluster, invertedRows);
            addBiclusterToResult(bicluster);
            reset();
        }
    }

    /**
     * Resets the values for the next iteration.
     */
    private void reset() {
        rowMeans.clear();
        columnMeans.clear();
        biclusterMean = 0;
        valueH = 0;
        invertedRows.clear();
        added = true;
        removed = true;
        inNodeAddition = false;
    }

    /**
     * Recalculates the necessary structures for multipleNodeDeletion.
     */
    public void multipleNodeDeletion() {
        rows = new BitSet(rowDim - 1);
        cols = new BitSet(colDim - 1);
        rows.set(0, rowDim);
        cols.set(0, colDim);
        rowMeans = new LinkedHashMap<Integer, Double>();
        columnMeans = new LinkedHashMap<Integer, Double>();
        recomputeValues();
        chooseMaxRAlgorithm2();
    }

    /**
     * Performs multiple node deletion until the score of the resulting
     * bicluster is lower or equal to alpha &lowast; sigma.
     */
    private void chooseMaxRAlgorithm2() {
        if (valueH > sigma) {
            removed = false;
            if (rowDim >= 100) {
                Map<Integer, Double> delRows = ReductionValuesRows();
                for (int i = rows.nextSetBit(0); i >= 0; i = rows
                    .nextSetBit(i + 1)) {
                    if ((Double) delRows.get(i) > alpha * valueH) {
                        rows.clear(i);
                        removed = true;
                    }
                }
                initiateColMeans();
                biclusterMean = meanOfBicluster(rows, cols);
                valueH = getValueH();
            }

            if (colDim >= 100) {
                Map<Integer, Double> delCols = ReductionValuesCols();
                for (int j = cols.nextSetBit(0); j >= 0; j = cols
                    .nextSetBit(j + 1)) {
                    if ((Double) delCols.get(j) > alpha * valueH) {
                        cols.clear(j);
                        removed = true;
                    }
                }
            }
            recomputeValues();

            if (removed) {
                chooseMaxRAlgorithm2();
            }
        }
    }


    /**
     * Performs single node deletion until the score of the resulting bicluster
     * is lower or equal to sigma.
     */
    private void singleNodeDeletion() {
        recomputeValues();
        if (valueH > sigma) {
            int maxDelRow = -1;
            int maxDelColumn = -1;
            Map<Integer, Double> delRows = ReductionValuesRows();
            Map<Integer, Double> delCols = ReductionValuesCols();

            if (delRows.size() != 0) {
                maxDelRow = chooseMax(delRows);
            }
            if (delCols.size() != 0) {
                maxDelColumn = chooseMax(delCols);
            }

            if ((Double) delRows.get(maxDelRow) >= (Double) delCols
                .get(maxDelColumn)) {
                rows.clear(maxDelRow);
            }
            else {
                cols.clear(maxDelColumn);
            }
            singleNodeDeletion();
        }
    }

    /**
     * Determines the key associated with the major doubleValue within the map.
     *
     * @param a a map with scores associated to their row
     * @return the key for the row with maximal score
     */
    public static int chooseMax(Map<Integer, Double> a) {
        if (a != null && a.size() != 0) {
            Set<Integer> set = a.keySet();
            Object[] array = set.toArray();
            Integer max = (Integer) array[0];
            for (int i = 0; i < array.length; i++) {
                if ((Double) a.get(array[i]) > (Double) a.get(max)) {
                    max = (Integer) array[i];
                }
            }
            return max;
        }
        throw new IllegalArgumentException(
            "the HashMap must contain at least one Element");
    }

    /**
     * Initiates node Addition.
     */
    public void nodeAddition() {
        invertedRows.clear();
        chooseMinAdditions();
    }

    /**
     * Determines which row can be added to the bicluster without increasing the
     * score and adds them to the bicluster.
     */
    private void chooseMinAdditions() {
        inNodeAddition = true;
        recomputeValues();
        added = false;

        Map<Integer, Double> colAdds = additionValuesCols();
        if (colAdds.size() != 0) {
            for (int j = cols.nextClearBit(0); j >= 0; j = cols
                .nextClearBit(j + 1)) {
                if (j >= colDim) {
                    break;
                }
                if ((Double) colAdds.get(j) <= valueH) {
                    added = true;
                    cols.set(j);
                }
            }
            initiateRowMeans();
            biclusterMean = meanOfBicluster(rows, cols);
            valueH = getValueH();
        }

        Map<Integer, Double> rowAdds = additionValuesRows();
        if (rowAdds.size() != 0) {
            for (int i = rows.nextClearBit(0); i >= 0; i = rows
                .nextClearBit(i + 1)) {
                if (i >= rowDim) {
                    break;
                }
                if ((Double) rowAdds.get(i) <= valueH) {
                    added = true;
                    rows.set(i);
                }
            }
        }

        Map<Integer, Double> rowAddsInv = ReductionValuesRowsInv();
        if (rowAddsInv.size() != 0) {
            for (int i = rows.nextClearBit(0); i >= 0; i = rows
                .nextClearBit(i + 1)) {
                if (i >= rowDim) {
                    break;
                }
                if ((Double) rowAddsInv.get(i) <= valueH) {
                    added = true;
                    rows.set(i);
                    invertedRows.set(i);
                }
            }
        }
        recomputeValues();
        inNodeAddition = false;
        if (multipleAddition > 1 && added) {
            multipleAddition--;
            chooseMinAdditions();
        }
        else {
            if (MULTIPLE_ADDITION_PARAM.isSet()) {

                multipleAddition = getParameterValue(MULTIPLE_ADDITION_PARAM);

            }

        }
    }

    /**
     * Calculates the scores for the rows not belonging to the current bicluster
     * as potential candidates to be added.
     *
     * @return list of scores mapped to their rows
     */
    protected Map<Integer, Double> additionValuesRows() {
        Map<Integer, Double> resultRows = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int i = rows.nextClearBit(0); i >= 0; i = rows.nextClearBit(i + 1)) {
            if (i >= rowDim) {
                break;
            }
            temp = 0;
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double value = valueAt(i, j);
                temp = temp + meanSQR(value, i, j);
            }
            resultRows.put(i, temp / cols.cardinality());
        }
        return resultRows;
    }

    /**
     * Calculates the scores for the columns not belonging to the current
     * bicluster as potential candidates to be added.
     *
     * @return list of scores mapped to their columns
     */
    protected Map<Integer, Double> additionValuesCols() {
        Map<Integer, Double> resultCols = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int j = cols.nextClearBit(0); j >= 0; j = cols.nextClearBit(j + 1)) {
            if (j >= colDim) {
                break;
            }
            temp = 0;
            for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
                double value = valueAt(i, j);
                temp = temp + meanSQR(value, i, j);
            }
            resultCols.put(j, temp / rows.cardinality());
        }
        return resultCols;
    }

    /**
     * Calculates the scores for the inverted rows not belonging to the current
     * bicluster as potential candidates to be added.
     *
     * @return list of scores mapped to their rows
     */
    protected Map<Integer, Double> ReductionValuesRowsInv() {
        Map<Integer, Double> resultRows = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int i = rows.nextClearBit(0); i >= 0; i = rows.nextClearBit(i + 1)) {
            if (i >= rowDim) {
                break;
            }
            temp = 0;
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double value = -valueAt(i, j);
                double rowMean = -rowMeans.get(i);
                double columnMean = columnMeans.get(j);
                double biclusterM = biclusterMean;

                temp = temp
                       + Math.pow(value - rowMean - columnMean + biclusterM, 2);
            }
            resultRows.put(i, temp / cols.cardinality());
        }
        return resultRows;
    }

    /**
     * The description of this Algorithm.
     */
    public Description getDescription() {
        Description abs = new Description(
            "ChengAndChurch",
            "a biclustering method on row- and columnScoreBases",
            "finding correlated values in a subset of rows and a subset of columns",
            "Yizong Cheng and George M. Church "
            + "Department of Genetics, Harvard Medical School, Boston, MA 02115 "
            + "Department of ECECS, University of Cincinnati, Cinncinati, OH 45221"
            + "yizong.cheng@uc.edu, church@salt2.med.harvard.edu");
        return abs;
    }

    /**
     * Initiates this Algorithm.
     */
    @Override
    public void biclustering() {
        long t = System.currentTimeMillis();
        initiateChengAndChurch();
        System.out.println(System.currentTimeMillis() - t);
    }

}
