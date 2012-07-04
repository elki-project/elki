package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Instance of this query for a particular database.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceQuery
 */
public class LinearScanKNNQuery<O, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanKNNQuery(DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
  }

  /**
   * Linear batch knn for arbitrary distance functions.
   * 
   * @param ids DBIDs to process
   * @param heaps Heaps to store the results in
   */
  private void linearScanBatchKNN(ArrayDBIDs ids, List<KNNHeap<DistanceDBIDPair<D>, D>> heaps) {
    // The distance is computed on database IDs
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      int index = 0;
      for (DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        KNNHeap<DistanceDBIDPair<D>, D> heap = heaps.get(index);
        heap.add(DBIDFactory.FACTORY.newDistancePair(distanceQuery.distance(iter2, iter), iter));
        index++;
      }
    }
  }

  @Override
  public KNNResult<D> getKNNForDBID(DBIDRef id, int k) {
    KNNHeap<DistanceDBIDPair<D>, D> heap = new KNNHeap<DistanceDBIDPair<D>, D>(k);
    if(PrimitiveDistanceQuery.class.isInstance(distanceQuery)) {
      O obj = relation.get(id);
      for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        heap.add(DBIDFactory.FACTORY.newDistancePair(distanceQuery.distance(obj, relation.get(iter)), iter));
      }
    }
    else {
      for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        heap.add(DBIDFactory.FACTORY.newDistancePair(distanceQuery.distance(id, iter), iter));
      }
    }
    return heap.toKNNList();
  }

  @Override
  public List<KNNResult<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final int size = ids.size();
    final List<KNNHeap<DistanceDBIDPair<D>, D>> heaps = new ArrayList<KNNHeap<DistanceDBIDPair<D>, D>>(size);
    for(int i = 0; i < size; i++) {
      heaps.add(new KNNHeap<DistanceDBIDPair<D>, D>(k));
    }
    linearScanBatchKNN(ids, heaps);
    // Serialize heaps
    List<KNNResult<D>> result = new ArrayList<KNNResult<D>>(size);
    for(KNNHeap<DistanceDBIDPair<D>, D> heap : heaps) {
      result.add(heap.toKNNList());
    }
    return result;
  }

  @Override
  public KNNResult<D> getKNNForObject(O obj, int k) {
    KNNHeap<DistanceDBIDPair<D>, D> heap = new KNNHeap<DistanceDBIDPair<D>, D>(k);
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      heap.add(DBIDFactory.FACTORY.newDistancePair(distanceQuery.distance(obj, iter), iter));
    }
    return heap.toKNNList();
  }
}