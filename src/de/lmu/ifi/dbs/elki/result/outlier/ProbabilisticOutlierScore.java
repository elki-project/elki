package de.lmu.ifi.dbs.elki.result.outlier;

/**
 * Outlier score that is a probability value in the range 0.0 - 1.0
 * 
 * But the baseline may be different from 0.0!
 * 
 * @author Erich Schubert
 */
public class ProbabilisticOutlierScore implements OutlierScoreMeta {
  /**
   * Actual minimum seen, if given by the algorithm.
   */
  private double actualMinimum = Double.NaN;
  /**
   * Actual maximum seen, if given by the algorithm.
   */
  private double actualMaximum = Double.NaN;
  /**
   * Theretical baseline specified by the algorithm. Defaults to 0.0 in short constructor.
   */
  private double theoreticalBaseline = Double.NaN;

  /**
   * Default constructor. No actual values, Baseline 0.0
   */
  public ProbabilisticOutlierScore() {
    this(Double.NaN, Double.NaN, 0.0);
  }

  /**
   * Constructor with baseline only.
   * 
   * @param theoreticalBaseline Baseline
   */
  public ProbabilisticOutlierScore(double theoreticalBaseline) {
    this(Double.NaN, Double.NaN, theoreticalBaseline);
  }

  /**
   * Constructor with actual values, and a baseline of 0.0
   * 
   * @param actualMinimum actual minimum seen
   * @param actualMaximum actual maximum seen
   */
  public ProbabilisticOutlierScore(double actualMinimum, double actualMaximum) {
    this(actualMinimum, actualMaximum, 0.0);
  }

  /**
   * Full constructor.
   * 
   * @param actualMinimum actual minimum seen
   * @param actualMaximum actual maximum seen
   * @param theoreticalBaseline theoretical baseline
   */
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