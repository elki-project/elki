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

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.generic.DistanceDBIDPairKNNList;
import de.lmu.ifi.dbs.elki.database.ids.generic.DoubleDistanceDBIDPairKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.generic.DoubleDistanceDBIDPairKNNList;
import de.lmu.ifi.dbs.elki.database.ids.integer.DoubleDistanceIntegerDBIDKNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparatorMinHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
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
public class DoubleOptimizedKNNQuery<O> extends LinearScanKNNQuery<O, DoubleDistance> {
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
  public DoubleOptimizedKNNQuery(PrimitiveDistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
    if (!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
      throw new UnsupportedOperationException("DoubleOptimizedKNNQuery instantiated for non-PrimitiveDoubleDistanceFunction!");
    }
    rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
  }

  @Override
  public KNNList<DoubleDistance> getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public KNNList<DoubleDistance> getKNNForObject(O obj, int k) {
    return getKNNForObjectBenchmarked(obj, k);
  }

  /**
   * This is the straightforward implementation using the optimized heap.
   * 
   * @param obj Query object
   * @param k Desired number of neighbors
   * @return kNN result
   */
  KNNList<DoubleDistance> getKNNForObjectKNNHeap(O obj, int k) {
    // Optimization for double distances.
    final DoubleDistanceKNNHeap heap = (DoubleDistanceKNNHeap) DBIDUtil.newHeap(DoubleDistance.FACTORY, k);
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      heap.add(rawdist.doubleDistance(obj, relation.get(iter)), iter);
    }
    return heap.toKNNList();
  }

  /**
   * This is the cleaner, supposedly faster implementation.
   * 
   * @param obj Query object
   * @param k Desired number of neighbors
   * @return kNN result
   */
  KNNList<DoubleDistance> getKNNForObjectClean(O obj, int k) {
    // Optimization for double distances.
    final TiedTopBoundedHeap<DoubleDistanceDBIDPair> heap = new TiedTopBoundedHeap<>(k, DoubleDistanceDBIDPairKNNHeap.COMPARATOR);
    final DBIDIter iter = relation.iterDBIDs();

    // First k elements don't need checking.
    double max = 0.;
    for (int i = 0; i < k && iter.valid(); i++, iter.advance()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      heap.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
      max = Math.max(max, doubleDistance);
    }
    // Remaining elements
    for (; iter.valid(); iter.advance()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      if (doubleDistance <= max) {
        heap.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
      }
      if (doubleDistance < max) {
        max = heap.peek().doubleDistance();
      }
    }
    return new DoubleDistanceDBIDPairKNNList(heap, k);
  }

  /**
   * It does not make sense, but this version is faster in our larger
   * benchmarks. Apparently, some JIT optimization kicks in better.
   * 
   * @param obj Query object
   * @param k Desired number of neighbors
   * @return kNN result
   */
  KNNList<DoubleDistance> getKNNForObjectBenchmarked(O obj, int k) {
    // THIS SHOULD BE SLOWER THAN THE VERSION ABOVE, BUT ISN'T!
    final TiedTopBoundedHeap<DistanceDBIDPair<DoubleDistance>> heap = new TiedTopBoundedHeap<>(k, DistanceDBIDResultUtil.BY_REVERSE_DISTANCE);
    final DBIDIter iter = relation.iterDBIDs();
    // First k elements don't need checking.
    double max = 0.;
    for (int i = 0; i < k && iter.valid(); i++, iter.advance()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      heap.add(DBIDFactory.FACTORY.newDistancePair(new DoubleDistance(doubleDistance), iter));
      max = Math.max(max, doubleDistance);
    }
    // Remaining elements
    for (; iter.valid(); iter.advance()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      if (doubleDistance <= max) {
        heap.add(DBIDFactory.FACTORY.newDistancePair(new DoubleDistance(doubleDistance), iter));
      }
      if (doubleDistance < max) {
        max = heap.peek().getDistance().doubleValue();
      }
    }
    return new DistanceDBIDPairKNNList<>(heap, k);
  }

  /**
   * Another attempt at getting a faster knn heap.
   * 
   * @param obj Query object
   * @param k Desired number of neighbors
   * @return kNN result
   */
  KNNList<DoubleDistance> getKNNForObjectBenchmarked2(O obj, int k) {
    final Heap<DoubleDistanceDBIDPair> heap = new Heap<>(k, DistanceDBIDResultUtil.BY_REVERSE_DISTANCE);
    final ArrayList<DoubleDistanceDBIDPair> ties = new ArrayList<>();
    final DBIDIter iter = relation.iterDBIDs();
    // First k elements don't need checking.
    double max = 0.;
    for (int i = 0; i < k && iter.valid(); i++, iter.advance()) {
      final double doubleDistance = rawdist.distance(obj, relation.get(iter)).doubleValue();
      heap.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
      max = Math.max(max, doubleDistance);
    }
    // Remaining elements
    for (; iter.valid(); iter.advance()) {
      final double doubleDistance = rawdist.distance(obj, relation.get(iter)).doubleValue();
      if (doubleDistance <= max) {
        if (doubleDistance < max) {
          DoubleDistanceDBIDPair prev = heap.replaceTopElement(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
          double newkdist = heap.peek().doubleDistance();
          if (newkdist < max) {
            max = newkdist;
            ties.clear();
          } else {
            ties.add(prev);
          }
        } else {
          ties.add(DBIDFactory.FACTORY.newDistancePair(doubleDistance, iter));
        }
      }
    }

    DoubleDistanceIntegerDBIDKNNList ret = new DoubleDistanceIntegerDBIDKNNList(k, k + ties.size());
    for (DoubleDistanceDBIDPair pair : ties) {
      ret.add(pair);
    }
    while (!heap.isEmpty()) {
      ret.add(heap.poll());
    }
    ret.sort(); // Actually, reverse.
    return ret;
  }
}
