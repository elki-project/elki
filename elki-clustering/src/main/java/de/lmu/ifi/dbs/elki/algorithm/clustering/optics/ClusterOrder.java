/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;

/**
 * Class to store the result of an ordering clustering algorithm such as OPTICS.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @opt nodefillcolor LemonChiffon
 */
public class ClusterOrder extends BasicResult implements OrderingResult {
  /**
   * Cluster order.
   */
  ArrayModifiableDBIDs ids;

  /**
   * Reachability storage.
   */
  WritableDoubleDataStore reachability;

  // TODO: add optional core sizes, too?

  /**
   * Predecessor storage.
   */
  WritableDBIDDataStore predecessor;

  /**
   * Constructor
   * 
   * @param ids Object IDs included
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public ClusterOrder(DBIDs ids, String name, String shortname) {
    super(name, shortname);
    this.ids = DBIDUtil.newArray(ids.size());
    reachability = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
    predecessor = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT);

    addChildResult(new MaterializedDoubleRelation("Reachability distance", "reachdist", reachability, ids));
    addChildResult(new MaterializedRelation<DBID>("OPTICS predecessor", "predecessor", TypeUtil.DBID, predecessor, ids));
  }

  /**
   * Constructor
   * 
   * @param ids Object IDs included
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public ClusterOrder(String name, String shortname, ArrayModifiableDBIDs ids, WritableDoubleDataStore reachability, WritableDBIDDataStore predecessor) {
    super(name, shortname);
    this.ids = ids;
    this.reachability = reachability;
    this.predecessor = predecessor;

    addChildResult(new MaterializedDoubleRelation("Reachability distance", "reachdist", reachability, ids));
    if(predecessor != null) {
      addChildResult(new MaterializedRelation<DBID>("OPTICS predecessor", "predecessor", TypeUtil.DBID, predecessor, ids));
    }
  }

  /**
   * Add an object to the cluster order.
   * 
   * @param id Object id
   * @param reach Reachability
   * @param pre Predecessor
   */
  public void add(DBIDRef id, double reach, DBIDRef pre) {
    ids.add(id);
    reachability.putDouble(id, reach);
    if(pre == null || pre instanceof DBIDVar && !((DBIDVar) pre).isSet()) {
      return;
    }
    predecessor.putDBID(id, pre);
  }

  @Override
  public ArrayDBIDs getDBIDs() {
    return ids;
  }

  /**
   * Get an iterator.
   */
  public DBIDArrayIter iter() {
    return ids.iter();
  }

  /**
   * Use the cluster order to sort the given collection ids.
   * 
   * Implementation of the {@link OrderingResult} interface.
   */
  @Override
  public ArrayModifiableDBIDs order(DBIDs ids) {
    ArrayModifiableDBIDs res = DBIDUtil.newArray(ids.size());
    for(DBIDIter it = this.ids.iter(); it.valid(); it.advance()) {
      if(ids.contains(it)) {
        res.add(it);
      }
    }
    return res;
  }

  /**
   * Size.
   * 
   * @return Size
   */
  public int size() {
    return ids.size();
  }

  /**
   * Get the reachability of an object.
   * 
   * @param id Object id
   * @return Reachability
   */
  public double getReachability(DBIDRef id) {
    return reachability.doubleValue(id);
  }

  /**
   * Get the predecessor.
   * 
   * @param id Current id.
   * @param out Output variable to store the predecessor.
   */
  public void getPredecessor(DBIDRef id, DBIDVar out) {
    if(predecessor == null) {
      out.unset();
      return;
    }
    predecessor.assignVar(id, out);
  }
}
