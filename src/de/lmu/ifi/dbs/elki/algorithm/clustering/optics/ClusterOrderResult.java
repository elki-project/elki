package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.ResultAdapter;

/**
 * Class to store the result of an ordering clustering algorithm such as OPTICS.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.has ClusterOrderEntry oneway - - contains
 * @apiviz.composedOf ClusterOrderResult.ClusterOrderAdapter
 * @apiviz.composedOf ClusterOrderResult.PredecessorAdapter
 * 
 * @param <E> entry type.
 */
public class ClusterOrderResult<E extends ClusterOrderEntry<?>> extends BasicResult implements IterableResult<E>, Relation<E> {
  /**
   * Cluster order storage
   */
  private ArrayList<E> clusterOrder;

  /**
   * Map of object IDs to their cluster order entry
   */
  private WritableDataStore<E> map;

  /**
   * The DBIDs we are defined for
   */
  DBIDs ids;

  /**
   * The database.
   */
  Database database;

  /**
   * Constructor
   * 
   * @param database Database to attach the result to
   * @param ids Object IDs included
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public ClusterOrderResult(Database database, DBIDs ids, String name, String shortname) {
    super(name, shortname);
    this.ids = ids;
    this.clusterOrder = new ArrayList<>(ids.size());
    this.map = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, ClusterOrderEntry.class);

    addChildResult(new ClusterOrderAdapter(this.clusterOrder));
  }

  /**
   * Retrieve the complete cluster order.
   * 
   * @return cluster order
   */
  public List<E> getClusterOrder() {
    return clusterOrder;
  }

  /**
   * Get the type of entries in this cluster order.
   * 
   * @return Cluster order
   */
  public Class<? super E> getEntryType() {
    if(clusterOrder.size() <= 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Class<? super E> cls = (Class<? super E>) clusterOrder.get(0).getClass();
    return cls;
  }

  /**
   * The cluster order is iterable
   */
  @Override
  public Iterator<E> iterator() {
    return clusterOrder.iterator();
  }

  /**
   * Add an object to the cluster order.
   * 
   * @param ce Entry
   */
  public void add(E ce) {
    clusterOrder.add(ce);
    map.put(ce.getID(), ce);
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public SimpleTypeInformation<E> getDataTypeInformation() {
    return new SimpleTypeInformation<>(ClusterOrderEntry.class);
  }

  @Override
  public DBIDs getDBIDs() {
    return ids;
  }

  @Override
  public DBIDIter iterDBIDs() {
    return ids.iter();
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public E get(DBIDRef id) {
    return map.get(id);
  }

  @Override
  public void set(DBIDRef id, E val) {
    map.put(id, val);
  }

  @Override
  public void delete(DBIDRef id) {
    throw new UnsupportedOperationException();
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
    private ArrayList<E> clusterOrder;

    /**
     * Constructor.
     * 
     * @param clusterOrder order to return
     */
    public ClusterOrderAdapter(final ArrayList<E> clusterOrder) {
      super();
      this.clusterOrder = clusterOrder;
    }

    @Override
    public DBIDs getDBIDs() {
      return ids;
    }

    /**
     * Use the cluster order to sort the given collection ids.
     * 
     * Implementation of the {@link OrderingResult} interface.
     */
    @Override
    public ArrayModifiableDBIDs iter(DBIDs ids) {
      ArrayModifiableDBIDs res = DBIDUtil.newArray(ids.size());
      for(E e : clusterOrder) {
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
}
