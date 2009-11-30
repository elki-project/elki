package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * Class to store the result of an ordering clustering algorithm such as OPTICS.
 * 
 * @author Erich Schubert
 * 
 * @param <D> distance type.
 */
public class ClusterOrderResult<D extends Distance<D>> extends MultiResult implements Iterable<ClusterOrderEntry<D>> {
  /**
   * Association ID for reachability distance.
   */
  public static final AssociationID<Distance<?>> REACHABILITY_ID = AssociationID.getOrCreateAssociationIDGenerics("reachability", Distance.class);

  /**
   * Predecessor ID for reachability distance.
   */
  public static final AssociationID<Integer> PREDECESSOR_ID = AssociationID.getOrCreateAssociationID("predecessor", Integer.class);

  /**
   * Cluster order storage
   */
  private ArrayList<ClusterOrderEntry<D>> clusterOrder;

  /**
   * Map of object IDs to their cluster order entry
   */
  private HashMap<Integer, ClusterOrderEntry<D>> map;

  /**
   * Constructor
   */
  public ClusterOrderResult() {
    super();
    clusterOrder = new ArrayList<ClusterOrderEntry<D>>();
    map = new HashMap<Integer, ClusterOrderEntry<D>>();
    
    addResult(new ClusterOrderAdapter(clusterOrder));
    addResult(new ReachabilityDistanceAdapter(map));
    addResult(new PredecessorAdapter(map));
  }

  /**
   * Retrieve the complete cluster order.
   * 
   * @return cluster order
   */
  public List<ClusterOrderEntry<D>> getClusterOrder() {
    return clusterOrder;
  }

  /**
   * The cluster order is iterable
   */
  @Override
  public Iterator<ClusterOrderEntry<D>> iterator() {
    return clusterOrder.iterator();
  }

  /**
   * Add an object to the cluster order.
   * 
   * @param id
   * @param predecessor
   * @param reachability
   */
  public void add(Integer id, Integer predecessor, D reachability) {
    add(new ClusterOrderEntry<D>(id, predecessor, reachability));
  }

  /**
   * Add an object ot the cluster order.
   * 
   * @param ce
   */
  public void add(ClusterOrderEntry<D> ce) {
    clusterOrder.add(ce);
    map.put(ce.getID(), ce);
  }

  /**
   * Ordering part of the result.
   * 
   * @author Erich Schubert
   */
  class ClusterOrderAdapter implements OrderingResult {
    /**
     * Access reference.
     */
    private ArrayList<ClusterOrderEntry<D>> clusterOrder;

    /**
     * Constructor.
     * 
     * @param clusterOrder order to return
     */
    public ClusterOrderAdapter(final ArrayList<ClusterOrderEntry<D>> clusterOrder) {
      super();
      this.clusterOrder = clusterOrder;
    }

    /**
     * Use the cluster order to sort the given collection ids.
     * 
     * Implementation of the {@link OrderingResult} interface.
     */
    @Override
    public Iterator<Integer> iter(Collection<Integer> ids) {
      ArrayList<Integer> res = new ArrayList<Integer>(ids.size());
      for(ClusterOrderEntry<D> e : clusterOrder) {
        if(ids.contains(e.getID())) {
          res.add(e.getID());
        }
      }

      // TODO: elements in ids that are not in clusterOrder are lost!
      return res.iterator();
    }

    @Override
    public String getName() {
      return "clusterorder";
    }
  }
  
  /**
   * Result containing the reachability distances.
   * 
   * @author Erich Schubert
   */
  class ReachabilityDistanceAdapter implements AnnotationResult<D> {
    /**
     * Access reference.
     */
    private HashMap<Integer, ClusterOrderEntry<D>> map;
    
    /**
     * Constructor.
     * 
     * @param map Map that stores the results.
     */
    public ReachabilityDistanceAdapter(HashMap<Integer, ClusterOrderEntry<D>> map) {
      super();
      this.map = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AssociationID<D> getAssociationID() {
      return (AssociationID<D>) REACHABILITY_ID;
    }

    @Override
    public D getValueFor(Integer objID) {
      return map.get(objID).getReachability();
    }

    @Override
    public String getName() {
      return "Reachability";
    }
  }

  /**
   * Result containing the predecessor ID.
   * 
   * @author Erich Schubert
   */
  class PredecessorAdapter implements AnnotationResult<Integer> {
    /**
     * Access reference.
     */
    private HashMap<Integer, ClusterOrderEntry<D>> map;
    
    /**
     * Constructor.
     * 
     * @param map Map that stores the results.
     */
    public PredecessorAdapter(HashMap<Integer, ClusterOrderEntry<D>> map) {
      super();
      this.map = map;
    }

    @Override
    public AssociationID<Integer> getAssociationID() {
      return PREDECESSOR_ID;
    }

    @Override
    public Integer getValueFor(Integer objID) {
      return map.get(objID).getPredecessorID();
    }

    @Override
    public String getName() {
      return "Predecessor";
    }
  }

  @Override
  public String getName() {
    return "optics";
  }
}
