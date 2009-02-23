package de.lmu.ifi.dbs.elki.utilities;

import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * Class to manage a simple Histogram.
 * 
 * @author Erich Schubert
 *
 * @param <T> Histogram data type.
 */
public class Histogram<T> implements Iterable<SimplePair<Double, T>> {
  /**
   * Interface to plug in constructors for type T.
   * 
   * @author Erich Schubert
   *
   * @param <T>
   */
  public static abstract class Constructor<T> {
    public abstract T make();
  }
  
  /**
   * Array shift to account for negative indices.
   */
  protected int offset = 0;

  /**
   * Size of array storage.
   */
  protected int size = 0;

  /**
   * Array 'base', i.e. the point of 0.0.
   * Usually the minimum.
   */
  protected double base;

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
  private Constructor<T> maker;

  /**
   * Histogram constructor
   * 
   * @param bins Number of bins to use.
   * @param min Minimum Value
   * @param max Maximum Value
   * @param maker Constructor for new elements.
   */
  public Histogram(int bins, double min, double max, Constructor<T> maker) {
    this.base = min;
    this.binsize = (max - min) / bins;
    this.size = bins;
    this.data = new ArrayList<T>(bins);
    this.maker = maker;
    for (int i = 0; i < bins; i++) {
      this.data.add(make());
    }
  }

  /**
   * Histogram constructor without 'Constructor' to generate new elements.
   * Empty bins will be initialized with 'null'.
   * 
   * @param bins Number of bins
   * @param min Minimum value
   * @param max Maximum value.
   */
  public Histogram(int bins, double min, double max) {
    this(bins, min, max, null);
  }
  
  /**
   * Call the constructor to produce a new element, if possible.
   * 
   * @return new element or null.
   */
  private T make() {
    if (maker != null) {
      return maker.make();
    }
    return null;
  }

  /**
   * Get the data at a given Coordinate.
   * 
   * @param coord Coordinate.
   * @return data element there (which may be a new empty bin or null)
   */
  public T get(double coord) {
    int bin = (int) Math.floor((coord - base) / binsize) + offset;
    // compare with allocated area
    if(bin < 0) {
      T n = make();
      if (n != null) {
        putBin(bin, n);
      }
      return n;
    }
    if(bin >= size) {
      T n = make();
      if (n != null) {
        putBin(bin, n);
      }
      return n;
    }
    return data.get(bin);
  }

  /**
   * Put data at a given coordinate.
   * Note: this replaces the contents, it doesn't "add" or "count".
   * 
   * @param coord
   * @param d
   */
  public void put(double coord, T d) {
    int bin = (int) Math.floor((coord - base) / binsize) + offset;
    putBin(bin, d);
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
        data.add(1, make());
      }
      // We still have bin < 0, thus (size - bin) > size!
      assert (data.size() == size - bin);
      offset = offset - bin;
      size = size - bin;
    }
    else if(bin >= size) {
      this.data.ensureCapacity(bin + 1);
      while (data.size() < bin) {
        data.add(make());
      }
      // add the new data.
      data.add(d);
      assert (data.size() == bin + 1);
      size = bin + 1;
    }
    else {
      this.data.set(bin, d);
    }
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
   * Convenience constructor for Double-based Histograms.
   * Uses a constructor to initialize bins with Double(0)
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Doubles.
   */
  public static final Histogram<Double> DoubleHistogram(int bins, double min, double max) {
    return new Histogram<Double>(bins, min, max, new Constructor<Double>() {
      @Override
      public Double make() {
        return new Double(0.0);
      }
    });
  }

  /**
   * Iterator class to iterate over all bins.
   * 
   * @author Erich Schubert
   */
  protected class Iter implements Iterator<SimplePair<Double, T>> {
    /**
     * Current bin number
     */
    int bin = 0;
    
    @Override
    public boolean hasNext() {
      return bin < size;
    }

    @Override
    public SimplePair<Double, T> next() {
      SimplePair<Double, T> pair = new SimplePair<Double, T>(base + (bin + 0.5 - offset) * binsize, data.get(bin));
      bin++;
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
  public Iterator<SimplePair<Double, T>> iterator() {
    return new Iter();
  }
}
