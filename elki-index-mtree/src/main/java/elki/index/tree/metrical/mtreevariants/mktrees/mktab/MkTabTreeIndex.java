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
package elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import java.util.ArrayList;
import java.util.List;

import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.index.KNNIndex;
import elki.index.RKNNIndex;
import elki.index.RangeIndex;
import elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import elki.index.tree.metrical.mtreevariants.query.*;
import elki.persistent.PageFile;

/**
 * MkTabTree used as database index.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public class MkTabTreeIndex<O> extends MkTabTree<O> implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
  /**
   * The relation indexed.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MkTabTreeIndex(Relation<O> relation, PageFile<MkTabTreeNode<O>> pagefile, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry> settings) {
    super(relation, pagefile, settings);
    this.relation = relation;
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   * 
   * @param object the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of
   *        the parent node
   */
  protected MkTabEntry createNewLeafEntry(DBID id, DBIDRef object, double parentDistance) {
    return new MkTabLeafEntry(id, parentDistance, knnDistances(object));
  }

  /**
   * Returns the knn distance of the object with the specified id.
   * 
   * @param object the query object
   * @return the knn distance of the object with the specified id
   */
  private double[] knnDistances(DBIDRef object) {
    KNNList knns = knnq.getKNN(object, getKmax() - 1);
    double[] distances = new double[getKmax()];
    int i = 0;
    for(DoubleDBIDListIter iter = knns.iter(); iter.valid() && i < getKmax(); iter.advance(), i++) {
      distances[i] = iter.doubleValue();
    }
    return distances;
  }

  @Override
  public void initialize() {
    super.initialize();
    List<MkTabEntry> objs = new ArrayList<>(relation.size());
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      objs.add(createNewLeafEntry(id, id, Double.NaN));
    }
    insertAll(objs);
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
