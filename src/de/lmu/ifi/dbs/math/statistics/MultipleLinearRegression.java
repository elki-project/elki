package de.lmu.ifi.dbs.math.statistics;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.utilities.output.Format;

/**
 * Multiple linear regression attempts to model the relationship between two or more
 * explanatory variables and a response variable by fitting a linear equation to observed data.
 * Every value of the independent variable x is associated with a value of the dependent
 * variable y.
 * <p/>
 * The population regression line for p explanatory variables x1, x2, ... , xp is defined
 * to be y = b0 + b1*x1 + b2*x2 + ... + bp*xp + e.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MultipleLinearRegression {

  /**
   * The (n x 1) - vector holding the y-values (y1, ..., yn)^T.
   */
  private final Vector y;

  /**
   * Holds the mean value of the y-values.
   */
  private final double y_mean;

  /**
   * The  (n x p+1)-matrix holding the x-values, where the i-th row has the form
   * (1 x1i ... x1p).
   */
  private final Matrix x;

  /**
   * The (p+1 x 1) - vector holding the estimated b-values (b0, b1, ..., bp)^T.
   */
  private final Vector b;

  /**
   * The (n x 1) - vector holding the estimated residuals (e1, ..., en)^T.
   */
  private final Vector e;

  /**
   * The error variance.
   */
  private final double variance;

  /**
   * Holds the matrix (x'x)^-1.
   */
  private final Matrix xx_inverse;

  /**
   * The sum of square residuals
   */
  private final double ssr;

  /**
   * The sum of square totals
   */
  private final double sst;

  /**
   * Provides a new multiple linear regression model with the specified
   * parameters.
   *
   * @param y the (n x 1) - vector holding the respnse values (y1, ..., yn)^T.
   * @param x the  (n x p+1)-matrix holding the explanatory values,
   *          where the i-th row has the form (1 x1i ... x1p).
   */
  public MultipleLinearRegression(Vector y, Matrix x) {
    if (y.getDimensionality() <= x.getColumnDimensionality())
      throw new IllegalArgumentException("Number of observed data has to be greater than " +
                                         "number of regressors: " +
                                         y.getDimensionality() + " > " +
                                         x.getColumnDimensionality());

    this.y = y;
    this.x = x;

    double sum = 0;
    for (int i = 0; i < y.getDimensionality(); i++) {
      sum += y.get(i);
    }
    y_mean = sum / y.getDimensionality();

    // estimate b, e
    xx_inverse = (x.transpose().times(x)).inverse();
    b = new Vector(xx_inverse.times(x.transpose()).times(y).getColumnPackedCopy());
//    b = new Vector(x.solve(y).getColumnPackedCopy());
    e = new Vector(y.minus(x.times(b)).getColumnPackedCopy());

    // sum of square residuals: ssr
    sum = 0;
    for (int i = 0; i < e.getDimensionality(); i++) {
      sum += e.get(i) * e.get(i);
    }
    ssr = sum;

    // sum of square totals: sst
    sum = 0;
    for (int i = 0; i < y.getDimensionality(); i++) {
      sum += Math.pow((y.get(i) - y_mean), 2);
    }
    sst = sum;

    // variance
    variance = ssr / (y.getDimensionality() - x.getColumnDimensionality() - 1);
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    StringBuffer msg = new StringBuffer();
    msg.append("\nx = ");
    msg.append(x.toString(9, 4));
    msg.append("\ny = ");
    msg.append(y.toString(9, 4));
    msg.append("\nb = ");
    msg.append(b.toString(9, 4));
    msg.append("\ne = ");
    msg.append(e.toString(9, 4));
    msg.append("error variance = " + Format.format(variance, 4));
    return msg.toString();
  }

  /**
   * Returns the sum of squares total.
   *
   * @return the sum of squares total
   */
  public double getSumOfSquaresTotal() {
    return sst;
  }

  /**
   * Returns the sum of square residuals.
   *
   * @return the sum of square residuals
   */
  public double getSumOfSquareResiduals() {
    return ssr;
  }

  /**
   * Returns the estimated coefficients
   *
   * @return the estimated coefficients
   */
  public Vector getEstimatedCoefficients() {
    return b;
  }

  /**
   * Returns the estimated residuals
   *
   * @return the estimated residuals
   */
  public Vector getEstimatedResiduals() {
    return e;
  }

  /**
   * Returns the coefficient of determination
   *
   * @return the coefficient of determination
   */
  public double coefficientOfDetermination() {
    return 1.0 - (ssr / sst);
  }

  /**
   * todo comment
   */
  public double estimateY(Matrix x) {
    return x.times(b).get(0, 0);
  }

  /**
   * todo comment
   */
  public boolean t_test_equals(double alpha, int j, double b_j) {
    double t_j = (b.get(j) - b_j) / Math.sqrt(variance * xx_inverse.get(j, j));
    System.out.println("t_" + j + " (" + (y.getDimensionality() - x.getColumnDimensionality()) + ") " + t_j);

    int n = y.getDimensionality() - x.getColumnDimensionality();
    double t_alpha = StudentDistribution.tValue(1.0 - alpha / 2, n);

    return Math.abs(t_j) <= t_alpha;
  }

  public double getVariance() {
    return variance;
  }


  public static void main(String[] args) {
//    int n = 10;
//    int p = 4;

//    Matrix x1 = Matrix.random(n, p);
//    Vector y1 = new Vector(Matrix.random(n, 1).getColumnPackedCopy());

    double[][] x1Array = new double[][]{
        {1, 1.0}, {1, 1}, {1, 1},
        {1, 2}, {1, 2}, {1, 2},
        {1, 3}, {1, 3}, {1, 3},
        {1, 4}, {1, 4}, {1, 4}};
    double[] y1Array = new double[]{
        1.0, 4, 3,
        2, 3, 4,
        5, 4, 5,
        4, 5, 7};

    Matrix x1 = new Matrix(x1Array);
    Vector y1 = new Vector(y1Array);

    MultipleLinearRegression regression = new MultipleLinearRegression(y1, x1);
    System.out.println("beta1 = " + regression.getEstimatedCoefficients());

    double[][] x2Array = new double[][]{{1, 1.0}, {1, 2}, {1, 3}, {1, 4}};
    double[] y2Array = new double[]{8.0/3, 3, 14.0/3, 16.0 / 3.0};
    Matrix x2 = new Matrix(x2Array);
    Vector y2 = new Vector(y2Array);
    MultipleLinearRegression regression2 = new MultipleLinearRegression(y2, x2);
    System.out.println("beta2 = " + regression2.getEstimatedCoefficients());

    System.out.println("");
    double[] y3Array = new double[]{1,2,5,4};
    double[] y4Array = new double[]{4,3,4,5};
    double[] y5Array = new double[]{3,4,5,7};
    MultipleLinearRegression r3 = new MultipleLinearRegression(new Vector(y3Array), x2);
    System.out.println("beta3 = " + r3.getEstimatedCoefficients());
    MultipleLinearRegression r4 = new MultipleLinearRegression(new Vector(y4Array), x2);
    System.out.println("beta4 = " + r4.getEstimatedCoefficients());
    MultipleLinearRegression r5 = new MultipleLinearRegression(new Vector(y5Array), x2);
    System.out.println("beta5 = " + r5.getEstimatedCoefficients());





  }


}
