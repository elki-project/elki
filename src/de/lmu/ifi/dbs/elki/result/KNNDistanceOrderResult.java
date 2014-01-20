package de.lmu.ifi.dbs.elki.result;

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

import java.util.Iterator;

/**
 * Wraps a list containing the knn distances.
 * 
 * @author Arthur Zimek
 */
public class KNNDistanceOrderResult extends BasicResult implements IterableResult<Double> {
  /**
   * Store the kNN Distances
   */
  private final double[] knnDistances;

  /**
   * Construct result
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param knnDistances distance list to wrap.
   */
  public KNNDistanceOrderResult(String name, String shortname, final double[] knnDistances) {
    super(name, shortname);
    this.knnDistances = knnDistances;
  }

  /**
   * Return an iterator.
   */
  @Override
  public Iterator<Double> iterator() {
    return new Iterator<Double>() {
      int pos = -1;

      @Override
      public boolean hasNext() {
        return (pos < knnDistances.length);
      }

      @Override
      public Double next() {
        ++pos;
        return knnDistances[pos];
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
