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
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.IntegerDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;

public class PointerPrototypeHierarchyOrderedRepresentationResult extends PointerPrototypeHierarchyRepresentationResult {
  
  private IntegerDataStore mergeOrder;
  
  /**
   * Constructor.
   *
   * @param ids
   * @param parent
   * @param parentDistance
   * @param prototype
   */
  public PointerPrototypeHierarchyOrderedRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance, IntegerDataStore mergeOrder, DBIDDataStore prototype) {
    super(ids, parent, parentDistance, prototype);
    this.mergeOrder = mergeOrder;
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
    ArrayDBIDs order = topologicalSort(ids, parent, mergeOrder);
    DBIDArrayIter it = order.iter();
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
    // Assertion only holds for exact e.g. single linkage
    //assert (siz.intValue(it.seek(last)) == ids.size());
    WritableIntegerDataStore pos = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, -1);
    WritableIntegerDataStore ins = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    int defins = 0;
    // Place elements based on their successor
    for(it.seek(last); it.valid(); it.retract()) {
      int size = siz.intValue(it);
      parent.assignVar(it, v1); // v1 = parent
      final int ipos = ins.intValue(v1);
      // Assertion only holds for exact e.g. single linkage
      // assert (ipos >= 0);
      if(ipos < 0 || DBIDUtil.equal(it, v1)) {
        // Root: use interval [defins; defins + size]
        ins.putInt(it, defins);
        pos.putInt(it, defins + size - 1);
        defins += size;
        continue;
      }
      // Insertion position of parent = leftmost
      pos.putInt(it, ipos + size - 1);
      ins.putInt(it, ipos);
      ins.increment(v1, size);
    }
    ins.destroy();
    return positions = pos;
  }

  /**
   * Perform topological sorting based on the successor order.
   *
   * @param oids IDs to sort
   * @param parent Parent relationship.
   * @param parentDistance Distance to parent.
   * @return Sorted order
   */
  public static ArrayDBIDs topologicalSort(DBIDs oids, DBIDDataStore parent, IntegerDataStore parentDistance) {
    // We used to simply use this:
    // But for e.g. Median Linkage, this would lead to problems, as links are
    // not necessarily performed in ascending order anymore!
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(oids);
    ids.sort(new DataStoreUtil.DescendingByIntegerDataStore(parentDistance));
    final int size = ids.size();
    ModifiableDBIDs seen = DBIDUtil.newHashSet(size);
    ArrayModifiableDBIDs order = DBIDUtil.newArray(size);
    DBIDVar v1 = DBIDUtil.newVar(), prev = DBIDUtil.newVar();
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      if(!seen.add(it)) {
        continue;
      }
      order.add(it);
      prev.set(it); // Copy
      while(!DBIDUtil.equal(prev, parent.assignVar(prev, v1))) {
        if(!seen.add(v1)) {
          break;
        }
        order.add(v1);
        prev.set(v1); // Copy
      }
    }
    // Reverse the array:
    for(int i = 0, j = size - 1; i < j; i++, j--) {
      order.swap(i, j);
    }
    return order;
  }
}
