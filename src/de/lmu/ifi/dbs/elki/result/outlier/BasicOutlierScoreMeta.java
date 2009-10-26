package de.lmu.ifi.dbs.elki.result.outlier;

public class BasicOutlierScoreMeta implements OutlierScoreMeta {
  double actualMinimum = Double.NaN;
  double actualMaximum = Double.NaN;
  double theoreticalMinimum = Double.NaN;
  double theoreticalMaximum = Double.NaN;
  double theoreticalBaseline = Double.NaN;
  
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    this(actualMinimum, actualMaximum, Double.NaN, Double.NaN, Double.NaN);
  }
  
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    this(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, Double.NaN);
  }
  
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
}
