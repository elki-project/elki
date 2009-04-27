package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to store the result of an ordering clustering algorithm such as OPTICS.
 * 
 * @author Erich Schubert
 *
 * @param <D> distance type.
 */
// TODO: Annotations!
public class ClusterOrderResult<D extends Distance<D>> implements OrderingResult, Iterable<ClusterOrderEntry<D>> {
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
   * Use the cluster order to sort the given collection ids.
   * 
   * Implementation of the {@link OrderingResult} interface.
   */
  @Override
  public Iterator<Integer> iter(Collection<Integer> ids) {
    ArrayList<Integer> res = new ArrayList<Integer>(ids.size());
    for (ClusterOrderEntry<D> e : clusterOrder)
      if (ids.contains(e.getID()))
        res.add(e.getID());
    
    // TODO: elements in ids that are not in clusterOrder are lost!
    return res.iterator();
  }

  /**
   * Retrieve annotations for the cluster order.
   * 
   * Implementation of the {@link AnnotationResult} interface.
   * @param objID Object ID
   * @return Annotations for this object
   */
  //@Override
  // TODO: re-add functionality!
  public Pair<String, Object>[] getAnnotations(Integer objID) {
    Pair<String, Object>[] anns = Pair.newArray(3);
    // TODO: do we really need to include the ID here?
    anns[0] = new Pair<String, Object>("ID", objID);
    anns[1] = new Pair<String, Object>("REACHABILITY", map.get(objID).getReachability());
    anns[2] = new Pair<String, Object>("PREDECESSOR", map.get(objID).getPredecessorID());
    return anns;
  }

}
