package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.IterableIteratorAdapter;

/**
 * Class to store the result of an ordering clustering algorithm such as OPTICS.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.result.ClusterOrderEntry oneway - - contains
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.result.ClusterOrderResult.ClusterOrderAdapter
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.result.ClusterOrderResult.ReachabilityDistanceAdapter
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.result.ClusterOrderResult.PredecessorAdapter
 * 
 * @param <D> distance type.
 */
public class ClusterOrderResult<D extends Distance<D>> extends TreeResult implements IterableResult<ClusterOrderEntry<D>> {
  /**
   * Association ID for reachability distance.
   */
  public static final AssociationID<? extends Distance<?>> REACHABILITY_ID = AssociationID.getOrCreateAssociationIDGenerics("reachability", Distance.class);

  /**
   * Predecessor ID for reachability distance.
   */
  public static final AssociationID<DBID> PREDECESSOR_ID = AssociationID.getOrCreateAssociationID("predecessor", DBID.class);

  /**
   * Cluster order storage
   */
  private ArrayList<ClusterOrderEntry<D>> clusterOrder;

  /**
   * Map of object IDs to their cluster order entry
   */
  private HashMap<DBID, ClusterOrderEntry<D>> map;

  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public ClusterOrderResult(String name, String shortname) {
    super(name, shortname);
    clusterOrder = new ArrayList<ClusterOrderEntry<D>>();
    map = new HashMap<DBID, ClusterOrderEntry<D>>();
    
    addPrimaryResult(new ClusterOrderAdapter(clusterOrder));
    addPrimaryResult(new ReachabilityDistanceAdapter(map));
    addPrimaryResult(new PredecessorAdapter(map));
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
   * @param id Object ID
   * @param predecessor Predecessor ID
   * @param reachability Reachability distance
   */
  public void add(DBID id, DBID predecessor, D reachability) {
    add(new ClusterOrderEntry<D>(id, predecessor, reachability));
  }

  /**
   * Add an object to the cluster order.
   * 
   * @param ce Entry
   */
  public void add(ClusterOrderEntry<D> ce) {
    clusterOrder.add(ce);
    map.put(ce.getID(), ce);
  }
  
  /**
   * Get the distance class
   * 
   * @return distance class. Can be {@code null} for an all-undefined result!
   */
  public Class<?> getDistanceClass() {
    for (ClusterOrderEntry<D> ce : clusterOrder) {
      D dist = ce.getReachability();
      if (dist != null) {
        return dist.getClass();
      }
    }
    return null;
  }

  /**
   * Ordering part of the result.
   * 
   * @author Erich Schubert
   */
  class ClusterOrderAdapter implements OrderingResult, ResultAdapter {
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
    public IterableIterator<DBID> iter(DBIDs ids) {
      ArrayModifiableDBIDs res = DBIDUtil.newArray(ids.size());
      for(ClusterOrderEntry<D> e : clusterOrder) {
        if(ids.contains(e.getID())) {
          res.add(e.getID());
        }
      }

      // TODO: elements in ids that are not in clusterOrder are lost!
      return new IterableIteratorAdapter<DBID>(res);
    }

    @Override
    public String getLongName() {
      return "Derived Object Order";
    }

    @Override
    public String getShortName() {
      return "clusterobjectorder";
    }
  }
  
  /**
   * Result containing the reachability distances.
   * 
   * @author Erich Schubert
   */
  class ReachabilityDistanceAdapter implements AnnotationResult<D>, ResultAdapter {
    /**
     * Access reference.
     */
    private HashMap<DBID, ClusterOrderEntry<D>> map;
    
    /**
     * Constructor.
     * 
     * @param map Map that stores the results.
     */
    public ReachabilityDistanceAdapter(HashMap<DBID, ClusterOrderEntry<D>> map) {
      super();
      this.map = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AssociationID<D> getAssociationID() {
      return (AssociationID<D>) REACHABILITY_ID;
    }

    @Override
    public D getValueFor(DBID objID) {
      return map.get(objID).getReachability();
    }

    @Override
    public String getLongName() {
      return "Reachability";
    }

    @Override
    public String getShortName() {
      return "reachability";
    }
  }

  /**
   * Result containing the predecessor ID.
   * 
   * @author Erich Schubert
   */
  class PredecessorAdapter implements AnnotationResult<DBID>, ResultAdapter {
    /**
     * Access reference.
     */
    private HashMap<DBID, ClusterOrderEntry<D>> map;
    
    /**
     * Constructor.
     * 
     * @param map Map that stores the results.
     */
    public PredecessorAdapter(HashMap<DBID, ClusterOrderEntry<D>> map) {
      super();
      this.map = map;
    }

    @Override
    public AssociationID<DBID> getAssociationID() {
      return PREDECESSOR_ID;
    }

    @Override
    public DBID getValueFor(DBID objID) {
      return map.get(objID).getPredecessorID();
    }

    @Override
    public String getLongName() {
      return "Predecessor";
    }
    
    @Override
    public String getShortName() {
      return "predecessor";
    }
  }
}
