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
    determneGlobalExtremum();
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

  public double[] determineExtrema(double[] minAlpha, double[] maxAlpha) {
    double[] result = new double[2];


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

  private void determneGlobalExtremum() {
    alpha_extreme = new double[p.length - 1];
    for (int n = 0; n < alpha_extreme.length; n++) {
      alpha_extreme[n] = globalExtremum_alpha_n(n);
      if (debug) {
        debugFine("alpha_" + (n + 1) + " " + alpha_extreme[n]);
      }
    }
    isExtremumMinimum = isExtremumMinumum();
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

  private boolean isExtremumMinumum() {
    Matrix hessian = hessianMatrix(alpha_extreme);

    System.out.println("hessian " + hessian);

    return false;

  }

  private Matrix hessianMatrix(double[] alpha) {
    Matrix h = new Matrix(p.length - 1, p.length - 1);

    // diagonal
    for (int n = 0; n < p.length - 1; n++) {
      double h_nn = secondOrderPartialDerivative(n, alpha);
      System.out.println("h_"+n+n+" = " + h_nn);
      h.set(n,n,h_nn);
    }

    // other
    for (int n = 0; n < p.length - 1; n++) {
      double h_nm = 0;
      for (int m = n + 1; m < p.length - 1; m++) {
        for (int j = 0; j < n; j++) {
          double alpha_j_minus_1 = j == 0 ? 0 : alpha[j - 1];
          h_nm += p[j] * Math.cos(alpha_j_minus_1);
          h_nm *= sinusProduct(j, n - 1, alpha);
        }
        h_nm -= p[n + 1] * Math.sin(alpha[n]);
        h.set(n, m, h_nm);
        h.set(m, n, h_nm);
      }
    }

    return h;
  }

  private double secondOrderPartialDerivative(int n, double[] alpha) {
    double h_nn = 0;
    for (int j = 0; j <= n + 1; j++) {
      double alpha_j_minus_1 = j == 0 ? 0 : alpha[j-1];
      System.out.println("   alpha_j_minus_1 " + alpha_j_minus_1);
      h_nn += -p[j] * Math.cos(alpha_j_minus_1);
      System.out.println("   1. h_nn "+h_nn);
      h_nn *= sinusProduct(j, n+1, alpha);
      System.out.println("   2. h_nn "+h_nn);
    }
    h_nn *= sinusProduct(n+1, p.length-1, alpha);
    return h_nn;
  }

  public static void main(String[] args) {
    double[] p = new double[]{1, 1, 1};
    ParameterizationFunction f = new ParameterizationFunction(p);

    System.out.println("global extremum " + Util.format(f.alpha_extreme, ", ", 5));
  }


}
