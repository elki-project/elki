package de.lmu.ifi.dbs.elki.result.outlier;

/**
 * Generic meta information about the value range of an outlier score.
 * 
 * All values can be {@link Double#NaN} if not specified by the algorithm or not computed (actual values).
 * For theoretical values, it is explicitly allowed to return {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY}.
 * 
 * @author Erich Schubert
 */
public interface OutlierScoreMeta {
  /**
   * Get the actual minimum of the value range.
   * 
   * @return minimum or {@link Double#NaN}
   */
  public double getActualMinimum();
  /**
   * Get the actual maximum of the value range.
   * 
   * @return maximum or {@link Double#NaN}
   */
  public double getActualMaximum();
  /**
   * Get the theoretical minimum of the value range.
   * 
   * @return theoretical minimum or {@link Double#NaN}
   */
  public double getTheoreticalMinimum();
  /**
   * Get the theoretical maximum of the value range.
   *
   * This value may be {@link Double#NEGATIVE_INFINITY} or {@link Double#NaN}.
   * 
   * @return theoretical maximum or {@link Double#NaN}
   */
  public double getTheoreticalMaximum();
  /**
   * Get the theoretical baseline of the value range.
   * 
   * It will be common to see {@link Double#POSITIVE_INFINITY} here.
   * 
   * @return theoretical baseline or {@link Double#NaN}
   */
  public double getTheoreticalBaseline();
  /**
   * Return a normalized value of the outlier score.
   * 
   * @param value outlier score
   * @return Normalized value (in 0.0-1.0)
   */
  public double normalizeScore(double value);
}
