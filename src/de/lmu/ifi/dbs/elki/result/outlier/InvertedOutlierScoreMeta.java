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
   * @param actualMinimum Actual minimum in data
   * @param actualMaximum Actual maximum in data
   * @param theoreticalMinimum Theoretical minimum of algorithm
   * @param theoreticalMaximum Theoretical maximum of algorithm
   * @param theoreticalBaseline Theoretical Baseline
   */
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum, double theoreticalBaseline) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, theoreticalBaseline);
  }

  /**
   * Constructor with range values.
   * 
   * @param actualMinimum Actual minimum in data
   * @param actualMaximum Actual maximum in data
   * @param theoreticalMinimum Theoretical minimum of algorithm
   * @param theoreticalMaximum Theoretical maximum of algorithm
   */
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum);
  }

  /**
   * Constructor with actual range only.
   * 
   * @param actualMinimum Actual minimum in data
   * @param actualMaximum Actual maximum in data
   */
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    super(actualMinimum, actualMaximum);
  }
  
  @Override
  public double normalizeScore(double value) {
    double center = 0.0;
    if (!Double.isNaN(theoreticalBaseline) && !Double.isInfinite(theoreticalBaseline)) {
      center = theoreticalBaseline;
    } else if (!Double.isNaN(theoreticalMaximum) && !Double.isInfinite(theoreticalMaximum)) {
      center = theoreticalMaximum;
    } else if (!Double.isNaN(actualMaximum) && !Double.isInfinite(actualMaximum)) {
      center = actualMaximum;
    }
    if (value > center) {
      return 0.0;
    }
    double min = Double.NaN;
    if (!Double.isNaN(theoreticalMinimum) && !Double.isInfinite(theoreticalMinimum)) {
      min = theoreticalMinimum;
    }
    else if (!Double.isNaN(actualMinimum) && !Double.isInfinite(actualMinimum)) {
      min = actualMinimum;
    }
    if (!Double.isNaN(min) && !Double.isInfinite(min) && min >= center) {
      return (value - center) / (min - center);
    }
    return - (value - center);
  }  
}