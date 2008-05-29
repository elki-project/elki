package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.biclustering.Bicluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Description;
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
 * Provides a BiclusteringAlgorithm which deletes or inserts rows/columns
 * dependent on their score; and finds biclusters with correlated values
 *
 * @author Noemi Andor
 * @param <V>
 * a certain subtype of RealVector - the data matrix is supposed to
 * consist of rows where each row relates to an object of type V and
 * the columns relate to the attribute values of these objects
 */

public class ChengAndChurch<V extends RealVector<V, Double>> extends
    AbstractBiclustering<V> {

    /**
     * seed for initializing random list for the masking values - key:
     * {@code maskingSeed}
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
     * treshhold value to determine the maximal acceptable score of a bicluster
     * <p/>
     * Key: {@code -sigma}
     * </p>
     */
    public static final DoubleParameter SIGMA_PARAM = new DoubleParameter(
        "sigma",
        "treshhold value to determine the maximal acceptable score of a bicluster",
        new GreaterEqualConstraint(0.0));

    /**
     * parameter for multiple node deletion to accelerate the algorithm
     * <p/>
     * Key: {@code -alpha}
     * </p>
     */
    public static final DoubleParameter ALPHA_PARAM = new DoubleParameter(
        "alpha",
        "parameter for multiple node deletion to accelerate the algorithm ",
        new GreaterEqualConstraint(1.0));

    /**
     * number of biclusters to be found
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
     * lower limit for maskingValues
     * <p/>
     * Key: {@code -begin}
     * </p>
     */
    public static final IntParameter BEGIN_PARAM = new IntParameter("begin",
                                                                    "lower limit for maskingValues");

    /**
     * upper limit for maskingValues
     * <p/>
     * Key: {@code -end}
     * </p>
     */
    public static final IntParameter END_PARAM = new IntParameter("end",
                                                                  "upper limit for maskingValues");

    /**
     * missing Value in database to be raplaced with maskingValues
     * <p/>
     * Key: {@code -missing}
     * </p>
     */
    public static final IntParameter MISSING_PARAM = new IntParameter(
        "missing",
        "missing Value in database to be raplaced with maskingValues");

    /**
     * sets the options for the ParameterValues maskingSeed, sigma, alpha, n,
     * missing, begin and end
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
     * Keeps the number of rows in the data matrix.
     */
    private int rowDim;

    /**
     * Keeps the number of columns in the data matrix.
     */
    private int colDim;

    /**
     * Keeps the position of inverted rows belonging to the current bicluster
     */
    private BitSet invertedRows = new BitSet();

    /**
     * Keeps the position of the columns belonging to the current bicluster
     */
    private BitSet cols;

    /**
     * Keeps the position of the rows belonging to the current bicluster
     */
    private BitSet rows;

    /**
     * marks the rows and columns to be masked. Is updated after each iteration
     */
    private BitSet toMask;

    /**
     * rows with missing values for masking
     */
    private BitSet missingRowsToMask;

    /**
     * columns with missing values for masking
     */
    private BitSet missingColsToMask;

    /**
     * lower threshold for random maskedValues
     */
    private int begin;

    /**
     * upper threshold for random maskedValues
     */
    private int end;

    /**
     * Factor to uniquely define one value to one row and column in the data
     * matrix. For example: factor = 100 => 512 means row 5 column 12
     */
    private int factor;

    /**
     * Keeps the rowMeans of all rows
     */
    private Map<Integer, Double> rowMeans;

    /**
     * Keeps the columnMeans of all columns
     */
    private Map<Integer, Double> columnMeans;

    /**
     * A list of all masked values maped to their row and column
     */
    private Map<Integer, Double> maskedVals = new LinkedHashMap<Integer, Double>();

    /**
     * mean of the current bicluster
     */
    private double biclusterMean;

    /**
     * threshold for bicluster score
     */
    private double sigma;

    /**
     * the current bicluster score
     */
    private double valueH;

    /**
     * a generator for masking found or missing rows/columns with random numbers
     */
    private Random r;

    /**
     * parameter for multiple node deletion
     */
    private double alpha;

    /**
     * number of biclusters to be found
     */
    private int n;

    /**
     * a value marking missing elements in the data matrix
     */
    private int missing;

    /**
     * information if some row/column has been removed from the current
     * bicluster
     */
    private boolean removed;

    /**
     * information if some row/column has been added in the current bicluster
     */
    private boolean added;

    /**
     * specifies that the current state of the iteration is in nodeAddition
     */
    private boolean inNodeAddition;

    /**
     * Parameter to indicate how many times the algorithm to add Nodes should be
     * performed for each iteration.
     */
    private int multipleAddition;

    /**
     * adds the parameterValues
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
     * calculates the score of the current bicluster, according to the rows and
     * columns set to true
     *
     * @return score of the bicluster
     */
    public double getValueH() {
        double h = 0;
        double value = 0;
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double wert;
                if (inNodeAddition && invertedRows.get(i)) {
                    wert = -valueAt(i, j);
                }
                else if (inNodeAddition && !invertedRows.get(i)) {
                    wert = valueAt(i, j);
                }
                else {
                    wert = maskedValueAt(i, j);
                }
                value = value + meanSQR(wert, i, j);
            }
        }
        h = value / (rows.cardinality() * cols.cardinality());
        return h;

    }

    /**
     * calculates the score of a single value within the data matrix
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
     * reinitiates the rowMeans
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
     * reinitiates the columnMeans
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
     * overrides valueAt in AbstractBiclustering to replace missing values with
     * random numbers
     */
    @Override
    protected double valueAt(int row, int col) {
        if (missingRowsToMask.get(row) && missingColsToMask.get(col)) {
            return maskedValueAt(row, col);
        }
        return super.valueAt(row, col);
    }

    /**
     * adjusts the rowMean to the different requests of Algorithm3;
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
     * adjusts the columnMean to the different requests of Algorithm3;
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
     * calculates the score of each row still belonging to the bicluster as a
     * potential candidate to be removed
     *
     * @return a list of scores mapped to their row
     */
    protected Map<Integer, Double> ReductionValuesRows() {
        Map<Integer, Double> ergRows = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            temp = 0;
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double wert = maskedValueAt(i, j);
                temp = temp + meanSQR(wert, i, j);
            }
            ergRows.put(i, temp / cols.cardinality());
        }
        return ergRows;
    }

    /**
     * calculates the score of each column still belonging to the bicluster as a
     * potential candidate to be removed
     *
     * @return a list of scores mapped to their column
     */
    protected Map<Integer, Double> ReductionValuesCols() {
        Map<Integer, Double> ergCols = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
            temp = 0;
            for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
                double wert = maskedValueAt(i, j);
                temp = temp + meanSQR(wert, i, j);
            }
            ergCols.put(j, temp / rows.cardinality());
        }
        return ergCols;
    }

    /**
     * removes a row from the bicluster
     *
     * @param i the row to be removed
     */
    protected void removeRow(int i) {
        rows.clear(i);
    }

    /**
     * removes a column from the bicluster
     *
     * @param j the column to be removed
     */
    protected void removeColumn(int j) {
        cols.clear(j);
    }

    /**
     * reinitiates the rowMean, the columnMean, the biclusterMean and the score
     * of the current bicluster
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
     *         found bicluster, the normal value of the row and column elsewise
     */
    protected double maskedValueAt(int row, int col) {
        if (maskedVals.containsKey(factor * row + col)) {
            return maskedVals.get(factor * row + col);
        }
        return super.valueAt(row, col);
    }

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

    @Override
    public void biclustering() {
        initiateAlgorithms();
    }

    /**
     * calculates the factor, dependent on the columnDimension, to uniquely map
     * one value to a single row and column
     */
    private void createFactor() {
        int floor = colDim;
        int zaehler = 10;
        while (floor > 9) {
            floor = floor / 10;
            zaehler = zaehler * 10;
        }
        factor = zaehler;
    }

    /**
     * initiates the necessary structures for the following algorithms
     */
    public void initiateAlgorithms() {
        this.rowDim = super.getRowDim();
        this.colDim = super.getColDim();
        createFactor();
        toMask = new BitSet();
        missingRowsToMask = new BitSet();
        missingColsToMask = new BitSet();
        findMissingValues();
        createMaskingValues();
        aplyAlgorithms();
    }

    /**
     * finds the missing values in the data matrix if the missingParameter is
     * set
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
                    toMask.set(factor * i + j);
                }
            }
        }
    }

    /**
     * Keeps all rows and columns previously belonging to some bicluster for
     * masking them
     */
    // private void maskMatrix() {
    // newRowsToMask.clear();
    // newColsToMask.clear();
    // for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
    // rowsToMask.set(i);
    // newRowsToMask.set(i);
    // }
    // for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
    // colsToMask.set(j);
    // newColsToMask.set(j);
    // }
    // }
    private void maskMatrix() {
        // toMask.clear();
        for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                toMask.set(factor * i + j);
            }
        }
    }

    /**
     * creates the random values for missing elements and maps them to the
     * corresponding row and column
     */
    private void createMaskingValues() {
        for (int i = toMask.nextSetBit(0); i >= 0; i = toMask.nextSetBit(i + 1)) {
            maskedVals.put(i, (double) r.nextInt(end) + begin);
        }
    }

    /**
     * initiates the multipleNodeDeletion, the singleNodeDeletion and the
     * nodeAddition, masks the remaining rows and columns within the resulting
     * bicluster adds the found bicluster to the result
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
//			if (i == 5) {
//				System.out.println("ready");
//			}
            maskMatrix();
            createMaskingValues();
            Bicluster<V> bicluster = defineBicluster(rows, cols);
            addInvertedRows(bicluster, invertedRows);
            addBiclusterToResult(bicluster);
            reset();
        }
    }

    /**
     * resets the values for the next iteration;
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
     * reinitiates the necessary structures for multipleNodeDeletion
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
     * performs multiple node deletion until the score of the resulting
     * bicluster is lower or equal to alpha * sigma
     */
    private void chooseMaxRAlgorithm2() {
        if (valueH > sigma) {
            removed = false;
            if (rowDim >= 100) {
                Map<Integer, Double> delRows = ReductionValuesRows();
                for (int i = rows.nextSetBit(0); i >= 0; i = rows
                    .nextSetBit(i + 1)) {
                    if ((Double) delRows.get(i) > alpha * valueH) {
                        removeRow(i);
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
                        removeColumn(j);
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
     * initiates the single node deletion
     */
    public void singleNodeDeletion() {
        chooseMaxRAlgorithm1();
    }

    /**
     * performs single node deletion until the score of the resulting bicluster
     * is lower or equal to sigma
     */
    private void chooseMaxRAlgorithm1() {
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
                removeRow(maxDelRow);
            }
            else {
                removeColumn(maxDelColumn);
            }
            chooseMaxRAlgorithm1();
        }
    }

    /**
     * determines the key associated with the major doubleValue within the map
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
     * initiates node Addition
     */
    public void nodeAddition() {
        invertedRows.clear();
        chooseMinAdditions();
    }

    /**
     * determines which row can be added to the bicluster without increasing the
     * score and adds them to the bicluster
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
                    addColumn(j);
                    System.out.println("line 733");
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
                    addRow(i);
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
                    addRowInv(i);
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
                try {
                    multipleAddition = getParameterValue(MULTIPLE_ADDITION_PARAM);
                }
                catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * adds a row to the current bicluster
     *
     * @param i the row to be added
     */
    private void addRow(int i) {
        rows.set(i);
    }

    /**
     * adds an inverted row to the current bicluster and marks it as inverted
     * row
     *
     * @param i the inverted row to be added
     */
    private void addRowInv(int i) {
        rows.set(i);
        invertedRows.set(i);
    }

    /**
     * adds a column to the current bicluster
     *
     * @param j the column to be added
     */
    private void addColumn(int j) {
        cols.set(j);
        System.out.println("line 808");
    }

    /**
     * calculates the scores for the rows not belonging to the current bicluster
     * as potential candidates to be added
     *
     * @return list of scores mapped to their rows
     */
    protected Map<Integer, Double> additionValuesRows() {
        Map<Integer, Double> ergRows = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int i = rows.nextClearBit(0); i >= 0; i = rows.nextClearBit(i + 1)) {
            if (i >= rowDim) {
                break;
            }
            temp = 0;
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double wert = valueAt(i, j);
                temp = temp + meanSQR(wert, i, j);
            }
            ergRows.put(i, temp / cols.cardinality());
        }
        return ergRows;
    }

    /**
     * calculates the scores for the columns not belonging to the current
     * bicluster as potential candidates to be added
     *
     * @return list of scores mapped to their columns
     */
    protected Map<Integer, Double> additionValuesCols() {
        Map<Integer, Double> ergCols = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int j = cols.nextClearBit(0); j >= 0; j = cols.nextClearBit(j + 1)) {
            if (j >= colDim) {
                break;
            }
            temp = 0;
            for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
                double wert = valueAt(i, j);
                temp = temp + meanSQR(wert, i, j);
            }
            ergCols.put(j, temp / rows.cardinality());
        }
        return ergCols;
    }

    /**
     * calculates the scores for the inverted rows not belonging to the current
     * bicluster as potential candidates to be added
     *
     * @return list of scores mapped to their rows
     */
    protected Map<Integer, Double> ReductionValuesRowsInv() {
        Map<Integer, Double> ergRows = new LinkedHashMap<Integer, Double>();
        double temp = 0;
        for (int i = rows.nextClearBit(0); i >= 0; i = rows.nextClearBit(i + 1)) {
            if (i >= rowDim) {
                break;
            }
            temp = 0;
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                double wert = -valueAt(i, j);
                double rowMean = -rowMeans.get(i);
                double columnMean = columnMeans.get(j);
                double biclusterM = biclusterMean;

                temp = temp
                       + Math.pow(wert - rowMean - columnMean + biclusterM, 2);
            }
            ergRows.put(i, temp / cols.cardinality());
        }
        return ergRows;
    }

    // private BitSet temp() {
    // BitSet neuerSet = new BitSet();
    // for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
    // neuerSet.set(rowDim - 1 - i);
    // }
    // System.out.println(neuerSet.cardinality());
    // return neuerSet;
    // }

}
