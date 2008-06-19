package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.biclustering.Bicluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;

/**
 * Provides a BiclusteringAlgorithm which deletes or inserts rows/columns in an
 * available bicluster dependent on the gain of this action, finding biclusters
 * with correlated values
 * 
 * @param <V>
 *            a certain subtype of RealVector - the data matrix is supposed to
 *            consist of rows where each row relates to an object of type V and
 *            the columns relate to the attribute values of these objects
 * 
 * @author Noemi Andor
 */
public class FLOC<V extends RealVector<V, Double>> extends
		AbstractBiclustering<V> {

	/**
	 * OptionID for the parameter {@link #ACTION_ORDER_PARAM}.
	 */
	public static final OptionID ACTION_ORDER_ID = OptionID
			.getOrCreateOptionID("FLOC.actionOrder",
					"specifies the order of actions");

	/**
	 * Parameter which indicates in which order actions should be performed. If
	 * its value is 0 (default) a weighted random order is performed, abetting
	 * actions with greater gain, if its value is 1 a random order is performed
	 * else, for values greater then 1 the normal order of actions (1..rowDim
	 * +colDim) is performed.
	 * <p>
	 * Default value: 0
	 * </p>
	 * <p>
	 * Key: {@code -FLOC.actionOrder}
	 * </p>
	 */
	public final IntParameter ACTION_ORDER_PARAM = new IntParameter(
			ACTION_ORDER_ID, new GreaterEqualConstraint(0));

	/**
	 * OptionID for the parameter {@link #SEED_PARAM}.
	 */
	public static final OptionID SEED_ID = OptionID.getOrCreateOptionID(
			"FLOC.seed", "seed for initial clusters");

	/**
	 * Seed for creating initial clusters.
	 * <p>
	 * Default value: 1
	 * </p>
	 * <p>
	 * Key: {@code -FLOC.seed}
	 * </p>
	 */
	public final IntParameter SEED_PARAM = new IntParameter(SEED_ID,
			new GreaterEqualConstraint(1));

	/**
	 * OptionID for the parameter {@link #K_PARAM}.
	 */
	public static final OptionID K_ID = OptionID.getOrCreateOptionID("FLOC.k",
			"indicates how many biclusters should be found");

	/**
	 * Parameter which indicates how many biclusters should be found.
	 * <p>
	 * Default value: 1
	 * </p>
	 * <p>
	 * Key: {@code -FLOC.k}
	 * </p>
	 */
	public final IntParameter K_PARAM = new IntParameter(K_ID,
			new GreaterEqualConstraint(1));

	/**
	 * OptionID for the parameter {@link #INITIAL_ROW_DIM_PARAM}.
	 */
	public static final OptionID INITIAL_ROW_DIM_ID = OptionID
			.getOrCreateOptionID("FLOC.initialRowDim",
					"Parameter to approximate the rowDimension of a initial bicluster");

	/**
	 * Parameter to approximate the rowDimension of an initial bicluster.
	 * <p>
	 * Key: {@code -FLOC.initialRowDim}
	 * </p>
	 */
	public final DoubleParameter INITIAL_ROW_DIM_PARAM = new DoubleParameter(
			INITIAL_ROW_DIM_ID, new LessEqualConstraint(1.0));

	/**
	 * OptionID for the parameter {@link #INITIAL_COL_DIM_PARAM}.
	 */
	public static final OptionID INITIAL_COL_DIM_ID = OptionID
			.getOrCreateOptionID("FLOC.initialColDim",
					"Parameter to approximate the columnDimension of a initial bicluster");

	/**
	 * Parameter to approximate the columnDimension of an initial bicluster.
	 * <p>
	 * Key: {@code -FLOC.initialColDim}
	 * </p>
	 */
	public final DoubleParameter INITIAL_COL_DIM_PARAM = new DoubleParameter(
			INITIAL_COL_DIM_ID, new LessEqualConstraint(1.0));

	/**
	 * OptionID for the parameter {@link #MISSING_PARAM}.
	 */
	public static final OptionID MISSING_ID = OptionID.getOrCreateOptionID(
			"FLOC.missing",
			"Keeps the value which marks a missing entry within the dataset");

	/**
	 * Keeps the value which marks a missing entry within the database.
	 * <p>
	 * Key: {@code -FLOC.missing}
	 * </p>
	 */
	public final DoubleParameter MISSING_PARAM = new DoubleParameter(MISSING_ID);

	/**
	 * A generator for creating the initial k clusters, used as well to define
	 * the order of actions to be performed.
	 */
	private Random r;

	/**
	 * The rowCluster currently worked at (by deleting or inserting a row).
	 */
	private BitSet currRows;

	/**
	 * The columnCluster currently worked at (by deleting or inserting a
	 * column).
	 */
	private BitSet currCols;

	/**
	 * Keeps the current set of rowClusters. Is changed by every iteration.
	 */
	private BitSet[] rowClusters;

	/**
	 * Keeps the current set of columnClusters. Is changed by every iteration.
	 */
	private BitSet[] colClusters;

	/**
	 * Every position i, keeps the cluster, in which a deletion/insertion of the
	 * row i would result in a maximal scoreReduction (best gain).
	 */
	private int[] bestClusterForRow;

	/**
	 * Every position j, keeps the cluster, in which a deletion/insertion of the
	 * column j would result in a maximal scoreReduction (best gain).
	 */
	private int[] bestClusterForCol;

	/**
	 * Keeps the value of the average residue of the currently best cluster
	 * alignment.
	 */
	private double bestClustering;

	/**
	 * Is set to false if the action with regard to the specified row hasn't
	 * been performed.
	 */
	private BitSet rowActionPerformed;

	/**
	 * Is set to false if the action with regard to the specified column hasn't
	 * been performed.
	 */
	private BitSet colActionPerformed;

	/**
	 * Keeps the number of rows in the database.
	 */
	private int rowDim;

	/**
	 * Keeps the number of columns in the database.
	 */
	private int colDim;

	/**
	 * Parameter which indicates in which order actions should be performed if
	 * its value is 0 (default) a weighted random order is performed, abetting
	 * actions with greater gain, if its value is 1 a random order is performed
	 * else, for values greater then 1 the normal order of actions (1..rowDim
	 * +colDim) is performed.
	 */
	private int actionOrder;

	/**
	 * Keeps the gain for each row and column.
	 */
	private Map<Integer, Double> gainMap;

	/**
	 * The maximal gain in the current iteration. Necessary only if a weighted
	 * order of actions is performed.
	 */
	private double maxGain;

	/**
	 * The minimal gain in the current iteration. Necessary only if a weighted
	 * order of actions is performed.
	 */
	private double minGain;

	/**
	 * Value indicating a missing entry within the database.
	 */
	private double missing;

	/**
	 * Keeps the number of iterations.
	 */
	private int iterations;

	/**
	 * The number of biclusters to be found.
	 */
	private int k;

	/**
	 * Value within 0 and 1 indicating the percentage of the rowDimension of the
	 * database as dimension for the initial clusters. A small difference
	 * between the initial Dimension and the dimension of the resultBiclusters,
	 * contributes to a small number of iterations, and so to a better
	 * efficiency of the algorithm.
	 */
	private double initialRowDim;

	/**
	 * Value within 0 and 1 indicating the percentage of the columnDimension of
	 * the database as dimension for the initial clusters. A small difference
	 * between the initial Dimension and the dimension of the resultBiclusters,
	 * contributes to a small number of iterations, and so to a better
	 * efficiency of the algorithm.
	 */
	private double initialColDim;

	/**
	 * Keeps the columnMeans of the currently processed bicluster.
	 */
	private List<Double> colMeans;

	/**
	 * Keeps the rowMeans of the currently processed bicluster.
	 */
	private List<Double> rowMeans;

	/**
	 * Keeps the row means of each row of every cluster.
	 */
	private List<Double>[] rowClusterMeans;

	/**
	 * Keeps the column means of each column of every cluster.
	 */
	private List<Double>[] colClusterMeans;

	/**
	 * Keeps the bicluster means of every cluster.
	 */
	private double[] biclusterMeans;

	/**
	 * Keeps the HValue of every cluster.
	 */
	private double[] valueHClusters;

	/**
	 * Keeps the biclusterMean of the currently processed bicluster.
	 */
	private double biclusterMean;

	/**
	 * Sets the options for the ParameterValues k, initialRowDim, initialColDim,
	 * seed, missing, and actionOrder. Adds the parameter values.
	 */
	public FLOC() {
		K_PARAM.setDefaultValue(1);
		INITIAL_ROW_DIM_PARAM.setOptional(false);
		INITIAL_COL_DIM_PARAM.setOptional(false);
		SEED_PARAM.setDefaultValue(1);
		SEED_PARAM.setOptional(true);
		MISSING_PARAM.setOptional(true);
		ACTION_ORDER_PARAM.setOptional(true);
		ACTION_ORDER_PARAM.setDefaultValue(0);
		this.addOption(SEED_PARAM);
		this.addOption(K_PARAM);
		this.addOption(INITIAL_ROW_DIM_PARAM);
		this.addOption(INITIAL_COL_DIM_PARAM);
		this.addOption(MISSING_PARAM);
		this.addOption(ACTION_ORDER_PARAM);
	}

	/**
	 * Calls
	 * {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
	 * and sets additionally the parameters for k, initialRowDim, initialColDim,
	 * seed, missing, and actionOrder
	 * 
	 * @see AbstractAlgorithm#setParameters(String[])
	 */
	@Override
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);
		k = this.getParameterValue(K_PARAM);
		initialRowDim = this.getParameterValue(INITIAL_ROW_DIM_PARAM);
		initialColDim = this.getParameterValue(INITIAL_COL_DIM_PARAM);
		int seed = this.getParameterValue(SEED_PARAM);
		r = new Random(seed);
		if (MISSING_PARAM.isSet()) {
			missing = this.getParameterValue(MISSING_PARAM);
		}
		if (ACTION_ORDER_PARAM.isSet()) {
			actionOrder = this.getParameterValue(ACTION_ORDER_PARAM);
		}
		if (actionOrder == 0) {
			gainMap = new HashMap<Integer, Double>();
			maxGain = 0;
			minGain = 99999;
		}
		return remainingParameters;
	}

	/**
	 * Initiates the necessary structures for the following algorithm.
	 */
	private void initiateFLOC() {
		iterations = 1;
		rowDim = super.getRowDim();
		colDim = super.getColDim();
		currRows = new BitSet();
		currCols = new BitSet();
		rowClusters = new BitSet[k];
		colClusters = new BitSet[k];

		rowActionPerformed = new BitSet();
		colActionPerformed = new BitSet();
		// rows = new BitSet();
		// rows.set(0, rowDim);
		// cols = new BitSet();
		// cols.set(0, colDim);
		// eraseMissingValues();
		createRandomCluster();
		// resetValues(rows, cols);
		performBestAction();
	}

	/**
	 * Adjusts the rowMean to the different requests of the FLOC_Algorithm;
	 * excluding missing entries from the computation.
	 * 
	 * @param row
	 *            the row to compute the mean value w.r.t. the given set of
	 *            columns (relates to database entry id
	 *            <code>{@link #rowIDs rowIDs[row]}</code>)
	 * @param cols
	 *            the set of columns to include in the computation of the mean
	 *            of the given row
	 * @return the mean value of the specified row over the specified columns
	 */
	@Override
	protected double meanOfRow(int row, BitSet cols) {
		if (!MISSING_PARAM.isSet()) {
			return super.meanOfRow(row, cols);
		}
		double sum = 0;
		int colVol = cols.cardinality();
		for (int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
			double val = valueAt(row, i);
			if (MISSING_PARAM.isSet() && val == missing) {
				colVol--;
			}

			else {
				sum += val;
			}
		}
		if (colVol == 0) {
			return 0;
		}
		return sum / colVol;
	}

	/**
	 * Adjusts the columnMean to the different requests of the FLOC_Algorithm
	 * excludes missing entries from the computation.
	 * 
	 * @param rows
	 *            the set of rows to include in the computation of the mean of
	 *            the given column
	 * @param col
	 *            the column index to compute the mean value w.r.t. the given
	 *            set of rows (relates to attribute
	 *            <code>{@link #colIDs colIDs[col]}</code> of the
	 *            corresponding database entries)
	 * @return the mean value of the specified column over the specified rows
	 */
	@Override
	protected double meanOfCol(BitSet rows, int col) {
		if (!MISSING_PARAM.isSet()) {
			return super.meanOfCol(rows, col);
		}
		double sum = 0;
		int rowVol = rows.cardinality();
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			double val = valueAt(i, col);
			if (MISSING_PARAM.isSet() && val == missing) {
				rowVol--;
			} else {
				sum += val;
			}
		}
		if (rowVol == 0) {
			return 0;
		}
		return sum / rowVol;
	}

	/**
	 * Adjusts the biclusterMean to the different requests of the FLOC_Algorithm
	 * excludes missing entries from the computation.
	 * 
	 * @param rows
	 *            the set of rows to include in the computation of the
	 *            biclusterMean
	 * @param cols
	 *            the set of columns to include in the computation of the
	 *            biclusterMean
	 * @return the biclusterMean of the specified rows over the specified
	 *         columns
	 */
	@Override
	protected double meanOfBicluster(BitSet rows, BitSet cols) {
		if (!MISSING_PARAM.isSet()) {
			return super.meanOfBicluster(rows, cols);
		}
		double sum = 0;
		int vol = rows.cardinality() * cols.cardinality();
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
				double val = valueAt(i, j);
				if (val == missing) {
					vol--;
				} else {
					sum += val;
				}
			}
		}
		return sum / vol;
	}

	// /**
	// * resets the values for the next processed cluster
	// *
	// * @param rows
	// * rows belonging to the currently processed bicluster
	// * @param cols
	// * columns belonging to the currently processed bicluster
	// */
	// private void resetValues(BitSet rows, BitSet cols) {
	// initiateRowMeans(rows, cols);
	// initiateColMeans(rows, cols);
	// biclusterMean = meanOfBicluster(rows, cols);
	// }

	/**
	 * Calculates the score of the current bicluster, according to the rows and
	 * columns set to true.
	 * 
	 * @param rows
	 *            rows belonging to the currently processed bicluster
	 * @param cols
	 *            columns belonging to the currently processed bicluster
	 * @return score of the bicluster
	 */
	public double getValueH(BitSet rows, BitSet cols) {
		double hValue = 0;
		int volume = rows.cardinality() * cols.cardinality();
		int rM = -1;
		int cM = -1;
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			rM++;
			cM = -1;
			for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
				cM++;
				double value = valueAt(i, j);
				double residue;
				if (MISSING_PARAM.isSet() && value == missing) {
					residue = 0;
					volume--;
				} else {
					double rowMean = rowMeans.get(rM);
					double columnMean = colMeans.get(cM);
					double biclusterM = biclusterMean;
					residue = Math.abs(value - rowMean - columnMean
							+ biclusterM);
				}
				hValue = hValue + residue;
			}
		}
		hValue = hValue / (volume);
		return hValue;

	}

	/**
	 * Recalculates the rowMeans.
	 * 
	 * @param currRows
	 *            rows belonging to the currently processed bicluster
	 * @param currCols
	 *            columns belonging to the currently processed bicluster
	 */
	protected List<Double> initiateRowMeans(BitSet currRows, BitSet currCols) {
		int z = 0;
		rowMeans = new ArrayList<Double>();
		for (int i = currRows.nextSetBit(0); i >= 0; i = currRows
				.nextSetBit(i + 1)) {
			rowMeans.add(meanOfRow(i, currCols));
			z++;
		}
		return rowMeans;
	}

	/**
	 * Recalculates the columnMeans.
	 * 
	 * @param currRows
	 *            rows belonging to the currently processed bicluster
	 * @param currCols
	 *            columns belonging to the currently processed bicluster
	 */
	protected List<Double> initiateColMeans(BitSet currRows, BitSet currCols) {
		int z = 0;
		colMeans = new ArrayList<Double>();
		for (int j = currCols.nextSetBit(0); j >= 0; j = currCols
				.nextSetBit(j + 1)) {
			colMeans.add(meanOfCol(currRows, j));
			z++;
		}
		return colMeans;
	}

	// private void eraseMissingValues() {
	// if (!MISSING_PARAM.isSet()) {
	// return;
	// }
	// for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
	// for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
	// if (super.valueAt(i, j) == missing) {
	// rows.clear(i);
	// cols.clear(j);
	// }
	// }
	// }
	//
	// rowDim = rows.cardinality();
	// colDim = cols.cardinality();
	// }

	/**
	 * Creates the initial k biclusters at the beginning of the algorithm.
	 */
	private void createRandomCluster() {
		for (int z = 0; z < k; z++) {
			currRows.clear();
			currCols.clear();
			int counter = 0;

			for (int i = r.nextInt(rowDim); counter < Math.ceil(initialRowDim
					* rowDim); i = r.nextInt(rowDim)) {
				if (!currRows.get(i)) {
					counter++;
				}
				currRows.set(i);
			}

			counter = 0;
			for (int j = r.nextInt(colDim); counter < Math.ceil(initialColDim
					* colDim); j = r.nextInt(colDim)) {
				if (!currCols.get(j)) {
					counter++;
				}
				currCols.set(j);
			}
			rowClusters[z] = (BitSet) currRows.clone();
			colClusters[z] = (BitSet) currCols.clone();
		}
	}

	/**
	 * Calculates the gain obtained by removing/ inserting a row in the
	 * currently processed rowCluster.
	 * 
	 * @param row
	 *            row responsible for the change of residue
	 * @return the gain obtained by removing/inserting parameter row
	 */
	private double rowGain(int cluster, int row) {
		currRows = (BitSet) rowClusters[cluster].clone();
		currCols = (BitSet) colClusters[cluster].clone();
		double oldHValue = valueHClusters[cluster];
		rowMeans = rowClusterMeans[cluster];
		colMeans = colClusterMeans[cluster];
		biclusterMean = biclusterMeans[cluster];

		// adjust rowMeans
		int counter = 0;
		for (int i = currRows.nextSetBit(0);; i = currRows.nextSetBit(i + 1)) {
			if (i < 0) {
				rowMeans.add(meanOfRow(row, currCols));
				break;
			}
			if (row < i) {
				rowMeans.add(counter, meanOfRow(row, currCols));
				break;
			}
			counter++;
		}

		// adjust columnMeans
		for (int i = 0; i < currCols.cardinality(); i++) {
			double newColMean = (colMeans.get(i) * currRows.cardinality() + valueAt(
					row, i))
					/ (currRows.cardinality() + 1);
			colMeans.set(i, newColMean);
		}

		// adjust biclusterMean
		biclusterMean = biclusterMean
				* (currRows.cardinality() * currCols.cardinality());
		for (int i = currCols.nextSetBit(0); i >= 0; i = currCols
				.nextSetBit(i + 1)) {
			biclusterMean = biclusterMean + valueAt(row, i);
		}
		biclusterMean = biclusterMean
				/ ((currRows.cardinality() + 1) * currCols.cardinality());
		currRows.flip(row);
		double newHValue = getValueH(currRows, currCols);
		currRows.flip(row);
		return oldHValue - newHValue;
	}

	/**
	 * Calculates the gain obtained by removing/ inserting a column in the
	 * currently processed columnCluster.
	 * 
	 * @param col
	 *            column responsible for the change of residue
	 * @return the gain obtained by removing/inserting parameter row
	 */
	private double colGain(int cluster, int col) {
		double oldHValue = valueHClusters[cluster];
		rowMeans = rowClusterMeans[cluster];
		colMeans = colClusterMeans[cluster];
		biclusterMean = biclusterMeans[cluster];

		// adjust colMeans
		int counter = 0;
		for (int i = currCols.nextSetBit(0);; i = currCols.nextSetBit(i + 1)) {
			if (i < 0) {
				colMeans.add(meanOfCol(currRows, col));
				break;
			}
			if (col < i) {
				colMeans.add(counter, meanOfCol(currRows, col));
				break;
			}
			counter++;
		}

		// adjust rowMeans
		for (int i = 0; i < currRows.cardinality(); i++) {
			double newRowMean = (rowMeans.get(i) * currCols.cardinality() + valueAt(
					i, col))
					/ (currCols.cardinality() + 1);
			rowMeans.set(i, newRowMean);
		}

		// adjust biclusterMean
		biclusterMean = biclusterMean
				* (currRows.cardinality() * currCols.cardinality());
		for (int i = currRows.nextSetBit(0); i >= 0; i = currRows
				.nextSetBit(i + 1)) {
			biclusterMean = biclusterMean + valueAt(i, col);
		}
		biclusterMean = biclusterMean
				/ (currRows.cardinality() * (currCols.cardinality() + 1));
		currRows.flip(col);
		double newHValue = getValueH(currRows, currCols);
		currRows.flip(col);
		return oldHValue - newHValue;
	}

	/**
	 * Identifies the cluster for the greatest gain.
	 * 
	 * @param row
	 *            row to be removed/inserted in a cluster
	 * @return the position of the cluster in which a deletion/insertion of row
	 *         results in the greatest gain
	 */
	private int bestRowGain(int row) {
		double initGain = -99999;
		int bestCluster = -1;
		for (int i = 0; i < rowClusters.length; i++) {
			double gain = rowGain(i, row);
			createGainMap(row, gain);
			if (gain > initGain) {
				initGain = gain;
				bestCluster = i;
			}
		}
		return bestCluster;
	}

	/**
	 * Identifies the cluster for the greatest gain
	 * 
	 * @param col
	 *            column to be removed/inserted in a cluster
	 * @return the position of the cluster in which a deletion/insertion of col
	 *         results in the greatest gain
	 */
	private int bestColGain(int col) {
		double initGain = -99999;
		int bestCluster = -1;
		for (int j = 0; j < colClusters.length; j++) {
			currRows = (BitSet) rowClusters[j].clone();
			currCols = (BitSet) colClusters[j].clone();
			double gain = colGain(j, col);
			createGainMap(col + rowDim, gain);
			if (gain > initGain) {
				initGain = gain;
				bestCluster = j;
			}
		}
		return bestCluster;
	}

	/**
	 * Maps the gains to their rows/ columns in the bicluster. Necessary only if
	 * a weighted order of action is selected.
	 * 
	 * @param pos
	 *            the row/column with the specified gain
	 * @param gain
	 *            the gain of the row with the specified position pos.
	 */
	private void createGainMap(int pos, double gain) {
		if (actionOrder != 0) {
			return;
		}
		gainMap.put(pos, gain);
		if (gain > maxGain) {
			maxGain = gain;
		}
		if (gain < minGain) {
			minGain = gain;
		}
	}

	/**
	 * Fills the array bestClusterForRow and bestClusterForCols with the
	 * clusterPositions on every rowPosition (bestClusterForRow[i] indicates the
	 * cluster with the greatest gain for row i).
	 */
	private void bestActions() {
		bestClustering = calculateAverageResidue();
		bestClusterForRow = new int[rowDim];
		bestClusterForCol = new int[colDim];
		for (int i = 0; i < rowDim; i++) {
			bestClusterForRow[i] = bestRowGain(i);
		}
		for (int j = 0; j < colDim; j++) {
			bestClusterForCol[j] = bestColGain(j);
		}
	}

	/**
	 * Performs all actions returned by bestActions() in the order specified by
	 * actionOrder. Keeps the clustering with the lowest averageResidue as
	 * initial clustering for the next iteration. If the averageResidue could
	 * not been reduced due to any action, the algorithm is finished and the
	 * clustering with the best average residue is returned.
	 */
	private void performBestAction() {
		bestActions();
		rowActionPerformed.set(0, rowDim);
		colActionPerformed.set(0, colDim);
		int bestLocation = -1;
		int[] order;
		if (actionOrder == 0) {
			order = weightedRandomSwitch();
		} else if (actionOrder == 1) {
			order = randomSwitch();
		} else {
			order = normalOrder();
		}
		for (int n = 0; n < order.length; n++) {
			int i = order[n];
			if (i < rowDim) {
				if (bestClusterForRow[i] == -1) {
					rowActionPerformed.clear();
					continue;
				}

				BitSet currCluster = rowClusters[bestClusterForRow[i]];
				if (currCluster.cardinality() <= 2 && currCluster.get(i)) {
					rowActionPerformed.clear();
					continue;
				}

				rowClusters[bestClusterForRow[i]].flip(i);
				double newAverageResidue = calculateAverageResidue();
				if (newAverageResidue < bestClustering) {
					bestClustering = newAverageResidue;
					bestLocation = n;
				}
			} else {
				int j = order[n] - rowDim;
				if (bestClusterForCol[j] == -1) {
					colActionPerformed.clear();
					continue;
				}
				BitSet currCluster = colClusters[bestClusterForCol[j]];
				if (currCluster.cardinality() <= 2 && currCluster.get(j)) {
					colActionPerformed.clear(j);
					continue;
				}

				colClusters[bestClusterForCol[j]].flip(j);
				double newAverageResidue = calculateAverageResidue();
				if (newAverageResidue < bestClustering) {
					bestClustering = newAverageResidue;
					bestLocation = n;
				}
			}
		}

		// undo unnecessary actions
		for (int n = bestLocation + 1; n < order.length; n++) {
			int i = order[n];
			if (i < rowDim) {
				if (!rowActionPerformed.get(i)) {
					continue;
				}
				rowClusters[bestClusterForRow[i]].flip(i);
			} else {
				int j = order[n] - rowDim;
				if (!colActionPerformed.get(j)) {
					continue;
				}
				colClusters[bestClusterForCol[j]].flip(j);
			}
		}

		// prepare for next iteration
		if (bestLocation != -1) {
			if (actionOrder == 0) {
				maxGain = 0;
				minGain = 99999;
				gainMap.clear();
			}
			verbose(iterations + ". average residue: " + bestClustering);
			iterations++;
			performBestAction();

		} else {
			if (isVerbose()) {
				verbose("number of iterations: " + iterations + "\n");
				verbose("average residue: " + bestClustering);
			}
			returnBiclusters();
		}
	}

	/**
	 * Allows a sequential order of actions only necessary if a normal order of
	 * actions is selected (actionOrder >1)
	 * 
	 * @return an array of row and columnIndices: [0,..., rowDim + colDim - 1]
	 */
	private int[] normalOrder() {
		int[] order = new int[rowDim + colDim];
		for (int i = 0; i < rowDim; i++) {
			order[i] = i;
		}
		return order;
	}

	/**
	 * Allows a random order of actions only necessary if a random order of
	 * actions is selected (actionOrder == 1)
	 * 
	 * @return an array of row and columnIndices in random order
	 */
	private int[] randomSwitch() {
		int g = 2 * (rowDim + colDim);
		int[] randomOrder = normalOrder();

		while (g > 0) {
			int r1 = r.nextInt(rowDim + colDim);
			int r2 = r.nextInt(rowDim + colDim);
			randomOrder[r1] = r2;
			randomOrder[r2] = r1;
			g--;
		}
		return randomOrder;
	}

	/**
	 * Allows a weighted order of actions, abetting the actions with greater
	 * gain. Only necessary if a weighted random order of actions is selected
	 * (actionOrder == 0).
	 * 
	 * @return an array of row and columnIndices
	 */
	private int[] weightedRandomSwitch() {
		double R = maxGain - minGain;
		int g = 2 * (rowDim + colDim);
		int[] randomOrder = normalOrder();

		while (g > 0) {
			int r1 = r.nextInt(rowDim + colDim);
			int r2 = r.nextInt(rowDim + colDim);
			if (r2 < r1) {
				int c = r1;
				r1 = r2;
				r2 = c;
			}
			double p = 0.5 + (gainMap.get(r2) - gainMap.get(r1)) / (2 * R);
			if (r.nextDouble() <= p) {
				randomOrder[r1] = r2;
				randomOrder[r2] = r1;
				g--;
			}
		}
		return randomOrder;
	}

	/**
	 * Calculates the average residue of all current clusters, after an action
	 * (row/column- deletion/insertion) has been performed on one of the
	 * clusters.
	 * 
	 * @return the new average residue of the clustering
	 */
	private double calculateAverageResidue() {
		rowClusterMeans = new List[k];
		colClusterMeans = new List[k];
		biclusterMeans = new double[k];
		valueHClusters = new double[k];
		double averageResidue = 0;
		for (int i = 0; i < k; i++) {
			BitSet currRows = rowClusters[i];
			BitSet currCols = colClusters[i];

			rowClusterMeans[i] = initiateRowMeans(currRows, currCols);
			colClusterMeans[i] = initiateColMeans(currRows, currCols);
			biclusterMeans[i] = meanOfBicluster(currRows, currCols);
			biclusterMean = biclusterMeans[i];
			valueHClusters[i] = getValueH(currRows, currCols);
			averageResidue = averageResidue + valueHClusters[i];
		}
		return averageResidue / k;
	}

	/**
	 * Adds the embedded biclusters to the final result of this algorithm.
	 */
	private void returnBiclusters() {
		for (int i = 0; i < k; i++) {
			Bicluster<V> bicluster = defineBicluster(rowClusters[i],
					colClusters[i]);
			addBiclusterToResult(bicluster);
		}
	}

	/**
	 * Initiates this Algorithm.
	 */
	@Override
	protected void biclustering() throws IllegalStateException {
		long t = System.currentTimeMillis();
		initiateFLOC();
		System.out.println(System.currentTimeMillis() - t);
	}

	/**
	 * The description of this Algorithm.
	 */
	public Description getDescription() {
		Description abs = new Description(
				"FLOC",
				"a biclustering method, calculating biclusters by deleting/inserting "
						+ "rows and columns depending on the gain of an action",
				"finding correlated values in a subset of rows and a subset of columns",
				"");
		return abs;
	}

}
