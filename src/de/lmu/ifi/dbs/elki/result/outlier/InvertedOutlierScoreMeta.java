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
}
