package de.lmu.ifi.dbs.elki.result.outlier;

/**
 * Class to signal a value-inverted outlier score, i.e. low values are outliers.
 * 
 * @author Erich Schubert
 */
public class InvertedOutlierScoreMeta extends BasicOutlierScoreMeta {
  /**
   * Constructor with all values.
   * 
   * @param actualMinimum
   * @param actualMaximum
   * @param theoreticalMinimum
   * @param theoreticalMaximum
   * @param theoreticalBaseline
   */
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum, double theoreticalBaseline) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, theoreticalBaseline);
  }

  /**
   * Constructor with range values.
   * 
   * @param actualMinimum
   * @param actualMaximum
   * @param theoreticalMinimum
   * @param theoreticalMaximum
   */
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum);
  }

  /**
   * Constructor with actual range only.
   * 
   * @param actualMinimum
   * @param actualMaximum
   */
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    super(actualMinimum, actualMaximum);
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
    if (value > center) {
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
      return - (value - center) / (max - center);
    }
    return - (value - center);
  }  
}
