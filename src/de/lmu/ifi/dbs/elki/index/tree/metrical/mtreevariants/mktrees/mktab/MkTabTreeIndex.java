package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnified;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeQueryUtil;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MkTreeRKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert.MTreeInsert;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * MkTabTree used as database index.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkTabTreeIndex<O, D extends Distance<D>> extends MkTabTree<O, D> implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
  /**
   * The knn query we use internally.
   */
  private final KNNQuery<O, D> knnQuery;

  /**
   * The relation indexed.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param splitStrategy Split strategy
   * @param insertStrategy Insertion strategy
   * @param k_max Maximum value for k
   */
  public MkTabTreeIndex(Relation<O> relation, PageFile<MkTabTreeNode<O, D>> pagefile, DistanceQuery<O, D> distanceQuery, MTreeSplit<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> splitStrategy, MTreeInsert<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> insertStrategy, int k_max) {
    super(pagefile, distanceQuery, splitStrategy, insertStrategy, k_max);
    this.relation = relation;
    this.knnQuery = this.getKNNQuery(getDistanceQuery());
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   * 
   * @param object the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of
   *        the parent node
   */
  protected MkTabEntry<D> createNewLeafEntry(DBID id, O object, D parentDistance) {
    return new MkTabLeafEntry<>(id, parentDistance, knnDistances(object));
  }

  /**
   * Returns the knn distance of the object with the specified id.
   * 
   * @param object the query object
   * @return the knn distance of the object with the specified id
   */
  private List<D> knnDistances(O object) {
    KNNList<D> knns = knnQuery.getKNNForObject(object, getKmax() - 1);
    List<D> distances = new ArrayList<>(knns.size());
    for (DistanceDBIDListIter<D> iter = knns.iter(); iter.valid(); iter.advance()) {
      distances.add(iter.getDistance());
    }
    return distances;
  }

  @Override
  public void initialize() {
    super.initialize();
    List<MkTabEntry<D>> objs = new ArrayList<>(relation.size());
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter); // FIXME: expensive
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, getDistanceFactory().undefinedDistance()));
    }
    insertAll(objs);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if (!this.distanceFunction.equals(distanceFunction)) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMTree<O, S, ?, ?> idx = (AbstractMTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return MTreeQueryUtil.getKNNQuery(idx, dq, hints);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RangeQuery<O, S> getRangeQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if (!this.distanceFunction.equals(distanceFunction)) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMTree<O, S, ?, ?> idx = (AbstractMTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return MTreeQueryUtil.getRangeQuery(idx, dq);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if (!this.getDistanceFunction().equals(distanceFunction)) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMkTreeUnified<O, S, ?, ?> idx = (AbstractMkTreeUnified<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return new MkTreeRKNNQuery<>(idx, dq);
  }

  @Override
  public String getLongName() {
    return "MkTab-Tree";
  }

  @Override
  public String getShortName() {
    return "mktabtree";
  }
}
