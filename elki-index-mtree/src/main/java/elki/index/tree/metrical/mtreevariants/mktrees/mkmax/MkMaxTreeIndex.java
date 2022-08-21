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
package elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import java.util.ArrayList;
import java.util.List;

import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.index.DynamicIndex;
import elki.index.KNNIndex;
import elki.index.RKNNIndex;
import elki.index.RangeIndex;
import elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import elki.index.tree.metrical.mtreevariants.query.*;
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
public class MkMaxTreeIndex<O> extends MkMaxTree<O> implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O>, DynamicIndex {
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
  protected MkMaxLeafEntry createNewLeafEntry(DBID id, DBIDRef object, double parentDistance) {
    KNNList knns = knnq.getKNN(object, getKmax() - 1);
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
    insert(createNewLeafEntry(DBIDUtil.deref(id), id, Double.NaN), false);
  }

  @Override
  public void insertAll(DBIDs ids) {
    List<MkMaxEntry> objs = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      objs.add(createNewLeafEntry(id, id, Double.NaN));
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
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.getDistance().equals(distanceQuery.getDistance()) ? //
            new MTreeKNNByObject<>(this, distanceQuery) : null;
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.getDistance().equals(distanceQuery.getDistance()) ? //
            new MTreeKNNByDBID<>(this, distanceQuery) : null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.getDistance().equals(distanceQuery.getDistance()) ? //
            new MTreeRangeByObject<>(this, distanceQuery) : null;
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.getDistance().equals(distanceQuery.getDistance()) ? //
            new MTreeRangeByDBID<>(this, distanceQuery) : null;
  }

  @Override
  public RKNNSearcher<O> rkNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return null;
  }

  @Override
  public RKNNSearcher<DBIDRef> rkNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.getDistance().equals(distanceQuery.getDistance()) ? //
            new MkTreeRKNNQuery<>(this, distanceQuery) : null;
  }
}
