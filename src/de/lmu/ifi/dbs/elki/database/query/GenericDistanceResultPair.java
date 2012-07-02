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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Trivial implementation using a generic pair.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class GenericDistanceResultPair<D extends Distance<D>> extends Pair<D, DBID> implements DistanceResultPair<D> {
  /**
   * Canonical constructor
   * 
   * @param first Distance
   * @param second Object ID
   */
  public GenericDistanceResultPair(D first, DBID second) {
    super(first, second);
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  @Override
  public final D getDistance() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  @Override
  public final void setDistance(D first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  @Override
  public final DBID getDBID() {
    return second;
  }
  
  @Override
  public int getIntegerID() {
    return DBIDFactory.FACTORY.asInteger(second);
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  @Override
  public final void setID(DBID second) {
    this.second = second;
  }

  @Override
  public int compareByDistance(DistanceResultPair<D> o) {
    return first.compareTo(o.getDistance());
  }

  @Override
  public int compareTo(DistanceResultPair<D> o) {
    final int ret = compareByDistance(o);
    if(ret != 0) {
      return ret;
    }
    return second.compareTo(o.getDBID());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof DistanceResultPair)) {
      return false;
    }
    DistanceResultPair<?> other = (DistanceResultPair<?>) obj;
    return first.equals(other.getDistance()) && DBIDUtil.equal(second, other);
  }

  @Override
  public String toString() {
    return "DistanceResultPair(" + getFirst() + ", " + getSecond() + ")";
  }
}