package de.lmu.ifi.dbs.elki.math;

import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Histogram with flexible size, guaranteed to be in [bin, 2*bin[
 * 
 * @author Erich Schubert
 * 
 * @param <T>
 */
public class FlexiHistogram<T,D> extends AggregatingHistogram<T,D> {
  /**
   * Adapter class, extended "maker".
   */
  private Adapter<T,D> maker;

  /**
   * Cache for elements when not yet initialized.
   */
  private ArrayList<Pair<Double, T>> tempcache = null;

  /**
   * Destination (minimum) size of the structure. At most 2*destsize bins are
   * allowed.
   */
  private int destsize;

  /**
   * Adapter interface to specify bin creation, data caching and combination.
   * 
   * @author Erich Schubert
   * 
   * @param <T>
   */
  public static abstract class Adapter<T,D> extends AggregatingHistogram.Adapter<T,D> {
    /**
     * Rule to combine two bins into one.
     * 
     * first and second MAY be modified and returned.
     * 
     * @param first First bin value
     * @param second Second bin value
     * @return combined bin value
     */
    public abstract T combine(T first, T second);

    /**
     * Clone a data passed to the algorithm for computing the initial size.
     * 
     * @param data Data to be cloned
     * @return cloned data
     */
    public abstract T cloneForCache(T data);
  }

  /**
   * Create a new histogram for an unknown data range.
   * 
   * The generated histogram is guaranteed to have within {@code bins} and
   * {@code 2*bins} bins in length.
   * 
   * @param bins Target number of bins
   * @param maker Adapter for data types and combination rules.
   */
  public FlexiHistogram(int bins, Adapter<T,D> maker) {
    super(bins, 0.0, 1.0, maker);
    this.destsize = bins;
    this.maker = maker;
    tempcache = new ArrayList<Pair<Double, T>>(this.destsize * 2);
  }

  private synchronized void materialize() {
    // already materialized?
    if(tempcache == null) {
      return;
    }
    // we can't really initialize, but since we have to, we'll just stick
    // to 0.0 and 1.0 as used in the constructor.
    if(tempcache.size() <= 0) {
      tempcache = null;
      return;
    }
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    for(Pair<Double, T> pair : tempcache) {
      min = Math.min(min, pair.first);
      max = Math.max(max, pair.first);
    }
    // TODO: auto-adjust min/max by some magic value?
    this.base = min;
    this.max = max;
    this.binsize = (max - min) / this.destsize;
    // initialize array
    this.data = new ArrayList<T>(this.destsize * 2);
    for(int i = 0; i < this.destsize; i++) {
      this.data.add(maker.make());
    }
    // re-insert data we have
    for(Pair<Double, T> pair : tempcache) {
      super.replace(pair.first, pair.second);
    }
    // delete cache, signal that we're initialized
    tempcache = null;
  }

  @Override
  public synchronized void replace(double coord, T d) {
    if(tempcache != null) {
      if(tempcache.size() < this.destsize * 2) {
        tempcache.add(new Pair<Double, T>(coord, maker.cloneForCache(d)));
        return;
      }
      else {
        materialize();
        // .. and continue below!
      }
    }
    // super class put will already handle histogram resizing
    super.replace(coord, d);
    // but not resampling
    testResample();
  }

  private void testResample() {
    while(super.size >= 2 * this.destsize) {
      // Resampling.
      ArrayList<T> newdata = new ArrayList<T>(this.destsize * 2);
      for(int i = 0; i < super.size; i += 2) {
        newdata.add(maker.combine(super.data.get(i), super.data.get(i + 1)));
      }
      // recalculate histogram base.
      double base = super.base - super.offset * super.binsize;
      // update data
      super.data = newdata;
      // update sizes
      super.base = base;
      super.offset = 0;
      super.size = newdata.size();
      super.binsize = super.binsize * 2;
      super.max = super.base + super.binsize * super.size;
    }
  }

  @Override
  public T get(double coord) {
    materialize();
    return super.get(coord);
  }

  @Override
  public double getBinsize() {
    materialize();
    return super.getBinsize();
  }

  @Override
  public double getCoverMaximum() {
    materialize();
    return super.getCoverMaximum();
  }

  @Override
  public double getCoverMinimum() {
    materialize();
    return super.getCoverMinimum();
  }

  @Override
  public ArrayList<T> getData() {
    materialize();
    return super.getData();
  }

  @Override
  public int getNumBins() {
    materialize();
    return super.getNumBins();
  }

  @Override
  public Iterator<Pair<Double, T>> iterator() {
    materialize();
    return super.iterator();
  }

  @Override
  public void add(double coord, D value) {
    materialize();
    super.add(coord, value);
  }

  /**
   * Convenience constructor for Integer-based Histograms. Uses a constructor to
   * initialize bins with Integer(0)
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integer.
   */
  public static FlexiHistogram<Integer, Integer> IntegerSumHistogram(int bins) {
    return new FlexiHistogram<Integer, Integer>(bins, new Adapter<Integer, Integer>() {
      @Override
      public Integer make() {
        return new Integer(0);
      }

      @Override
      public Integer cloneForCache(Integer data) {
        // no need to clone, Integer are singletons
        return data;
      }

      @Override
      public Integer combine(Integer first, Integer second) {
        return first + second;
      }

      @Override
      public Integer add(Integer existing, Integer data) {
        return existing + data;
      }
    });
  }

  /**
   * Convenience constructor for Double-based Histograms. Uses a constructor to
   * initialize bins with Double(0), and downsampling is done by summation.
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Doubles.
   */
  public static FlexiHistogram<Double, Double> DoubleSumHistogram(int bins) {
    return new FlexiHistogram<Double, Double>(bins, new Adapter<Double, Double>() {
      @Override
      public Double make() {
        return new Double(0.0);
      }

      @Override
      public Double cloneForCache(Double data) {
        // no need to clone, Doubles are singletons
        return data;
      }

      @Override
      public Double combine(Double first, Double second) {
        return first + second;
      }

      @Override
      public Double add(Double existing, Double data) {
        return existing + data;
      }
    });
  }

  /**
   * Convenience constructor for {@link MeanVariance}-based Histograms. Uses a
   * constructor to initialize bins with new {@link MeanVariance} objects
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for {@link MeanVariance}.
   */
  public static FlexiHistogram<MeanVariance, Double> MeanVarianceHistogram(int bins) {
    return new FlexiHistogram<MeanVariance, Double>(bins, new Adapter<MeanVariance, Double>() {
      @Override
      public MeanVariance make() {
        return new MeanVariance();
      }

      @Override
      public MeanVariance cloneForCache(MeanVariance data) {
        return new MeanVariance(data);
      }

      @Override
      public MeanVariance combine(MeanVariance first, MeanVariance second) {
        first.put(second);
        return first;
      }

      @Override
      public MeanVariance add(MeanVariance existing, Double data) {
        existing.put(data);
        return existing;
      }
    });
  }

  // TODO: MeanVariance
}
