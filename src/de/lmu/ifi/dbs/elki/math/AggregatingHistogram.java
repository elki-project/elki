package de.lmu.ifi.dbs.elki.math;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class for the typical case of an aggregating (e.g. counting, averaging)
 * Histogram.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Type of data in histogram
 * @param <D> Type of input data
 */
public class AggregatingHistogram<T, D> extends ReplacingHistogram<T> {
  /**
   * The class we are using for putting data.
   */
  private Adapter<T, D> putter;

  /**
   * Adapter class for an AggregatingHistogram
   * 
   * @author Erich Schubert
   *
   * @param <T> Histogram bin type
   * @param <D> Incoming data type
   */
  public static abstract class Adapter<T, D> extends ReplacingHistogram.Adapter<T> {
    /**
     * Update an existing histogram value with new data.
     * 
     * @param existing Existing histogram data
     * @param data New value
     * @return Aggregated value
     */
    public abstract T aggregate(T existing, D data);
  }

  /**
   * Constructor with Adapter. 
   * 
   * @param bins Number of bins
   * @param min Minimum value
   * @param max Maximum value
   * @param adapter Adapter
   */
  public AggregatingHistogram(int bins, double min, double max, Adapter<T, D> adapter) {
    super(bins, min, max, adapter);
    this.putter = adapter;
  }

  /**
   * Add a value to the histogram using the aggregation adapter.
   * 
   * @param coord Coordinate
   * @param value New value
   */
  public void aggregate(double coord, D value) {
    super.replace(coord, putter.aggregate(super.get(coord), value));
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
  public static AggregatingHistogram<MeanVariance, Double> MeanVarianceHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<MeanVariance, Double>(bins, min, max, new Adapter<MeanVariance, Double>() {
      @Override
      public MeanVariance make() {
        return new MeanVariance();
      }

      @Override
      public MeanVariance aggregate(MeanVariance existing, Double data) {
        existing.put(data);
        return existing;
      }
    });
  }

  /**
   * Convenience constructor for Integer-based Histograms. Uses a constructor to
   * initialize bins with Integer(0). Aggregation is done by adding the values
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integers.
   */
  public static AggregatingHistogram<Integer, Integer> IntSumHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<Integer, Integer>(bins, min, max, new Adapter<Integer, Integer>() {
      @Override
      public Integer make() {
        return new Integer(0);
      }

      @Override
      public Integer aggregate(Integer existing, Integer data) {
        return existing + data;
      }
    });
  }

  /**
   * Convenience constructor for Long-based Histograms. Uses a constructor to
   * initialize bins with Long(0L). Aggregation is done by adding the values
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Integers.
   */
  public static AggregatingHistogram<Long, Long> LongSumHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<Long, Long>(bins, min, max, new Adapter<Long, Long>() {
      @Override
      public Long make() {
        return new Long(0L);
      }

      @Override
      public Long aggregate(Long existing, Long data) {
        return existing + data;
      }
    });
  }

  /**
   * Convenience constructor for Integer-based Histograms. Uses a constructor to
   * initialize bins with Double(0.0). Aggregation is done by adding the values
   * 
   * @param bins Number of bins
   * @param min Minimum coordinate
   * @param max Maximum coordinate
   * @return New histogram for Double.
   */
  public static AggregatingHistogram<Double, Double> DoubleSumHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<Double, Double>(bins, min, max, new Adapter<Double, Double>() {
      @Override
      public Double make() {
        return new Double(0.0);
      }

      @Override
      public Double aggregate(Double existing, Double data) {
        return existing + data;
      }
    });
  }

  /**
   * Histograms that work like two {@link #IntSumHistogram}, component wise.
   * 
   * @param bins Number of bins.
   * @return
   */
  public static AggregatingHistogram<Pair<Integer, Integer>, Pair<Integer, Integer>> IntSumIntSumHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<Pair<Integer, Integer>, Pair<Integer, Integer>>(bins, min, max, new Adapter<Pair<Integer, Integer>, Pair<Integer, Integer>>() {
      @Override
      public Pair<Integer, Integer> make() {
        return new Pair<Integer, Integer>(0,0);
      }

      @Override
      public Pair<Integer, Integer> aggregate(Pair<Integer, Integer> existing, Pair<Integer, Integer> data) {
        existing.setFirst(existing.getFirst() + data.getFirst());
        existing.setSecond(existing.getSecond() + data.getSecond());
        return existing;
      }
    });
  }
  
  /**
   * Histograms that work like two {@link #LongSumHistogram}, component wise.
   * 
   * @param bins Number of bins.
   * @return
   */
  public static AggregatingHistogram<Pair<Long, Long>, Pair<Long, Long>> LongSumLongSumHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<Pair<Long, Long>, Pair<Long, Long>>(bins, min, max, new Adapter<Pair<Long, Long>, Pair<Long, Long>>() {
      @Override
      public Pair<Long, Long> make() {
        return new Pair<Long, Long>(0L,0L);
      }

      @Override
      public Pair<Long, Long> aggregate(Pair<Long, Long> existing, Pair<Long, Long> data) {
        existing.setFirst(existing.getFirst() + data.getFirst());
        existing.setSecond(existing.getSecond() + data.getSecond());
        return existing;
      }
    });
  }
}
