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

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.AbstractKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TiedTopBoundedHeap;

/**
 * Optimized linear scan query for {@link PrimitiveDoubleDistanceFunction}s.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDoubleDistanceFunction
 * 
 * @param <O> Object type
 */
public class LinearScanRawDoubleDistanceKNNQuery<O> extends LinearScanPrimitiveDistanceKNNQuery<O, DoubleDistance> {
  /**
   * Raw distance function.
   */
  PrimitiveDoubleDistanceFunction<O> rawdist;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  @SuppressWarnings("unchecked")
  public LinearScanRawDoubleDistanceKNNQuery(PrimitiveDistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
    if (!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
      throw new UnsupportedOperationException("LinearScanRawDoubleDistance instantiated for non-RawDoubleDistance!");
    }
    rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
  }

  @Override
  public KNNResult<DoubleDistance> getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public KNNResult<DoubleDistance> getKNNForObject(O obj, int k) {
    // Optimization for double distances.
    // We do NOT use the KNNHeap here. Benchmarking showed this to be faster!
    // Repeat: DO NOT CHANGE TO USE KNNHEAP WITHOUT BENCHMARKING.
    final TiedTopBoundedHeap<DoubleDistanceDBIDPair> heap = new TiedTopBoundedHeap<DoubleDistanceDBIDPair>(k, AbstractKNNHeap.COMPARATOR);
    double max = Double.POSITIVE_INFINITY;
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      final int size = heap.size();
      if (size < k) {
        heap.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
        // Update cutoff
        if (size + 1 >= k) {
          max = heap.peek().doubleDistance();
        }
      } else if (doubleDistance == max) {
        heap.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
      } else if (doubleDistance < max) { // == was handled before!
        heap.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
        max = heap.peek().doubleDistance();
      } else {
        continue; // Do not update max!
      }
    }
    return new DoubleDistanceKNNList(heap, k);
  }
}
