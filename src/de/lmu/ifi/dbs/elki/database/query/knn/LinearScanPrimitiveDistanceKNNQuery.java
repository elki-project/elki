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
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

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
public class LinearScanPrimitiveDistanceKNNQuery<O, D extends Distance<D>> extends LinearScanKNNQuery<O, D> {
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
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      for(int index = 0; index < size; index++) {
        O object = objs.get(index);
        KNNHeap<D> heap = heaps.get(index);
        heap.add(distanceQuery.distance(object, candidate), candidateID);
      }
    }
  }

  @Override
  public KNNResult<D> getKNNForDBID(DBID id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public List<KNNResult<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final int size = ids.size();
    final List<KNNHeap<D>> heaps = new ArrayList<KNNHeap<D>>(size);
    List<O> objs = new ArrayList<O>(size);
    for(DBID id : ids) {
      heaps.add(new KNNHeap<D>(k));
      objs.add(relation.get(id));
    }
    linearScanBatchKNN(objs, heaps);

    List<KNNResult<D>> result = new ArrayList<KNNResult<D>>(heaps.size());
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toKNNList());
    }
    return result;
  }

  @Override
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
    List<O> objs = new ArrayList<O>(heaps.size());
    List<KNNHeap<D>> kheaps = new ArrayList<KNNHeap<D>>(heaps.size());
    for(Entry<DBID, KNNHeap<D>> ent : heaps.entrySet()) {
      objs.add(relation.get(ent.getKey()));
      kheaps.add(ent.getValue());
    }
    linearScanBatchKNN(objs, kheaps);
  }
}