package de.lmu.ifi.dbs.elki.result.outlier;

public class ProbabilisticOutlierScore implements OutlierScoreMeta {
  private double actualMinimum = Double.NaN;
  private double actualMaximum = Double.NaN;
  private double theoreticalBaseline = Double.NaN;

  public ProbabilisticOutlierScore() {
    this(Double.NaN, Double.NaN, 0.0);
  }

  public ProbabilisticOutlierScore(double theoreticalBaseline) {
    this(Double.NaN, Double.NaN, theoreticalBaseline);
  }

  public ProbabilisticOutlierScore(double actualMinimum, double actualMaximum) {
    this(actualMinimum, actualMaximum, 0.0);
  }

  public ProbabilisticOutlierScore(double actualMinimum, double actualMaximum, double theoreticalBaseline) {
    super();
    this.actualMinimum = actualMinimum;
    this.actualMaximum = actualMaximum;
    this.theoreticalBaseline = theoreticalBaseline;
  }

  @Override
  public double getActualMinimum() {
    return actualMinimum;
  }

  @Override
  public double getActualMaximum() {
    return actualMaximum;
  }

  @Override
  public double getTheoreticalBaseline() {
    return theoreticalBaseline;
  }

  @Override
  public double getTheoreticalMaximum() {
    return 1.0;
  }

  @Override
  public double getTheoreticalMinimum() {
    return 0.0;
  }
}