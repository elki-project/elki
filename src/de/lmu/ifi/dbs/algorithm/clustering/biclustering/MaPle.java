package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.algorithm.result.clustering.biclustering.Bicluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.IntegerTriple;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides a BiclusteringAlgorithm which finds a bicluster based on all of its
 * 2 x 2 submatrices. If all 2 x 2 submatrices of one m x n Matrix have a score <=
 * sigma, this m x n -Matrix is considered to be a bicluster. On this way it
 * avoids replacing already found biclusters with random values. The Algorithm
 * finds biclusters with correlated values. It finds more bicluster at one time,
 * which may overlap.
 * 
 * @param <V>
 *            a certain subtype of RealVector - the data matrix is supposed to
 *            consist of rows where each row relates to an object of type V and
 *            the columns relate to the attribute values of these objects
 * 
 * @author Noemi Andor
 */
public class MaPle<V extends RealVector<V, Double>> extends
		AbstractBiclustering<V> {

	/**
	 * Parameter to indicate the minimum columnsize of the resulting biclusters
	 * <p> Key: {@code -nc} </p>
	 */
	public static final IntParameter NUMBER_COLS = new IntParameter("nc",
			"indicates the minimum columnsize of the resulting biclusters",
			new GreaterEqualConstraint(1));

	/**
	 * Parameter to indicate the minimum rowsize of the resulting biclusters
	 * <p> Key: {@code -nr}</p>
	 */
	public static final IntParameter NUMBER_ROWS = new IntParameter("nr",
			"indicates the minimum rowsize of the resulting biclusters",
			new GreaterEqualConstraint(1));

	/**
	 * threshold value to determine the maximal acceptable score of a bicluster
	 * <p>Key: {@code -sigma} </p>
	 */
	public static final DoubleParameter SIGMA_PARAM = new DoubleParameter(
			"sigma",
			"treshhold value to determine the maximal acceptable score of a bicluster",
			new GreaterEqualConstraint(0.0));

	/**
	 * Keeps the set of rowClusters.
	 */
	private ArrayList<BitSet> rowClusters;

	/**
	 * Keeps the set of columnClusters.
	 */
	private ArrayList<BitSet> colClusters;

	/**
	 * keeps the number of rows in the database
	 */
	private int rowDim;

	/**
	 * keeps the number of columns in the database
	 */
	private int colDim;

	/**
	 * keeps all rows of the database as a Bitset. Rows belonging to the
	 * database are set to true.
	 */
	private BitSet rows;

	/**
	 * keeps all columns of the database as a Bitset. Columns belonging to the
	 * database are set to true.
	 */
	private BitSet cols;

	/**
	 * Parameter which indicates how many columns a resulting bicluster should
	 * have at least
	 */
	private int nc;

	/**
	 * Parameter which indicates how many rows a resulting bicluster should have
	 * at least
	 */
	private int nr;

	/**
	 * Matches three integer-values with the corresponding columns, forming
	 * a rowMDS. All columns belonging to a MDS are sorted in ascending order
	 * and the difference between the greatest and the smallest value of such a
	 * MDS is less then
	 * 
	 * @param sigma.
	 *            The first two values of the key-array represent two rows. The
	 *            third value enumerates the MDS, in case the same two rows form
	 *            more then one MDS.
	 */
	private Map<IntegerTriple, BitSet> rowMDS;

	/**
	 * Matches three integer-values with the corresponding rows, forming a
	 * columnMDS. All rows belonging to a MDS are sorted in ascending order and
	 * the difference between the greatest and the smallest value of such a MDS
	 * is less then
	 * 
	 * @param sigma.
	 *            The first two values of the key-array represent two columns.
	 *            The third value enumerates the MDS, in case the same two
	 *            columns form more then one MDS.
	 */
	private Map<IntegerTriple, BitSet> colMDS;

	/**
	 * Threshold value to determine the maximal acceptable score of a bicluster
	 */
	private double sigma;

	/**
	 * Keeps the difference between the values of two rows/columns. 
	 * Will be sorted within the pairCluster-Method.
	 */
	private double[] diffArray;
	
	private int zaehler = 0;
	
	/**
	 * Keeps track of the row and column order after sorting
	 * 
	 * @param diffArray
	 *            of two rows/columns
	 */
	private int[] enumeration;

	/**
	 * Keeps the columns in an ordered form, corresponding to their descending occurrence
	 * in @param colMDS and @param rowMDS
	 */
	private int[] attributeList;

	/**
	 * Matches every row appearing in a columnMDS to the total number of occurrences of 
	 * that row in the columnMDS or every column appearing in a rowMDS to the total number of 
	 * occurrences of that column in the rowMDS
	 */
	private Map<Integer, Integer> clusterOccurances;

	/**
	 * Matches every row appearing in a rowMDS to the total number of occurrences of 
	 * that row in the rowMDS or every column appearing in a columnMDS to the total number of 
	 * occurrences of that column in the columnMDS
	 */
	private Map<Integer, Integer> pairOccurances;

	/**
	 * Keeps the currently handled row maximal Cluster
	 */
	private ArrayList<BitSet> currMaxCluster;

	/**
	 * adds the parameter values
	 */
	public MaPle() {
		this.addOption(NUMBER_COLS);
		this.addOption(NUMBER_ROWS);
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
		nc = this.getParameterValue(NUMBER_COLS);
		nr = this.getParameterValue(NUMBER_ROWS);
		sigma = this.getParameterValue(SIGMA_PARAM);
		return remainingParameters;
	}

	/**
	 * initiates the necessary structures for the following algorithm
	 */
	private void initiateMaple() {
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
	 * Creates the columnMDSs for every column pair of the Dataset
	 */
	private void setColMDS() {
		resetEnumeration(rows);
		for (int a = 0; a < colDim - 1; a++) {
			for (int b = a + 1; b < colDim; b++) {
				ArrayList<BitSet> mds = pairCluster(a, b, rows, nr, false);
				int size = mds.size();
				for (int i = 0; i < size; i++) {
					IntegerTriple cols = new IntegerTriple(a, b, i);
					colMDS.put(cols, mds.get(i));
					if(mds.get(i).get(188)){
						this.zaehler++;
					}
					// }
				}
			}
		}
	}

	/**
	 * Creates the rowMDSs for every row pair of the dataset, not already pruned
	 */
	private void setRowMDS() {
		resetEnumeration(cols);
		for (int x = rows.nextSetBit(0); x >= 0; x = rows.nextSetBit(x + 1)) {
			for (int y = rows.nextSetBit(x + 1); y >= 0; y = rows
					.nextSetBit(y + 1)) {
				ArrayList<BitSet> mds = pairCluster(x, y, cols, nc, true);
				if (mds != null) {
					int size = mds.size();
					for (int i = 0; i < size; i++) {
						IntegerTriple rows = new IntegerTriple(x, y, i);
						rowMDS.put(rows, mds.get(i));
					}
				}
			}
		}
	}

	/**
	 * Sets @param attributeList which keeps the columns in an ordered form, corresponding to their 
	 * descending occurrence in @param colMDS and @param rowMDS
	 */
	private void makeAL() {
		// Making AttributeRanks
		Map<Integer, BitSet> attributeRanks = new HashMap<Integer, BitSet>();
		Set<IntegerTriple> keySet = colMDS.keySet();
		Iterator<IntegerTriple> iterator = keySet.iterator();
		for (int i = 0; i < keySet.size(); i++) {
			IntegerTriple key = iterator.next();
			BitSet newSet = (BitSet) colMDS.get(key).clone();

			BitSet oldSet = attributeRanks.get(key.getFirst());
			if (oldSet != null) {
				newSet.or(oldSet);
			}
			attributeRanks.put(key.getFirst(), (BitSet) newSet.clone());

			newSet = (BitSet) colMDS.get(key).clone();
			oldSet = attributeRanks.get(key.getSecond());
			if (oldSet != null) {
				newSet.or(oldSet);
			}
			attributeRanks.put(key.getSecond(), (BitSet) newSet.clone());
		}
		int attributeSize = attributeRanks.size();
		attributeList = new int[attributeSize];
		
		for (int i = 0; i < attributeSize; i++) {
			attributeList[i] = -1;
		}

		label: for (int i = cols.nextSetBit(0); i >= 0; i = cols
				.nextSetBit(i + 1)) {
			for (int j = 0; j < attributeRanks.size(); j++) {
				BitSet currentSet = attributeRanks.get(i);
				if (attributeList[j] == -1) {
					attributeList[j] = i;
					break;
				}
				int currentRank = currentSet.cardinality();
				int previousRank = attributeRanks.get(attributeList[j])
						.cardinality();
				if (currentRank > previousRank) {
					// verschieben aller einträge nach rechts
					int temp = attributeList[j];
					attributeList[j] = i;
					int place = j + 1;
					while (place < colDim) {
						int c = attributeList[place];
						attributeList[place] = temp;
						if (c == -1) {
							continue label;
						}
						temp = c;
						place++;
					}
				}
			}
		}
	}

	/**
	 * Finds the set of intersections of all columnMDSs containing each pair of attributes
	 * of @param attributes with attribute @param a
	 * @param rows         an intersection of all columnMDSs containing a pair of attributes of @param attributes
	 * @param attributes   each pair of columns of @param attributes forms a key to a possible @param colMDS
	 * @param a			   new candidate-column for a cluster
	 * @return 			   set of intersections of all columnMDSs
	 */
	private ArrayList<BitSet> findRowMaxCluster(BitSet rows, BitSet attributes,
			int a) {
		ArrayList<BitSet> rowsSet = new ArrayList<BitSet>();
		if (a < 0) {
			IntegerTriple mdsKey = new IntegerTriple();
			mdsKey.setFirst(attributes.nextSetBit(0));
			mdsKey.setSecond(attributes.nextSetBit(mdsKey.getFirst() + 1));
			boolean exists = true;
			int zaehler = 0;
			while (exists) {
				mdsKey.setLast(zaehler);
				if (colMDS.containsKey(mdsKey)) {
					rowsSet.add(colMDS.get(mdsKey));
					zaehler++;
				} else {
					exists = false;
				}
			}
		} else{//a>=0
			BitSet currRows = (BitSet) rows.clone();
			rowsSet = makeAllPermutations(currRows, attributes, a, rowsSet);
		}
		return rowsSet;
	}

	/**
	 * Finds the intersections for every permutation of MDSs of each attributePair of @param attributes with @param a
	 * @param oldRows 		an intersection of all columnMDSs containing a pair of attributes of @param attributes
	 * @param rows         an intersection of all columnMDSs containing a pair of attributes of @param attributes
	 * @param attributes   each pair of columns of @param attributes forms a key to a possible @param colMDS
	 * @param a			   new candidate-column for a cluster
	 * @return 			   set of intersections of all columnMDSs
	 */
	private ArrayList<BitSet> makeAllPermutations(BitSet oldRows,
			BitSet attributes, int a, ArrayList<BitSet> rowsSet) {
		IntegerTriple mdsKey = new IntegerTriple();
		int i = attributes.nextSetBit(0);
		if (i < 0) {

			rowsSet.add((BitSet) oldRows.clone());
			return rowsSet;
		}
		mdsKey.setFirst(Math.min(i, a));
		mdsKey.setSecond(Math.max(i, a));
		int var = 0;
		mdsKey.setLast(var);
		while (colMDS.containsKey(mdsKey)) {
			BitSet mds = colMDS.get(mdsKey);
			BitSet newRows = (BitSet) oldRows.clone();
			newRows.and(mds);
			BitSet newAttributes = (BitSet) attributes.clone();
			newAttributes.clear(i);
			makeAllPermutations(newRows, newAttributes, a, rowsSet);
			var++;
			mdsKey.setLast(var);
		}
		return rowsSet;
	}

	/**
	 * creates the row- and columnMDSs, prunes them, makes the attributeList and assemblies the clusters
	 */
	private void mainAlgorithm() {
		setColMDS();
		System.out.println(this.zaehler);
//		firstPrune();
		setRowMDS();
		prune();
		makeAL();
		for (int i = 0; i <= attributeList.length - nc + 1; i++) {
			for (int j = i + 1; j <= attributeList.length - nc + 2; j++) {
				BitSet cols = new BitSet();
				cols.set(attributeList[i]);
				cols.set(attributeList[j]);
				currMaxCluster = findRowMaxCluster(null, cols, -1);
				for (int key = 0; key < currMaxCluster.size(); key++) {
					BitSet rows = currMaxCluster.get(key);
					search(rows, (BitSet) cols.clone(), attributeList[j]);
				}
			}
		}
	}

	/**
	 * Finds every set of rows which forms a bicluster with @param cols and column @param neu
	 * @param rows    set of rows forming a bicluster with @param cols
	 * @param cols	  set of columns forming a bicluster with @param rows
	 * @param neu	  new candidate-column for a cluster
	 */
	private void search(BitSet rows, BitSet cols, int neu) {
		int max = -1;
		for (int i = 0; i < attributeList.length; i++) {
			if (attributeList[i] == neu) {
				max = i;
				break;
			}
		}
		// Stellt PD her
		ArrayList<Integer> possibleAttributes = new ArrayList<Integer>();
		for (int z = max + 1; z < attributeList.length; z++) {
			int attribute = attributeList[z];
			int pd = 0;
			for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
				for (int j = rows.nextSetBit(i + 1); j >= 0; j = rows
						.nextSetBit(j + 1)) {

					for (int zaehler = 0; true; zaehler++) {
						IntegerTriple key = new IntegerTriple(i, j, zaehler);
						BitSet mds = rowMDS.get(key);
						if (mds == null) {
							break;
						}
						if (mds.get(attribute)) {
							pd++;
							// break; // muss womöglich weg da alle mds
							// berücksichtigt werden müssen, nicht nur
							// max 1 pro key
						}
					}
				}
			}
			if (pd >= (nr * (nr - 1)) / 2) {
				possibleAttributes.add(attribute);
			}
		}

		// Pruning 1
		if (possibleAttributes.size() + cols.cardinality() < nc) {
			return;
		}

		// Pruning2
		BitSet commonAttributes = new BitSet();
		for (int pd = 0; pd < possibleAttributes.size(); pd++) {
			int a = possibleAttributes.get(pd);
			if (objMDSContainsAttr(rows, a)) {
				commonAttributes.set(a);
			}
		}

		// Pruning 3
		BitSet validation = new BitSet();
		for (int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
			for (int j = 0; j < attributeList.length; j++) {
				if (i == attributeList[j]) {
					validation.set(j);
				}
			}
		}
		for (int i = 0; i < validation.cardinality(); i++) {
			if (!validation.get(i)) {
				if (objMDSContainsAttr(rows, attributeList[i])) {
					return;
				}
			}
		}

		// finding rowMaximalPClusters
		for (int i = 0; i < possibleAttributes.size(); i++) {
			int newAttribute = possibleAttributes.get(i);
			// if this possible attribute is a common attribute, it is extracted
			// directly
			if (commonAttributes.get(newAttribute)) {
				cols.set(newAttribute);
				continue;
				// search(rows, cols, newAttribute);
			}
			ArrayList<BitSet> rowMaxClusters = findRowMaxCluster(rows, cols,
					newAttribute);
			for (int j = 0; j < rowMaxClusters.size(); j++) {
				BitSet rMC = rowMaxClusters.get(j);
				BitSet newCols = (BitSet) cols.clone();
				newCols.set(newAttribute);
				search(rMC, newCols, newAttribute);
			}
		}

		if (cols.cardinality() >= nc && rows.cardinality() >= nr) {
			for (int i = 0; i < rowClusters.size(); i++) {
				if (isSubcluster(rows, rowClusters.get(i))
						&& isSubcluster(cols, colClusters.get(i))) {
					return;
				}
			}
		} else {
			return;
		}
		rowClusters.add(rows);
		colClusters.add(cols);
	}

	/**
	 * Checks if column @param a appears in every rowMDS with keys contained in @param rows
	 * @param rows		set of rows 
	 * @param a		    a column of the dataset
	 * @return		    true if @param a appears in every rowMDS built by pairs of @param rows, false otherwise
	 */
	private boolean objMDSContainsAttr(BitSet rows, int a) {
		boolean contains = false;
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			for (int j = rows.nextSetBit(i + 1); j >= 0; j = rows
					.nextSetBit(j + 1)) {
				contains = false;
				for (int zaehler = 0; true; zaehler++) {
					IntegerTriple key = new IntegerTriple(i, j, zaehler);
					BitSet currCols = rowMDS.get(key);
					if (currCols == null) {
						break;
					}
					if (currCols.get(a)) {
						contains = true;
						break;
					}
				}
				if (!contains) {
					return false;
				}
			}
		}
		return contains;
	}

	/**
	 * Matches every column/row of a key appearing in @param mds to the total number of occurrences of 
	 * that column/row in @param mds, setting pairOccurances
	 * Matches every column/row appearing in @param mds to the total number of occurrences of 
	 * that column/row in @param mds, setting clusterOccurances
	 * @param mds  rowMDS or colMDS
	 */
	private void countOccurances(Map<IntegerTriple, BitSet> mds) {
		clusterOccurances = new HashMap<Integer, Integer>();

		pairOccurances = new HashMap<Integer, Integer>();

		Set<IntegerTriple> colkeys = mds.keySet();
		Iterator<IntegerTriple> iterator = colkeys.iterator();
		for (int zaehler = 0; zaehler < colkeys.size(); zaehler++) {
			IntegerTriple iter = iterator.next();
			int newAttrValue = 1;
			if (pairOccurances.containsKey(iter.getFirst())) {
				newAttrValue = pairOccurances.get(iter.getFirst()) + 1;
			}
			int newAttrValue2 = 1;
			if (pairOccurances.containsKey(iter.getSecond())) {
				newAttrValue2 = pairOccurances.get(iter.getSecond()) + 1;
			}
			pairOccurances.put(iter.getFirst(), newAttrValue);
			pairOccurances.put(iter.getSecond(), newAttrValue2);

			BitSet objectSet = mds.get(iter);
			for (int i = objectSet.nextSetBit(0); i >= 0; i = objectSet
					.nextSetBit(i + 1)) {
				int newObjValue = 1;
				if (clusterOccurances.containsKey(i)) {
					newObjValue = clusterOccurances.get(i) + 1;
				}
				clusterOccurances.put(i, newObjValue);
			}
		}
	}

	/**
	 * Removes a row/column of all the rowMDSs and columnMDs containing it
	 * @param what		row/column to be removed
	 * @param where1	rowMDS or columnMDS
	 * @param where2	columnMDS or rowMDS
	 */
	private void removeFromMDS(int what, Map<IntegerTriple, BitSet> where1,
			Map<IntegerTriple, BitSet> where2) {
		Iterator<IntegerTriple> iterator = where1.keySet().iterator();
		ArrayList<IntegerTriple> toremove = new ArrayList<IntegerTriple>();
		for (int i = 0; i < where1.size(); i++) {
			IntegerTriple key = iterator.next();
			if (key.getFirst() == what || key.getSecond() == what) {
				toremove.add(key);
			}
		}
		for (int i = 0; i < toremove.size(); i++) {
			where1.remove(toremove.get(i));
		}

		iterator = where2.keySet().iterator();
		for (int i = 0; i < where2.size(); i++) {
			where2.get(iterator.next()).clear(what);
		}
	}

	/**
	 * Removes all MDSs containing less then @param min rows/columns from the column/row-MDSs
	 * @param mds row/column-MDS
	 * @param min minimal number of rows/columns which a bicluster must contain
	 */
	private void removeMDS(Map<IntegerTriple, BitSet> mds, int min) {
		ArrayList<IntegerTriple> toRemove = new ArrayList<IntegerTriple>();
		Iterator<IntegerTriple> iterator = mds.keySet().iterator();
		for (int i = 0; i < mds.size(); i++) {
			IntegerTriple key = iterator.next();
			if (mds.get(key).cardinality() < min) {
				toRemove.add(key);
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			mds.remove(toRemove.get(i));
		}
	}

	/**
	 * Prunes rows and columns before rowMDSs are built
	 */
	private void firstPrune() {
		countOccurances(colMDS);
		System.out.println(clusterOccurances.get(188));
		ArrayList<IntegerTriple> toremove = new ArrayList<IntegerTriple>();
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			Integer occurance = clusterOccurances.get(i);
			if (occurance == null || occurance < (nc * (nc - 1)) / 2) {
				Iterator<IntegerTriple>  iterator = colMDS.keySet().iterator();
				for (int j = 0; j < colMDS.size(); j++) {
					IntegerTriple iter = iterator.next();
					BitSet mds = colMDS.get(iter);
					mds.clear(i);
					if(mds.cardinality()<nc){
						toremove.add(iter);
					}
				}
				rows.clear(i);
				rowDim--;
			}
		}
		
		for (int i = 0; i < toremove.size(); i++) {
			colMDS.remove(toremove.get(i));
		}

		toremove = new ArrayList<IntegerTriple>();
		for (int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
			Integer occurance = pairOccurances.get(i);
			if (occurance == null || occurance < nc - 1) {
				cols.clear(i);
				colDim--;// variable ist womöglich nicht nötig
				Iterator<IntegerTriple> iterator = colMDS.keySet().iterator();
				for (int j = 0; j < colMDS.size(); j++) {
					IntegerTriple key = iterator.next();
					if (key.getFirst() == i || key.getSecond() == i) {
						toremove.add(key);
					}
				}
			}
		}
		for (int i = 0; i < toremove.size(); i++) {
			colMDS.remove(toremove.get(i));
		}

	}

	/**
	 * Prunes rows and columns by deleting them from the row- and columnMDSs if their occurrence is too small
	 */
	private void prune() {

		boolean pruning = true;

		while (pruning) {
			pruning = false;
			countOccurances(colMDS);

			// Lemma 3.1
			// Attributes
			for (int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
				Integer occurance = pairOccurances.get(i);
				if (occurance == null || occurance < nc - 1) {
					removeFromMDS(i, colMDS, rowMDS);
					cols.clear(i);
					colDim--;
					pruning = true;
				}

			}
			// Objects
			for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
				Integer occurance = clusterOccurances.get(i);
				if (occurance == null || occurance < (nc * (nc - 1)) / 2) {
					removeFromMDS(i, rowMDS, colMDS);
					rows.clear(i);
					rowDim--;
					pruning = true;
				}
			}

			removeMDS(rowMDS, nc);

			countOccurances(rowMDS);

			// Lemma 3.1
			// Attributes
			for (int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
				Integer occurance = clusterOccurances.get(i);
				if (occurance == null || occurance < (nr * (nr - 1)) / 2) {
					removeFromMDS(i, colMDS, rowMDS);
					cols.clear(i);
					colDim--;
					pruning = true;
				}

			}
			// Objects
			for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
				Integer occurance = pairOccurances.get(i);
				if (occurance == null || occurance < nr - 1) {
					removeFromMDS(i, rowMDS, colMDS);
					rows.clear(i);
					rowDim--;
					pruning = true;
				}
			}
			removeMDS(colMDS, nr);
		}

	}

	/**
	 * calculates the difference between the rows/columns of columns/rows x and
	 * y
	 * 
	 * @param x	row if @param cols is true, column otherwise
	 * @param y row if @param cols is true, column otherwise
	 * @param t set of columns if @param cols is true, set of rows otherwise
	 * @param cols indicates how @param x,  @param y and @param t should be interpreted
	 * @return difference between the rows/columns of columns/rows x and y
	 */
	private double[] fillDiffArray(int x, int y, BitSet t, boolean cols) {
		double[] diffArray = new double[t.cardinality()];
		int zaehler = 0;
		if (cols) {
			for (int j = t.nextSetBit(0); j >= 0; j = t.nextSetBit(j + 1)) {
				diffArray[zaehler] = valueAt(x, j) - valueAt(y, j);
				zaehler++;
			}
		} else {
			for (int i = t.nextSetBit(0); i >= 0; i = t.nextSetBit(i + 1)) {
				diffArray[zaehler] = valueAt(i, x) - valueAt(i, y);
				zaehler++;
			}
		}
		return diffArray;
	}

	/**
	 * Matches an array of three values with the corresponding columns/rows,
	 * forming a rowMDS/columnMDS. All columns/rows belonging to a MDS are
	 * sorted in ascending order. The difference between the greatest and the
	 * smallest value of such a MDS is less then @param sigma.
	 * @param x first row/column forming an row/column mds;
	 * @param y second row/column forming an row/column mds;
	 * @param t columns/rows containing potential candidates for the row/column mds
	 * @param min minimum number of columns/rows a mds must contain
	 * @param rows true if the mds is a rowMDS, false otherwise
	 * @return List of row- or columnMDSs.
	 */
	private ArrayList<BitSet> pairCluster(int x, int y, BitSet t, int anzahl,
			boolean cols) {
		int[] enumeration = this.enumeration.clone();
		ArrayList<BitSet> resultSet = new ArrayList<BitSet>();
		BitSet result = null;
		diffArray = fillDiffArray(x, y, t, cols);
		enumeration = sortArray(diffArray, enumeration);
		int start = 0;
		int end = 1;
		boolean neu = true;

		while (end < t.cardinality()) {
			double v = diffArray[end] - diffArray[start];
			if (Math.abs(v) <= sigma) {
				end++;
				neu = true;
			} else {
				if (end - start >= anzahl && neu) {
					result = setBitSet(start, end, enumeration);
					resultSet.add(result);
				}
				start++;
				neu = false;
			}
		}
		if (end - start >= anzahl && neu) {
			result = setBitSet(start, end, enumeration);
			resultSet.add(result);
		}
		return resultSet;
	}

	/**
	 * resets the enumeration in ascending order of the values of @param t
	 * @param t  set containing rows/columns to be clustered
	 */
	private void resetEnumeration(BitSet t) {
		enumeration = new int[t.cardinality()];
		int zaehler = 0;
		for (int i = t.nextSetBit(0); i >= 0; i = t.nextSetBit(i + 1)) {
			enumeration[zaehler] = i;
			zaehler++;
		}

	}

	private static boolean isSubcluster(BitSet a, BitSet b) {
		for (int i = a.nextSetBit(0); i >= 0; i = a.nextSetBit(i + 1)) {
			if (!b.get(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sorts @param diffArray in its ascending order and adjusts @param enumeration to the sorted 
	 * form of the cluster.
	 * @param cluster cluster to be sorted
	 * @param enumeration
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
			int zaehler = 0;
			while (c1 < cluster1.length && c2 < cluster2.length) {
				if (cluster1[c1] <= cluster2[c2]) {
					newCluster[zaehler] = cluster1[c1];
					newEnumeration[zaehler] = enum1[c1];
					c1++;
				} else {
					newCluster[zaehler] = cluster2[c2];
					newEnumeration[zaehler] = enum2[c2];
					c2++;
				}
				zaehler++;
			}
			if (c1 >= cluster1.length) {
				while (c2 < cluster2.length) {
					newCluster[zaehler] = cluster2[c2];
					newEnumeration[zaehler] = enum2[c2];
					c2++;
					zaehler++;
				}
			} else if (c2 >= cluster2.length) {
				while (c1 < cluster1.length) {
					newCluster[zaehler] = cluster1[c1];
					newEnumeration[zaehler] = enum1[c1];
					c1++;
					zaehler++;
				}
			}
			diffArray = newCluster;
			enumeration = newEnumeration;
		}

		return enumeration;
	}

	/**
	 * Sets the rows/columns forming a MDS into a BitSet
	 * @param start marks the place within the enumeration, where the MDS starts
	 * @param end  marks the place within the enumeration, where the MDS ends
	 * @return rows/columns forming a BitSet
	 */
	private BitSet setBitSet(int start, int end, int[] enumeration) {
		BitSet bitset = new BitSet();
		for (int i = start; i < end; i++) {
			bitset.set(enumeration[i]);
		}
		return bitset;
	}

	/**
	 * initiates this Algorithm and returns the found biclusters
	 */
	@Override
	protected void biclustering() throws IllegalStateException {
		initiateMaple();
		int size = rowClusters.size();
		for (int i = 0; i < size; i++) {
			Bicluster<V> bicluster = defineBicluster(rowClusters.get(i),
					colClusters.get(i));
			addBiclusterToResult(bicluster);
		}

	}

	/**
	 * the description of this Algorithm
	 */
	public Description getDescription() {
		Description abs = new Description(
				"MaPle",
				"A Fast Algorithm for Maximal Pattern-based Clustering",
				"finding correlated values in a subset of rows and a subset of columns",
				"");
		return abs;
	}

}
