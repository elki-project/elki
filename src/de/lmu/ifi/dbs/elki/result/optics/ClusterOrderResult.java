package de.lmu.ifi.dbs.elki.result.optics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.ResultAdapter;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIteratorAdapter;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;

/**
 * Class to store the result of an ordering clustering algorithm such as OPTICS.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.has ClusterOrderEntry oneway - - contains
 * @apiviz.composedOf ClusterOrderResult.ClusterOrderAdapter
 * @apiviz.composedOf ClusterOrderResult.ReachabilityDistanceAdapter
 * @apiviz.composedOf ClusterOrderResult.PredecessorAdapter
 * 
 * @param <D> distance type.
 */
public class ClusterOrderResult<D extends Distance<D>> extends BasicResult implements IterableResult<ClusterOrderEntry<D>> {
  /**
   * Association ID for reachability distance.
   */
  public static final AssociationID<? extends Distance<?>> REACHABILITY_ID = AssociationID.getOrCreateAssociationIDGenerics("reachability", new SimpleTypeInformation<Distance<?>>(Distance.class));

  /**
   * Predecessor ID for reachability distance.
   */
  public static final AssociationID<DBID> PREDECESSOR_ID = AssociationID.getOrCreateAssociationID("predecessor", TypeUtil.DBID);

  /**
   * Cluster order storage
   */
  private ArrayList<ClusterOrderEntry<D>> clusterOrder;

  /**
   * Map of object IDs to their cluster order entry
   */
  private HashMap<DBID, ClusterOrderEntry<D>> map;

  /**
   * The DBIDs we are defined for
   */
  private ModifiableDBIDs dbids;

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
    dbids = DBIDUtil.newHashSet();

    addChildResult(new ClusterOrderAdapter(clusterOrder));
    addChildResult(new ReachabilityDistanceAdapter(map, dbids));
    addChildResult(new PredecessorAdapter(map, dbids));
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
    add(new GenericClusterOrderEntry<D>(id, predecessor, reachability));
    dbids.add(id);
  }

  /**
   * Add an object to the cluster order.
   * 
   * @param ce Entry
   */
  public void add(ClusterOrderEntry<D> ce) {
    clusterOrder.add(ce);
    map.put(ce.getID(), ce);
    dbids.add(ce.getID());
  }

  /**
   * Get the distance class
   * 
   * @return distance class. Can be {@code null} for an all-undefined result!
   */
  public Class<?> getDistanceClass() {
    for(ClusterOrderEntry<D> ce : clusterOrder) {
      D dist = ce.getReachability();
      if(dist != null) {
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
  class ReachabilityDistanceAdapter implements Relation<D>, ResultAdapter {
    /**
     * Access reference.
     */
    private HashMap<DBID, ClusterOrderEntry<D>> map;

    /**
     * DBIDs
     */
    private DBIDs dbids;

    /**
     * Constructor.
     * 
     * @param map Map that stores the results.
     * @param dbids DBIDs we are defined for.
     */
    public ReachabilityDistanceAdapter(HashMap<DBID, ClusterOrderEntry<D>> map, DBIDs dbids) {
      super();
      this.map = map;
      this.dbids = dbids;
    }

    @Override
    public D get(DBID objID) {
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

    @Override
    public DBIDs getDBIDs() {
      return DBIDUtil.makeUnmodifiable(dbids);
    }

    @Override
    public IterableIterator<DBID> iterDBIDs() {
      return IterableUtil.fromIterator(dbids.iterator());
    }

    @Override
    public int size() {
      return dbids.size();
    }

    @Override
    public Database getDatabase() {
      return null; // FIXME
    }

    @SuppressWarnings("unused")
    @Override
    public void set(DBID id, D val) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public void delete(DBID id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SimpleTypeInformation<D> getDataTypeInformation() {
      return new SimpleTypeInformation<D>(Distance.class);
    }

    @Override
    public ResultHierarchy getHierarchy() {
      return ClusterOrderResult.this.getHierarchy();
    }

    @Override
    public void setHierarchy(ResultHierarchy hierarchy) {
      ClusterOrderResult.this.setHierarchy(hierarchy);
    }
  }

  /**
   * Result containing the predecessor ID.
   * 
   * @author Erich Schubert
   */
  class PredecessorAdapter implements Relation<DBID>, ResultAdapter {
    /**
     * Access reference.
     */
    private HashMap<DBID, ClusterOrderEntry<D>> map;

    /**
     * Database IDs
     */
    private DBIDs dbids;

    /**
     * Constructor.
     * 
     * @param map Map that stores the results.
     * @param dbids DBIDs we are defined for
     */
    public PredecessorAdapter(HashMap<DBID, ClusterOrderEntry<D>> map, DBIDs dbids) {
      super();
      this.map = map;
      this.dbids = dbids;
    }

    @Override
    public DBID get(DBID objID) {
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

    @Override
    public DBIDs getDBIDs() {
      return DBIDUtil.makeUnmodifiable(dbids);
    }

    @Override
    public IterableIterator<DBID> iterDBIDs() {
      return IterableUtil.fromIterator(dbids.iterator());
    }

    @Override
    public int size() {
      return dbids.size();
    }

    @Override
    public Database getDatabase() {
      return null; // FIXME
    }

    @SuppressWarnings("unused")
    @Override
    public void set(DBID id, DBID val) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public void delete(DBID id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SimpleTypeInformation<DBID> getDataTypeInformation() {
      return TypeUtil.DBID;
    }

    @Override
    public ResultHierarchy getHierarchy() {
      return ClusterOrderResult.this.getHierarchy();
    }

    @Override
    public void setHierarchy(ResultHierarchy hierarchy) {
      ClusterOrderResult.this.setHierarchy(hierarchy);
    }
  }
}
