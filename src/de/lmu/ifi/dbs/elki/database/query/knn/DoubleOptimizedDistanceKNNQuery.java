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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.integer.DoubleDistanceIntegerDBIDKNNHeap;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Optimized linear scan query for {@link PrimitiveDoubleDistanceFunction}s.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDoubleDistanceFunction
 * 
 * @param <O> Object type
 */
public class DoubleOptimizedDistanceKNNQuery<O> extends LinearScanDistanceKNNQuery<O, DoubleDistance> {
  /**
   * Raw distance function.
   */
  PrimitiveDoubleDistanceFunction<O> rawdist;

  /**
   * Constructor.newDoubleDistanceHeap
   * 
   * @param distanceQuery Distance function to use
   */
  @SuppressWarnings("unchecked")
  public DoubleOptimizedDistanceKNNQuery(PrimitiveDistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
    if(!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
      throw new UnsupportedOperationException("DoubleOptimizedKNNQuery instantiated for non-PrimitiveDoubleDistanceFunction!");
    }
    rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
  }

  @Override
  public DoubleDistanceKNNList getKNNForDBID(DBIDRef id, int k) {
    final O obj = this.relation.get(id); // Query object
    DoubleDistanceKNNHeap heap = new DoubleDistanceIntegerDBIDKNNHeap(k);
    linearScan(relation, rawdist, obj, heap);
    return heap.toKNNList();
  }

  @Override
  public DoubleDistanceKNNList getKNNForObject(O obj, int k) {
    DoubleDistanceKNNHeap heap = new DoubleDistanceIntegerDBIDKNNHeap(k);
    linearScan(relation, rawdist, obj, heap);
    return heap.toKNNList();
  }

  private static <O> void linearScan(Relation<? extends O> relation, PrimitiveDoubleDistanceFunction<? super O> rawdist, final O obj, DoubleDistanceKNNHeap heap) {
    double kdist = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double dist = rawdist.doubleDistance(obj, relation.get(iter));
      if(dist <= kdist) {
        kdist = heap.insert(dist, iter);
      }
    }
  }
}
