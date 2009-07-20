package de.lmu.ifi.dbs.elki.math;

/**
 * An extension of {@link Histogram} for the commonly used histograms storing
 * {@link Integer} counts adding a convenience function {@link #count} to increment
 * the counter of the given coordinate.
 * 
 * @author Erich Schubert
 *
 */
public class HistogramInteger extends Histogram<Integer> {
  /**
   * Constructor for a new histogram.
   * 
   * New bins will be initialized with 0.
   * 
   * @param bins Number of bins
   * @param min Minimum value
   * @param max Maximum value
   */
  public HistogramInteger(int bins, double min, double max) {
    super(bins, min, max, new Constructor<Integer>() {
      @Override
      public Integer make() {
        return new Integer(0);
      }
    });
  }

  /**
   * Constructor for a new histogram, with explicit constructor.
   * 
   * @param bins Number of bins
   * @param min Minimum value
   * @param max Maximum value
   * @param maker Constructor for bins
   */
  public HistogramInteger(int bins, double min, double max, Constructor<Integer> maker) {
    super(bins, min, max, maker);
  }

  /**
   * Increment the bin of the given coordinate by 1.
   * 
   * @param coord Coordinate to count.
   */
  public void count(double coord) {
    super.put(coord, super.get(coord) + 1);
  }
}
