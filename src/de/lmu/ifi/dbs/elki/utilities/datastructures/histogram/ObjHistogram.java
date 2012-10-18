package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;

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

/**
 * Basic interface for object based histograms (static and flexible).
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Iter
 * 
 * @param <T> data type
 */
public interface ObjHistogram<T> extends Histogram {
  /**
   * Get a histogram iterator.
   * 
   * @return Iterator
   */
  @Override
  public Iter<T> iter();

  /**
   * Aggregate new data into the histogram.
   * 
   * Note that the actual definition of "aggregate" depends on the application.
   * While a static histogram will likely perform this immediately, a flexible
   * histogram will cache a number of observations.
   * 
   * @param coord Coordinate
   * @param data Data
   */
  public void putData(double coord, T data);

  /**
   * Histogram iterator.
   * 
   * @author Erich Schubert
   */
  public static interface Iter<T> extends Histogram.Iter {
    /**
     * Get the value of the bin.
     * 
     * @return Bin value
     */
    public T getValue();
  }
}
