package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
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
  private void linearScanBatchKNN(ArrayDBIDs ids, List<KNNHeap<D>> heaps) {
    // The distance is computed on database IDs
    for(DBID candidateID : relation.iterDBIDs()) {
      Integer index = -1;
      for(DBID id : ids) {
        index++;
        KNNHeap<D> heap = heaps.get(index);
        heap.add(distanceQuery.distance(id, candidateID), candidateID);
      }
    }
  }

  @Override
  public KNNResult<D> getKNNForDBID(DBID id, int k) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    if(PrimitiveDistanceQuery.class.isInstance(distanceQuery)) {
      O obj = relation.get(id);
      for(DBID candidateID : relation.iterDBIDs()) {
        heap.add(distanceQuery.distance(obj, relation.get(candidateID)), candidateID);
      }
    }
    else {
      for(DBID candidateID : relation.iterDBIDs()) {
        heap.add(distanceQuery.distance(id, candidateID), candidateID);
      }
    }
    return heap.toKNNList();
  }

  @Override
  public List<KNNResult<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final int size = ids.size();
    final List<KNNHeap<D>> heaps = new ArrayList<KNNHeap<D>>(size);
    for(int i = 0; i < size; i++) {
      heaps.add(new KNNHeap<D>(k));
    }
    linearScanBatchKNN(ids, heaps);
    // Serialize heaps
    List<KNNResult<D>> result = new ArrayList<KNNResult<D>>(size);
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toKNNList());
    }
    return result;
  }

  @Override
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
    final int size = heaps.size();
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(size);
    List<KNNHeap<D>> kheaps = new ArrayList<KNNHeap<D>>(size);
    for(Entry<DBID, KNNHeap<D>> ent : heaps.entrySet()) {
      ids.add(ent.getKey());
      kheaps.add(ent.getValue());
    }
    linearScanBatchKNN(ids, kheaps);
  }

  @Override
  public KNNResult<D> getKNNForObject(O obj, int k) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      heap.add(distanceQuery.distance(obj, candidate), candidateID);
    }
    return heap.toKNNList();
  }
}