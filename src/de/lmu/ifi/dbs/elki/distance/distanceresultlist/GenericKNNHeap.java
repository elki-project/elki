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
   * Cached distance to k nearest neighbor (to avoid going through {@link #peek})
   */
  protected D knndistance = null;

  /**
   * Constructor.
   * 
   * @param k Heap size
   */
  protected GenericKNNHeap(int k) {
    super(k);
  }

  /**
   * Serialize to a {@link GenericKNNList}. This empties the heap!
   * 
   * @return KNNList with the heaps contents.
   */
  @Override
  public GenericKNNList<D> toKNNList() {
    return new GenericKNNList<D>(this);
  }

  @Override
  public void add(D distance, DBIDRef id) {
    if(size() < maxsize || knndistance.compareTo(distance) >= 0) {
      super.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
      if (size() >= maxsize) {
        knndistance = peek().getDistance();        
      }
    }
  }

  @Override
  public D getKNNDistance() {
    return knndistance;
  }
}