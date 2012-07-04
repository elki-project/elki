package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Class storing a double distance a DBID.
 * 
 * @author Erich Schubert
 */
class DoubleDistanceIntegerDBIDPair implements DoubleDistanceDBIDPair, IntegerDBIDRef {
  /**
   * The distance value
   */
  double distance;

  /**
   * The integer DBID
   */
  int id;

  /**
   * Constructor.
   * 
   * @param distance Distance value
   * @param id integer object ID
   */
  protected DoubleDistanceIntegerDBIDPair(double distance, int id) {
    super();
    this.distance = distance;
    this.id = id;
  }

  @Override
  public double doubleDistance() {
    return distance;
  }

  @Override
  public DoubleDistance getDistance() {
    return new DoubleDistance(distance);
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
  public int compareByDistance(DistanceDBIDPair<DoubleDistance> o) {
    if(o instanceof DoubleDistanceDBIDPair) {
      return Double.compare(distance, ((DoubleDistanceDBIDPair) o).doubleDistance());
    }
    return Double.compare(distance, o.getDistance().doubleValue());
  }

  @Override
  public String toString() {
    return distance + ":" + id;
  }

  @Override
  public boolean equals(Object o) {
    if(o instanceof DoubleDistanceIntegerDBIDPair) {
      DoubleDistanceIntegerDBIDPair p = (DoubleDistanceIntegerDBIDPair) o;
      return (this.id == p.id) && (this.distance == p.distance);
    }
    if(o instanceof DistanceIntegerDBIDPair) {
      DistanceIntegerDBIDPair<?> p = (DistanceIntegerDBIDPair<?>) o;
      if(p.distance instanceof DoubleDistance) {
        return (this.id == p.id) && (this.distance == ((DoubleDistance) p.distance).doubleValue());
      }
    }
    return false;
  }
}