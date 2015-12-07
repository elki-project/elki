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

import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.IntegerDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.BasicResult;

/**
 * The pointer representation of a hierarchical clustering. Each object is
 * represented by a parent object and the distance at which it joins the parent
 * objects cluster. This is a rather compact and bottom-up representation of
 * clusters, the class
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.ExtractFlatClusteringFromHierarchy}
 * can be used to extract partitions from this graph.
 *
 * This class can also compute dendrogram positions, but using a faster
 * algorithm than the one proposed by Sibson 1971, using only O(n log n) time
 * due to sorting, but using an additional temporary array.
 *
 * @author Erich Schubert
 */
public class PointerHierarchyRepresentationResult extends BasicResult {
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
  DoubleDataStore parentDistance;

  /**
   * Position storage, computed on demand.
   */
  IntegerDataStore positions = null;

  /**
   * Constructor.
   *
   * @param ids IDs processed.
   * @param parent Parent pointer.
   * @param parentDistance Distance to parent.
   */
  public PointerHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance) {
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
  public DoubleDataStore getParentDistanceStore() {
    return parentDistance;
  }

  /**
   * Get / compute the positions.
   *
   * @return Dendrogram positions
   */
  public IntegerDataStore getPositions() {
    if(positions != null) {
      return positions; // Return cached.
    }
    ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
    order.sort(new DataStoreUtil.AscendingByDoubleDataStoreAndId(parentDistance));
    DBIDArrayMIter it = order.iter();
    final int last = order.size() - 1;
    // Subtree sizes of each element:
    WritableIntegerDataStore siz = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, 1);
    DBIDVar v1 = DBIDUtil.newVar();
    for(it.seek(0); it.valid(); it.advance()) {
      if(DBIDUtil.equal(it, parent.assignVar(it, v1))) {
        continue;
      }
      siz.increment(v1, siz.intValue(it));
    }
    assert (siz.intValue(it.seek(last)) == ids.size());
    WritableIntegerDataStore pos = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, -1);
    WritableIntegerDataStore ins = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    int defins = 0;
    // Place elements based on their successor
    for(it.seek(last); it.valid(); it.retract()) {
      int size = siz.intValue(it);
      parent.assignVar(it, v1); // v1 = parent
      if(DBIDUtil.equal(it, v1)) {
        // Root: use interval [defins; defins + size]
        ins.putInt(it, defins);
        pos.putInt(it, defins + size - 1);
        defins += size;
        continue;
      }
      // Insertion position of parent = leftmost
      final int ipos = ins.intValue(v1);
      assert (ipos >= 0);
      pos.putInt(it, ipos + size - 1);
      ins.putInt(it, ipos);
      ins.increment(v1, size);
    }
    ins.destroy();
    return positions = pos;
  }
}
