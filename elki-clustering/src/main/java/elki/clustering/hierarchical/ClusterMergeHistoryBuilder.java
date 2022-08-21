/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.hierarchical;

import java.util.Arrays;

import elki.database.datastore.*;
import elki.database.ids.*;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

/**
 * Class to help building a pointer hierarchy.
 *
 * @author Erich Schubert
 * @since 0.7.1
 *
 * @has - - - ClusterMergeHistory
 */
public class ClusterMergeHistoryBuilder {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClusterMergeHistoryBuilder.class);

  /**
   * The DBIDs in this result.
   */
  protected final ArrayDBIDs ids;

  /**
   * Distance to the parent object.
   */
  protected double[] mergeDistance;

  /**
   * Cluster size storage.
   */
  protected int[] csize;

  /**
   * Store merge order.
   */
  protected int[] merges;

  /**
   * Parent for union-find, may be uninitialized.
   */
  protected int[] parent;

  /**
   * Merge counter (for merge ordering).
   */
  protected int mergecount = 0;

  /**
   * Prototype storage, may be {@code null}.
   */
  protected ArrayModifiableDBIDs prototypes;

  /**
   * Flag to indicate squared distances.
   */
  protected boolean isSquared;

  /**
   * Constructor.
   *
   * @param ids IDs
   * @param isSquared Flag to indicate squared distances
   */
  public ClusterMergeHistoryBuilder(ArrayDBIDs ids, boolean isSquared) {
    super();
    this.ids = ids;
    final int n = ids.size();
    this.mergeDistance = new double[n - 1]; // n-1 merges
    this.csize = new int[n - 1]; // n-1 merges
    this.merges = new int[(n - 1) << 1]; // two values each merge
    this.isSquared = isSquared;
  }

  /**
   * A more robust "add" operation (involving a union-find) where we may use
   * arbitrary objects i and j to refer to clusters, not only the largest ID
   * in each cluster.
   * <p>
   * TODO: further improve the union-find used here?
   * 
   * @param i First cluster
   * @param dist Link distance
   * @param j Second cluster
   * @return new cluster id
   */
  public int add(int i, double dist, int j) {
    if(parent == null) {
      assert mergecount == 0;
      parent = MathUtil.sequence(0, (ids.size() << 1) - 1);
    }
    int t = mergecount + ids.size(); // next
    // Follow i to its parent.
    for(int p = parent[i]; i != p;) {
      final int tmp = parent[p];
      parent[i] = t;
      i = p;
      p = tmp;
    }
    // Follow j to its parent.
    for(int p = parent[j]; j != p;) {
      final int tmp = parent[p];
      parent[j] = t;
      j = p;
      p = tmp;
    }
    int t2 = parent[i] = parent[j] = strictAdd(i, dist, j);
    assert t == t2;
    return t2;
  }

  /**
   * Add a merge to the pointer representation. This API requires that the
   * source object is <em>not</em> linked yet, and has a smaller ID than the
   * target, because of the pointer structure representation used by SLINK.
   *
   * @param source Current object
   * @param distance Link distance
   * @param target Parent
   * @return
   */
  public int strictAdd(int source, double distance, int target) {
    final int n = ids.size();
    assert source >= 0 && target >= 0;
    assert source < n + mergecount && target < n + mergecount;
    assert prototypes == null;
    assert source != target;
    int size = (source < n ? 1 : csize[source - n]) + (target < n ? 1 : csize[target - n]);
    mergeDistance[mergecount] = distance;
    csize[mergecount] = size;
    merges[mergecount << 1] = source;
    merges[(mergecount << 1) + 1] = target;
    return mergecount++ + n;
  }

  /**
   * Add an element to the pointer representation.
   *
   * @param source Current object
   * @param distance Link distance
   * @param target Parent
   * @param prototype Cluster prototype
   * @retunr new cluster id
   */
  public int strictAdd(int source, double distance, int target, DBIDRef prototype) {
    final int n = ids.size();
    if(mergecount == 0) {
      prototypes = DBIDUtil.newArray(n - 1);
    }
    assert source >= 0 && target >= 0;
    assert source < n + mergecount && target < n + mergecount;
    assert prototypes != null;
    assert source != target;
    int size = (source < n ? 1 : csize[source - n]) + (target < n ? 1 : csize[target - n]);
    mergeDistance[mergecount] = distance;
    csize[mergecount] = size;
    merges[mergecount << 1] = source;
    merges[(mergecount << 1) + 1] = target;
    prototypes.add(prototype);
    return mergecount++ + n;
  }

  /**
   * Finalize the result.
   *
   * @return Completed result
   */
  public ClusterMergeHistory complete() {
    if(mergecount != ids.size() - 1) {
      LOG.warning(mergecount + " merges were added to the hierarchy, expected " + (ids.size() - 1));
    }
    return prototypes != null ? //
        new ClusterPrototypeMergeHistory(ids, merges, mergeDistance, csize, isSquared, prototypes) : //
        new ClusterMergeHistory(ids, merges, mergeDistance, csize, isSquared);
  }

  /**
   * Build a result with additional coredists information.
   * 
   * @param coredists Core distances (coredists)
   * @return Completed result.
   */
  public ClusterDensityMergeHistory complete(WritableDoubleDataStore coredists) {
    if(mergecount != ids.size() - 1) {
      LOG.warning(mergecount + " merges were added to the hierarchy, expected " + (ids.size() - 1));
    }
    return new ClusterDensityMergeHistory(ids, merges, mergeDistance, csize, isSquared, coredists);
  }

  /**
   * Get the cluster size of the current object.
   *
   * @param id cluster id
   * @return Cluster size (initially 1).
   */
  public int getSize(int id) {
    return id < ids.size() ? 1 : csize[id - ids.size()];
  }

  /**
   * Set the cluster size of an object.
   *
   * @param id Object to set
   * @param size Cluster size
   */
  public void setSize(int id, int size) {
    assert id >= ids.size();
    csize[id - ids.size()] = size;
  }

  /**
   * Find an optimized processing order, where merges are by ascending depth
   * where possible (not guaranteed for non-reducible linkages).
   * <p>
   * E.g., (a,b,2), (c,d,1), (ab,cd,3) is a valid merge sequence, but not well
   * ordered because the n largest splits do not come last.
   * 
   * @return reverse map to the optimized order, or {@code null} if well
   *         ordered, in case you need to remap additional data structures.
   */
  public int[] optimizeOrder() {
    if(checkMonotone()) {
      return null; // No need to reorder
    }
    final int m = mergecount, n = ids.size();
    int[] o1 = MathUtil.sequence(0, m);
    // Sort by max height, merge height, merge number:
    IntegerArrayQuickSort.sort(o1, (a, b) -> {
      final int sa = csize[a], sb = csize[b];
      if(sa < sb) {
        return -1;
      }
      if(sa > sb) {
        return 1;
      }
      final double da = mergeDistance[a], db = mergeDistance[b];
      return da < db ? -1 : da > db ? +1 : (a > b ? -1 : +1);
    });
    // Now we need to ensure merges are consistent in their order
    byte[] seen = new byte[m];
    int[] order = new int[m];
    int size = 0;
    for(int it : o1) {
      if(seen[it] > 0) {
        continue;
      }
      size = addRecursive(order, size, seen, it, n);
      seen[order[size++] = it] = 1;
    }
    assert size == m;
    // Rewrite the result:
    double[] md2 = new double[mergeDistance.length];
    int[] csize2 = new int[csize.length];
    int[] merges2 = new int[merges.length];
    ArrayModifiableDBIDs prototypes2 = prototypes == null ? null : DBIDUtil.newArray(csize.length);
    DBIDVar tmp = prototypes == null ? null : DBIDUtil.newVar();
    int[] reverse = new int[m];
    Arrays.fill(reverse, -1);
    for(int i = 0; i < m; i++) {
      final int oi = order[i];
      final int a = merges[oi << 1], b = merges[(oi << 1) + 1];
      reverse[oi] = i;
      assert a < n || reverse[a - n] >= 0;
      merges2[i << 1] = a < n ? a : reverse[a - n] + n;
      assert b < n || reverse[b - n] >= 0;
      merges2[(i << 1) + 1] = b < n ? b : reverse[b - n] + n;
      md2[i] = mergeDistance[oi];
      csize2[i] = csize[oi];
      if(prototypes != null) {
        prototypes2.add(prototypes2.assignVar(oi, tmp));
      }
    }
    mergeDistance = md2;
    csize = csize2;
    merges = merges2;
    parent = null;
    prototypes = prototypes2;
    return reverse;
  }

  /**
   * Check if merge distances are monotone.
   *
   * @return boolean result
   */
  private boolean checkMonotone() {
    double cur = mergeDistance.length > 0 ? mergeDistance[0] : Double.NaN;
    for(int i = 1; i < mergeDistance.length; i++) {
      double next = mergeDistance[i];
      if(next < cur) {
        return false; // not monotone
      }
      cur = next;
    }
    return true;
  }

  /**
   * Recursively add merges (children first) to the order, to obtain a monotone
   * ordering. As we processed entries with smaller distances first, this
   * usually will not require much recursion, and subtrees with smaller height
   * should already come first. Using the max height helps with inversions in
   * irreducible linkages.
   *
   * @param order Output order
   * @param size Current size
   * @param seen Mask indicating processed entries
   * @param it Current entry
   * @param n Number of primary objects
   * @return Size after additions
   */
  private int addRecursive(int[] order, int size, byte[] seen, int it, int n) {
    int a = merges[it << 1] - n, b = merges[(it << 1) + 1] - n;
    if(a >= 0 && seen[a] == 0) {
      size = addRecursive(order, size, seen, a, n);
      seen[order[size++] = a] = 1;
    }
    if(b >= 0 && seen[b] == 0) {
      size = addRecursive(order, size, seen, b, n);
      seen[order[size++] = b] = 1;
    }
    return size;
  }
}
