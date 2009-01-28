package experimentalcode.noemi;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering.AbstractBiclustering;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.model.Bicluster;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Provides a biclustering algorithm which deletes or inserts rows/columns
 * dependent on their score and finds biclusters with correlated values.
 *
 * @author Noemi Andor
 * @param <V> a certain subtype of RealVector - the data matrix is supposed to
 * consist of rows where each row relates to an object of type V and
 * the columns relate to the attribute values of these objects
 */

public class ChengAndChurch<V extends RealVector<V, Double>> extends AbstractBiclustering<V> {

    /**
     * OptionID for the parameter {@link #SEED_PARAM}.
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID(
        "chengandchurch.random",
        "seed for initializing random list for the masking values");

    /**
     * Parameter to specifiy the seed for initializing a random list for the masking values.
     * <p>Default value: 1</p>
     * <p>Key: {@code -chengandchurch.random}</p>
     */
    private final LongParameter SEED_PARAM = new LongParameter(SEED_ID, true, 1L);

    /**
     * OptionID for the parameter {@link #MULTIPLE_ADDITION_PARAM}.
     */
    public static final OptionID MULTIPLE_ADDITION_ID = OptionID
        .getOrCreateOptionID("chengandchurch.multipleAddition",
            "indicates how many times the algorithm to add Nodes should be performed");

    /**
     * Parameter to indicate how many times the algorithm to add Nodes should be
     * performed for each iteration. A greater value will result in a more
     * accurate result but will require more time.
     * <p/>
     * Default value: 1
     * </p>
     * <p/>
     * Key: {@code -chengandchurch.multipleAddition}
     * </p>
     */
    public final IntParameter MULTIPLE_ADDITION_PARAM = new IntParameter(
        MULTIPLE_ADDITION_ID, new GreaterEqualConstraint(1));

    /**
     * OptionID for the parameter {@link #SIGMA_PARAM}.
     */
    public static final OptionID SIGMA_ID = OptionID
        .getOrCreateOptionID("chengandchurch.sigma",
            "treshhold value to determine the maximal acceptable score of a bicluster");

    /**
     * Treshhold value to determine the maximal acceptable score of a bicluster.
     * <p/>
     * Key: {@code -chengandchurch.sigma}
     * </p>
     */
    public final DoubleParameter SIGMA_PARAM = new DoubleParameter(SIGMA_ID,
        new GreaterEqualConstraint(0.0));

    /**
     * OptionID for the parameter {@link #ALPHA_PARAM}.
     */
    public static final OptionID ALPHA_ID = OptionID
        .getOrCreateOptionID("chengandchurch.alpha",
            "parameter for multiple node deletion to accelerate the algorithm ");

