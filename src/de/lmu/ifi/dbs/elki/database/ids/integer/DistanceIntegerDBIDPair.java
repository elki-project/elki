package de.lmu.ifi.dbs.elki.database.ids.integer;
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Class storing a double distance a DBID.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
class DistanceIntegerDBIDPair<D extends Distance<D>> implements DistanceDBIDPair<D>, IntegerDBIDRef {
  /**
   * The distance value
   */
  D distance;

  /**
   * The integer DBID
   */
  int id;

  /**
   * Constructor.
   * 
   * @param distance Distance
   * @param id Object ID
   */
  protected DistanceIntegerDBIDPair(D distance, int id) {
    super();
    this.distance = distance;
    this.id = id;
  }

  @Override
  public D getDistance() {
    return distance;
  }

  @Override
  public DBIDRef deref() {
    return this;
  }

  @Override
  public int getIntegerID() {
    return id;
  }

  @Override
  public int compareByDistance(DistanceDBIDPair<D> o) {
    return distance.compareTo(o.getDistance());
  }

  @Override
  public String toString() {
    return distance.toString() + ":" + id;
  }

  @Override
  public boolean equals(Object o) {
    if(o instanceof DistanceIntegerDBIDPair) {
      DistanceIntegerDBIDPair<?> p = (DistanceIntegerDBIDPair<?>) o;
      return (this.id == p.id) && distance.equals(p.getDistance());
    }
    if(o instanceof DoubleDistanceIntegerDBIDPair && distance instanceof DoubleDistance) {
      DoubleDistanceIntegerDBIDPair p = (DoubleDistanceIntegerDBIDPair) o;
      return (this.id == p.id) && (((DoubleDistance) this.distance).doubleValue() == p.distance);
    }
    return false;
  }
}