package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * A parameterization function decribes all lines in a
 * d-dimensional feature space intersecting in one point p.
 * A single line in d-dimensional space is uniquely determined by a
 * translation vector p and (d-1) angles alpha_i belonging to the normal vector n.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ParameterizationFunction extends AbstractDatabaseObject {
  /**
   * A small number to handle numbers near 0 as 0.
   */
  public static final double DELTA = 1E-10;

  /**
   * Holds the values of the point p.
   */
  private double[] p;

  /**
   * Holds the alpha values of the global extremum.
   */
  private double[] alpha_extreme;

  /**
   * True, if global extremum is a minum,
   * false, if global extremum is a maximum.
   */
  private boolean isExtremumMinimum;

  /**
   * Provides a new parameterization function decribing all lines in a
   * d-dimensional feature space intersecting in one point p.
   *
   * @param p the values of the point p
   */
  public ParameterizationFunction(double[] p) {
    super();
    this.debug = true;
    this.p = p;
    determineGlobalExtremum();
  }

  /**
   * Computes the function value at <code>alpha</code>.
   *
   * @param alpha the values of the d-1 angles
   * @return the function value at alpha
   */
  public double function(double[] alpha) {
    int d = p.length;
    if (alpha.length != d - 1) {
      throw new IllegalArgumentException("Parameter alpha must have a " +
                                         "dimensionality of " + (d - 1) +
                                         ", read: " + alpha.length);
    }

    double result = 0;
    for (int i = 0; i < d; i++) {
      double alpha_i_minus_1 = i == 0 ? 0 : alpha[i - 1];
      result += p[i] * Math.cos(alpha_i_minus_1) * sinusProduct(i, d - 1, alpha);
    }
    return result;
  }

  public double[] determineAlphaExtrema(double[] minAlpha, double[] maxAlpha) {
    double[] result = new double[2];

    for (int i = 0; i < minAlpha.length; i++) {
      
    }


    return result;
  }

  /**
   * Computes the product of all sinus values of the specified angles
   * from start to end index.
   *
   * @param start the index to start
   * @param end   the index to end
   * @param alpha the array of angles
   * @return the product of all sinus values of the specified angles
   *         from start to end index
   */
  private double sinusProduct(int start, int end, double[] alpha) {
    double result = 1;
    for (int j = start; j < end; j++) {
      result *= Math.sin(alpha[j]);
    }
    return result;
  }

  private void determineGlobalExtremum() {
    alpha_extreme = new double[p.length - 1];
    for (int n = 0; n < alpha_extreme.length; n++) {
      alpha_extreme[n] = globalExtremum_alpha_n(n);
      if (debug) {
        debugFine("alpha_" + (n + 1) + " " + alpha_extreme[n]);
      }
    }
    determineIsExtremumMinumum();
  }

  private double globalExtremum_alpha_n(int n) {
    double tan = 0;
    for (int i = 0; i <= n; i++) {
      tan += p[i] * p[i];
    }
    tan = Math.sqrt(tan) / p[n + 1];
    tan = p[n] >= 0 ? tan : -tan;

    if (debug) {
      debugFine("tan alpha_" + (n + 1) + " = " + tan);
    }
    double alpha_n = Math.atan(tan);
    if (alpha_n < 0) {
      alpha_n = Math.PI + alpha_n;
    }
    return alpha_n;
  }

  private void determineIsExtremumMinumum() {
    Matrix hessian = hessianMatrix(alpha_extreme);
    Matrix minusHessian = hessian.times(-1);
    if (debug) {
      debugFine("Hessian " + hessian);
    }

    boolean determinantGreaterZero = true;
    boolean minusDeterminantGreaterZero = true;
    for (int i = 0; i < p.length - 1; i++) {
      Matrix a = hessian.getMatrix(0, i, 0, i);
      Matrix minusA = minusHessian.getMatrix(0, i, 0, i);
      double det = a.det();
      double minusDet = minusA.det();
      if (debug) {
        debugFine("\ndet  A_" + (i + 1) + (i + 1) + " " + det +
                  "\ndet -A_" + (i + 1) + (i + 1) + " " + minusDet);
      }
      determinantGreaterZero &= det > 0;
      minusDeterminantGreaterZero &= minusDet > 0;
    }

    if (determinantGreaterZero && minusDeterminantGreaterZero) {
      throw new IllegalStateException("Should never happen!");
    }
    if (!determinantGreaterZero && !minusDeterminantGreaterZero) {
      throw new IllegalStateException("Houston, we have a problem!");
    }
    if (determinantGreaterZero) isExtremumMinimum = true;
    else if (minusDeterminantGreaterZero) isExtremumMinimum = false;
  }

  private Matrix hessianMatrix(double[] alpha) {
    Matrix h = new Matrix(p.length - 1, p.length - 1);

    // diagonal
    for (int n = 0; n < p.length - 1; n++) {
      double h_nn = secondOrderPartialDerivative(n, alpha);
      h.set(n, n, h_nn);
    }

    // other
    for (int n = 0; n < p.length - 1; n++) {
      for (int m = n + 1; m < p.length - 1; m++) {
        double h_nm = secondOrderPartialDerivative(n, m, alpha);
        h.set(n, m, h_nm);
        h.set(m, n, h_nm);
      }
    }

    return h;
  }

  private double secondOrderPartialDerivative(int n, double[] alpha) {
    double h_nn = 0;
    for (int j = 0; j <= n + 1; j++) {
      double alpha_j_minus_1 = j == 0 ? 0 : alpha[j - 1];
      double prod = -p[j] * Math.cos(alpha_j_minus_1);
      prod *= sinusProduct(j, n + 1, alpha);
      h_nn += prod;
    }
    h_nn *= sinusProduct(n + 1, p.length - 1, alpha);
    if (Math.abs(h_nn) < DELTA) h_nn = 0;
    return h_nn;
  }

  private double secondOrderPartialDerivative(int n, int m, double[] alpha) {
    if (m < n) return secondOrderPartialDerivative(m, n, alpha);
    if (n == m) return secondOrderPartialDerivative(n, alpha);
    double h_nm = 0;
    for (int j = 0; j <= n; j++) {
      double alpha_j_minus_1 = j == 0 ? 0 : alpha[j - 1];
//      System.out.println("   *** j=" + j + ", n=" + n);
      double prod = p[j] * Math.cos(alpha_j_minus_1);
//      System.out.println("       -p[j] * cos(j-1) " + prod);
      prod *= sinusProduct(j, n, alpha);
//      System.out.println("       sinusProduct " + sinusProduct(j, n, alpha));
      prod *= Math.cos(alpha[n]);
//      System.out.println("       cos(n) " + Math.cos(alpha[n]));
//      System.out.println("       prod " + +prod);
      h_nm += prod;
    }
    h_nm -= p[n + 1] * Math.sin(alpha[n]);
    h_nm *= sinusProduct(n + 1, m, alpha);
    h_nm *= Math.cos(alpha[m]);
    h_nm *= sinusProduct(m + 1, p.length - 1, alpha);
    if (Math.abs(h_nm) < DELTA) h_nm = 0;
    return h_nm;
  }

  public static void main(String[] args) {
    double[] p = new double[]{1, -1};
    ParameterizationFunction f = new ParameterizationFunction(p);

    System.out.println("Global extremum at (" + Util.format(f.alpha_extreme, ", ", 5) + ") = " + f.function(f.alpha_extreme));
    if (f.isExtremumMinimum)
      System.out.println("Global extremum is a Minimum");
    else
      System.out.println("Global extremum is a Maximum");
  }


}
