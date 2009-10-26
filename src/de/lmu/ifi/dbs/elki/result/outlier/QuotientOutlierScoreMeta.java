package de.lmu.ifi.dbs.elki.result.outlier;

public class QuotientOutlierScoreMeta extends BasicOutlierScoreMeta {
  public QuotientOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    super(actualMinimum, actualMaximum);
  }

  public QuotientOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum);
  }

  public QuotientOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum, double theoreticalBaseline) {
    super(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, theoreticalBaseline);
  }
}
