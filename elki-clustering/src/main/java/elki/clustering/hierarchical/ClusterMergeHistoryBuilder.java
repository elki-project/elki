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
package elki.clustering.hierarchical;

import elki.database.datastore.*;
import elki.database.ids.*;
import elki.logging.Logging;
import elki.math.MathUtil;

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
   * Track cluster merges
   */
  protected int[] clusterid;

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
    this.clusterid = new int[(n << 1) - 1]; // N + (N-1)
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
    assert prototypes == null;
    assert source != target;
    final int n = ids.size();
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
   * @param ids Object to set
   * @param size Cluster size
   */
  public void setSize(int id, int size) {
    assert id > ids.size();
    assert csize[size - ids.size()] == size;
    csize[size - ids.size()] = size;
  }
}
