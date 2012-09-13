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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNList;
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
    if(!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
      throw new UnsupportedOperationException("LinearScanRawDoubleDistance instantiated for non-RawDoubleDistance!");
    }
    rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
  }

  @Override
  public DoubleDistanceKNNList getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public DoubleDistanceKNNList getKNNForObject(O obj, int k) {
    // Optimization for double distances.
    final DoubleDistanceKNNHeap heap = new DoubleDistanceKNNHeap(k);
    // Checking max here has a surprisingly large effect on performance
    // Probably due to inlining by the Hotspot VM.
    double max = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      if (doubleDistance <= max) {
        heap.add(doubleDistance, iter);
        max = heap.doubleKNNDistance();
      }
    }
    return heap.toKNNList();
  }
}