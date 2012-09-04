package de.lmu.ifi.dbs.elki.math.histograms;

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

import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Class to manage a simple Histogram.
 * 
 * Note: the iterator returns pairs containing the coordinate and the bin value!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.math.histograms.ReplacingHistogram.Adapter
 * 
 * @param <T> Histogram data type.
 */
public class ReplacingHistogram<T> implements Iterable<DoubleObjPair<T>> {
  /**
   * Interface to plug in a data type T.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Data type
   */
  public abstract static class Adapter<T> {
    /**
     * Construct a new T when needed.
     * 
     * @return new T
     */
    public abstract T make();
  }

  /**
   * Array shift to account for negative indices.
   */
  protected int offset = 0;

  /**
   * Size of array storage.
   */
  protected int size;

  /**
   * Array 'base', i.e. the point of 0.0. Usually the minimum.
   */
  protected double base;

  /**
   * To avoid introducing an extra bucket for the maximum value.
   */
  protected double max;

  /**
   * Width of a bin.
   */
  protected double binsize;

  /**
   * Data storage
   */
  protected ArrayList<T> data;

  /**
   * Constructor for new elements
   */
  private Adapter<T> maker;

  /**
   * Histogram constructor
   * 
   * @param bins Number of bins to use.
   * @param min Minimum Value
   * @param max Maximum Value
   * @param maker Constructor for new elements.
   */
  public ReplacingHistogram(int bins, double min, double max, Adapter<T> maker) {
    this.base = min;
    this.max = max;
    this.binsize = (max - min) / bins;
    this.size = bins;
    this.data = new ArrayList<T>(bins);
    this.maker = maker;
    for(int i = 0; i < bins; i++) {
      this.data.add(maker.make());
    }
  }

  /**
   * Histogram constructor without 'Constructor' to generate new elements. Empty
   * bins will be initialized with 'null'.
   * 
   * @param bins Number of bins
   * @param min Minimum value
   * @param max Maximum value.
   */
  public ReplacingHistogram(int bins, double min, double max) {
    this(bins, min, max, null);
  }

  /**
   * Get the data at a given Coordinate.
   * 
   * @param coord Coordinate.
   * @return data element there (which may be a new empty bin or null)
   */
  public T get(double coord) {
    int bin = getBinNr(coord);
    // compare with allocated area
    if(bin < 0) {
      T n = maker.make();
      return n;
    }
    if(bin >= size) {
      T n = maker.make();
      return n;
    }
    return data.get(bin);
  }

  /**
   * Put data at a given coordinate. Note: this replaces the contents, it
   * doesn't "add" or "count".
   * 
   * @param coord Coordinate
   * @param d New Data
   */
  public void replace(double coord, T d) {
    int bin = getBinNr(coord);
    putBin(bin, d);
  }

  /**
   * Compute the bin number. Has a special case for rounding max down to the
   * last bin.
   * 
   * @param coord Coordinate
   * @return bin number
   */
  protected int getBinNr(double coord) {
    if(Double.isInfinite(coord) || Double.isNaN(coord)) {
      throw new UnsupportedOperationException("Encountered non-finite value in Histogram: " + coord);
    }
    if(coord == max) {
      // System.err.println("Triggered special case: "+ (Math.floor((coord -
      // base) / binsize) + offset) + " vs. " + (size - 1));
      return size - 1;
    }
    return (int) Math.floor((coord - base) / binsize) + offset;
  }

  /**
   * Internal put function to handle the special cases of histogram resizing.
   * 
   * @param bin bin number
   * @param d data to put
   */
  private void putBin(int bin, T d) {
    if(bin < 0) {
      // make sure to have enough space
      data.ensureCapacity(size - bin);
      // insert new data in front.
      data.add(0, d);
      // fill the gap. Note that bin < 0.
      for(int i = bin + 1; i < 0; i++) {
        data.add(1, maker.make());
      }
      // We still have bin < 0, thus (size - bin) > size!
      assert (data.size() == size - bin);
      offset = offset - bin;
      size = size - bin;
      // drop max value when resizing
      max = Double.MAX_VALUE;
    }
    else if(bin >= size) {
      this.data.ensureCapacity(bin + 1);
      while(data.size() < bin) {
        data.add(maker.make());
      }
      // add the new data.
      data.add(d);
      assert (data.size() == bin + 1);
      size = bin + 1;
      // drop max value when resizing
      max = Double.MAX_VALUE;
    }
    else {
      this.data.set(bin, d);
    }
  }

  /**
   * Get the number of bins actually in use.
   * 
   * @return number of bins
   */
  public int getNumBins() {
    return size;
  }

