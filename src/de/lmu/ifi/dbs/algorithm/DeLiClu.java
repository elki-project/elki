package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.KNNJoinResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.DeLiCluTreeDatabase;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.rstar.deliclu.DeLiCluNode;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.*;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * DeLiClu provides the DeLiClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiClu<O extends RealVector, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> {

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

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
  private KNNJoin<O, D> knnJoin = new KNNJoin<O, D>();

  /**
   * The number of nodes of the DeLiCluTree.
   */
  private int numNodes;

  /**
   * Sets minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since DeliClu is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public DeLiClu() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  public void runInTime(Database<O> database) throws IllegalStateException {

    try {
      if (!(database instanceof DeLiCluTreeDatabase))
        throw new IllegalArgumentException("Database must be an instance of " +
                                           DeLiCluTreeDatabase.class.getName());

      if (!(getDistanceFunction() instanceof SpatialDistanceFunction))
        throw new IllegalArgumentException("Distance Function must be an instance of " +
                                           SpatialDistanceFunction.class.getName());

      DeLiCluTreeDatabase<O> db = (DeLiCluTreeDatabase<O>) database;
      SpatialDistanceFunction<O, D> distFunction = (SpatialDistanceFunction<O, D>) getDistanceFunction();
      // todo rausnehmen beim testen!
      numNodes = db.getIndex().numNodes();
      System.out.println("num nodes " + numNodes);

//      System.out.println(database.toString());
      // first do the knn-Join
      if (isVerbose()) {
        System.out.println("\nknnJoin... ");
      }
      knnJoin.run(database);
      KNNJoinResult<O, D> knns = (KNNJoinResult<O, D>) knnJoin.getResult();

      Progress progress = new Progress(database.size());
      int size = database.size();

      if (isVerbose()) {
        System.out.println("\nDeLiClu... ");
      }

      clusterOrder = new ClusterOrder<O, D>(database, getDistanceFunction());
      heap = new DefaultHeap<D, SpatialObjectPair>();

      // add start object to cluster order and (root, root) to priority queue
      Integer startID = getStartObject(db);
      clusterOrder.add(startID, null, distFunction.infiniteDistance());
      int numHandled = 1;
      db.setHandled(startID);
      Entry rootEntry = db.getRootEntry();
      SpatialObjectPair spatialObjectPair = new SpatialObjectPair(rootEntry, rootEntry, true);
      updateHeap(distFunction.nullDistance(), spatialObjectPair);

      while (numHandled != size) {
        HeapNode<D, SpatialObjectPair> pqNode = heap.getMinNode();

        // pair of nodes
        if (pqNode.getValue().isExpandable) {
          expandNodes(db, distFunction, pqNode.getValue(), knns);
        }

        // pair of objects
        else {
          SpatialObjectPair dataPair = pqNode.getValue();
          // set handled
          List<Entry> path = db.setHandled(dataPair.entry1.getID());
          if (path == null)
            throw new RuntimeException("snh: parent(" + dataPair.entry1.getID() + ") = null!!!");
          // add to cluster order
          clusterOrder.add(dataPair.entry1.getID(), dataPair.entry2.getID(), pqNode.getKey());
          numHandled++;
          // reinsert expanded leafs
          reinsertExpanded(distFunction, db, path, knns);

          if (isVerbose()) {
            progress.setProcessed(numHandled);
            System.out.print("\r" + progress.toString());
          }
        }
      }
      System.out.println("\nDeLiClu I/O = " + db.getIOAccess());
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("DeliClu", "Density-Based Hierarchical Clustering",
                           "Algorithm to find density-connected sets in a database based on the parameters " +
                           "minimumPoints.", "Unpublished");
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return knnJoin.setParameters(remainingParameters);
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = new AttributeSettings(this);
    attributeSettings.addSetting(MINPTS_P, Integer.toString(minpts));

    result.add(attributeSettings);
    return result;
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
   * @param database the database storing the objects
   * @return the id of the start object for the run method
   */
  private Integer getStartObject(DeLiCluTreeDatabase<O> database) {
    Iterator<Integer> it = database.iterator();
    if (! it.hasNext()) return null;
    else
      return it.next();
  }

  /**
   * Adds the specified entry with the specified key tp the heap.
   * If the entry's object is already in the heap, it will only be updated.
   *
   * @param reachability the reachability of the entry's object
   * @param pair         the entry to be added
   */
  private void updateHeap(D reachability, SpatialObjectPair pair) {
    Integer index = heap.getIndexOf(pair);

    // entry is already in the heap
    if (index != null) {
      if (! pair.isExpandable) {
        HeapNode<D, SpatialObjectPair> heapNode = heap.getNodeAt(index);
        int compare = heapNode.getKey().compareTo(reachability);
        if (compare < 0) return;
        if (compare == 0 && heapNode.getValue().entry2.getID() < pair.entry2.getID()) return;

        heapNode.setValue(pair);
        heapNode.setKey(reachability);
        heap.flowUp(index);
      }
    }

    // entry is not in the heap
    else {
      heap.addNode(new DefaultHeapNode<D, SpatialObjectPair>(reachability, pair));
    }
  }

  /**
   * Expands the spatial nodes of the specified pair.
   *
   * @param db           the database storing the objects
   * @param distFunction the spatial distance function of this algorithm
   * @param nodePair     the pair of nodes to be expanded
   * @param knns         the knn list
   */
  private void expandNodes(DeLiCluTreeDatabase db, SpatialDistanceFunction<O, D> distFunction,
                           SpatialObjectPair nodePair, KNNJoinResult<O, D> knns) {

    DeLiCluNode node1 = (DeLiCluNode) db.getNode(nodePair.entry1.getID());
    DeLiCluNode node2 = (DeLiCluNode) db.getNode(nodePair.entry2.getID());

    if (node1.isLeaf())
      expandLeafNodes(distFunction, node1, node2, knns);
    else
      expandDirNodes(distFunction, node1, node2);

    db.getIndex().setExpanded(nodePair.entry2, nodePair.entry1);
  }

  /**
   * Expands the specified directory nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param node1        the first node
   * @param node2        the second node
   */
  private void expandDirNodes(SpatialDistanceFunction<O, D> distFunction,
                              DeLiCluNode node1, DeLiCluNode node2) {

    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of unhandled - handled children of node1-node2 into pq
    for (int i = 0; i < numEntries_1; i++) {
      if (! node1.hasUnhandled(i)) continue;

      Entry entry1 = node1.getEntry(i);
      for (int j = 0; j < numEntries_2; j++) {
        if (! node2.hasHandled(j)) continue;

        Entry entry2 = node2.getEntry(j);
        D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());

        SpatialObjectPair nodePair = new SpatialObjectPair(entry1, entry2, true);
        updateHeap(distance, nodePair);
      }
    }
  }

  /**
   * Expands the specified directory nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param node1        the first node
   * @param node2        the second node
   * @param knns         the knn list
   */
  private void expandLeafNodes(SpatialDistanceFunction<O, D> distFunction,
                               DeLiCluNode node1, DeLiCluNode node2,
                               KNNJoinResult<O, D> knns) {

    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of unhandled - handled children of node1-node2 into pq
    for (int i = 0; i < numEntries_1; i++) {
      if (! node1.hasUnhandled(i)) continue;

      Entry entry1 = node1.getEntry(i);
      for (int j = 0; j < numEntries_2; j++) {
        if (! node2.hasHandled(j)) continue;

        Entry entry2 = node2.getEntry(j);

        D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
        D reach = Util.max(distance, knns.getKNNDistance(entry2.getID()));
        SpatialObjectPair dataPair = new SpatialObjectPair(entry1, entry2, false);
        updateHeap(reach, dataPair);
      }
    }

  }

  /**
   * Reinserts the objects of the already expanded nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param db           the database storing the objects
   * @param path         the path of the object inserted last
   * @param knns         the knn list
   */
  private void reinsertExpanded(SpatialDistanceFunction<O, D> distFunction,
                                DeLiCluTreeDatabase<O> db,
                                List<Entry> path,
                                KNNJoinResult<O, D> knns) {

    Entry rootEntry = path.remove(path.size() - 1);
    reinsertExpanded(distFunction, db, path, path.size() - 1, rootEntry, knns);
  }


  private void reinsertExpanded(SpatialDistanceFunction<O, D> distFunction,
                                DeLiCluTreeDatabase<O> db,
                                List<Entry> path,
                                int index,
                                Entry parentEntry, KNNJoinResult<O, D> knns) {

    DeLiCluNode parentNode = (DeLiCluNode) db.getNode(parentEntry.getID());
    Entry entry2 = path.get(index);

    if (entry2.isLeafEntry()) {
      for (int i = 0; i < parentNode.getNumEntries(); i++) {
        if (! parentNode.hasUnhandled(i)) continue;

        Entry entry1 = parentNode.getEntry(i);
        D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
        D reach = Util.max(distance, knns.getKNNDistance(entry2.getID()));
        SpatialObjectPair dataPair = new SpatialObjectPair(entry1, entry2, false);
        updateHeap(reach, dataPair);
      }
    }

    else {
      Set<Integer> expanded = db.getIndex().getExpanded(entry2);

      for (int i = 0; i < parentNode.getNumEntries(); i++) {
        Entry entry1 = parentNode.getEntry(i);

        // not yet expanded
        if (! expanded.contains(entry1.getID())) {
          SpatialObjectPair nodePair = new SpatialObjectPair(entry1, entry2, true);
          D distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
          updateHeap(distance, nodePair);
        }

        // already expanded
        else {
          reinsertExpanded(distFunction, db, path, index - 1, entry1, knns);
        }
      }
    }
  }

  /**
   * Encapsulates an entry in the cluster order.
   */
  public class SpatialObjectPair implements Identifiable, Serializable {
    /**
     * The first entry of this pair.
     */
    Entry entry1;

    /**
     * The second entry of this pair.
     */
    Entry entry2;

    /**
     * Indicates whether this pair is expandable or not.
     */
    boolean isExpandable;

    /**
     * Creates a new entry with the specified parameters.
     *
     * @param entry1       the first entry of this pair
     * @param entry2       the second entry of this pair
     * @param isExpandable if true, this pair is expandable (a pair of nodes),
     *                     otherwise this pair is not expandable (a pair of objects)
     */
    public SpatialObjectPair(Entry entry1, Entry entry2, boolean isExpandable) {
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
     * @return a negative integer, zero, or a positive integer as this
     *         object is less than, equal to, or greater than the specified
     *         object.
     */
    public int compareTo(Identifiable o) {
      SpatialObjectPair other = (SpatialObjectPair) o;

      if (this.entry1.getID() < other.entry1.getID())
        return -1;
      if (this.entry1.getID() > other.entry1.getID())
        return +1;

      if (this.entry2.getID() < other.entry2.getID())
        return -1;
      if (this.entry2.getID() > other.entry2.getID())
        return +1;
      return 0;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      if (! isExpandable) return entry1.getID() + " - " + entry2.getID();
      return "n_" + entry1.getID() + " - n_" + entry2.getID();
    }

    /**
     * Returns the unique id of this object.
     *
     * @return the unique id of this object
     */
    public Integer getID() {
      // data
      if (! isExpandable) {
        return entry1.getID() + (numNodes * numNodes);
      }

      // nodes
      else {
        return numNodes * (entry1.getID() - 1) + entry2.getID();
      }
    }

  }


}
