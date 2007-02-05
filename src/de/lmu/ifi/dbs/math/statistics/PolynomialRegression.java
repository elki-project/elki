package de.lmu.ifi.dbs.math.statistics;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;

import java.util.Random;

/**
 * A polynomial fit is a specific type of multiple regression.
 * The simple regression model (a first-order polynomial) can be trivially extended
 * to higher orders.
 * <p/>
 * The regression model y = b0 + b1*x + b2*x^2 + ... + bp*x^p + e
 * is a system of polynomial equations of order p with
 * polynomial coefficients { b0 ... bp}.
 * The model can be expressed using data matrix x, target vector y and parameter vector ?.
 * The ith row of X and Y will contain the x and y value for the ith data sample.
 * <p/>
 * The variables will be transformed in the following way:
 * x => x1, ..., x^p => xp
 * Then the model can be written as a multiple
 * linear equation model: y = b0 + b1*x1 + b2*x2 + ... + bp*xp + e
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PolynomialRegression extends MultipleLinearRegression {

  /**
   * The order of the polynom.
   */
  public final int p;

  /**
   * Provides a new polynomial regression model with the specified
   * parameters.
   *
   * @param p the order of the polynom.
   * @param x the (n x 1)-vector holding the x-values (x1, ..., xn)^T.
   */
  public PolynomialRegression(Vector y, Vector x, int p) {
    super(y, xMatrix(x, p));
//    System.out.println("x "+x);
//    System.out.println("y"+y);
    this.p = p;
  }


  private static Matrix xMatrix(Vector x, int p) {
    int n = x.getRowDimensionality();

    Matrix result = new Matrix(n, p + 1);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < p + 1; j++) {
        result.set(i, j, Math.pow(x.get(i), j));
      }
    }
    return result;
  }

  /**
   * Returns the adapted coefficient of determination
   *
   * @return the adapted coefficient of determination
   */
  public double adaptedCoefficientOfDetermination() {
    int n = getEstimatedResiduals().getRowDimensionality();
    return 1.0 - ((n - 1.0) / (n * 1.0 - p)) * (1 - coefficientOfDetermination());
  }

  /**
   * todo comment
   */
  public double estimateY(double x) {
    return super.estimateY(xMatrix(new Vector(new double[]{x}), p));
  }

  public static void main(String[] args) {
//    int n = 10;
//    int p = 4;

//    Vector x = new Vector(Matrix.random(n, 1).getColumnPackedCopy());
//    Vector y = new Vector(Matrix.random(n, 1).getColumnPackedCopy());
//
//    System.out.println(x.toString(9,4));
//
//    PolynomialRegression regression = new PolynomialRegression(y, x, 3);
//
//    regression.leastSquareEstimator();
//    System.out.println(""+regression);
//    System.out.println(""+regression.sumOfSquareResiduals());

    Random random = new Random(210571);
    int T = 100;
    Vector x = new Vector(T);
    Vector y = new Vector(T);

    for (int i = 0; i < T; i++) {
      double x_i = i+1;
      x.set(i, x_i);
      y.set(i, 5 + 2 * x_i + 3 * Math.pow(x_i, 2) + 4 * Math.pow(x_i, 3) + (random.nextDouble() / 100));
    }

    PolynomialRegression r1 = new PolynomialRegression(y, x, 1);
    double sqr1 = r1.getSumOfSquareResiduals();

    PolynomialRegression r2 = new PolynomialRegression(y, x, 2);
    double sqr2 = r2.getSumOfSquareResiduals();

    PolynomialRegression r3 = new PolynomialRegression(y, x, 3);
    double sqr3 = r3.getSumOfSquareResiduals();

    PolynomialRegression r4 = new PolynomialRegression(y, x, 4);
    double sqr4 = r4.getSumOfSquareResiduals();

    int k = 4;
    double f1 = ((sqr1 - sqr4) / (k - 1)) / (sqr4 / (T - k));
    double f2 = ((sqr2 - sqr4) / (k - 2)) / (sqr4 / (T - k));
    double f3 = ((sqr3 - sqr4) / (k - 3)) / (sqr4 / (T - k));

    System.out.println("");
//    System.out.println("r1 "+r1);
//    System.out.println("b1 " + r1.getEstimatedCoefficients().toString(9, 4));
    System.out.println("sqr1 " + sqr1);
    System.out.println("r1^2 " + r1.coefficientOfDetermination());
    System.out.println("r1^2 " + r1.adaptedCoefficientOfDetermination());

    System.out.println("");
//    System.out.println("\n\nr2 "+r2);
//    System.out.println("b2 " + r2.getEstimatedCoefficients().toString(9, 4));
    System.out.println("sqr2 " + sqr2);
    System.out.println("r2^2 " + r2.coefficientOfDetermination());
    System.out.println("r2^2 " + r2.adaptedCoefficientOfDetermination());

    System.out.println("");
//    System.out.println("\n\nr3 "+r3);
//    System.out.println("b3 " + r3.getEstimatedCoefficients().toString(9, 4));
    System.out.println("sqr3 " + sqr3);
    System.out.println("r3^2 " + r3.coefficientOfDetermination());
    System.out.println("r3^2 " + r3.adaptedCoefficientOfDetermination());

    System.out.println("");
//    System.out.println("\n\nr4 "+r4);
//    System.out.println("b4 " + r4.getEstimatedCoefficients().toString(9, 4));
    System.out.println("sqr4 " + sqr4);
    System.out.println("r4^2 " + r4.coefficientOfDetermination());
    System.out.println("r4^2 " + r4.adaptedCoefficientOfDetermination());

    System.out.println("");
    System.out.println("Modell 1: F(" + (k - 1) + ", " + (T - k) + ") " + f1);
    System.out.println("Modell 2: F(" + (k - 2) + ", " + (T - k) + ") " + f2);
    System.out.println("Modell 3: F(" + (k - 3) + ", " + (T - k) + ") " + f3);

    System.out.println("");
    double f11 = ((r4.coefficientOfDetermination() - r1.coefficientOfDetermination()) / (k-1)) / ((1-r4.coefficientOfDetermination()) / (T -k));
    double f21 = ((r4.coefficientOfDetermination() - r2.coefficientOfDetermination()) / (k-2)) / ((1-r4.coefficientOfDetermination()) / (T -k));
    double f31 = ((r4.coefficientOfDetermination() - r3.coefficientOfDetermination()) / (k-3)) / ((1-r4.coefficientOfDetermination()) / (T -k-1));
    System.out.println("Modell 1: F(" + (k - 1) + ", " + (T - k) + ") " + f11);
    System.out.println("Modell 2: F(" + (k - 2) + ", " + (T - k) + ") " + f21);
    System.out.println("Modell 3: F(" + (k - 3) + ", " + (T - k) + ") " + f31);
    System.out.println(r3.getEstimatedCoefficients().toString(9,12));
    System.out.println(r4.getEstimatedCoefficients().toString(9,12));

    for (int i = 1; i <= k; i++) {
      System.out.println(r4.t_test_equals(0.05, i, 0));
    }

  }
}
