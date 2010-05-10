package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.KNNJoin;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * DeLiClu provides the DeLiClu algorithm, a hierarchical algorithm to find
 * density-connected sets in a database.
 * <p>
 * Reference: <br>
 * E. Achtert, C. Böhm, P. Kröger: DeLiClu: Boosting Robustness, Completeness,
 * Usability, and Efficiency of Hierarchical Clustering by a Closest Pair
 * Ranking. <br>
 * In Proc. 10th Pacific-Asia Conference on Knowledge Discovery and Data Mining
 * (PAKDD 2006), Singapore, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of FeatureVector handled by this Algorithm
 * @param <D> the type of Distance used
 */
@Title("DeliClu: Density-Based Hierarchical Clustering")
@Description("Hierachical algorithm to find density-connected sets in a database based on the parameter 'minpts'.")
@Reference(authors = "E. Achtert, C. Böhm, P. Kröger", title = "DeLiClu: Boosting Robustness, Completeness, Usability, and Efficiency of Hierarchical Clustering by a Closest Pair Ranking", booktitle = "Proc. 10th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD 2006), Singapore, 2006", url = "http://dx.doi.org/10.1007/11731139_16")
public class DeLiClu<O extends NumberVector<O, ?>, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, ClusterOrderResult<D>> {
  /**
   * OptionID for {@link #MINPTS_PARAM}
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("deliclu.minpts", "Threshold for minimum number of points within a cluster.");

  /**
   * Parameter to specify the threshold for minimum number of points within a
   * cluster, must be an integer greater than 0.
   * <p>
   * Key: {@code -deliclu.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

  /**
   * The priority queue for the algorithm.
   */
  private UpdatableHeap<SpatialObjectPair> heap;

  /**
   * Holds the knnJoin algorithm.
   */
  private KNNJoin<O, D, DeLiCluNode, DeLiCluEntry> knnJoin;

  /**
   * The number of nodes of the DeLiCluTree.
   */
  protected int numNodes;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DeLiClu(Parameterization config) {
    super(config);
    if(config.grab(MINPTS_PARAM)) {
      int minpts = MINPTS_PARAM.getValue();
      // knn join
      ListParameterization kNNJoinParameters = new ListParameterization();
      // parameter k
      kNNJoinParameters.addParameter(KNNJoin.K_ID, Integer.toString(minpts));
      // parameter distance function
      kNNJoinParameters.addParameter(KNNJoin.DISTANCE_FUNCTION_ID, getDistanceFunction());
      knnJoin = new KNNJoin<O, D, DeLiCluNode, DeLiCluEntry>(kNNJoinParameters);
      kNNJoinParameters.logAndClearReportedErrors();
    }
  }

  /**
   * Performs the DeLiClu algorithm on the given database.
   * 
   */
  @Override
  protected ClusterOrderResult<D> runInTime(Database<O> database) throws IllegalStateException {
    if(!(database instanceof SpatialIndexDatabase<?, ?, ?>)) {
      throw new IllegalArgumentException("Database must be an instance of " + SpatialIndexDatabase.class.getName());
    }
    SpatialIndexDatabase<O, DeLiCluNode, DeLiCluEntry> db = ClassGenericsUtil.castWithGenericsOrNull(SpatialIndexDatabase.class, database);

    if(!(db.getIndex() instanceof DeLiCluTree<?>)) {
      throw new IllegalArgumentException("Index must be an instance of " + DeLiCluTree.class.getName());
    }
    DeLiCluTree<O> index = (DeLiCluTree<O>) db.getIndex();

    if(!(getDistanceFunction() instanceof SpatialDistanceFunction<?, ?>)) {
      throw new IllegalArgumentException("Distance Function must be an instance of " + SpatialDistanceFunction.class.getName());
    }
    SpatialDistanceFunction<O, D> distFunction = (SpatialDistanceFunction<O, D>) getDistanceFunction();

    numNodes = index.numNodes();

    // first do the knn-Join
    if(logger.isVerbose()) {
      logger.verbose("knnJoin...");
    }
    AnnotationFromHashMap<KNNList<D>> knns = knnJoin.run(database);

    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("DeLiClu", database.size(), logger) : null;
    int size = database.size();

    ClusterOrderResult<D> clusterOrder = new ClusterOrderResult<D>();
    heap = new UpdatableHeap<SpatialObjectPair>();

    // add start object to cluster order and (root, root) to priority queue
    DBID startID = getStartObject(db);
    clusterOrder.add(startID, null, distFunction.infiniteDistance());
    int numHandled = 1;
    index.setHandled(db.get(startID));
    SpatialEntry rootEntry = db.getRootEntry();
    SpatialObjectPair spatialObjectPair = new SpatialObjectPair(distFunction.nullDistance(), rootEntry, rootEntry, true);
    heap.add(spatialObjectPair);

    while(numHandled < size) {
      if (heap.isEmpty()) {
        throw new AbortException("DeLiClu heap was empty when it shouldn't have been.");
      }
      SpatialObjectPair dataPair = heap.poll();

      // pair of nodes
      if(dataPair.isExpandable) {
        expandNodes(index, distFunction, dataPair, knns);
      }
      // pair of objects
      else {
        // set handled
        LeafEntry e1 = (LeafEntry) dataPair.entry1;
        LeafEntry e2 = (LeafEntry) dataPair.entry2;
        List<TreeIndexPathComponent<DeLiCluEntry>> path = index.setHandled(db.get(e1.getDBID()));
        if(path == null) {
          throw new RuntimeException("snh: parent(" + e1.getDBID() + ") = null!!!");
        }
        // add to cluster order
        clusterOrder.add(e1.getDBID(), e2.getDBID(), dataPair.distance);
        numHandled++;
        // reinsert expanded leafs
        reinsertExpanded(distFunction, index, path, knns);

        if(progress != null) {
          progress.setProcessed(numHandled, logger);
        }
      }
    }
    if (progress != null) {
      progress.ensureCompleted(logger);
    }
    return clusterOrder;
  }

