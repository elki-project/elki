package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.IntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Hierarchical clustering with prototypes (used by {@link MiniMax}).
 * 
 * @author Julian Erhard
 */
public class PointerPrototypeHierarchyRepresentationResult extends PointerHierarchyRepresentationResult {
  /**
   * Prototypes
   */
  DBIDDataStore prototypes;

  /**
   * Constructor.
   *
   * @param ids Objects clustered
   * @param parent Parent object
   * @param parentDistance Merging distance
   * @param mergeOrder Merging order
   * @param prototypes Cluster prototypes
   */
  public PointerPrototypeHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance, IntegerDataStore mergeOrder, DBIDDataStore prototypes) {
    super(ids, parent, parentDistance, mergeOrder);
    this.prototypes = prototypes;
  }

  /**
   * Constructor.
   *
   * @param ids Objects clustered
   * @param parent Parent object
   * @param parentDistance Merging distance
   * @param prototypes Cluster prototypes
   */
  public PointerPrototypeHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance, DBIDDataStore prototypes) {
    this(ids, parent, parentDistance, null, prototypes);
  }

  /**
   * @return the set of prototypes
   */
  public DBIDDataStore getPrototypes() {
    return prototypes;
  }

  /**
   * Extract the prototype of a given cluster. When the argument is not a valid
   * cluster of this Pointer Hierarchy, the return value is unspecified.
   * 
   * @param cluster A valid cluster of this Pointer Hierarchy
   * @return The prototype of the cluster
   */
  public DBID findPrototype(DBIDs cluster) {
    if(cluster.isEmpty()) {
      return null;
    }
    else if(cluster.size() >= 2) {
      // The prototype is stored at the same ID that has the
      // first (smallest) ID of the cluster as a parent.
      DBIDVar var = DBIDUtil.newVar();
      DBID firstID = DBIDUtil.deref(cluster.iter());

      // The entry in pi for the first ID contains a DBID of a different
      // cluster.
      // Hence, skip this one by advancing.
      DBIDIter i = cluster.iter().advance();
      for(; i.valid(); i.advance()) {
        parent.assignVar(i, var);
        if(DBIDUtil.equal(firstID, var)) {
          break;
        }
      }
      return prototypes.get(i);
    }
    else {
      // Set the only point in the cluster as prototype.
      return DBIDUtil.deref(cluster.iter());
    }
  }

}
