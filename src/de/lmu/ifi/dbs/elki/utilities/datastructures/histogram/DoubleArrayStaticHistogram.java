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
 * Histogram containing an array of doubles (i.e. {@code double[]}). This is
 * actually one of the simpler specializations, as arrays are objects not
 * primitive in Java.
 * 
 * @author Erich Schubert
 */
public final class DoubleArrayStaticHistogram extends AbstractObjStaticHistogram<double[]> {
  /**
   * Desired number of columns in each bin.
   */
  private final int cols;

  /**
   * Constructor.
   * 
   * @param bins Number of bins
   * @param min Minimum value for the coordinates
   * @param max Maximum value for the coordinates
   * @param cols Number of columns in each bin.
   */
  public DoubleArrayStaticHistogram(int bins, double min, double max, int cols) {
    super(bins, min, max);
    this.cols = cols;
    for (int i = 0; i < bins; i++) {
      data[i] = new double[cols];
    }
  }

  /**
   * Increment histogram by a double[].
   * 
   * @param coord Coordinate
   * @param data Data to increment by.
   */
  public void increment(double coord, double[] data) {
    double[] existing = get(coord);
    for (int i = 0; i < existing.length; i++) {
      existing[i] += data[i];
    }
  }

  /**
   * {@inheritDoc}
   * 
   * Data is combined by incrementing.
   * 
   * @Deprecated use the explicit {@link #increment} instead.
   */
  @Deprecated
  @Override
  public void putData(double coord, double[] data) {
    increment(coord, data);
  }

  @Override
  protected double[] makeObject() {
    return new double[cols];
  }
}
