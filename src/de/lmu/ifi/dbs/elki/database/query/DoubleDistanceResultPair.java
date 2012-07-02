package de.lmu.ifi.dbs.elki.database.query;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Optimized DistanceResultPair that avoids/postpones an extra layer of boxing
 * for double values.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceResultPair implements DistanceResultPair<DoubleDistance> {
  /**
   * Distance value
   */
  double distance;

  /**
   * Object ID
   */
  DBID id;

  /**
   * Constructor.
   * 
   * @param distance Distance value
   * @param id Object ID
   */
  public DoubleDistanceResultPair(double distance, DBID id) {
    super();
    this.distance = distance;
    this.id = id;
  }

  @Override
  public DoubleDistance getDistance() {
    return new DoubleDistance(distance);
  }

  @Override
  public void setDistance(DoubleDistance distance) {
    this.distance = distance.doubleValue();
  }

  @Override
  public DBID getDBID() {
    return id;
  }

  @Override
  public int getIntegerID() {
    return DBIDFactory.FACTORY.asInteger(id);
  }

  @Override
  public boolean sameDBID(DBIDRef other) {
    return id.sameDBID(other);
  }

  @Override
  public int compareDBID(DBIDRef other) {
    return id.compareDBID(other);
  }

  @Override
  public void setID(DBID id) {
    this.id = id;
  }

  /**
   * @deprecated Use {@link #getDoubleDistance} or {@link #getDistance} for clearness.
   */
  @Deprecated
  @Override
  public DoubleDistance getFirst() {
    return getDistance();
  }

  /**
   * @deprecated Use {@link #getDBID} for clearness.
   */
  @Deprecated
  @Override
  public DBID getSecond() {
    return id;
  }

  @Override
  public int compareByDistance(DistanceResultPair<DoubleDistance> o) {
    if(o instanceof DoubleDistanceResultPair) {
      DoubleDistanceResultPair od = (DoubleDistanceResultPair) o;
      final int delta = Double.compare(distance, od.distance);
      if(delta != 0) {
        return delta;
      }
    }
    else {
      final int delta = Double.compare(distance, o.getDistance().doubleValue());
      if(delta != 0) {
        return delta;
      }
    }
    return 0;
  }

  @Override
  public int compareTo(DistanceResultPair<DoubleDistance> o) {
    final int delta = this.compareByDistance(o);
    if(delta != 0) {
      return delta;
    }
    return id.compareTo(o.getDBID());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof DistanceResultPair)) {
      return false;
    }
    if(obj instanceof DoubleDistanceResultPair) {
      DoubleDistanceResultPair ddrp = (DoubleDistanceResultPair) obj;
      return distance == ddrp.distance && id.sameDBID(ddrp.id);
    }
    DistanceResultPair<?> other = (DistanceResultPair<?>) obj;
    return other.getDistance().equals(distance) && id.sameDBID(other.getDBID());
  }

  /**
   * Get the distance as double value.
   * 
   * @return distance value
   */
  public double getDoubleDistance() {
    return distance;
  }

  @Override
  public String toString() {
    return "DistanceResultPair(" + distance + ", " + id + ")";
  }
}