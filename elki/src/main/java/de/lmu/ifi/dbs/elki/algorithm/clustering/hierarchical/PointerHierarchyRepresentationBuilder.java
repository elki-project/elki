package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Class to help building a pointer hierarchy.
 *
 * @author Erich Schubert
 * @since 0.7.1
 *
 * @apiviz.has PointerHierarchyRepresentationResult
 */
public class PointerHierarchyRepresentationBuilder {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PointerHierarchyRepresentationBuilder.class);

  /**
   * The DBIDs in this result.
   */
  protected final DBIDs ids;

  /**
   * The parent DBID relation.
   */
  protected WritableDBIDDataStore parent;

  /**
   * Distance to the parent object.
   */
  protected WritableDoubleDataStore parentDistance;

  /**
   * Last linking distance.
   */
  protected double prevdist;

  /**
   * Cluster size storage. May be uninitialized!
   */
  protected WritableIntegerDataStore csize;

  /**
   * Constructor.
   *
   * @param ids IDs
   */
  public PointerHierarchyRepresentationBuilder(DBIDs ids) {
    super();
    this.ids = ids;
    this.parent = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    this.parentDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    this.prevdist = Double.NEGATIVE_INFINITY;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      parent.put(it, it);
    }
  }

  /**
   * Add an element to the pointer representation.
   *
   * Important: If an algorithm does not produce links in an increasing fashion,
   * a warning will be issued and the linking distance will be increased.
   * Otherwise, the hierarchy would be misinterpreted when links are executed
   * ordered by their distance.
   *
   * @param cur Current object
   * @param distance Link distance
   * @param par Parent
   */
  public void add(DBIDRef cur, double distance, DBIDRef par) {
    if(distance < prevdist) {
      LOG.warning("Non-monotone hierarchical clustering detected. Adjusting linking distance from " + distance + " to " + prevdist);
      distance = prevdist;
    }
    parent.putDBID(cur, par);
    double olddist = parentDistance.putDouble(cur, distance);
    assert (olddist == Double.POSITIVE_INFINITY) : "Object was already linked!";
    prevdist = distance;
  }

  /**
   * Finalize the result.
   *
   * @return Completed result
   */
  public PointerHierarchyRepresentationResult complete() {
    if(csize != null) {
      csize.destroy();
      csize = null;
    }
    return new PointerHierarchyRepresentationResult(ids, parent, parentDistance);
  }

  /**
   * Get the cluster size of the current object.
   *
   * @param id Object id
   * @return Cluster size (initially 1).
   */
  public int getSize(DBIDRef id) {
    if(csize == null) {
      csize = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 1);
    }
    return csize.intValue(id);
  }

  /**
   * Set the cluster size of an object.
   *
   * @param id Object to set
   * @param size Cluster size
   */
  public void setSize(DBIDRef id, int size) {
    if(csize == null) {
      csize = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 1);
    }
    csize.putInt(id, size);
  }

  /**
   * Test if an object is already linked.
   *
   * @param id Object
   * @return {@code true} if the object is already linked.
   */
  public boolean isLinked(DBIDRef id) {
    return parentDistance.doubleValue(id) < Double.POSITIVE_INFINITY;
  }

  /**
   * Get the current linking distance of an object.
   *
   * @param id Object
   * @return Distance, or infinity
   */
  public double getDistance(DBIDRef id) {
    return parentDistance.doubleValue(id);
  }
}
