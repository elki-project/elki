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
package de.lmu.ifi.dbs.elki.utilities.datastructures.unionfind;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Union-find algorithm for {@link StaticDBIDs}, with optimizations.
 * <p>
 * To instantiate, use {@link UnionFindUtil#make}, which will automatically
 * choose the best implementation available.
 * <p>
 * This is the weighted quick union approach, weighted by count and using
 * path-halving for optimization.
 * <p>
 * This version needs more memory than {@link WeightedQuickUnionRangeDBIDs} but
 * can work with any unmodifiable DBID set (although {@link ArrayDBIDs} are
 * recommended).
 * <p>
 * Reference:
 * <p>
 * R. Sedgewick<br>
 * 1.3 Union-Find Algorithms<br>
 * Algorithms in C, Parts 1-4
 *
 * @author Evgeniy Faerman
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "R. Sedgewick", //
    title = "Algorithms in C, Parts 1-4", //
    booktitle = "", //
    bibkey = "DBLP:books/daglib/0004943")
public class WeightedQuickUnionStaticDBIDs implements UnionFind {
  /**
   * Object ID range.
   */
  private ArrayDBIDs ids;

  /**
   * Index, to map DBID to offset.
   */
  private WritableIntegerDataStore index;

  /**
   * Parent element
   */
  private int[] parent;

  /**
   * Weight, for optimization.
   */
  private int[] weight;

  /**
   * Constructor (package private, use {@link UnionFindUtil#make}).
   *
   * @param ids Range to use
   */
  WeightedQuickUnionStaticDBIDs(StaticDBIDs ids) {
    this.ids = DBIDUtil.ensureArray(ids);
    index = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    int j = 0;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      index.put(iter, j++);
    }
    weight = new int[ids.size()];
    Arrays.fill(weight, 1);
    parent = new int[ids.size()];
    for(int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
  }

  @Override
  public int find(DBIDRef element) {
    int cur = index.intValue(element);
    assert (cur >= 0 && cur < ids.size());
    int p = parent[cur], tmp;
    while(cur != p) {
      tmp = p;
      p = parent[cur] = parent[p]; // Perform simple path compression.
      cur = tmp;
    }
    return cur;
  }

  @Override
  public int union(DBIDRef first, DBIDRef second) {
    int firstComponent = find(first), secondComponent = find(second);
    if(firstComponent == secondComponent) {
      return firstComponent;
    }
    final int w1 = weight[firstComponent], w2 = weight[secondComponent];
    if(w1 > w2) {
      parent[secondComponent] = firstComponent;
      weight[firstComponent] += w2;
      return firstComponent;
    }
    else {
      parent[firstComponent] = secondComponent;
      weight[secondComponent] += w1;
      return secondComponent;
    }
  }

  @Override
  public boolean isConnected(DBIDRef first, DBIDRef second) {
    return find(first) == find(second);
  }

  @Override
  public DBIDs getRoots() {
    ArrayModifiableDBIDs roots = DBIDUtil.newArray();
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      // roots or one element in component
      if(parent[iter.getOffset()] == iter.getOffset()) {
        roots.add(iter);
      }
    }
    return roots;
  }
}
