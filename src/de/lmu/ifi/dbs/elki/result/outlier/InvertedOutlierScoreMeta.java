package de.lmu.ifi.dbs.elki.result.outlier;

public class InvertedOutlierScoreMeta extends BasicOutlierScoreMeta {
  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum, double theoreticalBaseline) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, theoreticalBaseline);
  }

  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum);
  }

  public InvertedOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    super(actualMinimum, actualMaximum);
  }
}
