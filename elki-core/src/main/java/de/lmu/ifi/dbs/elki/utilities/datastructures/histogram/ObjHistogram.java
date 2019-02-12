/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;

/**
 * Histogram class storing double values.
 * 
 * The histogram will start with "bin" bins, but it can grow dynamicall to the
 * left and right.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @param <T> Data type
 */
public class ObjHistogram<T> extends AbstractStaticHistogram {
  /**
   * Data store
   */
  Object[] data;

  /**
   * Special value storage: infinity, NaN
   */
  Object[] special = null;

  /**
   * Supplier for empty bins.
   */
  BucketFactory<T> supplier;

  /**
   * Constructor.
   * 
   * @param bins Number of bins
   * @param min Cover minimum
   * @param max Cover maximum
   * @param supplier Supplier to fill empty bins
   */
  public ObjHistogram(int bins, double min, double max, BucketFactory<T> supplier) {
    super(bins, min, max);
    // -1 is used by ObjDynamicHistogram to delay initialization.
    if(bins >= 0) {
      data = new Object[bins];
      if(supplier != null) {
        for(int i = 0; i < bins; i++) {
          data[i] = supplier.make();
        }
      }
    }
    this.supplier = supplier;
  }

  /**
   * Access the value of a bin with new data.
   * 
   * @param coord Coordinate
   * @return bin contents
   */
  @SuppressWarnings("unchecked")
  public T get(double coord) {
    if(coord == Double.NEGATIVE_INFINITY) {
      return getSpecial(0);
    }
    if(coord == Double.POSITIVE_INFINITY) {
      return getSpecial(1);
    }
    if(Double.isNaN(coord)) {
      return getSpecial(2);
    }
    int bin = getBinNr(coord);
    if(bin < 0) {
      if(size - bin > data.length) {
        // Reallocate. TODO: use an arraylist-like grow strategy!
        Object[] tmpdata = new Object[growSize(data.length, size - bin)];
        System.arraycopy(data, 0, tmpdata, -bin, size);
        data = tmpdata;
      }
      else {
        // Shift in place
        System.arraycopy(data, 0, data, -bin, size);
      }
      for(int i = 0; i < -bin; i++) {
        data[i] = supplier.make();
      }
      // Note that bin is negative, -bin is the shift offset!
      offset -= bin;
      size -= bin;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
      return (T) data[0];
    }
    else if(bin >= size) {
      if(bin >= data.length) {
        Object[] tmpdata = new Object[growSize(data.length, bin + 1)];
        System.arraycopy(data, 0, tmpdata, 0, size);
        data = tmpdata;
      }
      for(int i = size; i <= bin; i++) {
        data[i] = supplier.make();
      }
      size = bin + 1;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
      return (T) data[bin];
    }
    else {
      return (T) data[bin];
    }
  }

  /**
   * Ensure that we have storage for special values (infinity, NaN)
   * 
   * @param idx Index to return.
   */
  @SuppressWarnings("unchecked")
  protected T getSpecial(int idx) {
    if(special == null) {
      special = new Object[] { supplier.make(), supplier.make(), supplier.make() };
    }
    return (T) special[idx];
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
  public class Iter extends AbstractStaticHistogram.Iter implements Histogram.Iter {
    /**
     * Get the value of the bin.
     * 
     * @return Bin value
     */
    @SuppressWarnings("unchecked")
    public T getValue() {
      return (T) data[bin];
    }
  }

  /**
   * Function to make new buckets.
   * 
   * @author Erich Schubert
   *
   * @param <T> Data type
   */
  @FunctionalInterface
  public interface BucketFactory<T> {
    T make();
  }
}
