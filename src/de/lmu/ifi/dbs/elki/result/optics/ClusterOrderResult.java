package de.lmu.ifi.dbs.elki.result.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
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
   * Cluster order storage
   */
  private ArrayList<ClusterOrderEntry<D>> clusterOrder;

  /**
   * Map of object IDs to their cluster order entry
   */
  private WritableDataStore<ClusterOrderEntry<D>> map;

  /**
   * The DBIDs we are defined for
   */
  ModifiableDBIDs dbids;

  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public ClusterOrderResult(String name, String shortname) {
    super(name, shortname);
    clusterOrder = new ArrayList<ClusterOrderEntry<D>>();
    dbids = DBIDUtil.newHashSet();
    map = DataStoreUtil.makeStorage(dbids, DataStoreFactory.HINT_DB, ClusterOrderEntry.class);

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

    @Override
    public DBIDs getDBIDs() {
      return dbids;
    }

    /**
     * Use the cluster order to sort the given collection ids.
     * 
     * Implementation of the {@link OrderingResult} interface.
     */
    @Override
    public ArrayModifiableDBIDs iter(DBIDs ids) {
      ArrayModifiableDBIDs res = DBIDUtil.newArray(ids.size());
      for(ClusterOrderEntry<D> e : clusterOrder) {
        if(ids.contains(e.getID())) {
          res.add(e.getID());
        }
      }
      return res;
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
    private DataStore<ClusterOrderEntry<D>> map;

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
    public ReachabilityDistanceAdapter(DataStore<ClusterOrderEntry<D>> map, DBIDs dbids) {
      super();
      this.map = map;
      this.dbids = dbids;
    }

    @Override
    public D get(DBIDRef objID) {
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
    public DBIDIter iterDBIDs() {
      return dbids.iter();
    }

    @Override
    public int size() {
      return dbids.size();
    }

    @Override
    public Database getDatabase() {
      return null; // FIXME
    }

    @Override
    public void set(DBIDRef id, D val) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(DBIDRef id) {
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
    private DataStore<ClusterOrderEntry<D>> map;

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
    public PredecessorAdapter(DataStore<ClusterOrderEntry<D>> map, DBIDs dbids) {
      super();
      this.map = map;
      this.dbids = dbids;
    }

    @Override
    public DBID get(DBIDRef objID) {
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
    public DBIDIter iterDBIDs() {
      return dbids.iter();
    }

    @Override
    public int size() {
      return dbids.size();
    }

    @Override
    public Database getDatabase() {
      return null; // FIXME
    }

    @Override
    public void set(DBIDRef id, DBID val) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(DBIDRef id) {
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
