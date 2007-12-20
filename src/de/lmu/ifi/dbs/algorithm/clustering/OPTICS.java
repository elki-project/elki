package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusterOrder;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * OPTICS provides the OPTICS algorithm.
 *
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 * @author Elke Achtert
 */
public class OPTICS<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> {

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = DBSCAN.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = DBSCAN.EPSILON_D;

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = DBSCAN.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = DBSCAN.MINPTS_D;

  /**
   * Parameter epsilon.
   */
  private PatternParameter epsilonParameter;

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * Minimum points.
   */
  private int minpts;

  /**
   * Provides the result of the algorithm.
   */
  private ClusterOrder<O, D> clusterOrder;

  /**
   * Holds a set of processed ids.
   */
  private Set<Integer> processedIDs;

  /**
   * The priority queue for the algorithm.
   */
  private Heap<D, COEntry> heap;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes.
   */
  public OPTICS() {
    super();
    // epsilon
    epsilonParameter = new PatternParameter(EPSILON_P, EPSILON_D);
    optionHandler.put(epsilonParameter);

    // global constraint
    try {
      GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(epsilonParameter,
                                                                                  (ClassParameter<DistanceFunction<?,?>>) optionHandler.getOption(DISTANCE_FUNCTION_P));
      optionHandler.setGlobalParameterConstraint(gpc);
    }
    catch (UnusedParameterException e) {
      verbose("Could not instantiate global parameter constraint concerning parameter " + EPSILON_P + " and " + DISTANCE_FUNCTION_P
              + " because parameter " + DISTANCE_FUNCTION_P + " is not specified! " + e.getMessage());
    }

    // minpts
    optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  @Override
protected void runInTime(Database<O> database) {
    Progress progress = new Progress("Clustering", database.size());

    int size = database.size();
    processedIDs = new HashSet<Integer>(size);
    clusterOrder = new ClusterOrder<O, D>(database, getDistanceFunction());
    heap = new DefaultHeap<D, COEntry>();
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      Integer id = it.next();
      if (!processedIDs.contains(id)) {
        expandClusterOrder(database, id, progress);
      }
    }
    if (isVerbose())
    {
      verbose("");
    }
  }

  /**
   * OPTICS-function expandClusterOrder.
   *
   * @param database the database on which the algorithm is run
   * @param objectID the currently processed object
   * @param progress the progress object to actualize the current progess if the
   *                 algorithm
   */
  protected void expandClusterOrder(Database<O> database, Integer objectID, Progress progress) {

    clusterOrder.add(objectID, null, getDistanceFunction().infiniteDistance());
    processedIDs.add(objectID);

    if (isVerbose()) {
      progress.setProcessed(processedIDs.size());
      progress(progress);
    }

    List<QueryResult<D>> neighbours = database.rangeQuery(objectID, epsilon, getDistanceFunction());
    D coreDistance = neighbours.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbours.get(minpts - 1).getDistance();

    if (!getDistanceFunction().isInfiniteDistance(coreDistance)) {
      for (QueryResult<D> neighbour : neighbours) {
        if (processedIDs.contains(neighbour.getID())) {
          continue;
        }
        D reachability = Util.max(neighbour.getDistance(), coreDistance);
        updateHeap(reachability, new COEntry(neighbour.getID(), objectID));
      }

      while (!heap.isEmpty()) {
        final HeapNode<D, COEntry> pqNode = heap.getMinNode();
        COEntry current = pqNode.getValue();
        clusterOrder.add(current.objectID, current.predecessorID, pqNode.getKey());
        processedIDs.add(current.objectID);

        neighbours = database.rangeQuery(current.objectID, epsilon, getDistanceFunction());
        coreDistance = neighbours.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbours.get(minpts - 1)
            .getDistance();

        if (!getDistanceFunction().isInfiniteDistance(coreDistance)) {
          for (QueryResult<D> neighbour : neighbours) {
            if (processedIDs.contains(neighbour.getID())) {
              continue;
            }
            D distance = neighbour.getDistance();
            D reachability = Util.max(distance, coreDistance);
            updateHeap(reachability, new COEntry(neighbour.getID(), current.objectID));
          }
        }
        if (isVerbose()) {
          progress.setProcessed(processedIDs.size());
          progress(progress);
        }
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        "OPTICS",
        "Density-Based Hierarchical Clustering",
        "Algorithm to find density-connected sets in a database based on the parameters minimumPoints and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.",
        "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander: OPTICS: Ordering Points to Identify the Clustering Structure. In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)");
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon
    epsilon = optionHandler.getParameterValue(epsilonParameter);

    // minpts
    minpts = (Integer) optionHandler.getOptionValue(MINPTS_P);

    return remainingParameters;
  }

  /**
   * @see Algorithm#getResult()
   */
  public Result<O> getResult() {
    return clusterOrder;
  }

  /**
   * Adds the specified entry with the specified key tp the heap. If the
   * entry's object is already in the heap, it will only be updated.
   *
   * @param reachability the reachability of the entry's object
   * @param entry        the entry to be added
   */
  private void updateHeap(D reachability, COEntry entry) {
    Integer index = heap.getIndexOf(entry);
    // entry is already in the heap
    if (index != null) {
      HeapNode<D, COEntry> heapNode = heap.getNodeAt(index);
      int compare = heapNode.getKey().compareTo(reachability);
      if (compare < 0) {
        return;
      }
      if (compare == 0 && heapNode.getValue().predecessorID < entry.predecessorID) {
        return;
      }
      heapNode.setValue(entry);
      heapNode.setKey(reachability);
      heap.flowUp(index);
    }

    // entry is not in the heap
    else {
      heap.addNode(new DefaultHeapNode<D, COEntry>(reachability, entry));
    }
  }

  /**
   * Encapsulates an entry in the cluster order.
   */
  public class COEntry implements Identifiable<COEntry>, Serializable {
    /**
     * The id of the entry.
     */
    public Integer objectID;

    /**
     * The id of the entry's predecessor.
     */
    Integer predecessorID;

    /**
     * Creates a new entry with the specified parameters.
     *
     * @param objectID      the id of the entry
     * @param predecessorID the id of the entry's predecessor
     */
    public COEntry(Integer objectID, Integer predecessorID) {
      this.objectID = objectID;
      this.predecessorID = predecessorID;
    }

    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object. <p/>
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this
     *         object is less than, equal to, or greater than the specified
     *         object.
     */
    public int compareTo(Identifiable<COEntry> o) {
      COEntry other = (COEntry) o;
      if (this.objectID < other.objectID) {
        return -1;
      }
      if (this.objectID > other.objectID) {
        return +1;
      }
      if (this.predecessorID < other.predecessorID) {
        return -1;
      }
      if (this.predecessorID > other.predecessorID) {
        return +1;
      }
      return 0;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
      return objectID + " (" + predecessorID + ")";
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final COEntry coEntry = (COEntry) o;

      return objectID.equals(coEntry.objectID);
    }

    /**
     * Returns a hash code value for the object. This method is supported
     * for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     *
     * @return hash code value for the object
     */
    @Override
    public int hashCode() {
      return objectID.hashCode();
    }

    /**
     * Returns the unique id of this object.
     *
     * @return the unique id of this object
     */
    public Integer getID() {
      return objectID;
    }
  }
}