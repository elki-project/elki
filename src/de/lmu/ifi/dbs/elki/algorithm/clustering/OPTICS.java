package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Identifiable;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.elki.utilities.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * OPTICS provides the OPTICS algorithm.
 * <p>
 * Reference: M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander: OPTICS:
 * Ordering Points to Identify the Clustering Structure. <br>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
public class OPTICS<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, ClusterOrderResult<D>> {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   * <p>
   * Key: {@code -optics.epsilon}
   * </p>
   */
  private final StringParameter EPSILON_PARAM = new StringParameter(EPSILON_ID);

  /**
   * Hold the value of {@link #EPSILON_PARAM}.
   */
  private String epsilon;

  /**
   * OptionID for {@link #MINPTS_PARAM}
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in " + "the epsilon-neighborhood of a point.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -optics.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #MINPTS_PARAM}.
   */
  private int minpts;

  /**
   * Provides the result of the algorithm.
   */
  private ClusterOrderResult<D> clusterOrder;

  /**
   * Holds a set of processed ids.
   */
  private Set<Integer> processedIDs;

  /**
   * The priority queue for the algorithm.
   */
  private Heap<D, COEntry> heap;

  /**
   * Provides the OPTICS algorithm, adding parameters {@link #EPSILON_PARAM} and
   * {@link #MINPTS_PARAM} to the option handler additionally to parameters of
   * super class.
   */
  public OPTICS(Parameterization config) {
    super(config);
    if(config.grab(this, EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }

    if(config.grab(this, MINPTS_PARAM)) {
      minpts = MINPTS_PARAM.getValue();
    }

    // global constraint epsilon <-> distance function
    GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint<DistanceFunction<O, D>>(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM);
    addGlobalParameterConstraint(gpc);
  }

  /**
   * Performs the OPTICS algorithm on the given database.
   * 
   */
  @Override
  protected ClusterOrderResult<D> runInTime(Database<O> database) {
    FiniteProgress progress = new FiniteProgress("Clustering", database.size());

    int size = database.size();
    processedIDs = new HashSet<Integer>(size);
    clusterOrder = new ClusterOrderResult<D>();
    heap = new DefaultHeap<D, COEntry>();
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    for(Integer id : database) {
      if(!processedIDs.contains(id)) {
        expandClusterOrder(database, id, progress);
      }
    }
    if(logger.isVerbose()) {
      logger.verbose("");
    }
    return clusterOrder;
  }

  /**
   * OPTICS-function expandClusterOrder.
   * 
   * @param database the database on which the algorithm is run
   * @param objectID the currently processed object
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrder(Database<O> database, Integer objectID, FiniteProgress progress) {

    clusterOrder.add(objectID, null, getDistanceFunction().infiniteDistance());
    processedIDs.add(objectID);

    if(isVerbose()) {
      progress.setProcessed(processedIDs.size());
      logger.progress(progress);
    }

    List<DistanceResultPair<D>> neighbors = database.rangeQuery(objectID, epsilon, getDistanceFunction());
    D coreDistance = neighbors.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbors.get(minpts - 1).getDistance();

    if(!getDistanceFunction().isInfiniteDistance(coreDistance)) {
      for(DistanceResultPair<D> neighbor : neighbors) {
        if(processedIDs.contains(neighbor.getID())) {
          continue;
        }
        D reachability = DistanceUtil.max(neighbor.getDistance(), coreDistance);
        updateHeap(reachability, new COEntry(neighbor.getID(), objectID));
      }

      while(!heap.isEmpty()) {
        final HeapNode<D, COEntry> pqNode = heap.getMinNode();
        COEntry current = pqNode.getValue();
        clusterOrder.add(current.objectID, current.predecessorID, pqNode.getKey());
        processedIDs.add(current.objectID);

        neighbors = database.rangeQuery(current.objectID, epsilon, getDistanceFunction());
        coreDistance = neighbors.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbors.get(minpts - 1).getDistance();

        if(!getDistanceFunction().isInfiniteDistance(coreDistance)) {
          for(DistanceResultPair<D> neighbor : neighbors) {
            if(processedIDs.contains(neighbor.getID())) {
              continue;
            }
            D distance = neighbor.getDistance();
            D reachability = DistanceUtil.max(distance, coreDistance);
            updateHeap(reachability, new COEntry(neighbor.getID(), current.objectID));
          }
        }
        if(isVerbose()) {
          progress.setProcessed(processedIDs.size());
          logger.progress(progress);
        }
      }
    }
  }

  public Description getDescription() {
    return new Description("OPTICS", "Density-Based Hierarchical Clustering", "Algorithm to find density-connected sets in a database based on the parameters minimumPoints and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.", "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander: " + "OPTICS: Ordering Points to Identify the Clustering Structure. " + "In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)");
  }

  public ClusterOrderResult<D> getResult() {
    return clusterOrder;
  }

  /**
   * Adds the specified entry with the specified key tp the heap. If the entry's
   * object is already in the heap, it will only be updated.
   * 
   * @param reachability the reachability of the entry's object
   * @param entry the entry to be added
   */
  private void updateHeap(D reachability, COEntry entry) {
    Integer index = heap.getIndexOf(entry);
    // entry is already in the heap
    if(index != null) {
      HeapNode<D, COEntry> heapNode = heap.getNodeAt(index);
      int compare = heapNode.getKey().compareTo(reachability);
      if(compare < 0) {
        return;
      }
      if(compare == 0 && heapNode.getValue().predecessorID < entry.predecessorID) {
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
  public class COEntry implements Identifiable {
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
     * @param objectID the id of the entry
     * @param predecessorID the id of the entry's predecessor
     */
    public COEntry(Integer objectID, Integer predecessorID) {
      this.objectID = objectID;
      this.predecessorID = predecessorID;
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
    public int compareTo(Identifiable o) {
      COEntry other = (COEntry) o;
      if(this.objectID < other.objectID) {
        return -1;
      }
      if(this.objectID > other.objectID) {
        return 1;
      }
      if(this.predecessorID < other.predecessorID) {
        return -1;
      }
      if(this.predecessorID > other.predecessorID) {
        return 1;
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
     * Indicates whether some other object is "equal to" this one. The result is
     * <code>true</code> if and only if the argument is not <code>null</code>
     * and is an <code>COEntry</code> object and
     * <code>objectID.equals(((COEntry) o).objectID)</code> returns
     * <code>true</code>.
     * 
     * @param o the object to compare with
     * @return <code>true</code> if the specified object is equal to this one,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if(o == null || getClass() != o.getClass()) {
        return false;
      }

      // noinspection unchecked
      return objectID.equals(((COEntry) o).objectID);
    }

    /**
     * Returns a hash code value for the object which is the hash code of the
     * <code>objectID</code> of this object.
     * 
     * @return the hash code of the <code>objectID</code> of this object
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