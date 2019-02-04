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
package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/**
 * Instance of this query for a particular database.
 * 
 * This is a subtle optimization: for primitive queries, it is clearly faster to
 * retrieve the query object from the relation only once!
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - PrimitiveDistanceQuery
 * @assoc - - - PrimitiveDistanceFunction
 */
public class LinearScanPrimitiveDistanceKNNQuery<O> extends AbstractDistanceKNNQuery<O> implements LinearScanQuery {
  /**
   * Unboxed distance function.
   */
  private PrimitiveDistanceFunction<? super O> rawdist;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceKNNQuery(PrimitiveDistanceQuery<O> distanceQuery) {
    super(distanceQuery);
    rawdist = distanceQuery.getDistanceFunction();
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    final Relation<? extends O> relation = getRelation();
    return linearScan(relation, relation.iterDBIDs(), relation.get(id), DBIDUtil.newHeap(k)).toKNNList();
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    final Relation<? extends O> relation = getRelation();
    return linearScan(relation, relation.iterDBIDs(), obj, DBIDUtil.newHeap(k)).toKNNList();
  }

  /**
   * Main loop of the linear scan.
   * 
   * @param relation Data relation
   * @param iter ID iterator
   * @param obj Query object
   * @param heap Output heap
   * @return Heap
   */
  private KNNHeap linearScan(Relation<? extends O> relation, DBIDIter iter, final O obj, KNNHeap heap) {
    final PrimitiveDistanceFunction<? super O> rawdist = this.rawdist;
    double max = Double.POSITIVE_INFINITY;
    while(iter.valid()) {
      final double dist = rawdist.distance(obj, relation.get(iter));
      if(dist <= max) {
        max = heap.insert(dist, iter);
      }
      iter.advance();
    }
    return heap;
  }

  @Override
  public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final Relation<? extends O> relation = getRelation();
    final int size = ids.size();
    final List<KNNHeap> heaps = new ArrayList<>(size);
    List<O> objs = new ArrayList<>(size);
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      heaps.add(DBIDUtil.newHeap(k));
      objs.add(relation.get(iter));
    }
    linearScanBatchKNN(objs, heaps);

    List<KNNList> result = new ArrayList<>(heaps.size());
    for(KNNHeap heap : heaps) {
      result.add(heap.toKNNList());
    }
    return result;
  }

  /**
   * Perform a linear scan batch kNN for primitive distance functions.
   * 
   * @param objs Objects list
   * @param heaps Heaps array
   */
  protected void linearScanBatchKNN(List<O> objs, List<KNNHeap> heaps) {
    final PrimitiveDistanceFunction<? super O> rawdist = this.rawdist;
    final Relation<? extends O> relation = getRelation();
    final int size = objs.size();
    // Linear scan style KNN.
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      O candidate = relation.get(iter);
      for(int index = 0; index < size; index++) {
        final KNNHeap heap = heaps.get(index);
        final double dist = rawdist.distance(objs.get(index), candidate);
        if(dist <= heap.getKNNDistance()) {
          heap.insert(dist, iter);
        }
      }
    }
  }
}