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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import java.util.Comparator;
import de.lmu.ifi.dbs.elki.database.datastore.*;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.result.BasicResult;

/**
 * The pointer representation of a hierarchical clustering. Each object is
 * represented by a parent object and the distance at which it joins the parent
 * objects cluster. This is a rather compact and bottom-up representation of
 * clusters, the classes
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters}
 * and
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByHeight}.
 * can be used to extract partitions from this graph.
 * <p>
 * This class can also compute dendrogram positions, but using a faster
 * algorithm than the one proposed by Sibson 1971, using only O(n log n) time
 * due to sorting, but using an additional temporary array.
 *
 * @author Erich Schubert
 * @since 0.6.0
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
   * Merge order, useful for non-monotonous hierarchies.
   */
  IntegerDataStore mergeOrder = null;

  /**
   * Flag for squared distances.
   */
  boolean isSquared = false;

  /**
   * Constructor.
   *
   * @param ids IDs processed.
   * @param parent Parent pointer.
   * @param parentDistance Distance to parent.
   * @param isSquared Flag to indicate squared distances
   */
  public PointerHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance, boolean isSquared) {
    this(ids, parent, parentDistance, isSquared, null);
  }

  /**
   * Constructor.
   *
   * @param ids IDs processed.
   * @param parent Parent pointer.
   * @param parentDistance Distance to parent.
   * @param isSquared Flag to indicate squared distances
   * @param mergeOrder Order in which to execute merges
   */
  public PointerHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance, boolean isSquared, IntegerDataStore mergeOrder) {
    super("Pointer Representation", "pointer-representation");
    this.ids = ids;
    this.parent = parent;
    this.parentDistance = parentDistance;
    this.mergeOrder = mergeOrder;
    this.isSquared = isSquared;
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
    final ArrayDBIDs order = topologicalSort();
    WritableIntegerDataStore siz = computeSubtreeSizes(order);
    WritableIntegerDataStore pos = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, -1);
    WritableIntegerDataStore ins = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);

    int defins = 0; // Next root insertion position.
    DBIDVar v1 = DBIDUtil.newVar();
    // Place elements based on their successor
    for(DBIDArrayIter it = order.iter().seek(order.size() - 1); it.valid(); it.retract()) {
      final int size = siz.intValue(it);
      parent.assignVar(it, v1); // v1 = parent
      final int ipos = ins.intValue(v1); // Position of parent
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
    siz.destroy();
    ins.destroy();
    return positions = pos;
  }

  /**
   * Get the flag for squared distances.
   *
   * @return {@code true} if squared distances are used.
   */
  public boolean isSquared() {
    return isSquared;
  }

  /**
   * Compute the size of all subtrees.
   *
   * @param order Object order
   * @return Subtree sizes
   */
  private WritableIntegerDataStore computeSubtreeSizes(DBIDs order) {
    WritableIntegerDataStore siz = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 1);
    DBIDVar v1 = DBIDUtil.newVar();
    for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
      if(DBIDUtil.equal(it, parent.assignVar(it, v1))) {
        continue;
      }
      siz.increment(v1, siz.intValue(it));
    }
    return siz;
  }

  /**
   * Compute the maximum height of nodes.
   * 
   * This is necessary, because some linkages may sometimes yield anomalies,
   * where {@code d(a+b,c) < min(d(a,c), d(b,c))}.
   * 
   * @return Maximum height.
   */
  private WritableDoubleDataStore computeMaxHeight() {
    WritableDoubleDataStore maxheight = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
    DBIDVar v1 = DBIDUtil.newVar();
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double d = parentDistance.doubleValue(it);
      if(d > maxheight.doubleValue(it)) {
        maxheight.putDouble(it, d);
      }
      if(d > maxheight.doubleValue(parent.assignVar(it, v1))) {
        maxheight.putDouble(v1, d);
      }
    }
    return maxheight;
  }

  /**
   * Topological sort the object IDs.
   *
   * Even when we have this predefined merge order, it may be sub-optimal.
   * Such cases arise for example when using NNChain with single-link, as it
   * does not guarantee to discover merges in ascending order.
   * 
   * Therefore, we employ the following logic:
   * We process merges in merge order, and note the maximum height of the
   * subtree.
   * We then order points by this maximum height, then by their merge order.
   * 
   * @return Sorted object ids.
   */
  public ArrayDBIDs topologicalSort() {
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(this.ids);
    if(mergeOrder != null) {
      ids.sort(new DataStoreUtil.AscendingByIntegerDataStore(mergeOrder));
      WritableDoubleDataStore maxheight = computeMaxHeight();
      ids.sort(new Sorter(maxheight));
      maxheight.destroy();
    }
    else {
      ids.sort(new DataStoreUtil.DescendingByDoubleDataStoreAndId(parentDistance));
    }

    // We used to simply sort by merging distance
    // But for e.g. Median Linkage, this would lead to problems, as links are
    // not necessarily performed in ascending order anymore!
    final int size = ids.size();

    ModifiableDBIDs seen = DBIDUtil.newHashSet(size);
    ArrayModifiableDBIDs order = DBIDUtil.newArray(size);
    DBIDVar v1 = DBIDUtil.newVar(), prev = DBIDUtil.newVar();
    // Process merges in descending order
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      if(!seen.add(it)) {
        continue;
      }
      final int begin = order.size();
      order.add(it);
      prev.set(it); // Copy
      // Follow parents of prev -> v1 - these need to come before prev.
      while(!DBIDUtil.equal(prev, parent.assignVar(prev, v1))) {
        if(!seen.add(v1)) {
          break;
        }
        order.add(v1);
        prev.set(v1); // Copy
      }
      // Reverse the inserted path:
      for(int i = begin, j = order.size() - 1; i < j; i++, j--) {
        order.swap(i, j);
      }
    }
    // Reverse everything
    for(int i = 0, j = size - 1; i < j; i++, j--) {
      order.swap(i, j);
    }
    return order;
  }

  /**
   * Class for generating / optimizing the merge order.
   *
   * @author Erich Schubert
   */
  private final class Sorter implements Comparator<DBIDRef> {
    /**
     * Maximum height.
     */
    private WritableDoubleDataStore maxheight;

    /**
     * Constructor.
     *
     * @param maxheight Maximum height
     */
    public Sorter(WritableDoubleDataStore maxheight) {
      this.maxheight = maxheight;
    }

    @Override
    public int compare(DBIDRef o1, DBIDRef o2) {
      int c = Double.compare(maxheight.doubleValue(o2), maxheight.doubleValue(o1));
      if(c == 0) {
        c = Double.compare(parentDistance.doubleValue(o2), parentDistance.doubleValue(o1));
      }
      if(c == 0) {
        c = Integer.compare(mergeOrder.intValue(o2), mergeOrder.intValue(o1));
      }
      return c;
    }
  }
}
