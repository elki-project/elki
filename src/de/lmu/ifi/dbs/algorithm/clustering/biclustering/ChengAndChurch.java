package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.LongParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides a BiclusteringAlgorithm which deletes or inserts rows/columns
 * dependent on their score; and finds biclusters with correlated values
 * 
 * @param <V>
 *            a certain subtype of RealVector - the data matrix is supposed to
 *            consist of rows where each row relates to an object of type V and
 *            the columns relate to the attribute values of these objects
 * 
 * @author Noemi Andor
 */

public class ChengAndChurch<V extends RealVector<V, Double>> extends
		AbstractBiclustering<V> {

	public static final LongParameter SEED_PARAM = new LongParameter(
			"maskingSeed",
			"seed for initializing random list for the masking values");

	public static final DoubleParameter SIGMA_PARAM = new DoubleParameter(
			"sigma", "treshhold value to determine the maximal acceptable score of a bicluster", new GreaterEqualConstraint(0.0));
	

	public static final DoubleParameter ALPHA_PARAM = new DoubleParameter(
			"alpha", "parameter for multiple node deletion to accelerate the algorithm ", new GreaterEqualConstraint(1.0));
	
	public static final IntParameter N_PARAM = new IntParameter(
			"n", "number of biclusters to be found ", new GreaterEqualConstraint(1));
	
	public static final IntParameter BEGIN_PARAM = new IntParameter(
			"begin", "lower limit for maskingValues");
	
	public static final IntParameter END_PARAM = new IntParameter(
			"end", "upper limit for maskingValues");
	
	public static final IntParameter MISSING_PARAM = new IntParameter(
			"missing", "missing Value in database to be raplaced with maskingValues");
	
	static {
		SEED_PARAM.setDefaultValue(1L);
		SEED_PARAM.setOptional(true);
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
     * All rows belonging to previously found biclusters for masking
     */
	private BitSet rowsToMask;
	
	/**
     * All columns belonging to previously found biclusters for masking
     */
	private BitSet colsToMask;
	
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
     * Factor to uniquely define one value to one row and column in the data matrix. For example: factor = 100 => 512 means row 5 column 12
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
	 * information if some row/column has been removed from the current bicluster
	 */
	private boolean removed;

	/**
	 * information if some row/column has been added in the current bicluster
	 */
	private boolean added;
	
	/**
	 * specifies that the current state of the iteration is in Algorithm3
	 */
	private boolean inAlgorithm3;

	public ChengAndChurch() {
		this.addOption(SEED_PARAM);
		this.addOption(SIGMA_PARAM);
		this.addOption(ALPHA_PARAM);
		this.addOption(N_PARAM);
		this.addOption(MISSING_PARAM);
		this.addOption(BEGIN_PARAM);
		this.addOption(END_PARAM);
	}

	@Override
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);
		long seed = this.getParameterValue(SEED_PARAM);
		r = new Random(seed);
		sigma = this.getParameterValue(SIGMA_PARAM);
		alpha = this.getParameterValue(ALPHA_PARAM);
		n = this.getParameterValue(N_PARAM);
		begin = this.getParameterValue(BEGIN_PARAM);
		end = this.getParameterValue(END_PARAM);
		if(MISSING_PARAM.isSet())
		{
			missing = this.getParameterValue(MISSING_PARAM);
		}
		return remainingParameters;
	}

	public double getValueH() {
		double h = 0;
		double value = 0;
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
				double wert;
				if (inAlgorithm3 && invertedRows.get(i)) {
					wert = -valueAt(i, j);
				} else if (inAlgorithm3) {
					wert = valueAt(i, j);
				} else {
					wert = maskedValueAt(i, j);
				}
				value = value + meanSQR(wert, i, j);
			}
		}
		h = value / (rows.cardinality() * cols.cardinality());
		return h;

	}

	public double meanSQR(double wert, int i, int j) {
		double rowMean = (Double) rowMeans.get(i);
		double columnMean = (Double) columnMeans.get(j);
		double biclusterM = biclusterMean;
		double value = wert;

		double residue = Math.pow(value - rowMean - columnMean + biclusterM, 2);
		return residue;
	}

	protected void initiateRowMeans() {
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			rowMeans.put(i, meanOfRow(i, cols));
		}
		if (inAlgorithm3) {
			for (int i = rows.nextClearBit(0); i >= 0; i = rows
					.nextClearBit(i + 1)) {
				if (i >= rowDim) {
					break;
				}
				rowMeans.put(i, meanOfRow(i, cols));
			}
		}
	}

	protected void initiateColMeans() {
		for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
			columnMeans.put(j, meanOfCol(rows, j));
		}
		if (inAlgorithm3) {
			for (int j = cols.nextClearBit(0); j >= 0; j = cols
					.nextClearBit(j + 1)) {
				if (j >= colDim) {
					break;
				}
				columnMeans.put(j, meanOfRow(j, cols));
			}
		}
	}

	@Override
	protected double valueAt(int row, int col) {
		if (missingRowsToMask.get(row) && missingColsToMask.get(col)) {
			return maskedValueAt(row, col);
		} else {
			return super.valueAt(row, col);
		}
	}

	@Override
	protected double meanOfRow(int row, BitSet cols) {
		double sum = 0;
		if (inAlgorithm3 && invertedRows.get(row)) {
			for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
				sum += -valueAt(row, j);
			}
		} else if (inAlgorithm3 && !invertedRows.get(row)) {
			for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
				sum += valueAt(row, j);
			}
		} else {
			for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
				sum += maskedValueAt(row, j);
			}
		}
		return sum / cols.cardinality();
	}

	@Override
	protected double meanOfCol(BitSet rows, int col) {
		double sum = 0;
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			if (inAlgorithm3 && invertedRows.get(i)) {
				sum += -valueAt(i, col);
			} else if (inAlgorithm3 && !invertedRows.get(i)) {
				sum += valueAt(i, col);
			} else {
				sum += maskedValueAt(i, col);
			}
		}
		return sum / rows.cardinality();

	}

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

	protected void removeRow(int i) {
		rows.clear(i);
	}

	protected void removeColumn(int j) {
		cols.clear(j);
	}

	protected void recomputeValues() {
		initiateRowMeans();
		initiateColMeans();
		biclusterMean = meanOfBicluster(rows, cols);
		valueH = getValueH();
	}

	protected double maskedValueAt(int row, int col) {
		end = end + (0 - begin);
		if (rowsToMask.get(row) && colsToMask.get(col)) {
			return maskedVals.get(factor * row + col);
		} else {
			return valueAt(row, col);
		}
	}

	public Description getDescription() {
		Description abs = new Description("shortTitle", "longTitle",
				"purposeAndDescription", "reference");
		return abs;
	}

	public void biclustering() {
		initiateAlgorithm4();
	}

	private void createFactor() {
		int floor = (int)Math.floor(colDim / 10);
		int zaehler = 10;
		while (floor > 1) {
			floor = floor / 10;
			zaehler = zaehler * 10;
		}
		factor = zaehler;
	}

	// im folgenden: Algorithmus4

	public void initiateAlgorithm4() {
		this.rowDim = super.getRowDim();
		this.colDim = super.getColDim();
		createFactor();
		rowsToMask = new BitSet();
		colsToMask = new BitSet();
		missingRowsToMask = new BitSet();
		missingColsToMask = new BitSet();
		findMissingValues();
		createMaskingValues();
		applyAlgorithms();
	}

	private void findMissingValues() {
		if(!MISSING_PARAM.isSet())
		{
			return;
		}
		for (int i = 0; i < rowDim; i++) {
			for (int j = 0; j < colDim; j++) {
				if (super.valueAt(i, j) == missing) {
					rowsToMask.set(i);
					colsToMask.set(j);
					missingRowsToMask.set(i);
					missingColsToMask.set(j);
				}
			}
		}
	}

	private void maskMatrix() {
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			rowsToMask.set(i);
		}
		for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
			colsToMask.set(j);
		}
	}

	private void createMaskingValues() {
		end = end - begin;
		for (int i = rowsToMask.nextSetBit(0); i >= 0; i = rowsToMask
				.nextSetBit(i + 1)) {
			for (int j = colsToMask.nextSetBit(0); j >= 0; j = colsToMask
					.nextSetBit(j + 1)) {
				maskedVals.put(factor * i + j, (double) r.nextInt(end) + begin);
			}
		}
	}

	public void applyAlgorithms() {
		for (int i = 0; i < n; i++) {
			initiateAlgorithm2();
			initiateAlgorithm1();
			initiateAlgorithm3();
			if(isVerbose())
	        {
	            verbose("Score of bicluster" + (i+1) + ": " +valueH);
	            verbose("number of rows: " + rows.cardinality());
	            verbose("number of columns: " + cols.cardinality() + "\n");
	        }
			maskMatrix();
			createMaskingValues();
			//n--;
			addBiclusterToResult(defineBicluster(rows, cols));
		}
	}

	// im folgenden Algorithmus2:

	public void initiateAlgorithm2() {
		rows = new BitSet(rowDim - 1);
		cols = new BitSet(colDim - 1);
		rows.set(0, rowDim);
		cols.set(0, colDim);
		rowMeans = new LinkedHashMap<Integer, Double>();
		;
		columnMeans = new LinkedHashMap<Integer, Double>();
		;
		chooseMaxRAlgorithm2();
	}

	private void chooseMaxRAlgorithm2() {
		recomputeValues();
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
			}

			initiateColMeans();
			biclusterMean = meanOfBicluster(rows, cols);
			valueH = getValueH();

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

			if (removed) {
				chooseMaxRAlgorithm2();
			}
		}
	}

	// im Algorithmus1:

	public void initiateAlgorithm1() {
		chooseMaxRAlgorithm1();
	}

	private void chooseMaxRAlgorithm1() {
		recomputeValues();
		if (valueH > sigma) {
			int maxDelRow = -1;
			int maxDelColumn = -1;
			Map<Integer, Double> delRows = ReductionValuesRows();
			Map<Integer, Double> delCols = ReductionValuesCols();

			if (delRows.size() != 0 && delRows != null) {
				maxDelRow = chooseMax(delRows);
			}
			if (delCols.size() != 0 && delCols != null) {
				maxDelColumn = chooseMax(delCols);
			}

			if ((Double) delRows.get(maxDelRow) >= (Double) delCols
					.get(maxDelColumn)) {
				removeRow(maxDelRow);
			} else {
				removeColumn(maxDelColumn);
			}
		}
	}

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
		} else {
			throw new IllegalArgumentException(
					"the HashMap must contain at least one Element");
		}
	}

	// im folgenden Algorithmus3

	public void initiateAlgorithm3() {
		invertedRows.clear();
		chooseMinAdditions();
	}

	private void chooseMinAdditions() {
		inAlgorithm3 = true;
		recomputeValues();
		added = false;

		Map<Integer, Double> colAdds = ReductionValuesCols2();
		if (colAdds.size() != 0) {
			for (int j = cols.nextClearBit(0); j >= 0; j = cols
					.nextClearBit(j + 1)) {
				if (j >= colDim) {
					break;
				}
				if ((Double) colAdds.get(j) <= valueH) {
					added = true;
					addColumn(j);
				}
			}
			initiateRowMeans();
			biclusterMean = meanOfBicluster(rows, cols);
			valueH = getValueH();
		}

		Map<Integer, Double> rowAdds = ReductionValuesRows2();
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
		inAlgorithm3 = false;
		// if(added)
		// {
		// chooseMinAdditions();
		// }
	}

	private void addRow(int i) {
		rows.set(i);
	}

	private void addRowInv(int i) {
		rows.set(i);
		invertedRows.set(i);
	}

	private void addColumn(int j) {
		cols.set(j);
	}

	protected Map<Integer, Double> ReductionValuesRows2() {
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

	protected Map<Integer, Double> ReductionValuesCols2() {
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
				double rowMean = -(Double) rowMeans.get(i);
				double columnMean = (Double) columnMeans.get(j);
				double biclusterM = biclusterMean;

				temp = temp
						+ Math.pow(wert - rowMean - columnMean + biclusterM, 2);
			}
			ergRows.put(i, temp / cols.cardinality());
		}
		return ergRows;
	}

//	private BitSet temp() {
//		BitSet neuerSet = new BitSet();
//		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
//			neuerSet.set(rowDim - 1 - i);
//		}
//		System.out.println(neuerSet.cardinality());
//		return neuerSet;
//	}

}
