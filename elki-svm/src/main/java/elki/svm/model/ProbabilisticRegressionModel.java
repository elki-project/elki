package elki.svm.model;

import elki.svm.data.DataSet;

/**
 * Probabilistic regression model
 */
public class ProbabilisticRegressionModel extends RegressionModel {
  /**
   * Estimated probability
   */
  public double probA;

  /**
   * Constructor.
   * 
   * @param probA Probability
   */
  public ProbabilisticRegressionModel(double probA) {
    super();
    this.probA = probA;
  }

  /**
   * Get the regression model probability, constant.
   * 
   * @return Probability
   */
  public double probability() {
    return probA;
  }

  /**
   * Predict for a single data point.
   * 
   * @param x Data set
   * @param xi Point offset
   * @return Prediction score
   */
  public double predict_prob(DataSet x, int xi) {
    double[] sv_coef = this.sv_coef[0];
    double sum = -rho[0];
    for(int i = 0; i < sv_indices.length; i++) {
      sum += sv_coef[i] * x.similarity(xi, sv_indices[i]);
    }
    return sum;
  }
}
