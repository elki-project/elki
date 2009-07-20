package de.lmu.ifi.dbs.elki.math;


public class AggregatingHistogram<T, D> extends ReplacingHistogram<T> {
  private Adapter<T, D> putter;

  public static abstract class Adapter<T, D> extends ReplacingHistogram.Adapter<T> {
    public abstract T add(T existing, D data);
  }

  public AggregatingHistogram(int bins, double min, double max, Adapter<T, D> adapter) {
    super(bins, min, max, adapter);
    this.putter = adapter;
  }
  
  public void add(double coord, D value) {
    super.put(coord, putter.add(super.get(coord), value));
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
  public static AggregatingHistogram<MeanVariance, Double> MeanVarianceHistogram(int bins, double min, double max) {
    return new AggregatingHistogram<MeanVariance, Double>(bins, min, max, new Adapter<MeanVariance, Double>() {
      @Override
      public MeanVariance make() {
        return new MeanVariance();
      }

      @Override
      public MeanVariance add(MeanVariance existing, Double data) {
        existing.put(data);
        return existing;
      }
    });
  }
  
  /**
   * Convenience constructor for Integer-based Histograms.
   * Uses a constructor to initialize bins with Integer(0).
   * Aggregation is done by adding the values
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
      public Integer add(Integer existing, Integer data) {
        return existing + data;
      }
    });
  }
  
  /**
   * Convenience constructor for Integer-based Histograms.
   * Uses a constructor to initialize bins with Double(0.0).
   * Aggregation is done by adding the values
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
      public Double add(Double existing, Double data) {
        return existing + data;
      }
    });
  }
}
