package de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.biclustering.Bicluster;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.IntegerTriple;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides a BiclusteringAlgorithm which finds a bicluster based on all of its
 * 2 &times 2 submatrices. If all 2 &times 2 submatrices of one m &times n
 * Matrix have a score &le; sigma, this m &times n -Matrix is considered to be a
 * bicluster. On this way it avoids replacing already found biclusters with
 * random values. The Algorithm finds biclusters with correlated values. It
 * finds more bicluster at one time, which may overlap.
 * 
 * @param <V>
 *            a certain subtype of RealVector - the data matrix is supposed to
 *            consist of rows where each row relates to an object of type V and
 *            the columns relate to the attribute values of these objects
 * 
 * @author Noemi Andor
 */
public class PClustering<V extends RealVector<V, Double>> extends
		AbstractBiclustering<V> {

	/**
	 * OptionID for the parameter {@link #NUMBER_COLS_PARAM}.
	 */
	public static final OptionID NUMBER_COLS_ID = OptionID.getOrCreateOptionID("PClustering.nc",
			"indicates the minimum columnsize of the resulting biclusters");

	
	/**
	 * Parameter to indicate the minimum columnsize of the resulting biclusters.
	 * <p>
	 * Key: {@code -PClustering.nc}
	 * </p>
	 */
	public final IntParameter NUMBER_COLS_PARAM = new IntParameter(NUMBER_COLS_ID,
			new GreaterEqualConstraint(1));
	
	/**
	 * OptionID for the parameter {@link #NUMBER_ROWS_PARAM}.
	 */
	public static final OptionID NUMBER_ROWS_ID = OptionID.getOrCreateOptionID("PClustering.nr",
			"indicates the minimum rowsize of the resulting biclusters");

	/**
	 * Parameter to indicate the minimum rowsize of the resulting biclusters.
	 * <p>
	 * Key: {@code -PClustering.nr}
	 * </p>
	 */
	public final IntParameter NUMBER_ROWS_PARAM = new IntParameter(NUMBER_ROWS_ID,
			new GreaterEqualConstraint(1));
	
	/**
	 * OptionID for the parameter {@link #SIGMA_PARAM}.
	 */
	public static final OptionID SIGMA_ID = OptionID.getOrCreateOptionID(
			"PClustering.sigma",
			"treshhold value to determine the maximal acceptable score of a bicluster");

	/**
	 * Threshold value to determine the maximal acceptable score of a bicluster.
	 * <p>
	 * Key: {@code -PClustering.sigma}
	 * </p>
	 */
	public final DoubleParameter SIGMA_PARAM = new DoubleParameter(SIGMA_ID,
			new GreaterEqualConstraint(0.0));

	/**
	 * Parameter which indicates how many columns a resulting bicluster should
	 * have at least.
	 */
	private int nc;

	/**
	 * Parameter which indicates how many rows a resulting bicluster should have
	 * at least.
	 */
	private int nr;

	/**
	 * Keeps the number of rows in the database.
	 */
	private int rowDim;

	/**
	 * Keeps the number of columns in the database.
	 */
	private int colDim;

	/**
	 * Threshold value to determine the maximal acceptable score of a bicluster
	 */
	private double sigma;

	/**
	 * Keeps all rows of the database as a BitSet. Rows belonging to the
	 * database are set to true.
	 */
	private BitSet rows;

	/**
	 * Keeps all columns of the database as a BitSet. Columns belonging to the
	 * database are set to true.
	 */
	private BitSet cols;

	/**
	 * Matches three integer-values with the corresponding columns, forming a
	 * rowMDS. All columns belonging to a MDS are sorted in ascending order and
	 * the difference between the greatest and the smallest value of such a MDS
	 * is less then sigma. The first two values of the key-array represent two
	 * rows. The third value enumerates the MDS, in case the same two rows form
	 * more then one MDS.
	 */
	private Map<IntegerTriple, BitSet> rowMDS;

	/**
	 * Matches three integer-values with the corresponding rows, forming a
	 * columnMDS. All rows belonging to a MDS are sorted in ascending order and
	 * the difference between the greatest and the smallest value of such a MDS
	 * is less then sigma. The first two values of the key-array represent two
	 * columns. The third value enumerates the MDS, in case the same two columns
	 * form more then one MDS.
	 */
	private Map<IntegerTriple, BitSet> colMDS;

	/**
	 * Keeps the rowMDSs after pruning, and joins row-keys mapped to the same
	 * columns in the same node. Following the edges of the tree, one reaches
	 * the node with the rows corresponding to the path.
	 */
	private BiclusteringTree tree;

	/**
	 * Keeps track of the row and column order after sorting the differenceArray
	 * of two rows/columns.
	 */
	private int[] enumeration;

	/**
	 * Keeps the difference between the values (attributes/objects) of two
	 * rows/columns.
	 */
	private double[] diffArray;

	/**
	 * Value indicating if a pruning has been performed in the current iteration
	 * step. True indicates that the pruning-step should be performed one more
	 * time.
	 */
	private boolean pruning;

	/**
	 * Keeps the set of rowClusters.
	 */
	private ArrayList<BitSet> rowClusters;

	/**
	 * Keeps the set of columnClusters.
	 */
	private ArrayList<BitSet> colClusters;

	/**
	 * Keeps the rows/columns to be pruned in the current pruning-step.
	 */
	private ArrayList<IntegerTriple> pruningKeys;

	/**
	 * Matches every row appearing in a columnMDS to the total number of
	 * occurrences of that row in the columnMDSs or every column appearing in a
	 * rowMDS to the total number of occurrences of that column in the rowMDSs
	 */
	private Map<Integer, Integer> clusterOccurrences;

	/**
	 * Matches every row appearing in a rowMDS to the total number of
	 * occurrences of that row in the rowMDSs or every column appearing in a
	 * columnMDS to the total number of occurrences of that column in the
	 * columnMDSs
	 */
	private Map<Integer, Integer> pairOccurrences;

	/**
	 * Adds the parameter values.
	 */
	public PClustering() {
		this.addOption(NUMBER_COLS_PARAM);
		this.addOption(NUMBER_ROWS_PARAM);
		this.addOption(SIGMA_PARAM);
	}

	/**
	 * Calls
	 * {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
	 * and sets additionally the parameters for nc, nr, and sigma
	 * 
	 * @see AbstractAlgorithm#setParameters(String[])
	 */
	@Override
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);
		nc = this.getParameterValue(NUMBER_COLS_PARAM);
		nr = this.getParameterValue(NUMBER_ROWS_PARAM);
		sigma = this.getParameterValue(SIGMA_PARAM);
		return remainingParameters;
	}

	/**
	 * Initiates the necessary structures for the following algorithm.
	 */
	private void initiatePClustering() {
		pruning = true;
		rowMDS = new HashMap<IntegerTriple, BitSet>();
		colMDS = new HashMap<IntegerTriple, BitSet>();
		rowDim = super.getRowDim();
		colDim = super.getColDim();
		rows = new BitSet();
		rows.set(0, rowDim);
		cols = new BitSet();
		cols.set(0, colDim);
		rowClusters = new ArrayList<BitSet>();
		colClusters = new ArrayList<BitSet>();
		mainAlgorithm();
	}

	/**
	 * Creates the columnMDSs for every columnpair of the dataset.
	 */
	private void setColMDS() {
		for (int a = cols.nextSetBit(0); a >= 0; a = cols.nextSetBit(a + 1)) {
			for (int b = cols.nextSetBit(a + 1); b >= 0; b = cols
					.nextSetBit(b + 1)) {
				ArrayList<BitSet> mds = pairCluster(a, b, rows, nr, false);
				int size = mds.size();
				for (int i = 0; i < size; i++) {
					IntegerTriple cols = new IntegerTriple(a, b, i);
					colMDS.put(cols, mds.get(i));
				}
			}
		}
	}

	/**
	 * Creates the rowMDSs for every rowpair of the dataset, not already pruned.
	 */
	private void setRowMDS() {
		for (int x = rows.nextSetBit(0); x >= 0; x = rows.nextSetBit(x + 1)) {
			for (int y = rows.nextSetBit(x + 1); y >= 0; y = rows
					.nextSetBit(y + 1)) {
				ArrayList<BitSet> mds = pairCluster(x, y, cols, nc, true);
				int size = mds.size();
				for (int i = 0; i < size; i++) {
					IntegerTriple rows = new IntegerTriple(x, y, i);
					rowMDS.put(rows, mds.get(i));
				}
			}
		}
	}

	/**
	 * Inserts the pruned rowMDSs into the tree.
	 */
	private void fillTree() {
		tree = new BiclusteringTree();
		Set<IntegerTriple> keys = rowMDS.keySet();
		Iterator<IntegerTriple> iterator = keys.iterator();
		for (int zaehler = 0; zaehler < keys.size(); zaehler++) {
			IntegerTriple iter = iterator.next();
			BitSet i = new BitSet();
			i.set(iter.getFirst());
			i.set(iter.getSecond());
			BitSet cols = rowMDS.get(iter);
			tree.insertTree(cols, i, cols.cardinality());
		}

	}

	/**
	 * Makes a post-order-traversal of the tree (left child &rarr; right child
	 * &rarr; root). For every node encountered by the post-order the
	 * pairCluster-method is applied on: the objects in this node and every
	 * column-pair of the edges leading from this node to the root. Every object
	 * not belonging to this pairCluster is deleted from the node. The remaining
	 * objects and the columns are added to the result set.
	 * 
	 * @param child
	 *            current child of the tree
	 */
	private void postOrder(BiclusteringTree child) {
		for (int i = 0; i < child.getChildCount(); i++)

			postOrder(child.getChildren().get(i));

		if (child.getNode().cardinality() > 0) {
			BitSet objects = child.getNode();
			BitSet columns = child.getEdgesToRoot();
			BitSet newObjects = (BitSet) objects.clone();
			for (int a = columns.nextSetBit(0); a >= 0; a = columns
					.nextSetBit(a + 1)) {
				for (int b = columns.nextSetBit(a + 1); b >= 0; b = columns
						.nextSetBit(b + 1)) {
					ArrayList<BitSet> c = pairCluster(a, b, newObjects, nr,
							false);
					newObjects = removeNotContainedElements(c, newObjects);

				}
			}
			if (child.getParent().getNode().cardinality() > 0
					&& child.getParent() != null) {
				child.getParent().setNode(objects);
			}
			if (newObjects.size() >= nr) {
				rowClusters.add(newObjects);
				colClusters.add(columns);
			}
		}
	}

	/**
	 * removes the objects not contained in c from oldObjects
	 * 
	 * @param c
	 *            current pairClusters
	 * @param oldObjects
	 *            objects contained in the treenode currently worked at
	 * @return oldObjects with eventually deleted values
	 */
	private BitSet removeNotContainedElements(ArrayList<BitSet> c,
			BitSet oldObjects) {
		BitSet objects = (BitSet) oldObjects.clone();
		int size = c.size();
		label: for (int o = objects.nextSetBit(0); o >= 0; o = objects
				.nextSetBit(o + 1)) {
			for (int i = 0; i < size; i++) {
				if (c.get(i).get(o)) {
					continue label;
				}
			}
			objects.clear(o);
		}
		return objects;
	}

	/**
	 * Calculates the difference between the rows/columns of columns/rows x and
	 * y
	 * 
	 * @param x
	 *            row if parameter cols is true, column otherwise
	 * @param y
	 *            row if parameter cols is true, column otherwise
	 * @param t
	 *            set of columns if cols is true, set of rows otherwise
	 * @param cols
	 *            indicates how the parameters x, y and t should be interpreted
	 * @return difference between the rows/columns of columns/rows x and y
	 */
	private double[] fillDiffArray(int x, int y, BitSet t, boolean cols) {
		double[] diffArray = new double[t.cardinality()];
		int counter = 0;
		if (cols) {
			for (int j = t.nextSetBit(0); j >= 0; j = t.nextSetBit(j + 1)) {
				diffArray[counter] = valueAt(x, j) - valueAt(y, j);
				counter++;
			}
		} else {
			for (int i = t.nextSetBit(0); i >= 0; i = t.nextSetBit(i + 1)) {
				diffArray[counter] = valueAt(i, x) - valueAt(i, y);
				counter++;
			}
		}
		return diffArray;
	}

	/**
	 * Prunes rows and columns before rowMDSs are built.
	 */
	private void firstPrune() {
		countOccurrences(colMDS);
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			Integer occurrence = clusterOccurrences.get(i);
			if (occurrence == null || occurrence < nr - 1) {
				Iterator<IntegerTriple> iterator = colMDS.keySet().iterator();
				for (int j = 0; j < colMDS.size(); j++) {
					colMDS.get(iterator.next()).clear(i);
				}
				rows.clear(i);
				rowDim--;
			}
		}
	}

	/**
	 * Matches every column/row of a key appearing in parameter mds to the total
	 * number of occurrences of that column/row in mds, setting pairOccurances.
	 * Matches every column/row appearing in mds to the total number of
	 * occurrences of that column/row in mds, setting clusterOccurances
	 * 
	 * @param mds
	 *            rowMDS or colMDS
	 */
	private void countOccurrences(Map<IntegerTriple, BitSet> mds) {
		clusterOccurrences = new HashMap<Integer, Integer>();

		pairOccurrences = new HashMap<Integer, Integer>();

		Set<IntegerTriple> colkeys = mds.keySet();
		Iterator<IntegerTriple> iterator = colkeys.iterator();
		for (int zaehler = 0; zaehler < colkeys.size(); zaehler++) {
			IntegerTriple iter = iterator.next();
			int newAttrValue = 1;
			if (pairOccurrences.containsKey(iter.getFirst())) {
				newAttrValue = pairOccurrences.get(iter.getFirst()) + 1;
			}
			int newAttrValue2 = 1;
			if (pairOccurrences.containsKey(iter.getSecond())) {
				newAttrValue2 = pairOccurrences.get(iter.getSecond()) + 1;
			}
			pairOccurrences.put(iter.getFirst(), newAttrValue);
			pairOccurrences.put(iter.getSecond(), newAttrValue2);

			BitSet objectSet = mds.get(iter);
			for (int i = objectSet.nextSetBit(0); i >= 0; i = objectSet
					.nextSetBit(i + 1)) {
				int newObjValue = 1;
				if (clusterOccurrences.containsKey(i)) {
					newObjValue = clusterOccurrences.get(i) + 1;
				}
				clusterOccurrences.put(i, newObjValue);
			}
		}
	}

	/**
	 * Creates the row- and columnMDSs, prunes them until no more pruning takes
	 * place, inserts the remaining rowMDS in the tree by calling fillTree().
	 * Makes the postOrderTraversal of the tree by calling method postOrder().
	 */
	private void mainAlgorithm() {
		setColMDS();
		firstPrune();
		setRowMDS();

		while (pruning) {
			// rowPruning
			pruning = false;
			pruningKeys = new ArrayList<IntegerTriple>();
			Set<IntegerTriple> keys = rowMDS.keySet();
			Iterator<IntegerTriple> iterator = keys.iterator();
			for (int zaehler = 0; zaehler < keys.size(); zaehler++) {
				IntegerTriple i = iterator.next();
				pruneRowMDS(i);
			}
			for (int i = 0; i < pruningKeys.size(); i++) {
				rowMDS.remove(pruningKeys.get(i));
			}

			// columnPruning
			pruningKeys = new ArrayList<IntegerTriple>();
			keys = colMDS.keySet();
			iterator = keys.iterator();
			for (int counter = 0; counter < keys.size(); counter++) {
				IntegerTriple i = iterator.next();
				pruneColMDS(i);
			}
			for (int i = 0; i < pruningKeys.size(); i++) {
				colMDS.remove(pruningKeys.get(i));
			}
		}
		fillTree();
		postOrder(tree);
	}

	/**
	 * Matches an array of three values with the corresponding columns/rows,
	 * forming a rowMDS/columnMDS. All columns/rows belonging to a MDS are
	 * sorted in ascending order. The difference between the greatest and the
	 * smallest value of such a MDS is less then sigma.
	 * 
	 * @param x
	 *            first row/column forming an row/column mds;
	 * @param y
	 *            second row/column forming an row/column mds;
	 * @param t
	 *            columns/rows containing potential candidates for the
	 *            row/column mds.
	 * @param min
	 *            minimum number of columns/rows a mds must contain. Is set to
	 *            nr if mds is a rowCluster, nc otherwise.
	 * @param rows
	 *            true if x and y are rows, false if they are columns.
	 * @return List of row- or columnMDSs.
	 */
	private ArrayList<BitSet> pairCluster(int x, int y, BitSet t, int min,
			boolean rows) {
		ArrayList<BitSet> resultSet = new ArrayList<BitSet>();
		BitSet result = null;
		diffArray = fillDiffArray(x, y, t, rows);
		resetEnumeration(t);
		enumeration = sortArray(diffArray, enumeration);
		int start = 0;
		int end = 1;
		boolean nEW = true;

		while (end < t.cardinality()) {
			double v = diffArray[end] - diffArray[start];
			if (Math.abs(v) <= sigma) {
				end++;
				nEW = true;
			} else {
				if (end - start >= min && nEW) {
					result = setBitSet(start, end);
					resultSet.add(result);
				}
				start++;
				nEW = false;
			}
		}
		if (end - start >= min && nEW) {
			result = setBitSet(start, end);
			resultSet.add(result);
		}
		return resultSet;
	}

	/**
	 * Prunes rowMDSs. A column a is removed from a rowMDS, if and only if, the
	 * two rows of the rowMDS appear in less then nc columnMDSs containing
	 * column a. If the removal of column a makes the cardinality of the MDS
	 * less then nc, the whole MDS is deleted.
	 * 
	 * @param rowkey
	 *            key of the rowMDS currently worked at
	 */
	private void pruneRowMDS(IntegerTriple rowkey) {
		int counter = 0;
		int x = rowkey.getFirst();
		int y = rowkey.getSecond();
		BitSet mdsRows = rowMDS.get(rowkey);// Columns assigned to rows "rowkey"
		for (int i = mdsRows.nextSetBit(0); i >= 0; i = mdsRows
				.nextSetBit(i + 1)) {
			Set<IntegerTriple> keys = colMDS.keySet();
			Iterator<IntegerTriple> iterator = keys.iterator();
			for (int zaehler = 0; zaehler < keys.size(); zaehler++) {
				IntegerTriple colkey = iterator.next();
				if (colkey.getFirst() == i || colkey.getSecond() == i) {
					BitSet mdsCols = colMDS.get(colkey);
					if (mdsCols.get(x) && mdsCols.get(y)) {
						counter++;
					}
					if (counter == nc - 1) {
						break;
					}
				}
			}
			if (counter < nc - 1) {
				mdsRows.clear(i);
				pruning = true;
				if (mdsRows.cardinality() < nc) {
					pruningKeys.add(rowkey);
					return;
				}
			}
		}

	}

	/**
	 * Prunes colMDSs. A row a is removed from a colMDS, if and only if, the
	 * two columns of the colMDS appear in less then nr rowMDSs containing
	 * row a. If the removal of row a makes the cardinality of the MDS
	 * less then nr, the whole MDS is deleted.
	 * 
	 * @param colkey
	 *            key of the colMDS currently worked at
	 */
	private void pruneColMDS(IntegerTriple colkey) {
		int counter = 0;
		int x = colkey.getFirst();
		int y = colkey.getSecond();
		BitSet mdsCols = colMDS.get(colkey);// Rows assigned to columns "colkey"
		for (int i = mdsCols.nextSetBit(0); i >= 0; i = mdsCols
				.nextSetBit(i + 1)) {
			Set<IntegerTriple> keys = rowMDS.keySet();
			Iterator<IntegerTriple> iterator = keys.iterator();
			for (int zaehler = 0; zaehler < keys.size(); zaehler++) {
				IntegerTriple rowkey = iterator.next();
				if (rowkey.getFirst() == i || rowkey.getSecond() == i) {
					BitSet mdsRows = rowMDS.get(rowkey);
					if (mdsRows.get(x) && mdsRows.get(y)) {
						counter++;
					}
					if (counter == nr - 1) {
						break;
					}
				}
			}
			if (counter < nr - 1) {
				mdsCols.clear(i);
				pruning = true;
				if (mdsCols.cardinality() < nr) {
					pruningKeys.add(colkey);
					return;
				}
			}
		}

	}

	/**
	 * resets the enumeration in ascending order of the values in t
	 * @param t
	 *            set containing rows/columns to be clustered
	 */
	private void resetEnumeration(BitSet t) {
		enumeration = new int[t.cardinality()];
		int counter = 0;
		for (int i = t.nextSetBit(0); i >= 0; i = t.nextSetBit(i + 1)) {
			enumeration[counter] = i;
			counter++;
		}

	}

	/**
	 * Sets the rows/columns forming a MDS into a BitSet.
	 * 
	 * @param start
	 *            marks the place within the enumeration, where the MDS starts
	 * @param end
	 *            marks the place within the enumeration, where the MDS ends
	 * @return rows/columns forming a BitSet
	 */
	private BitSet setBitSet(int start, int end) {
		BitSet bitset = new BitSet();
		for (int i = start; i < end; i++) {
			bitset.set(enumeration[i]);
		}
		return bitset;
	}

	/**
	 * Sets the differenceArray currently worked at to its ascending order
	 * according to the sorted form of parameter cluster, and switches the
	 * indices of enumeration to the sorted form of cluster.
	 * 
	 * @param cluster
	 *            cluster to be sorted
	 * @param enumeration
	 *            keeps the indices of cluster
	 * @return the sorted form of enumeration
	 */
	public int[] sortArray(double[] cluster, int[] enumeration) {
		int n = cluster.length;
		int first = (int) Math.floor((double) n / 2);
		int sec = (int) Math.ceil((double) n / 2);
		if (cluster.length > 1) {
			double[] cluster1 = new double[first];
			int[] enum1 = new int[first];
			for (int i = 0; i < first; i++) {
				cluster1[i] = cluster[i];
				enum1[i] = enumeration[i];
			}
			double[] cluster2 = new double[sec];
			int[] enum2 = new int[sec];
			for (int i = 0; i < sec; i++) {
				cluster2[i] = cluster[first + i];
				enum2[i] = enumeration[first + i];
			}
			diffArray = cluster1.clone();
			enum1 = sortArray(cluster1, enum1);
			cluster1 = diffArray.clone();
			diffArray = cluster2.clone();
			enum2 = sortArray(cluster2, enum2);
			cluster2 = diffArray.clone();
			double[] newCluster = new double[n];
			int[] newEnumeration = new int[n];
			int c1 = 0;
			int c2 = 0;
			int counter = 0;
			while (c1 < cluster1.length && c2 < cluster2.length) {
				if (cluster1[c1] <= cluster2[c2]) {
					newCluster[counter] = cluster1[c1];
					newEnumeration[counter] = enum1[c1];
					c1++;
				} else {
					newCluster[counter] = cluster2[c2];
					newEnumeration[counter] = enum2[c2];
					c2++;
				}
				counter++;
			}
			if (c1 >= cluster1.length) {
				while (c2 < cluster2.length) {
					newCluster[counter] = cluster2[c2];
					newEnumeration[counter] = enum2[c2];
					c2++;
					counter++;
				}
			} else if (c2 >= cluster2.length) {
				while (c1 < cluster1.length) {
					newCluster[counter] = cluster1[c1];
					newEnumeration[counter] = enum1[c1];
					c1++;
					counter++;
				}
			}
			diffArray = newCluster;
			enumeration = newEnumeration;
		}

		return enumeration;
	}

	/**
	 * Initiates this Algorithm and returns the found biclusters.
	 */
	@Override
	protected void biclustering() throws IllegalStateException {
		long t = System.currentTimeMillis();
		initiatePClustering();
		returnBiclusters();
		System.out.println(System.currentTimeMillis() - t);
	}

	/**
	 * Adds the embedded biclusters to the final result of this algorithm.
	 */
	private void returnBiclusters() {
		int size = rowClusters.size();
		for (int i = 0; i < size; i++) {
			Bicluster<V> bicluster = defineBicluster(rowClusters.get(i),
					colClusters.get(i));
			addBiclusterToResult(bicluster);
		}
	}

	/**
	 * The description of this Algorithm.
	 */
	public Description getDescription() {
		Description abs = new Description(
				"PClustering",
				"Clustering by Pattern Similarity in large Data Sets",
				"finding correlated values in a subset of rows and a subset of columns",
				"");
		return abs;
	}
}
