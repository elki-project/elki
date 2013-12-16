package de.lmu.ifi.dbs.elki.database.ids.generic;

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

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Heap for collecting double-valued KNN instances.
 * 
 * See also: {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newHeap}!
 * 
 * Experiments have shown that it <em>can</em> be much more performant to track
 * the knndistance <em>outside</em> of the heap, and do comparisons on the
 * stack: <blockquote>
 * 
 * <pre>
 * {@code
 * double knndist = Double.POSITIVE_INFINITY;
 * DoubleDistanceDBIDPairKNNHeap heap = new DoubleDistanceDBIDPairKNNHeap(k);
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
public class DoubleDistanceDBIDPairKNNHeap extends AbstractKNNHeap<DoubleDistanceDBIDPair, DoubleDistance> implements DoubleDistanceKNNHeap {
  /**
   * Comparator class.
   */
  public static final Comparator<DoubleDistanceDBIDPair> COMPARATOR = new Comp();

  /**
   * Reverse comparator.
   */
  public static final Comparator<DoubleDistanceDBIDPair> REVERSE_COMPARATOR = new RComp();

  /**
   * Cached distance to k nearest neighbor (to avoid going through {@link #peek}
   * too often).
   */
  protected double knndistance = Double.POSITIVE_INFINITY;

  /**
   * Constructor.
   * 
   * See also: {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newHeap}!
   * 
   * @param k Heap size
   */
  public DoubleDistanceDBIDPairKNNHeap(int k) {
    super(k);
  }

  /**
   * Serialize to a {@link DoubleDistanceDBIDPairKNNList}. This empties the
   * heap!
   * 
   * @return KNNList with the heaps contents.
   */
  @Override
  public DoubleDistanceDBIDPairKNNList toKNNList() {
    return new DoubleDistanceDBIDPairKNNList(this);
  }

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   * @return knn distance.
   */
  @Override
  public final double insert(final double distance, final DBIDRef id) {
    if (size() < getK() || knndistance >= distance) {
      heap.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
      heapModified();
    }
    return knndistance;
  }

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   */
  @Override
  @Deprecated
  public final void insert(final Double distance, final DBIDRef id) {
    if (size() < getK() || knndistance >= distance) {
      heap.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
      heapModified();
    }
  }

  // @Override
  protected void heapModified() {
    // super.heapModified();
    if (size() >= getK()) {
      knndistance = heap.peek().doubleDistance();
    }
  }

  @Override
  public void insert(final DoubleDistanceDBIDPair e) {
    if (size() < getK() || knndistance >= e.doubleDistance()) {
      heap.add(e);
      heapModified();
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
  public void insert(DoubleDistance dist, DBIDRef id) {
    insert(dist.doubleValue(), id);
  }

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  @Override
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

  /**
   * Comparator to use.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class Comp implements Comparator<DoubleDistanceDBIDPair> {
    @Override
    public int compare(DoubleDistanceDBIDPair o1, DoubleDistanceDBIDPair o2) {
      return -Double.compare(o1.doubleDistance(), o2.doubleDistance());
    }
  }

  /**
   * Comparator to use.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class RComp implements Comparator<DoubleDistanceDBIDPair> {
    @Override
    public int compare(DoubleDistanceDBIDPair o1, DoubleDistanceDBIDPair o2) {
      return Double.compare(o1.doubleDistance(), o2.doubleDistance());
    }
  }
}
