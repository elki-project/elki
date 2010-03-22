package de.lmu.ifi.dbs.elki.math;

import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to manage a simple Histogram.
 * 
 * @author Erich Schubert
 *
 * @param <T> Histogram data type.
 */
public class ReplacingHistogram<T> implements Iterable<Pair<Double, T>> {
  /**
   * Interface to plug in a data type T.
   * 
   * @author Erich Schubert
   *
   * @param <T> Data type
   */
  public static abstract class Adapter<T> {
    /**
     * Construct a new T when needed.
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
   * Array 'base', i.e. the point of 0.0.
   * Usually the minimum.
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
    for (int i = 0; i < bins; i++) {
      this.data.add(maker.make());
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
   * Put data at a given coordinate.
   * Note: this replaces the contents, it doesn't "add" or "count".
   * 
   * @param coord Coordinate
   * @param d New Data
   */
  public void replace(double coord, T d) {
    int bin = getBinNr(coord);
    putBin(bin, d);
  }

  /**
   * Compute the bin number.
   * Has a special case for rounding max down to the last bin.
   *
   * @param coord Coordinate
   * @return bin number
   */
  protected int getBinNr(double coord) {
    if (coord == max) {
      //System.err.println("Triggered special case: "+ (Math.floor((coord - base) / binsize) + offset) + " vs. " + (size - 1));
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
      while (data.size() < bin) {
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
   */
  protected class Iter implements Iterator<Pair<Double, T>> {
    /**
     * Current bin number
     */
    int bin = 0;
    
    @Override
    public boolean hasNext() {
      return bin < size;
    }

    @Override
    public Pair<Double, T> next() {
      Pair<Double, T> pair = new Pair<Double, T>(base + (bin + 0.5 - offset) * binsize, data.get(bin));
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
  public Iterator<Pair<Double, T>> iterator() {
    return new Iter();
  }
  
  /**
   * Convenience constructor for Integer-based Histograms.
   * Uses a constructor to initialize bins with Integer(0)
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
   * Convenience constructor for Double-based Histograms.
   * Uses a constructor to initialize bins with Double(0)
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
   * Convenience constructor for Histograms with pairs of Integers
   * Uses a constructor to initialize bins with Pair(Integer(0),Integer(0))
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integer pairs.
   */
  public static ReplacingHistogram<Pair<Integer,Integer>> IntIntHistogram(int bins, double min, double max) {
    return new ReplacingHistogram<Pair<Integer,Integer>>(bins, min, max, new Adapter<Pair<Integer,Integer>>() {
      @Override
      public Pair<Integer,Integer> make() {
        return new Pair<Integer,Integer>(0,0);
      }
    });
  }

  /**
   * Convenience constructor for Histograms with pairs of Doubles
   * Uses a constructor to initialize bins with Pair(Double(0),Double(0))
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Double pairs.
   */
  public static ReplacingHistogram<Pair<Double,Double>> DoubleDoubleHistogram(int bins, double min, double max) {
    return new ReplacingHistogram<Pair<Double,Double>>(bins, min, max, new Adapter<Pair<Double,Double>>() {
      @Override
      public Pair<Double,Double> make() {
        return new Pair<Double,Double>(0.0,0.0);
      }
    });
  }
}
