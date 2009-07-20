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
// TODO: Add resizing strategy: when size >> 2*initial size, do a downsampling.
public class Histogram<T> implements Iterable<Pair<Double, T>> {
  /**
   * Interface to plug in constructors for type T.
   * 
   * @author Erich Schubert
   *
   * @param <T>
   */
  public static abstract class Constructor<T> {
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
  protected int size = 0;

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
    this.max = max;
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
    int bin = getBinNr(coord);
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
  private int getBinNr(double coord) {
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
        data.add(1, make());
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
        data.add(make());
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
   * Get the raw data. Note that this does NOT include the coordinates.
   * 
   * @return raw data array.
   */
  public ArrayList<T> getData() {
    return data;
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
  public static final HistogramInteger IntHistogram(int bins, double min, double max) {
    return new HistogramInteger(bins, min, max);
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
   * Convenience constructor for Histograms with pairs of Integers
   * Uses a constructor to initialize bins with Pair(Integer(0),Integer(0))
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integer pairs.
   */
  public static final Histogram<Pair<Integer,Integer>> IntIntHistogram(int bins, double min, double max) {
    return new Histogram<Pair<Integer,Integer>>(bins, min, max, new Constructor<Pair<Integer,Integer>>() {
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
  public static final Histogram<Pair<Double,Double>> DoubleDoubleHistogram(int bins, double min, double max) {
    return new Histogram<Pair<Double,Double>>(bins, min, max, new Constructor<Pair<Double,Double>>() {
      @Override
      public Pair<Double,Double> make() {
        return new Pair<Double,Double>(0.0,0.0);
      }
    });
  }

  /**
   * Convenience constructor for {@link MeanVariance}-based Histograms.
   * Uses a constructor to initialize bins with new {@link MeanVariance} objects
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for {@link MeanVariance}.
   */
  public static final Histogram<MeanVariance> MeanVarianceHistogram(int bins, double min, double max) {
    return new Histogram<MeanVariance>(bins, min, max, new Constructor<MeanVariance>() {
      @Override
      public MeanVariance make() {
        return new MeanVariance();
      }
    });
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

}
