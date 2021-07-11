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

/**
 * Class to help building a pointer hierarchy.
 *
 * @author Erich Schubert
 * @since 0.7.1
 *
 * @has - - - PointerHierarchyResult
 */
public class PointerHierarchyBuilder {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PointerHierarchyBuilder.class);

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
   * Cluster size storage. May be uninitialized!
   */
  protected WritableIntegerDataStore csize;

  /**
   * Store merge order.
   */
  protected WritableIntegerDataStore order;

  /**
   * Merge counter (for merge ordering).
   */
  protected int mergecount = 0;

  /**
   * Prototype storage, may be {@code null}.
   */
  protected WritableDBIDDataStore prototypes;

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
  public PointerHierarchyBuilder(DBIDs ids, boolean isSquared) {
    super();
    this.ids = ids;
    this.parent = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    this.parentDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      parent.put(it, it);
    }
    this.order = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, ids.size());
    this.isSquared = isSquared;
  }

  /**
   * Helper variables used for union-find.
   * <p>
   * TODO: once Java has value types, this likely is no longer beneficial.
   */
  private DBIDVar p = DBIDUtil.newVar(), q = DBIDUtil.newVar(),
      n = DBIDUtil.newVar();

  /**
   * A more robust "add" operation (involving a union-find) where we may use
   * arbitrary objects i and j to refer to clusters, not only the largest ID
   * in each cluster.
   * <p>
   * TODO: worth implementing a union-find halving optimization?
   * 
   * @param i First cluster
   * @param dist Link distance
   * @param j Second cluster
   */
  public void add(DBIDRef i, double dist, DBIDRef j) {
    p.set(i);
    // Follow p to its parent.
    while(!DBIDUtil.equal(p, parent.assignVar(p, n))) {
      p.set(n);
    }
    // Follow q to its parent.
    q.set(j);
    while(!DBIDUtil.equal(q, parent.assignVar(q, n))) {
      q.set(n);
    }
    // By definition of the pointer representation, the largest element in
    // each cluster is the cluster lead.
    // The extraction methods currently rely on this!
    final int c = DBIDUtil.compare(p, q);
    if(c == 0) {
      throw new IllegalStateException("Merging cluster to itself!");
    }
    strictAdd(c > 0 ? p : q, dist, c > 0 ? q : p);
  }

  /**
   * Add a merge to the pointer representation. This API requires that the
   * source object is <em>not</em> linked yet, and has a smaller ID than the
   * target, because of the pointer structure representation used by SLINK.
   *
   * @param source Current object
   * @param distance Link distance
   * @param target Parent
   */
  public void strictAdd(DBIDRef source, double distance, DBIDRef target) {
    assert prototypes == null;
    assert DBIDUtil.compare(source, target) > 0;
    parent.putDBID(source, target);
    double olddist = parentDistance.putDouble(source, distance);
    assert (olddist == Double.POSITIVE_INFINITY) : "Object was already linked!";
    order.putInt(source, mergecount);
    ++mergecount;
  }

  /**
   * A more robust "add" operation (involving a union-find) where we may use
   * arbitrary objects i and j to refer to clusters, not only the largest ID
   * in each cluster.
   * <p>
   * TODO: worth implementing a union-find halving optimization?
   * 
   * @param i First cluster
   * @param dist Link distance
   * @param j Second cluster
   * @param prototype Cluster prototype
   */
  public void add(DBIDRef i, double dist, DBIDRef j, DBIDRef prototype) {
    p.set(i);
    // Follow p to its parent.
    while(!DBIDUtil.equal(p, parent.assignVar(p, n))) {
      p.set(n);
    }
    // Follow q to its parent.
    q.set(j);
    while(!DBIDUtil.equal(q, parent.assignVar(q, n))) {
      q.set(n);
    }
    // By definition of the pointer representation, the largest element in
    // each cluster is the cluster lead.
    // The extraction methods currently rely on this!
    final int c = DBIDUtil.compare(p, q);
    if(c == 0) {
      throw new IllegalStateException("Merging cluster to itself!");
    }
    strictAdd(c > 0 ? p : q, dist, c > 0 ? q : p, prototype);
  }

  /**
   * Add an element to the pointer representation.
   *
   * @param source Current object
   * @param distance Link distance
   * @param target Parent
   * @param prototype Cluster prototype
   */
  public void strictAdd(DBIDRef source, double distance, DBIDRef target, DBIDRef prototype) {
    if(mergecount == 0) {
      prototypes = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    }
    assert prototypes != null;
    assert DBIDUtil.compare(source, target) > 0;
    parent.putDBID(source, target);
    prototypes.putDBID(source, prototype);
    double olddist = parentDistance.putDouble(source, distance);
    assert (olddist == Double.POSITIVE_INFINITY) : "Object was already linked!";
    order.putInt(source, mergecount);
    ++mergecount;
  }

  /**
   * Finalize the result.
   *
   * @return Completed result
   */
  public PointerHierarchyResult complete() {
    if(csize != null) {
      csize.destroy();
      csize = null;
    }
    if(mergecount != ids.size() - 1) {
      LOG.warning(mergecount + " merges were added to the hierarchy, expected " + (ids.size() - 1));
    }
    return prototypes != null ? //
        new PointerPrototypeHierarchyResult(ids, parent, parentDistance, isSquared, order, prototypes) : //
        new PointerHierarchyResult(ids, parent, parentDistance, isSquared, order);
  }

  /**
   * Build a result with additional density information.
   * 
   * @param coredists Core distances (density)
   * @return Completed result.
   */
  public PointerDensityHierarchyResult complete(WritableDoubleDataStore coredists) {
    if(csize != null) {
      csize.destroy();
      csize = null;
    }
    if(mergecount != ids.size() - 1) {
      LOG.warning(mergecount + " merges were added to the hierarchy, expected " + (ids.size() - 1));
    }
    return new PointerDensityHierarchyResult(ids, parent, parentDistance, isSquared, coredists);
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
