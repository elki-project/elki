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
import de.lmu.ifi.dbs.index.spatial.rtree.DeLiCluNode;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * DeLiClu provides the DeLiClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiClu<T extends RealVector> extends DistanceBasedAlgorithm<T> {
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
  private ClusterOrder<T> clusterOrder;

  /**
   * The priority queue for the algorithm.
   */
  private Heap<Distance, SpatialObjectPair> heap;

  /**
   * Holds the knnJoin algorithm.
   */
  private KNNJoin<T> knnJoin = new KNNJoin<T>();


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
  public void runInTime(Database<T> database) throws IllegalStateException {
    
    try {
      if (!(database instanceof DeLiCluTreeDatabase))
        throw new IllegalArgumentException("Database must be an instance of " +
                                           DeLiCluTreeDatabase.class.getName());

      if (!(getDistanceFunction() instanceof SpatialDistanceFunction))
        throw new IllegalArgumentException("Distance Function must be an instance of " +
                                           SpatialDistanceFunction.class.getName());

      // first do the knn-Join
      if (isVerbose()) {
        System.out.println("\nknnJoin... ");
      }
      knnJoin.run(database);
      KNNJoinResult<T> knns = (KNNJoinResult<T>) knnJoin.getResult();

      DeLiCluTreeDatabase<T> db = (DeLiCluTreeDatabase<T>) database;
      SpatialDistanceFunction<T> distFunction = (SpatialDistanceFunction<T>) getDistanceFunction();

      Progress progress = new Progress(database.size());
      int size = database.size();

      if (isVerbose()) {
        System.out.println("\nDeLiClu... ");
      }

      clusterOrder = new ClusterOrder<T>(database, getDistanceFunction());
      heap = new DefaultHeap<Distance, SpatialObjectPair>();

      // add start object to cluster order and (root, root) to priority queue
      Integer startID = getStartObject(db);
      clusterOrder.add(startID, null, distFunction.infiniteDistance());
      int numHandled = 1;
      db.setHandled(startID);

      Entry rootEntry = db.getRootEntry();
      SpatialObjectPair spatialObjectPair = new SpatialObjectPair(rootEntry, rootEntry, true);
      updateHeap(distFunction.nullDistance(), spatialObjectPair);

      while (numHandled != size) {
        HeapNode<Distance, SpatialObjectPair> pqNode = heap.getMinNode();

        // pair of nodes
        if (pqNode.getValue().isExpandable) {
//          System.out.println("expand " + pqNode + "(" + pqNode.getKey() + ")");
          SpatialObjectPair nodePair = pqNode.getValue();
          expandNodes(db, distFunction, nodePair, knns);
//          System.out.println("expanded " + nodePair.entry1.getID() + " - " + nodePair.entry2.getID());
          db.getIndex().setExpanded(nodePair.entry1.getID(), nodePair.entry2.getID());
        }

        // pair of objects
        else {
//          System.out.println("node " + pqNode + "   #=" + numHandled);
          SpatialObjectPair dataPair = pqNode.getValue();

          // set handled
          Integer parentID = db.setHandled(dataPair.entry1.getID());
          if (parentID == null)
            throw new RuntimeException("snh: parent(" + dataPair.entry1.getID() + ") = null!!!");

//          if (dataPair.entry1.getID() == 870) {
//            System.out.println("parent (870) "+ parentID);
//            System.out.println(heap.getNodeAt(0));
//          }
//          if (dataPair.entry1.getID() == 877) {
//            System.out.println("parent (877) "+ parentID);
//          }

          // add to cluster order
          clusterOrder.add(dataPair.entry1.getID(), dataPair.entry2.getID(), pqNode.getKey());
          numHandled++;
          // reinsert expanded leafs
          reinsertExpanded(distFunction, db, dataPair.entry1, parentID, knns);

          if (isVerbose()) {
            progress.setProcessed(numHandled);
            System.out.print("\r" + progress.toString());
          }
        }
      }
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
  public Result<T> getResult() {
    return clusterOrder;
  }

  /**
   * Returns the id of the start object for the run method.
   *
   * @param database the database storing the objects
   * @return the id of the start object for the run method
   */
  private Integer getStartObject(DeLiCluTreeDatabase database) {
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
   * @param entry        the entry to be added
   */
  private void updateHeap(Distance reachability, SpatialObjectPair entry) {
    Integer index = entry.isExpandable ? null : heap.getIndexOf(entry);
    // entry is already in the heap
    if (index != null) {
      HeapNode<Distance, SpatialObjectPair> heapNode = heap.getNodeAt(index);
      int compare = heapNode.getKey().compareTo(reachability);
      if (compare < 0) return;
      if (compare == 0 && heapNode.getValue().entry2.getID() < entry.entry2.getID()) return;

      heapNode.setValue(entry);
      heapNode.setKey(reachability);
      heap.flowUp(index);
    }

    // entry is not in the heap
    else {
      heap.addNode(new DefaultHeapNode<Distance, SpatialObjectPair>(reachability, entry));
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
  private void expandNodes(DeLiCluTreeDatabase db, SpatialDistanceFunction<T> distFunction,
                           SpatialObjectPair nodePair, KNNJoinResult<T> knns) {
    DeLiCluNode node1 = (DeLiCluNode) db.getNode(nodePair.entry1.getID());
    DeLiCluNode node2 = (DeLiCluNode) db.getNode(nodePair.entry2.getID());

    if (node1.isLeaf())
      expandLeafNodes(distFunction, node1, node2, knns);
    else
      expandDirNodes(distFunction, node1, node2);
  }

  /**
   * Expands the specified directory nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param node1        the first node
   * @param node2        the second node
   */
  private void expandDirNodes(SpatialDistanceFunction<T> distFunction,
                              DeLiCluNode node1, DeLiCluNode node2) {

    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of children of node1-node2 into pq
    for (int i = 0; i < numEntries_1; i++) {
      int start_j = node1.equals(node2) ? i : 0;
      Entry entry1 = null;

      for (int j = start_j; j < numEntries_2; j++) {
        // insert only not yet handled nodes
        if (node1.isHandled(i) && node2.isHandled(j))
          continue;

        if (entry1 == null) {
          entry1 = node1.getEntry(i);
        }

        Entry entry2 = node2.getEntry(j);
        Distance distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());

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
  private void expandLeafNodes(SpatialDistanceFunction<T> distFunction,
                               DeLiCluNode node1, DeLiCluNode node2,
                               KNNJoinResult<T> knns) {

    if (node1.areAllHandled() && node2.areAllHandled()) return;

//    System.out.println("expand Leaf Nodes " + node1 + ", " + node2);
    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of children of node1-node2 into pq
    for (int i = 0; i < numEntries_1; i++) {
      int start_j = node1.equals(node2) ? i : 0;
      Entry entry1 = null;

      for (int j = start_j; j < numEntries_2; j++) {
        // insert only pairs of unhandled - handled
        if (node1.isHandled(i) && node2.isHandled(j)) continue;
        if (node1.equals(node2) && i == j) continue;
        if (!node1.isHandled(i) && !node2.isHandled(j)) continue;

        // swap the nodes if necessary
        boolean swap = node1.isHandled(i) && !node2.isHandled(j);

        if (entry1 == null) entry1 = node1.getEntry(i);
        Entry entry2 = node2.getEntry(j);

        Distance distance = distFunction.distance(entry1.getMBR(), entry2.getMBR());
        Distance reach = swap ?
                         Util.max(distance, knns.getKNNDistance(entry1.getID())) :
                         Util.max(distance, knns.getKNNDistance(entry2.getID()));

        SpatialObjectPair dataPair = swap ?
                                     new SpatialObjectPair(entry2, entry1, false) :
                                     new SpatialObjectPair(entry1, entry2, false);

        updateHeap(reach, dataPair);
      }
    }

  }

  /**
   * Reinserts the data objects of the nodes which are already expanded with the
   * specified parent.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param db           the database storing the objects
   * @param pre          the new predecessor
   * @param parentID     the id of the predecessors parent
   * @param knns         the knn list
   */
  private void reinsertExpanded(SpatialDistanceFunction<T> distFunction, DeLiCluTreeDatabase<T> db,
                                Entry pre, Integer parentID, KNNJoinResult<T> knns) {

    List<Integer> expanded = db.getIndex().getExpanded(parentID);
//    if (pre.getID() == 870) {
//      System.out.println("expanded ("+parentID+") = " + expanded);
//      List<Integer> expanded1 = db.getIndex().getExpanded(1);
//      System.out.println("expanded(1) " + expanded1);
//
//      for (Integer id : expanded) {
//        DeLiCluNode node = (DeLiCluNode) db.getNode(id);
//        System.out.println("XXXX node "+node);
//        int numEntries = node.getNumEntries();
//        for (int i = 0; i < numEntries; i++) {
//          System.out.println(node.getEntry(i));
//        }
//      }

//    }

    for (Integer id : expanded) {
      DeLiCluNode node = (DeLiCluNode) db.getNode(id);

      int numEntries = node.getNumEntries();
      for (int i = 0; i < numEntries; i++) {
        if (! node.isHandled(i)) {
          Entry o = node.getEntry(i);
          Distance distance = distFunction.distance(o.getMBR(), pre.getMBR());
          Distance reach = Util.max(distance, knns.getKNNDistance(pre.getID()));

          SpatialObjectPair dataPair = new SpatialObjectPair(o, pre, false);
          updateHeap(reach, dataPair);
        }
      }
    }
  }


  /**
   * Encapsulates an entry in the cluster order.
   */
  public class SpatialObjectPair implements Comparable<SpatialObjectPair>, Serializable {
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
     * @param other the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this
     *         object is less than, equal to, or greater than the specified
     *         object.
     */
    public int compareTo(SpatialObjectPair other) {
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
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final SpatialObjectPair SpatialObjectPair = (SpatialObjectPair) o;

      return entry1.equals(SpatialObjectPair.entry1);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     *
     * @return hash code value for the object
     */
    public int hashCode() {
      if (isExpandable)
        return entry1.hashCode();
      else
        return -1;
    }
  }


}
