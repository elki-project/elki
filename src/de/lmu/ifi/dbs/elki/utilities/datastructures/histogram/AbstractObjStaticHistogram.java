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
 * 
 * @param <T> Data type
 */
public abstract class AbstractObjStaticHistogram<T> extends AbstractStaticHistogram implements ObjHistogram<T> {
  /**
   * Constructor.
   * 
   * @param bins Number of bins
   * @param min Cover minimum
   * @param max Cover maximum
   */
  public AbstractObjStaticHistogram(int bins, double min, double max) {
    super(bins, min, max);
    if (bins >= 0) {
      // -1 will be used by FlexiHistogram to delay initialization.
      data = new Object[bins];
    }
  }

  /**
   * Data store
   */
  Object[] data;

  /**
   * Access the value of a bin with new data.
   * 
   * @param coord Coordinate
   * @return bin contents
   */
  @SuppressWarnings("unchecked")
  public T get(double coord) {
    int bin = getBinNr(coord);
    if (bin < 0) {
      if (size - bin > data.length) {
        // Reallocate. TODO: use an arraylist-like grow strategy!
        Object[] tmpdata = new Object[growSize(data.length, size - bin)];
        System.arraycopy(data, 0, tmpdata, -bin, size);
        data = tmpdata;
      } else {
        // Shift in place
        System.arraycopy(data, 0, data, -bin, size);
      }
      for (int i = 0; i < -bin; i++) {
        data[i] = makeObject();
      }
      // Note that bin is negative, -bin is the shift offset!
      offset -= bin;
      size -= bin;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
      return (T) data[0];
    } else if (bin >= size) {
      if (bin >= data.length) {
        Object[] tmpdata = new Object[growSize(data.length, bin + 1)];
        System.arraycopy(data, 0, tmpdata, 0, size);
        data = tmpdata;
      }
      for (int i = size; i <= bin; i++) {
        data[i] = makeObject();
      }
      size = bin + 1;
      // TODO: modCounter++; and have iterators fast-fail
      // Unset max value when resizing
      max = Double.MAX_VALUE;
      return (T) data[bin];
    } else {
      return (T) data[bin];
    }
  }

  /**
   * Class to make a new object for the data store.
   * 
   * @return New instance.
   */
  protected abstract T makeObject();

  @Override
  public Iter iter() {
    return new Iter();
  }

  /**
   * Iterator class.
   * 
   * @author Erich Schubert
   */
  public class Iter extends AbstractStaticHistogram.Iter implements ObjHistogram.Iter<T> {
    @SuppressWarnings("unchecked")
    @Override
    public T getValue() {
      return (T) data[bin];
    }
  }
}
