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
 * Histogram class storing double values.
 * 
 * The histogram will start with "bin" bins, but it can grow dynamicall to the
 * left and right.
 * 
 * @author Erich Schubert
 */
public class LongStaticHistogram extends AbstractStaticHistogram {
  /**
   * Constructor.
   * 
   * @param bins Number of bins
   * @param min Cover minimum
   * @param max Cover maximum
   */
  public LongStaticHistogram(int bins, double min, double max) {
    super(bins, min, max);
    data = new long[bins];
  }

  /**
   * Data store
   */
  long[] data;

  /**
   * Increment the value of a bin.
   * 
   * @param coord Coordinate
   * @param val Value
   */
  public void increment(double coord, long val) {
    int bin = getBinNr(coord);
    if (bin < 0) {
      long[] tmpdata = new long[size - bin];
      System.arraycopy(data, 0, tmpdata, -bin, size);
      tmpdata[0] = val;
      data = tmpdata;
      // Note that bin is negative, -bin is the shift offset!
      assert (data.length == size - bin);
      offset -= bin;
      size -= bin;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
    } else if (bin >= size) {
      long[] tmpdata = new long[bin + 1];
      System.arraycopy(data, 0, tmpdata, 0, size);
      tmpdata[bin] = val;
      data = tmpdata;
      size = bin + 1;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
    } else {
      data[bin] += val;
    }
  }

  /**
   * Replace the value of a bin.
   * 
   * @param coord Coordinate
   * @param val Value
   */
  public void replace(double coord, long val) {
    int bin = getBinNr(coord);
    if (bin < 0) {
      long[] tmpdata = new long[size - bin];
      System.arraycopy(data, 0, tmpdata, -bin, size);
      tmpdata[0] = val;
      data = tmpdata;
      // Note that bin is negative, -bin is the shift offset!
      offset -= bin;
      size -= bin;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
    } else if (bin >= size) {
      long[] tmpdata = new long[bin + 1];
      System.arraycopy(data, 0, tmpdata, 0, size);
      tmpdata[bin] = val;
      data = tmpdata;
      size = bin + 1;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
    } else {
      data[bin] = val;
    }
  }

  @Override
  public Iter iter() {
    return new Iter();
  }

  /**
   * Iterator class.
   * 
   * @author Erich Schubert
   */
  public class Iter extends AbstractStaticHistogram.Iter {
    /**
     * Get the value of the bin.
     * 
     * @return Bin value
     */
    public long binValue() {
      return data[bin];
    }
  }
}