    /**
     * Parameter for multiple node deletion to accelerate the algorithm.
     * <p/>
     * Key: {@code -chengandchurch.alpha}
     * </p>
     */
    public final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID,
        new GreaterEqualConstraint(1.0));

    /**
     * OptionID for the parameter {@link #N_PARAM}.
     */
    public static final OptionID N_ID = OptionID.getOrCreateOptionID(
        "chengandchurch.n", "number of biclusters to be found ");

    /**
     * Number of biclusters to be found.
     * <p/>
     * Default value: 1
     * </p>
     * <p/>
     * Key: {@code -chengandchurch.n}
     * </p>
     */
    public final IntParameter N_PARAM = new IntParameter(N_ID,
        new GreaterEqualConstraint(1));

    /**
     * OptionID for the parameter {@link #BEGIN_PARAM}.
     */
    public static final OptionID BEGIN_ID = OptionID.getOrCreateOptionID(
        "chengandchurch.begin", "lower limit for maskingValues");

    /**
     * Lower limit for maskingValues.
     * <p/>
     * Key: {@code -chengandchurch.begin}
     * </p>
     */
    public final IntParameter BEGIN_PARAM = new IntParameter(BEGIN_ID);

    /**
     * OptionID for the parameter {@link #END_PARAM}.
     */
    public static final OptionID END_ID = OptionID.getOrCreateOptionID(
        "chengandchurch.end", "upper limit for maskingValues");

    /**
     * Upper limit for maskingValues.
     * <p/>
     * Key: {@code -chengandchurch.end}
     * </p>
     */
    public final IntParameter END_PARAM = new IntParameter(END_ID);

    /**
     * OptionID for the parameter {@link #MISSING_PARAM}.
     */
    public static final OptionID MISSING_ID = OptionID.getOrCreateOptionID(
        "chengandchurch.missing",
        "missing Value in database to be raplaced with maskingValues");

    /**
     * Missing Value in database to be replaced with maskingValues.
     * <p/>
     * Key: {@code -chengandchurch.missing}
     * </p>
     */
    public final IntParameter MISSING_PARAM = new IntParameter(MISSING_ID);

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
    private Map<IntIntPair, Double> maskedVals;

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
    private Random random;

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
     * Sets the options for the ParameterValues random, sigma, alpha, n,
     * missing, begin and end. Adds the parameterValues.
     */
    public ChengAndChurch() {
        MULTIPLE_ADDITION_PARAM.setOptional(true);
        MULTIPLE_ADDITION_PARAM.setDefaultValue(1);
        SIGMA_PARAM.setOptional(false);
        ALPHA_PARAM.setOptional(false);
        N_PARAM.setDefaultValue(1);
        MISSING_PARAM.setOptional(true);
        BEGIN_PARAM.setOptional(false);
        END_PARAM.setOptional(false);

        this.addOption(SEED_PARAM);
        this.addOption(MULTIPLE_ADDITION_PARAM);
        this.addOption(SIGMA_PARAM);
        this.addOption(N_PARAM);
        this.addOption(MISSING_PARAM);
        this.addOption(BEGIN_PARAM);
        this.addOption(END_PARAM);
    }

    /**
     * Calls the super method
     * and sets additionally the parameters for random, sigma, alpha, n, begin,
     * end.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        long seed = SEED_PARAM.getValue();
        random = new Random(seed);
        sigma = SIGMA_PARAM.getValue();
        alpha = ALPHA_PARAM.getValue();
        n = N_PARAM.getValue();
        begin = BEGIN_PARAM.getValue();
        end = END_PARAM.getValue() - begin;
        if (MISSING_PARAM.isSet()) {
            missing = MISSING_PARAM.getValue();
        }
        if (MULTIPLE_ADDITION_PARAM.isSet()) {
            multipleAddition = MULTIPLE_ADDITION_PARAM.getValue();
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

        double residue = (val - rowMean - columnMean + biclusterM)
            * (val - rowMean - columnMean + biclusterM);
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
    protected Map<Integer, Double> reductionValuesRows() {
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
    protected Map<Integer, Double> reductionValuesCols() {
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
        IntIntPair key = new IntIntPair(row, col);
        if (maskedVals.containsKey(key)) {
            return maskedVals.get(key);
        }
        return super.valueAt(row, col);
    }

    /**
     * Initiates the necessary structures for the following algorithm.
     */
    public void initiateChengAndChurch() {
        maskedVals = new LinkedHashMap<IntIntPair, Double>();
        this.rowDim = super.getRowDim();
        this.colDim = super.getColDim();
        missingRowsToMask = new BitSet();
        missingColsToMask = new BitSet();
        findMissingValues();
        applyAlgorithms();
    }

    /**
     * Finds the missing values in the dataset if the missingParameter is set.
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
                    IntIntPair key = new IntIntPair(i, j);
                    maskedVals.put(key, (double) random.nextInt(end) + begin);
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
                IntIntPair key = new IntIntPair(i, j);
                maskedVals.put(key, (double) random.nextInt(end) + begin);
            }
        }
    }

    /**
     * Initiates the multipleNodeDeletion, the singleNodeDeletion and the
     * nodeAddition, masks the remaining rows and columns within the resulting
     * bicluster adds the found bicluster to the result.
     */
    public void applyAlgorithms() {
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
     * Recalculates the necessary structures for multipleNodeDeletion. Performs
     * multiple node deletion until the score of the resulting bicluster is
     * lower or equal to alpha &lowast; sigma.
     */
    public void multipleNodeDeletion() {
        rows = new BitSet(rowDim - 1);
        cols = new BitSet(colDim - 1);
        rows.set(0, rowDim);
        cols.set(0, colDim);
        rowMeans = new LinkedHashMap<Integer, Double>();
        columnMeans = new LinkedHashMap<Integer, Double>();
        recomputeValues();
        removed = true;
        while (removed) {
            removed = false;
            if (valueH > sigma) {
                if (rowDim >= 100) {
                    Map<Integer, Double> delRows = reductionValuesRows();
                    for (int i = rows.nextSetBit(0); i >= 0; i = rows
                        .nextSetBit(i + 1)) {
                        if (delRows.get(i) > alpha * valueH) {
                            rows.clear(i);
                            removed = true;
                        }
                    }
                    initiateColMeans();
                    biclusterMean = meanOfBicluster(rows, cols);
                    valueH = getValueH();
                }

                if (colDim >= 100) {
                    Map<Integer, Double> delCols = reductionValuesCols();
                    for (int j = cols.nextSetBit(0); j >= 0; j = cols
                        .nextSetBit(j + 1)) {
                        if (delCols.get(j) > alpha * valueH) {
                            cols.clear(j);
                            removed = true;
                        }
                    }
                }
                recomputeValues();

                // if (removed) {
                // chooseMaxRAlgorithm2();
                // }
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
            Map<Integer, Double> delRows = reductionValuesRows();
            Map<Integer, Double> delCols = reductionValuesCols();

            if (delRows.size() != 0) {
                maxDelRow = chooseMax(delRows);
            }
            if (delCols.size() != 0) {
                maxDelColumn = chooseMax(delCols);
            }

            if (delRows.get(maxDelRow) >= delCols.get(maxDelColumn)) {
                rows.clear(maxDelRow);
            }
            else {
                cols.clear(maxDelColumn);
            }
            singleNodeDeletion();
        }
    }

    /**
     * Determines the key associated with the doubleValue within the map.
     *
     * @param a a map with scores associated to their row
     * @return the key for the row with maximal score
     */
    public static int chooseMax(Map<Integer, Double> a) {
        if (a != null && a.size() != 0) {
            Iterator<Integer> iter = a.keySet().iterator();
            Integer max = iter.next();
            for (int i = 1; i < a.size(); i++) {
                Integer potentialMax = iter.next();
                if (a.get(potentialMax) > a.get(max)) {
                    max = potentialMax;
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
                if (colAdds.get(j) <= valueH) {
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
                if (rowAdds.get(i) <= valueH) {
                    added = true;
                    rows.set(i);
                }
            }
        }

        Map<Integer, Double> rowAddsInv = reductionValuesRowsInv();
        if (rowAddsInv.size() != 0) {
            for (int i = rows.nextClearBit(0); i >= 0; i = rows
                .nextClearBit(i + 1)) {
                if (i >= rowDim) {
                    break;
                }
                if (rowAddsInv.get(i) <= valueH) {
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
                // restores the value of multipleAddition for the next iteration
                // if the parameter is not set, the previous if-clause will
                // never be entered and
                // therefore the parameter will remain unchanged.
                try {
                    multipleAddition = MULTIPLE_ADDITION_PARAM.getValue();
                }
                catch (UnusedParameterException e) {
                    // We tested for isSet(), thus this should not be reachable.
                    throw new AbortException("Should not be possible." + e.toString());
                }

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
    protected Map<Integer, Double> reductionValuesRowsInv() {
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
                double term = (value - rowMean - columnMean + biclusterM);
                temp = temp + term * term;
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
            "Y. Cheng and G. M. Church. Biclustering of expression data. In Proceedings of the 8th International"
                + "Conference on Intelligent Systems for Molecular Biology (ISMB), San Diego, CA, 2000.");
		return abs;
	}

	/**
	 * Initiates this Algorithm.
	 */
	@Override
	public void biclustering() {
		long t = System.currentTimeMillis();
		initiateChengAndChurch();
		if(isVerbose()){
		verbose("Runtime: "+(System.currentTimeMillis() - t));}
	}

}