  /**
   * Returns the id of the start object for the run method.
   * 
   * @param database the database storing the objects
   * @return the id of the start object for the run method
   */
  private DBID getStartObject(SpatialIndexDatabase<O, DeLiCluNode, DeLiCluEntry> database) {
    Iterator<DBID> it = database.iterator();
    if(!it.hasNext()) {
      return null;
    }
    else {
      return it.next();
    }
  }
  
  /**
   * Expands the spatial nodes of the specified pair.
   * 
   * @param index the index storing the objects
   * @param distFunction the spatial distance function of this algorithm
   * @param nodePair the pair of nodes to be expanded
   * @param knns the knn list
   */
  private void expandNodes(DeLiCluTree<O> index, SpatialDistanceFunction<O, D> distFunction, SpatialObjectPair nodePair, AnnotationResult<KNNList<D>> knns) {
    DeLiCluNode node1 = index.getNode(nodePair.entry1.getEntryID());
    DeLiCluNode node2 = index.getNode(nodePair.entry2.getEntryID());

    if(node1.isLeaf()) {
      expandLeafNodes(distFunction, node1, node2, knns);
    }
    else {
      expandDirNodes(distFunction, node1, node2);
    }

    index.setExpanded(nodePair.entry2, nodePair.entry1);
  }

