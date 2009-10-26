package de.lmu.ifi.dbs.elki.result.outlier;

public interface OutlierScoreMeta {
  public double getActualMinimum();
  public double getActualMaximum();
  public double getTheoreticalMinimum();
  public double getTheoreticalMaximum();
  public double getTheoreticalBaseline();
}
