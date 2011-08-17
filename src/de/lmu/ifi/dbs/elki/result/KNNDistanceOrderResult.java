package de.lmu.ifi.dbs.elki.result;
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

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Wraps a list containing the knn distances.
 * 
 * @author Arthur Zimek
 * @param <D> the type of Distance used by this Result
 * 
 */
public class KNNDistanceOrderResult<D extends Distance<D>> extends BasicResult implements IterableResult<D> {
  /**
   * Store the kNN Distances
   */
  private final List<D> knnDistances;

  /**
   * Construct result
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param knnDistances distance list to wrap.
   */
  public KNNDistanceOrderResult(String name, String shortname, final List<D> knnDistances) {
    super(name, shortname);
    this.knnDistances = knnDistances;
  }

  /**
   * Return an iterator.
   */
  @Override
  public Iterator<D> iterator() {
    return knnDistances.iterator();
  }
}
