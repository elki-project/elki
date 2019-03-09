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
package elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import java.util.ArrayList;
import java.util.List;

import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.query.rknn.RKNNQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.index.DynamicIndex;
import elki.index.KNNIndex;
import elki.index.RKNNIndex;
import elki.index.RangeIndex;
import elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import elki.index.tree.metrical.mtreevariants.query.MTreeKNNQuery;
import elki.index.tree.metrical.mtreevariants.query.MTreeRangeQuery;
import elki.index.tree.metrical.mtreevariants.query.MkTreeRKNNQuery;
import elki.persistent.PageFile;
import elki.utilities.exceptions.NotImplementedException;

/**
 * MkMax tree
 *
 * @author Elke Achtert
 * @since 0.4.0
 *
 * @param <O> Object type
 */
public class MkMaxTreeIndex<O> extends MkMaxTree<O>implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O>, DynamicIndex {
  /**
   * Relation indexed.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   *
   * @param relation Relation
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MkMaxTreeIndex(Relation<O> relation, PageFile<MkMaxTreeNode<O>> pagefile, MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry> settings) {
    super(relation, pagefile, settings);
    this.relation = relation;
  }

  /**
   * @return a new MkMaxLeafEntry representing the specified data object
   */
  protected MkMaxLeafEntry createNewLeafEntry(DBID id, O object, double parentDistance) {
    KNNList knns = knnq.getKNNForObject(object, getKmax() - 1);
    double knnDistance = knns.getKNNDistance();
    return new MkMaxLeafEntry(id, parentDistance, knnDistance);
  }

  @Override
  public void initialize() {
    super.initialize();
    insertAll(relation.getDBIDs());
  }

  @Override
  public void insert(DBIDRef id) {
    insert(createNewLeafEntry(DBIDUtil.deref(id), relation.get(id), Double.NaN), false);
  }

  @Override
  public void insertAll(DBIDs ids) {
    List<MkMaxEntry> objs = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, Double.NaN));
    }
    insertAll(objs);
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   *
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @Override
  public final boolean delete(DBIDRef id) {
    throw new NotImplementedException();
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   *
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @Override
  public void deleteAll(DBIDs ids) {
    throw new NotImplementedException();
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    Distance<? super O> distanceFunction = (Distance<? super O>) distanceQuery.getDistance();
    if(!this.getDistance().equals(distanceFunction)) {
      getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new MTreeKNNQuery<>(this, distanceQuery);
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    Distance<? super O> distanceFunction = (Distance<? super O>) distanceQuery.getDistance();
    if(!this.getDistance().equals(distanceFunction)) {
      getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new MTreeRangeQuery<>(this, distanceQuery);
  }

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    Distance<? super O> distanceFunction = (Distance<? super O>) distanceQuery.getDistance();
    if(!this.getDistance().equals(distanceFunction)) {
      getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new MkTreeRKNNQuery<>(this, distanceQuery);
  }

  @Override
  public String getLongName() {
    return "MkMax-Tree";
  }

  @Override
  public String getShortName() {
    return "mkmaxtree";
  }
}
