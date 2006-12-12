package de.lmu.ifi.dbs.algorithm.clustering;

import java.io.Serializable;
import java.util.*;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.KNNJoin;
import de.lmu.ifi.dbs.algorithm.result.KNNJoinResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusterOrder;
import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.IndexPathComponent;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.deliclu.DeLiCluEntry;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.deliclu.DeLiCluNode;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Identifiable;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * DeLiClu provides the DeLiClu algorithm.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiClu<O extends NumberVector, D extends Distance<D>> extends
		DistanceBasedAlgorithm<O, D> {

	/**
	 * Parameter minimum points.
	 */
	public static final String MINPTS_P = OPTICS.MINPTS_P;

	/**
	 * Description for parameter minimum points.
	 */
	public static final String MINPTS_D = OPTICS.MINPTS_D;

	/**
	 * Minimum points.
	 */
	private int minpts;

	/**
	 * Provides the result of the algorithm.
	 */
	private ClusterOrder<O, D> clusterOrder;

	/**
	 * The priority queue for the algorithm.
	 */
	private Heap<D, SpatialObjectPair> heap;

	/**
	 * Holds the knnJoin algorithm.
	 */
	private KNNJoin<O, D, DeLiCluNode, DeLiCluEntry> knnJoin = new KNNJoin<O, D, DeLiCluNode, DeLiCluEntry>();

	/**
	 * The number of nodes of the DeLiCluTree.
	 */
	private int numNodes;

	/**
	 * Sets minimum points to the optionhandler additionally to the parameters
	 * provided by super-classes.
	 */
	public DeLiClu() {
		super();
		optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P,MINPTS_D,new GreaterConstraint(0)));
	}

	/**
	 * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
	 */
	protected void runInTime(Database<O> database) throws IllegalStateException {
		if (!(database instanceof SpatialIndexDatabase)) {
			throw new IllegalArgumentException(
					"Database must be an instance of "
							+ SpatialIndexDatabase.class.getName());
		}
		SpatialIndexDatabase<O, DeLiCluNode, DeLiCluEntry> db = (SpatialIndexDatabase<O, DeLiCluNode, DeLiCluEntry>) database;

		if (!(db.getIndex() instanceof DeLiCluTree)) {
			throw new IllegalArgumentException("Index must be an instance of "
					+ DeLiCluTree.class.getName());
		}
		DeLiCluTree<O> index = (DeLiCluTree<O>) db.getIndex();

		if (!(getDistanceFunction() instanceof SpatialDistanceFunction)) {
			throw new IllegalArgumentException(
					"Distance Function must be an instance of "
							+ SpatialDistanceFunction.class.getName());
		}
		SpatialDistanceFunction<O, D> distFunction = (SpatialDistanceFunction<O, D>) getDistanceFunction();

		numNodes = index.numNodes();

		// first do the knn-Join
		if (isVerbose()) {
			verbose("\nknnJoin...");
		}
		knnJoin.run(database);
		KNNJoinResult<O, D> knns = (KNNJoinResult<O, D>) knnJoin.getResult();

		Progress progress = new Progress("Clustering", database.size());
		int size = database.size();

		if (isVerbose()) {
			verbose("\nDeLiClu...");
		}

		clusterOrder = new ClusterOrder<O, D>(database, getDistanceFunction());
		heap = new DefaultHeap<D, SpatialObjectPair>();

		// add start object to cluster order and (root, root) to priority queue
		Integer startID = getStartObject(db);
		clusterOrder.add(startID, null, distFunction.infiniteDistance());
		int numHandled = 1;
		index.setHandled(db.get(startID));
		SpatialEntry rootEntry = db.getRootEntry();
		SpatialObjectPair spatialObjectPair = new SpatialObjectPair(rootEntry,
				rootEntry, true);
		updateHeap(distFunction.nullDistance(), spatialObjectPair);

		while (numHandled != size) {
			HeapNode<D, SpatialObjectPair> pqNode = heap.getMinNode();

			// pair of nodes
			if (pqNode.getValue().isExpandable) {
				expandNodes(index, distFunction, pqNode.getValue(), knns);
			}

			// pair of objects
			else {
				SpatialObjectPair dataPair = pqNode.getValue();
				// set handled
				List<IndexPathComponent<DeLiCluEntry>> path = index
						.setHandled(db.get(dataPair.entry1.getID()));
				if (path == null)
					throw new RuntimeException("snh: parent("
							+ dataPair.entry1.getID() + ") = null!!!");
				// add to cluster order
				clusterOrder.add(dataPair.entry1.getID(), dataPair.entry2
						.getID(), pqNode.getKey());
				numHandled++;
				// reinsert expanded leafs
				reinsertExpanded(distFunction, index, path, knns);

				if (isVerbose()) {
					progress.setProcessed(numHandled);
					progress(new ProgressLogRecord(LogLevel.PROGRESS, Util
							.status(progress), progress.getTask(), progress
							.status()));
				}
			}
		}
	}

	/**
	 * @see Algorithm#getDescription()
	 */
	public Description getDescription() {
		return new Description(
				"DeliClu",
				"Density-Based Hierarchical Clustering",
				"Algorithm to find density-connected sets in a database based on the parameter "
						+ MINPTS_P,
				"Elke Achtert, Christian B\u00f6hm, Peer Kr\u00f6ger: DeLiClu: Boosting "
						+ "Robustness, Completeness, Usability, and Efficiency of Hierarchical Clustering "
						+ "by a Closest Pair Ranking, "
						+ "In Proc. 10th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD 2006), "
						+ "Singapore, 2006, pp. 119-128.");
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// minpts
		minpts = (Integer) optionHandler.getOptionValue(MINPTS_P);
		

		// knn join
		List<String> params = new ArrayList<String>(Arrays
				.asList(remainingParameters));
		params.add(OptionHandler.OPTION_PREFIX + KNNJoin.K_P);
		params.add(Integer.toString(minpts));
		remainingParameters = knnJoin.setParameters(params
				.toArray(new String[params.size()]));
		setParameters(args, remainingParameters);
		return remainingParameters;
	}

	/**
	 * Returns the parameter setting of this algorithm.
	 * 
	 * @return the parameter setting of this algorithm
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> attributeSettings = super
				.getAttributeSettings();

		AttributeSettings mySettings = attributeSettings.get(0);
		mySettings.addSetting(MINPTS_P, Integer.toString(minpts));

		attributeSettings.addAll(knnJoin.getAttributeSettings());
		return attributeSettings;
	}

	/**
	 * @see Algorithm#getResult()
	 */
	public Result<O> getResult() {
		return clusterOrder;
	}

	/**
	 * Returns the id of the start object for the run method.
	 * 
	 * @param database
	 *            the database storing the objects
	 * @return the id of the start object for the run method
	 */
	private Integer getStartObject(
			SpatialIndexDatabase<O, DeLiCluNode, DeLiCluEntry> database) {
		Iterator<Integer> it = database.iterator();
		if (!it.hasNext()) {
			return null;
		} else {
			return it.next();
		}
	}

	/**
	 * Adds the specified entry with the specified key tp the heap. If the
	 * entry's object is already in the heap, it will only be updated.
	 * 
	 * @param reachability
	 *            the reachability of the entry's object
	 * @param pair
	 *            the entry to be added
	 */
	private void updateHeap(D reachability, SpatialObjectPair pair) {
		Integer index = heap.getIndexOf(pair);

		// entry is already in the heap
		if (index != null) {
			if (!pair.isExpandable) {
				HeapNode<D, SpatialObjectPair> heapNode = heap.getNodeAt(index);
				int compare = heapNode.getKey().compareTo(reachability);
				if (compare < 0) {
					return;
				}
				if (compare == 0
						&& heapNode.getValue().entry2.getID() < pair.entry2
								.getID()) {
					return;
				}

				heapNode.setValue(pair);
				heapNode.setKey(reachability);
				heap.flowUp(index);
			}
		}

		// entry is not in the heap
		else {
			heap.addNode(new DefaultHeapNode<D, SpatialObjectPair>(
					reachability, pair));
		}
	}

	/**
	 * Expands the spatial nodes of the specified pair.
	 * 
	 * @param index
	 *            the index storing the objects
	 * @param distFunction
	 *            the spatial distance function of this algorithm
	 * @param nodePair
	 *            the pair of nodes to be expanded
	 * @param knns
	 *            the knn list
	 */
	private void expandNodes(DeLiCluTree<O> index,
			SpatialDistanceFunction<O, D> distFunction,
			SpatialObjectPair nodePair, KNNJoinResult<O, D> knns) {

		DeLiCluNode node1 = index.getNode(nodePair.entry1.getID());
		DeLiCluNode node2 = index.getNode(nodePair.entry2.getID());

		if (node1.isLeaf()) {
			expandLeafNodes(distFunction, node1, node2, knns);
		} else {
			expandDirNodes(distFunction, node1, node2);
		}

		index.setExpanded(nodePair.entry2, nodePair.entry1);
	}

	/**
	 * Expands the specified directory nodes.
	 * 
	 * @param distFunction
	 *            the spatial distance function of this algorithm
	 * @param node1
	 *            the first node
	 * @param node2
	 *            the second node
	 */
	private void expandDirNodes(SpatialDistanceFunction<O, D> distFunction,
			DeLiCluNode node1, DeLiCluNode node2) {

		int numEntries_1 = node1.getNumEntries();
		int numEntries_2 = node2.getNumEntries();

		// insert all combinations of unhandled - handled children of
		// node1-node2 into pq
		for (int i = 0; i < numEntries_1; i++) {
			DeLiCluEntry entry1 = node1.getEntry(i);
			if (!entry1.hasUnhandled()) {
				continue;
			}
			for (int j = 0; j < numEntries_2; j++) {
				DeLiCluEntry entry2 = node2.getEntry(j);

				if (!entry2.hasHandled()) {
					continue;
				}
				D distance = distFunction.distance(entry1.getMBR(), entry2
						.getMBR());

				SpatialObjectPair nodePair = new SpatialObjectPair(entry1,
						entry2, true);
				updateHeap(distance, nodePair);
			}
		}
	}

	/**
	 * Expands the specified directory nodes.
	 * 
	 * @param distFunction
	 *            the spatial distance function of this algorithm
	 * @param node1
	 *            the first node
	 * @param node2
	 *            the second node
	 * @param knns
	 *            the knn list
	 */
	private void expandLeafNodes(SpatialDistanceFunction<O, D> distFunction,
			DeLiCluNode node1, DeLiCluNode node2, KNNJoinResult<O, D> knns) {

		int numEntries_1 = node1.getNumEntries();
		int numEntries_2 = node2.getNumEntries();

		// insert all combinations of unhandled - handled children of
		// node1-node2 into pq
		for (int i = 0; i < numEntries_1; i++) {
			DeLiCluEntry entry1 = node1.getEntry(i);
			if (!entry1.hasUnhandled()) {
				continue;
			}
			for (int j = 0; j < numEntries_2; j++) {
				DeLiCluEntry entry2 = node2.getEntry(j);
				if (!entry2.hasHandled()) {
					continue;
				}

				D distance = distFunction.distance(entry1.getMBR(), entry2
						.getMBR());
				D reach = Util.max(distance, knns
						.getKNNDistance(entry2.getID()));
				SpatialObjectPair dataPair = new SpatialObjectPair(entry1,
						entry2, false);
				updateHeap(reach, dataPair);
			}
		}

	}

	/**
	 * Reinserts the objects of the already expanded nodes.
	 * 
	 * @param distFunction
	 *            the spatial distance function of this algorithm
	 * @param index
	 *            the index storing the objects
	 * @param path
	 *            the path of the object inserted last
	 * @param knns
	 *            the knn list
	 */
	private void reinsertExpanded(SpatialDistanceFunction<O, D> distFunction,
			DeLiCluTree<O> index, List<IndexPathComponent<DeLiCluEntry>> path,
			KNNJoinResult<O, D> knns) {

		SpatialEntry rootEntry = path.remove(0).getEntry();
		reinsertExpanded(distFunction, index, path, 0, rootEntry, knns);
	}

	private void reinsertExpanded(SpatialDistanceFunction<O, D> distFunction,
			DeLiCluTree<O> index, List<IndexPathComponent<DeLiCluEntry>> path,
			int pos, SpatialEntry parentEntry, KNNJoinResult<O, D> knns) {

		DeLiCluNode parentNode = index.getNode(parentEntry.getID());
		SpatialEntry entry2 = path.get(pos).getEntry();

		if (entry2.isLeafEntry()) {
			for (int i = 0; i < parentNode.getNumEntries(); i++) {
				DeLiCluEntry entry1 = parentNode.getEntry(i);
				if (entry1.hasHandled()) {
					continue;
				}
				D distance = distFunction.distance(entry1.getMBR(), entry2
						.getMBR());
				D reach = Util.max(distance, knns
						.getKNNDistance(entry2.getID()));
				SpatialObjectPair dataPair = new SpatialObjectPair(entry1,
						entry2, false);
				updateHeap(reach, dataPair);
			}
		}

		else {
			Set<Integer> expanded = index.getExpanded(entry2);
			for (int i = 0; i < parentNode.getNumEntries(); i++) {
				SpatialEntry entry1 = parentNode.getEntry(i);

				// not yet expanded
				if (!expanded.contains(entry1.getID())) {
					SpatialObjectPair nodePair = new SpatialObjectPair(entry1,
							entry2, true);
					D distance = distFunction.distance(entry1.getMBR(), entry2
							.getMBR());
					updateHeap(distance, nodePair);
				}

				// already expanded
				else {
					reinsertExpanded(distFunction, index, path, pos + 1,
							entry1, knns);
				}
			}
		}
	}

	/**
	 * Encapsulates an entry in the cluster order.
	 */
	public class SpatialObjectPair implements Identifiable<SpatialObjectPair>, Serializable {
		/**
		 * The first entry of this pair.
		 */
		SpatialEntry entry1;

		/**
		 * The second entry of this pair.
		 */
		SpatialEntry entry2;

		/**
		 * Indicates whether this pair is expandable or not.
		 */
		boolean isExpandable;

		/**
		 * Creates a new entry with the specified parameters.
		 * 
		 * @param entry1
		 *            the first entry of this pair
		 * @param entry2
		 *            the second entry of this pair
		 * @param isExpandable
		 *            if true, this pair is expandable (a pair of nodes),
		 *            otherwise this pair is not expandable (a pair of objects)
		 */
		public SpatialObjectPair(SpatialEntry entry1, SpatialEntry entry2,
				boolean isExpandable) {
			this.entry1 = entry1;
			this.entry2 = entry2;
			this.isExpandable = isExpandable;
		}

		/**
		 * Compares this object with the specified object for order. Returns a
		 * negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object. <p/>
		 * 
		 * @param o
		 *            the Object to be compared.
		 * @return a negative integer, zero, or a positive integer as this
		 *         object is less than, equal to, or greater than the specified
		 *         object.
		 */
		public int compareTo(Identifiable<SpatialObjectPair> o) {
			SpatialObjectPair other = (SpatialObjectPair) o;

			if (this.entry1.getID() < other.entry1.getID()) {
				return -1;
			}
			if (this.entry1.getID() > other.entry1.getID()) {
				return +1;
			}
			if (this.entry2.getID() < other.entry2.getID()) {
				return -1;
			}
			if (this.entry2.getID() > other.entry2.getID()) {
				return +1;
			}
			return 0;
		}

		/**
		 * Returns a string representation of the object.
		 * 
		 * @return a string representation of the object.
		 */
		public String toString() {
			if (!isExpandable) {
				return entry1.getID() + " - " + entry2.getID();
			}
			return "n_" + entry1.getID() + " - n_" + entry2.getID();
		}

		/**
		 * Returns the unique id of this object.
		 * 
		 * @return the unique id of this object
		 */
		public Integer getID() {
			// data
			if (!isExpandable) {
				return entry1.getID() + (numNodes * numNodes);
			}

			// nodes
			else {
				return numNodes * (entry1.getID() - 1) + entry2.getID();
			}
		}

	}

}