  /**
   * Expands the specified directory nodes.
   * 
   * @param distFunction the spatial distance function of this algorithm
   * @param node1 the first node
   * @param node2 the second node
   */
  private void expandDirNodes(SpatialDistanceFunction<O, D> distFunction, DeLiCluNode node1, DeLiCluNode node2) {
    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of unhandled - handled children of
    // node1-node2 into pq
    for(int i = 0; i < numEntries_1; i++) {
      DeLiCluEntry entry1 = node1.getEntry(i);
      if(!entry1.hasUnhandled()) {
        continue;
      }
      for(int j = 0; j < numEntries_2; j++) {
        DeLiCluEntry entry2 = node2.getEntry(j);

        if(!entry2.hasHandled()) {
          continue;
        }
        D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());

        SpatialObjectPair nodePair = new SpatialObjectPair(distance, entry1, entry2, true);
        heap.add(nodePair);
      }
    }
  }

  /**
   * Expands the specified leaf nodes.
   * 
   * @param distFunction the spatial distance function of this algorithm
   * @param node1 the first node
   * @param node2 the second node
   * @param knns the knn list
   */
  private void expandLeafNodes(SpatialDistanceFunction<O, D> distFunction, DeLiCluNode node1, DeLiCluNode node2, AnnotationResult<KNNList<D>> knns) {
    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of unhandled - handled children of
    // node1-node2 into pq
    for(int i = 0; i < numEntries_1; i++) {
      DeLiCluEntry entry1 = node1.getEntry(i);
      if(!entry1.hasUnhandled()) {
        continue;
      }
      for(int j = 0; j < numEntries_2; j++) {
        DeLiCluEntry entry2 = node2.getEntry(j);
        if(!entry2.hasHandled()) {
          continue;
        }

        D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
        D reach = DistanceUtil.max(distance, knns.getValueFor(((LeafEntry)entry2).getDBID()).getKNNDistance());
        SpatialObjectPair dataPair = new SpatialObjectPair(reach, entry1, entry2, false);
        heap.add(dataPair);
      }
    }
  }

  /**
   * Reinserts the objects of the already expanded nodes.
   * 
   * @param distFunction the spatial distance function of this algorithm
   * @param index the index storing the objects
   * @param path the path of the object inserted last
   * @param knns the knn list
   */
  private void reinsertExpanded(SpatialDistanceFunction<O, D> distFunction, DeLiCluTree<O> index, List<TreeIndexPathComponent<DeLiCluEntry>> path, AnnotationResult<KNNList<D>> knns) {
    SpatialEntry rootEntry = path.remove(0).getEntry();
    reinsertExpanded(distFunction, index, path, 0, rootEntry, knns);
  }

  private void reinsertExpanded(SpatialDistanceFunction<O, D> distFunction, DeLiCluTree<O> index, List<TreeIndexPathComponent<DeLiCluEntry>> path, int pos, SpatialEntry parentEntry, AnnotationResult<KNNList<D>> knns) {
    DeLiCluNode parentNode = index.getNode(parentEntry.getEntryID());
    SpatialEntry entry2 = path.get(pos).getEntry();

    if(entry2.isLeafEntry()) {
      for(int i = 0; i < parentNode.getNumEntries(); i++) {
        DeLiCluEntry entry1 = parentNode.getEntry(i);
        if(entry1.hasHandled()) {
          continue;
        }
        D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
        D reach = DistanceUtil.max(distance, knns.getValueFor(((LeafEntry)entry2).getDBID()).getKNNDistance());
        SpatialObjectPair dataPair = new SpatialObjectPair(reach, entry1, entry2, false);
        heap.add(dataPair);
      }
    }
    else {
      Set<Integer> expanded = index.getExpanded(entry2);
      for(int i = 0; i < parentNode.getNumEntries(); i++) {
        SpatialEntry entry1 = parentNode.getEntry(i);

        // not yet expanded
        if(!expanded.contains(entry1.getEntryID())) {
          D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
          SpatialObjectPair nodePair = new SpatialObjectPair(distance, entry1, entry2, true);
          heap.add(nodePair);
        }

        // already expanded
        else {
          reinsertExpanded(distFunction, index, path, pos + 1, entry1, knns);
        }
      }
    }
  }

  /**
   * Encapsulates an entry in the cluster order.
   */
  public class SpatialObjectPair implements Comparable<SpatialObjectPair> {
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
     * The current distance.
     */
    D distance;

    /**
     * Creates a new entry with the specified parameters.
     * 
     * @param entry1 the first entry of this pair
     * @param entry2 the second entry of this pair
     * @param isExpandable if true, this pair is expandable (a pair of nodes),
     *        otherwise this pair is not expandable (a pair of objects)
     */
    public SpatialObjectPair(D distance, SpatialEntry entry1, SpatialEntry entry2, boolean isExpandable) {
      this.distance = distance;
      this.entry1 = entry1;
      this.entry2 = entry2;
      this.isExpandable = isExpandable;
    }

    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p/>
     * 
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(SpatialObjectPair other) {
      /*
      if(this.entry1.getEntryID().compareTo(other.entry1.getEntryID()) > 0) {
        return -1;
      }
      if(this.entry1.getEntryID().compareTo(other.entry1.getEntryID()) < 0) {
        return 1;
      }
      if(this.entry2.getEntryID().compareTo(other.entry2.getEntryID()) > 0) {
        return -1;
      }
      if(this.entry2.getEntryID().compareTo(other.entry2.getEntryID()) < 0) {
        return 1;
      }
      return 0;*/
      // FIXME: inverted?
      return this.distance.compareTo(other.distance);
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
      if(!isExpandable) {
        return entry1.getEntryID() + " - " + entry2.getEntryID();
      }
      return "n_" + entry1.getEntryID() + " - n_" + entry2.getEntryID();
    }

    /**
     * Returns the unique id of this object.
     * 
     * @return the unique id of this object
     */
    public Integer getID() {
      // data
      if(!isExpandable) {
        return entry1.getEntryID() + (numNodes * numNodes);
      }

      // nodes
      else {
        return numNodes * (entry1.getEntryID() - 1) + entry2.getEntryID();
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (!(SpatialObjectPair.class.isInstance(obj))) {
        return false;
      }
      SpatialObjectPair other = (SpatialObjectPair) obj;
      return this.entry1.equals(other.entry1) && this.entry2.equals(other.entry2);
    }

    @Override
    public int hashCode() {
      final long prime = 2654435761L;
      long result = 1;
      result = prime * result + ((entry1 == null) ? 0 : entry1.hashCode());
      result = prime * result + ((entry2 == null) ? 0 : entry2.hashCode());
      return (int) result;
    }
  }
}