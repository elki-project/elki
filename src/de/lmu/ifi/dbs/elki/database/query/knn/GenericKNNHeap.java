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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Heap for collecting kNN candiates with arbitrary distance types.
 * 
 * For double distances, see {@link DoubleDistanceKNNHeap}
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class GenericKNNHeap<D extends Distance<D>> extends AbstractKNNHeap<DistanceDBIDPair<D>, D> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Maximum distance, usually infiniteDistance
   */
  private final D maxdist;

  /**
   * Constructor with distance threshold
   * 
   * @param k Heap size
   * @param maxdist Maximum distance
   */
  public GenericKNNHeap(int k, D maxdist) {
    super(k);
    this.maxdist = maxdist;
  }

  /**
   * Constructor.
   * 
   * @param k Heap size
   */
  public GenericKNNHeap(int k) {
    super(k);
    this.maxdist = null;
  }

  @Override
  public boolean add(D distance, DBIDRef id) {
    if(maxdist != null) {
      if(maxdist.compareTo(distance) < 0) {
        return true;
      }
    }
    if(size() < maxsize || peek().getDistance().compareTo(distance) >= 0) {
      return super.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
    }
    return true; /* "success" */
  }
}