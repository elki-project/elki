package de.lmu.ifi.dbs.elki.database.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

/**
 * Class that consists of a pair (distance, object ID) commonly returned for kNN
 * and range queries.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public interface DistanceResultPair<D extends Distance<?>> extends PairInterface<D, DBID>, Comparable<DistanceResultPair<D>> {
  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public D getDistance();

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public void setDistance(D first);

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public DBID getDBID();

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public void setID(DBID second);

  /**
   * Compare value, but by distance only.
   * 
   * @param o Other object
   * @return comparison result, as by Double.compare(this, other)
   */
  public int compareByDistance(DistanceResultPair<D> o);
}