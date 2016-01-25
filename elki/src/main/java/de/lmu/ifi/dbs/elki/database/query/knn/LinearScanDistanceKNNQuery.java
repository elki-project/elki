package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;

/**
 * Instance of this query for a particular database.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceQuery
 */
public class LinearScanDistanceKNNQuery<O> extends AbstractDistanceKNNQuery<O> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanDistanceKNNQuery(DistanceQuery<O> distanceQuery) {
    super(distanceQuery);
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    KNNHeap heap = DBIDUtil.newHeap(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double dist = distanceQuery.distance(id, iter);
      if(dist <= max) {
        max = heap.insert(dist, iter);
      }
    }
    return heap.toKNNList();
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    KNNHeap heap = DBIDUtil.newHeap(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double dist = distanceQuery.distance(obj, iter);
      if(dist <= max) {
        max = heap.insert(dist, iter);
      }
    }
    return heap.toKNNList();
  }

  @Override
  public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final int size = ids.size();
    final List<KNNHeap> heaps = new ArrayList<>(size);
    for(int i = 0; i < size; i++) {
      heaps.add(DBIDUtil.newHeap(k));
    }
    linearScanBatchKNN(ids, heaps);
    // Serialize heaps
    List<KNNList> result = new ArrayList<>(size);
    for(KNNHeap heap : heaps) {
      result.add(heap.toKNNList());
    }
    return result;
  }

  /**
   * Linear batch knn for arbitrary distance functions.
   * 
   * @param ids DBIDs to process
   * @param heaps Heaps to store the results in
   */
  private void linearScanBatchKNN(ArrayDBIDs ids, List<KNNHeap> heaps) {
    // The distance is computed on database IDs
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      int index = 0;
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        KNNHeap heap = heaps.get(index);
        heap.insert(distanceQuery.distance(iter2, iter), iter);
        index++;
      }
    }
  }
}
