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
 * Array storing double values.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Iter
 */
public interface DoubleHistogram extends Histogram {
  /**
   * Increment the histogram at a given position.
   * 
   * @param coord Coordinate
   * @param value Value to increment by
   */
  public void increment(double coord, double value);
  
  @Override
  public Iter iter();
  
  /**
   * Iterator interface.
   *
   * @author Erich Schubert
   */
  public static interface Iter extends Histogram.Iter {
    /**
     * Get the value of the bin.
     * 
     * @return Bin value
     */
    public double getValue();
  }
}
