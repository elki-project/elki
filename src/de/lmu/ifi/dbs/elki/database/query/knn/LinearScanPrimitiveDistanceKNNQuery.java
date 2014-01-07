package de.lmu.ifi.dbs.elki.database.query.knn;

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

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Instance of this query for a particular database.
 * 
 * This is a subtle optimization: for primitive queries, it is clearly faster to
 * retrieve the query object from the relation only once!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDistanceQuery
 */
public class LinearScanPrimitiveDistanceKNNQuery<O, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceKNNQuery(PrimitiveDistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
  }

  /**
   * Perform a linear scan batch kNN for primitive distance functions.
   * 
   * @param objs Objects list
   * @param heaps Heaps array
   */
  protected void linearScanBatchKNN(List<O> objs, List<KNNHeap<D>> heaps) {
    final int size = objs.size();
    // Linear scan style KNN.
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      O candidate = relation.get(iter);
      for(int index = 0; index < size; index++) {
        O object = objs.get(index);
        heaps.get(index).insert(distanceQuery.distance(object, candidate), iter);
      }
    }
  }

  @Override
  public KNNList<D> getKNNForDBID(DBIDRef id, int k) {
    final O obj = relation.get(id);
    KNNHeap<D> heap = DBIDUtil.newHeap(distanceQuery.getDistanceFactory(), k);
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      heap.insert(distanceQuery.distance(obj, iter), iter);
    }
    return heap.toKNNList();
  }

  @Override
  public KNNList<D> getKNNForObject(O obj, int k) {
    KNNHeap<D> heap = DBIDUtil.newHeap(distanceQuery.getDistanceFactory(), k);
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      heap.insert(distanceQuery.distance(obj, iter), iter);
    }
    return heap.toKNNList();
  }

  @Override
  public List<KNNList<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final int size = ids.size();
    final List<KNNHeap<D>> heaps = new ArrayList<>(size);
    List<O> objs = new ArrayList<>(size);
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      heaps.add(DBIDUtil.newHeap(distanceQuery.getDistanceFactory(), k));
      objs.add(relation.get(iter));
    }
    linearScanBatchKNN(objs, heaps);

    List<KNNList<D>> result = new ArrayList<>(heaps.size());
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toKNNList());
    }
    return result;
  }
}