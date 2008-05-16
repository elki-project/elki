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
	 * parameter which indicates in which order actions should be performed if
	 * its value is 0 (default) a weighted random order is performed, abetting
	 * actions with greater gain, if its value is 1 a random order is performed
	 * else, for values greater then 1 the normal order of actions (1..rowDim
	 * +colDim) is performed
	 * <p>
	 * Default value: 0
	 * </p>
	 * <p>
	 * Key: {@code -actionOrder}
	 * </p>
	 */
	public static final IntParameter ACTION_ORDER_PARAM = new IntParameter(
			"actionOrder", "specifies the order of actions",
			new GreaterEqualConstraint(0));

	/**
	 * seed for creating initial clusters
	 * <p>
	 * Default value: 1
	 * </p>
	 * <p>
	 * Key: {@code -seed}
	 * </p>
	 */
	public static final IntParameter SEED_PARAM = new IntParameter("seed",
			"seed for initial clusters", new GreaterEqualConstraint(1));

	/**
	 * Parameter which indicates how many biclusters should be found
	 * <p>
	 * Default value: 1
	 * </p>
	 * <p>
	 * Key: {@code -k}
	 * </p>
	 */
	public static final IntParameter K_PARAM = new IntParameter("k",
			"indicates how many biclusters should be found",
			new GreaterEqualConstraint(1));

	/**
	 * Parameter to approximate the rowDimension of an initial bicluster
	 * <p>
	 * Key: {@code -initialRowDim}
	 * </p>
	 */
	public static final DoubleParameter INITIAL_ROW_DIM_PARAM = new DoubleParameter(
			"initialRowDim",
			"Parameter to approximate the rowDimension of a initial bicluster",
			new LessEqualConstraint(1.0));

	/**
	 * Parameter to approximate the columnDimension of an initial bicluster
	 * <p>
	 * Key: {@code -initialColDim}
	 * </p>
	 */
	public static final DoubleParameter INITIAL_COL_DIM_PARAM = new DoubleParameter(
			"initialColDim",
			"Parameter to approximate the columnDimension of a initial bicluster",
			new LessEqualConstraint(1.0));

	/**
	 * Keeps the value which marks a missing entry within the database
	 * <p>
	 * Key: {@code -missing}
	 * </p>
	 */
	public static final DoubleParameter MISSING_PARAM = new DoubleParameter(
			"missing",
			"Keeps the value which marks a missing entry within the dataset");

	/**
	 * Sets the options for the ParameterValues k, initialRowDim, initialColDim,
	 * seed, missing, and actionOrder
	 */
	static {
		K_PARAM.setDefaultValue(1);
		INITIAL_ROW_DIM_PARAM.setOptional(false);
		INITIAL_COL_DIM_PARAM.setOptional(false);
		SEED_PARAM.setDefaultValue(1);
		SEED_PARAM.setOptional(true);
		MISSING_PARAM.setOptional(true);
		ACTION_ORDER_PARAM.setOptional(true);
		ACTION_ORDER_PARAM.setDefaultValue(0);
	}

	/**
	 * a generator for creating the initial k clusters, used as well to define
	 * the order of actions to be performed
	 */
	private Random r;

	// /**
	// * the rows belonging to the database are set true. If a row contains a
	// * missing value, the row in the BitSet is set to false
	// */
	// private BitSet rows;

	// /**
	// * the columns belonging to the database are set true. if a column
	// contains
	// * a missing value, the row in the BitSet is set to false
	// */
	// private BitSet cols;

	/**
	 * the rowCluster currently worked at (by deleting or inserting a row)
	 */
	private BitSet currRows;

	/**
	 * the columnCluster currently worked at (by deleting or inserting a column)
	 */
	private BitSet currCols;

	/**
	 * keeps the current set of rowClusters. Is changed by every iteration
	 */
	private BitSet[] rowClusters;

	/**
	 * keeps the current set of columnClusters. Is changed by every iteration
	 */
	private BitSet[] colClusters;

	/**
	 * every position i, keeps the cluster, in which a deletion/insertion of the
	 * row i would result in a maximal scoreReduction (best gain)
	 */
	private int[] bestClusterForRow;

	/**
	 * every position j, keeps the cluster, in which a deletion/insertion of the
	 * column j would result in a maximal scoreReduction (best gain)
	 */
	private int[] bestClusterForCol;

	/**
	 * keeps the value of the average Residue of the currently best cluster
	 * alignment
	 */
	private double bestClustering;

	/**
	 * is set to false if the action with regard to the specified row hasn't
	 * been performed
	 */
	private BitSet rowActionPerformed;

	/**
	 * is set to false if the action with regard to the specified column hasn't
	 * been performed
	 */
	private BitSet colActionPerformed;

	/**
	 * keeps the number of rows in the database
	 */
	private int rowDim;

	/**
	 * keeps the number of columns in the database
	 */
	private int colDim;

	/**
	 * parameter which indicates in which order actions should be performed if
	 * its value is 0 (default) a weighted random order is performed, abetting
	 * actions with greater gain, if its value is 1 a random order is performed
	 * else, for values greater then 1 the normal order of actions (1..rowDim
	 * +colDim) is performed
	 */
	private int actionOrder;

	/**
	 * keeps the gain for each row and column
	 */
	private Map<Integer, Double> gainMap;

	/**
	 * the maximal gain in the current iteration. Necessary only if a weighted
	 * order of actions is performed
	 */
	private double maxGain;

	/**
	 * the minimal gain in the current iteration. Necessary only if a weighted
	 * order of actions is performed
	 */
	private double minGain;

	/**
	 * value indicating a missing entry within the database
	 */
	private double missing;

	/**
	 * keeps the number of iterations
	 */
	private int iterations = 1;

	/**
	 * the number of biclusters to be found
	 */
	private int k;

	/**
	 * value within 0 and 1 indicating the percentage of the rowDimension of the
	 * database as dimension for the initial clusters. A small difference
	 * between the initial Dimension and the dimension of the resultBiclusters,
	 * contributes to a small number of iterations, and so to a better
	 * efficiency of the algorithm
	 */
	private double initialRowDim;

	/**
	 * value within 0 and 1 indicating the percentage of the columnDimension of
	 * the database as dimension for the initial clusters. A small difference
	 * between the initial Dimension and the dimension of the resultBiclusters,
	 * contributes to a small number of iterations, and so to a better
	 * efficiency of the algorithm
	 */
	private double initialColDim;

	/**
	 * keeps the columnMeans of the currently processed bicluster
	 */
	private List<Double> colMeans;

	/**
	 * keeps the rowMeans of the currently processed bicluster
	 */
	private List<Double> rowMeans;

	private List<Double>[] rowClusterMeans;
	private List<Double>[] colClusterMeans;
	private double[] biclusterMeans;
	private double[] valueHClusters;

	/**
	 * keeps the biclusterMeans of the currently processed bicluster
	 */
	private double biclusterMean;

	/**
	 * adds the parameter values
	 */
	public FLOC() {
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
	 * initiates the necessary structures for the following algorithm
	 */
	private void initaiteFLOC() {
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
	 * adjusts the rowMean to the different requests of the FLOC_Algorithm;
	 * excluding missing entries from the computation
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
	 * adjusts the columnMean to the different requests of the FLOC_Algorithm
	 * excludes missing entries from the computation
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
	 * adjusts the biclusterMean to the different requests of the FLOC_Algorithm
	 * excludes missing entries from the computation
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
	 * calculates the score of the current bicluster, according to the rows and
	 * columns set to true
	 * 
	 * @param rows
	 *            rows belonging to the currently processed bicluster
	 * @param cols
	 *            columns belonging to the currently processed bicluster
	 * @return score of the bicluster
	 */
	public double getValueH(BitSet rows, BitSet cols) {
		double h = 0;
		double value = 0;
		int volume = rows.cardinality() * cols.cardinality();
		int rM = -1;
		int cM = -1;
		for (int i = rows.nextSetBit(0); i>=0; i=rows.nextSetBit(i+1)) {
			rM++;
			cM = -1;
			for (int j = cols.nextSetBit(0); j>=0; j = cols.nextSetBit(j+1)) {
				cM++;
				double wert = valueAt(i, j);
				double residue;
				if (MISSING_PARAM.isSet() && wert == missing) {
					residue = 0;
					volume--;
				} else {
					double rowMean = rowMeans.get(rM);
					double columnMean = colMeans.get(cM);
					double biclusterM = biclusterMean;
					residue = mod(wert - rowMean - columnMean + biclusterM);
				}
				value = value + residue;
			}
		}
		h = value / (volume);
		return h;

	}

	/**
	 * reinitiates the rowMeans
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
	 * reinitiates the columnMeans
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
	 * creates the initial k biclusters at the beginning of the algorithm
	 */
	private void createRandomCluster() {
		for (int z = 0; z < k; z++) {
			currRows.clear();
			currCols.clear();
			int zaehler = 0;

			for (int i = r.nextInt(rowDim); zaehler < Math.ceil(initialRowDim
					* rowDim); i = r.nextInt(rowDim)) {
				if (!currRows.get(i)) {
					zaehler++;
				}
				currRows.set(i);
			}

			zaehler = 0;
			for (int j = r.nextInt(colDim); zaehler < Math.ceil(initialColDim
					* colDim); j = r.nextInt(colDim)) {
				if (!currCols.get(j)) {
					zaehler++;
				}
				currCols.set(j);
			}
			rowClusters[z] = (BitSet) currRows.clone();
			colClusters[z] = (BitSet) currCols.clone();
		}
	}

	/**
	 * calculates the gain obtained by removing/ inserting
	 * 
	 * @param row
	 *            in the currently processed rowCluster
	 * @param row
	 *            row to which the gain corresponds
	 * @return the gain of
	 * @param row
	 */
	private double rowGain(int cluster, int row) {
		currRows = (BitSet) rowClusters[cluster].clone();
		currCols = (BitSet) colClusters[cluster].clone();
		double oldHValue = valueHClusters[cluster];
		rowMeans = rowClusterMeans[cluster];
		colMeans = colClusterMeans[cluster];
		biclusterMean = biclusterMeans[cluster];
		if(currCols.cardinality()!=colMeans.size()){
			System.out.println();
		}
		// currRows.flip(row);
		// adjust rowMeans
		int zaehler = 0;
		for (int i = currRows.nextSetBit(0);; i = currRows.nextSetBit(i + 1)) {
			if (i < 0) {
				rowMeans.add(meanOfRow(row, currCols));
				break;
			}
			if (row < i) {
				rowMeans.add(zaehler, meanOfRow(row, currCols));
				break;
			}
			zaehler++;
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
	 * calculates the gain obtained by removing/ inserting
	 * 
	 * @param col
	 *            in the currently processed columnCluster
	 * @param col
	 *            column to which the gain corresponds
	 * @return the gain of
	 * @param col
	 */
	private double colGain(int cluster, int col) {
		double oldHValue = valueHClusters[cluster];
		// currCols.flip(col);
		rowMeans = rowClusterMeans[cluster];
		colMeans = colClusterMeans[cluster];
		biclusterMean = biclusterMeans[cluster];
		// adjust colMeans
		int zaehler = 0;
		for (int i = currCols.nextSetBit(0); ; i = currCols
				.nextSetBit(i + 1)) {
			if(i<0){
				colMeans.add(meanOfCol(currRows, col));
				break;
			}
			if (col < i) {
				colMeans.add(zaehler, meanOfCol(currRows, col));
				break;
			}
			zaehler++;
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
	 * identifies the cluster for the greatest gain
	 * 
	 * @param row
	 *            row to be removed/inserted in a cluster
	 * @return the position of the cluster in which a deletion/insertion of
	 * @param row
	 *            results in the greatest gain
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
	 * identifies the cluster for the greatest gain
	 * 
	 * @param col
	 *            column to be removed/inserted in a cluster
	 * @return the position of the cluster in which a deletion/insertion of
	 * @param col
	 *            results in the greatest gain
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
	 * maps the gains to their rows/ columns in the bicluster. Necessary only if
	 * a weighted order of action is selected
	 * 
	 * @param pos
	 *            the row/column with the specified gain
	 * @param gain
	 *            the gain of
	 * @param pos
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
	 * fills the array bestClusterForRow and bestClusterForCols with the
	 * clusterPosisions on every rowPosition (bestClusterForRow[i] indicates the
	 * cluster with the greatest gain for row i)
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
	 * performs all actions returned by bestActions() in the order specified by
	 * actionOrder. Keeps the clustering with the lowest averageResidue as
	 * initial clustering for the next iteration. If the averageResidue could
	 * not been reduced due to any action, the algorithm is finished and the
	 * clustering with the best average residue is returned
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
	 * allows a sequential order of actions only necessary if a normal order of
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
	 * allows a random order of actions only necessary if a random order of
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
	 * allows a weighted order of actions, abetting the actions with greater
	 * gain. Only necessary if a weighted random order of actions is selected
	 * (actionOrder == 0)
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
			// BitSet probabilitySet = new BitSet(100);
			// probabilitySet.set(0, (int) Math.round(p * 100));
			// if (probabilitySet.get(r.nextInt(100))) {
			if (r.nextDouble() <= p) {
				randomOrder[r1] = r2;
				randomOrder[r2] = r1;
				g--;
			}
		}
		return randomOrder;
	}

	/**
	 * calculates the average residue of all current clusters, after an action
	 * (row/column- deletion/insertion) has been performed on one of the
	 * clusters
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
	 * adds the embedded biclusters to the final result of this algorithm
	 */
	private void returnBiclusters() {
		for (int i = 0; i < k; i++) {
			Bicluster<V> bicluster = defineBicluster(rowClusters[i],
					colClusters[i]);
			addBiclusterToResult(bicluster);
		}
	}

	/**
	 * initiates this Algorithm
	 */
	@Override
	protected void biclustering() throws IllegalStateException {
		initaiteFLOC();
	}

	/**
	 * the description of this Algorithm
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

	/**
	 * returns the modulo of a double value
	 * 
	 * @param x
	 *            value whose modulo will be returned
	 * @return modulo of
	 * @param x
	 */
	private static double mod(double x) {
		if (x < 0) {
			return -x;
		}
		return x;
	}

}
