package de.lmu.ifi.dbs.elki.result.outlier;

/**
 * Basic outlier score. Straightforward implementation of the {@link OutlierScoreMeta} interface.
 * 
 * @author Erich Schubert
 */
public class BasicOutlierScoreMeta implements OutlierScoreMeta {
  /**
   * Store the actual minimum
   */
  double actualMinimum = Double.NaN;
  /**
   * Store the actual maximum
   */
  double actualMaximum = Double.NaN;
  /**
   * Store the theoretical minimum
   */
  double theoreticalMinimum = Double.NaN;
  /**
   * Store the theoretical maximum
   */
  double theoreticalMaximum = Double.NaN;
  /**
   * Store the theoretical baseline
   */
  double theoreticalBaseline = Double.NaN;
  
  /**
   * Constructor with actual values only.
   * 
   * @param actualMinimum actual minimum
   * @param actualMaximum actual maximum
   */
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    this(actualMinimum, actualMaximum, Double.NaN, Double.NaN, Double.NaN);
  }
  
  /**
   * Constructor with all range values
   * 
   * @param actualMinimum actual minimum
   * @param actualMaximum actual maximum
   * @param theoreticalMinimum theoretical minimum
   * @param theoreticalMaximum theoretical maximum
   */
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    this(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, Double.NaN);
  }
  
  /**
   * Full constructor - all values.
   * 
   * @param actualMinimum actual minimum
   * @param actualMaximum actual maximum
   * @param theoreticalMinimum theoretical minimum
   * @param theoreticalMaximum theoretical maximum
   * @param theoreticalBaseline theoretical baseline
   */
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum, double theoreticalBaseline) {
    super();
    this.actualMinimum = actualMinimum;
    this.actualMaximum = actualMaximum;
    this.theoreticalMinimum = theoreticalMinimum;
    this.theoreticalMaximum = theoreticalMaximum;
    this.theoreticalBaseline = theoreticalBaseline;
  }

  @Override
  public double getActualMaximum() {
    return actualMaximum;
  }

  @Override
  public double getActualMinimum() {
    return actualMinimum;
  }

  @Override
  public double getTheoreticalBaseline() {
    return theoreticalBaseline;
  }

  @Override
  public double getTheoreticalMaximum() {
    return theoreticalMaximum;
  }

  @Override
  public double getTheoreticalMinimum() {
    return theoreticalMinimum;
  }

  @Override
  public double normalizeScore(double value) {
    double center = 0.0;
    if (!Double.isNaN(theoreticalBaseline) && !Double.isInfinite(theoreticalBaseline)) {
      center = theoreticalBaseline;
    } else if (!Double.isNaN(theoreticalMinimum) && !Double.isInfinite(theoreticalMinimum)) {
      center = theoreticalMinimum;
    } else if (!Double.isNaN(actualMinimum) && !Double.isInfinite(actualMinimum)) {
      center = actualMinimum;
    }
    if (value < center) {
      return 0.0;
    }
    double max = Double.NaN;
    if (!Double.isNaN(theoreticalMaximum) && !Double.isInfinite(theoreticalMaximum)) {
      max = theoreticalMaximum;
    }
    else if (!Double.isNaN(actualMaximum) && !Double.isInfinite(actualMaximum)) {
      max = actualMaximum;
    }
    if (!Double.isNaN(max) && !Double.isInfinite(max) && max >= center) {
      return (value - center) / (max - center);
    }
    return value - center;
  }
}
