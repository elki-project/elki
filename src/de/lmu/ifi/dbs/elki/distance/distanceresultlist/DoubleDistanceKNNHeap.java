package de.lmu.ifi.dbs.elki.distance.distanceresultlist;

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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Heap for collecting double-valued KNN instances.
 * 
 * See also: {@link KNNUtil#newHeap}!
 * 
 * Experiments have shown that it can be much more performant to track the
 * knndistance <em>outside</em> of the heap, and do comparisons on the stack:
 * <blockquote>
 * 
 * <pre>
 * {@code
 * double knndist = Double.POSITIVE_INFINITY;
 * DoubleDistanceKNNHeap heap = new DoubleDistanceKNNHeap(k);
 * for (DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
 *   double dist = computeDistance(iditer, ...);
 *   if (dist < knndist) {
 *     heap.add(dist, iditer);
 *     if (heap.size() >= k) {
 *       max = heap.doubleKNNDistance();
 *     }
 *   }    
 * }
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * The reason probably is that {@code knndist} resides on the stack and can be
 * better optimized by the hotspot compiler.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceKNNHeap extends AbstractKNNHeap<DoubleDistanceDBIDPair, DoubleDistance> {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Cached distance to k nearest neighbor (to avoid going through {@link #peek}
   * too often).
   */
  protected double knndistance = Double.POSITIVE_INFINITY;

  /**
   * Constructor.
   * 
   * See also: {@link KNNUtil#newHeap}!
   * 
   * @param k Heap size
   */
  public DoubleDistanceKNNHeap(int k) {
    super(k);
  }

  /**
   * Serialize to a {@link DoubleDistanceKNNList}. This empties the heap!
   * 
   * @return KNNList with the heaps contents.
   */
  @Override
  public DoubleDistanceKNNList toKNNList() {
    return new DoubleDistanceKNNList(this);
  }

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   */
  public void add(double distance, DBIDRef id) {
    if(distance <= knndistance) {
      super.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
      if(size() >= maxsize) {
        knndistance = peek().doubleDistance();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @deprecated if you know your distances are double-valued, you should be
   *             using the primitive type.
   * 
   */
  @Override
  @Deprecated
  public void add(DoubleDistance dist, DBIDRef id) {
    add(dist.doubleValue(), id);
  }

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  public double doubleKNNDistance() {
    return knndistance;
  }

  /**
   * {@inheritDoc}
   * 
   * @deprecated if you know your distances are double-valued, you should be
   *             using the primitive type.
   */
  @Override
  @Deprecated
  public DoubleDistance getKNNDistance() {
    return new DoubleDistance(knndistance);
  }
}