  /**
   * Get the size (width) of a bin.
   * 
   * @return bin size
   */
  public double getBinsize() {
    return binsize;
  }

  /**
   * Mean of bin
   * 
   * @param bin Bin number
   * @return Mean
   */
  public double getBinMean(int bin) {
    return base + (bin + 0.5 - offset) * binsize;
  }

  /**
   * Minimum of bin
   * 
   * @param bin Bin number
   * @return Lower bound
   */
  public double getBinMin(int bin) {
    return base + (bin - offset) * binsize;
  }

  /**
   * Maximum of bin
   * 
   * @param bin Bin number
   * @return Upper bound
   */
  public double getBinMax(int bin) {
    return base + (bin + 1 - offset) * binsize;
  }

  /**
   * Get minimum (covered by bins, not data!)
   * 
   * @return minimum
   */
  public double getCoverMinimum() {
    return base - offset * binsize;
  }

  /**
   * Get maximum (covered by bins, not data!)
   * 
   * @return maximum
   */
  public double getCoverMaximum() {
    return base + (size - offset) * binsize;
  }

  /**
   * Get the raw data. Note that this does NOT include the coordinates.
   * 
   * @return raw data array.
   */
  public ArrayList<T> getData() {
    return data;
  }

  /**
   * Make a new bin.
   * 
   * @return new bin.
   */
  protected T make() {
    return maker.make();
  }

  /**
   * Iterator class to iterate over all bins.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Iter implements Iterator<DoubleObjPair<T>> {
    /**
     * Current bin number
     */
    int bin = 0;

    @Override
    public boolean hasNext() {
      return bin < size;
    }

    @Override
    public DoubleObjPair<T> next() {
      DoubleObjPair<T> pair = new DoubleObjPair<T>(base + (bin + 0.5 - offset) * binsize, data.get(bin));
      bin++;
      return pair;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Histogram iterators cannot be modified.");
    }
  }

  /**
   * Iterator class to iterate over all bins.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class RIter implements Iterator<DoubleObjPair<T>> {
    /**
     * Current bin number
     */
    int bin = size - 1;

    @Override
    public boolean hasNext() {
      return bin >= 0;
    }

    @Override
    public DoubleObjPair<T> next() {
      DoubleObjPair< T> pair = new DoubleObjPair<T>(base + (bin + 0.5 - offset) * binsize, data.get(bin));
      bin--;
      return pair;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Histogram iterators cannot be modified.");
    }
  }

  /**
   * Get an iterator over all histogram bins.
   */
  @Override
  public Iterator<DoubleObjPair<T>> iterator() {
    return new Iter();
  }

  /**
   * Get an iterator over all histogram bins.
   */
  // TODO: is there some interface to implement.
  public Iterator<DoubleObjPair<T>> reverseIterator() {
    return new RIter();
  }

  /**
   * Convenience constructor for Integer-based Histograms. Uses a constructor to
   * initialize bins with Integer(0)
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integers.
   */
  public static ReplacingHistogram<Integer> IntHistogram(int bins, double min, double max) {
    return new ReplacingHistogram<Integer>(bins, min, max, new Adapter<Integer>() {
      @Override
      public Integer make() {
        return new Integer(0);
      }
    });
  }

  /**
   * Convenience constructor for Double-based Histograms. Uses a constructor to
   * initialize bins with Double(0)
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Doubles.
   */
  public static ReplacingHistogram<Double> DoubleHistogram(int bins, double min, double max) {
    return new ReplacingHistogram<Double>(bins, min, max, new Adapter<Double>() {
      @Override
      public Double make() {
        return new Double(0.0);
      }
    });
  }

  /**
   * Convenience constructor for Histograms with pairs of Integers Uses a
   * constructor to initialize bins with Pair(Integer(0),Integer(0))
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integer pairs.
   */
  public static ReplacingHistogram<IntIntPair> IntIntHistogram(int bins, double min, double max) {
    return new ReplacingHistogram<IntIntPair>(bins, min, max, new Adapter<IntIntPair>() {
      @Override
      public IntIntPair make() {
        return new IntIntPair(0, 0);
      }
    });
  }

  /**
   * Convenience constructor for Histograms with pairs of Doubles Uses a
   * constructor to initialize bins with Pair(Double(0),Double(0))
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Double pairs.
   */
  public static ReplacingHistogram<DoubleDoublePair> DoubleDoubleHistogram(int bins, double min, double max) {
    return new ReplacingHistogram<DoubleDoublePair>(bins, min, max, new Adapter<DoubleDoublePair>() {
      @Override
      public DoubleDoublePair make() {
        return new DoubleDoublePair(0.0, 0.0);
      }
    });
  }
}
