package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.BasicResult;

/**
 * The pointer representation of a hierarchical clustering. Each object is
 * represented by a parent object and the distance at which it joins the parent
 * objects cluster.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class PointerHierarchyRepresentationResult<D extends Distance<D>> extends BasicResult {
  /**
   * The DBIDs in this result.
   */
  DBIDs ids;

  /**
   * The parent DBID relation.
   */
  DBIDDataStore parent;

  /**
   * Distance to the parent object.
   */
  DataStore<D> parentDistance;

  /**
   * Constructor.
   * 
   * @param ids IDs processed.
   * @param parent Parent pointer.
   * @param parentDistance Distance to parent.
   */
  public PointerHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DataStore<D> parentDistance) {
    super("Pointer Representation", "pointer-representation");
    this.ids = ids;
    this.parent = parent;
    this.parentDistance = parentDistance;
  }

  /**
   * Get the clustered DBIDs.
   * 
   * @return DBIDs
   */
  public DBIDs getDBIDs() {
    return ids;
  }

  /**
   * Get the parent DBID relation.
   * 
   * @return Parent relation.
   */
  public DBIDDataStore getParentStore() {
    return parent;
  }

  /**
   * Get the distance to the parent.
   * 
   * @return Parent distance.
   */
  public DataStore<D> getParentDistanceStore() {
    return parentDistance;
  }
}
