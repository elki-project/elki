package elki.svm.model;

public class ProbabilisticRegressionModel extends RegressionModel {
  public double[] probA;

  public ProbabilisticRegressionModel(double[] probA) {
    super();
    this.probA = probA;
  }
}
