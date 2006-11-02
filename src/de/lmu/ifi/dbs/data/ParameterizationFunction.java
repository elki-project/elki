package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.util.Arrays;

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
  private double[] alphaExtremum;

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
//    this.debug = true;
    this.p = p;
    determineGlobalExtremum();

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\np = " + Format.format(p));
      msg.append("\n" + this.toString());
      msg.append("\nextremum " + Util.format(alphaExtremum) + " minimum " + isExtremumMinimum);
      msg.append("\nvalue = " + function(alphaExtremum));
      msg.append("\n");
      this.debugFine(msg.toString());
    }
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
      double alpha_i = i == d - 1 ? 0 : alpha[i];
      result += p[i] * sinusProduct(0, i, alpha) * Math.cos(alpha_i);
    }
    return result;
  }

  public HyperBoundingBox determineAlphaMinMax(HyperBoundingBox box) {
    if (box.getDimensionality() != p.length - 1) {
      throw new IllegalArgumentException("Box needs to have dimensionality d=" + (p.length - 1) +
                                         ", read: " + box.getDimensionality());
    }
    double[] alpha_min = new double[p.length - 1];
    double[] alpha_max = new double[p.length - 1];

    for (int d = p.length - 2; d >= 0; d--) {
      alpha_min[d] = determineAlphaMin(d, box.getMin(d + 1), box.getMax(d + 1), alpha_min);
      alpha_max[d] = determineAlphaMax(d, box.getMin(d + 1), box.getMax(d + 1), alpha_max);
    }


    return new HyperBoundingBox(alpha_min, alpha_max);
  }

  public double[] getPointCoordinates() {
    return p;
  }

  private boolean isMinimum(int n, double[] alpha_extreme) {
    double[] alpha_extreme_l = new double[alpha_extreme.length];
    double[] alpha_extreme_r = new double[alpha_extreme.length];
    double[] alpha_extreme_my = new double[alpha_extreme.length];
    System.arraycopy(alpha_extreme, 0, alpha_extreme_l, 0, alpha_extreme.length);
    System.arraycopy(alpha_extreme, 0, alpha_extreme_r, 0, alpha_extreme.length);
    System.arraycopy(alpha_extreme, 0, alpha_extreme_my, 0, alpha_extreme.length);
    Arrays.fill(alpha_extreme_l, 0, n, 1);
    Arrays.fill(alpha_extreme_r, 0, n, 1);
    Arrays.fill(alpha_extreme_my, 0, n, 1);
    alpha_extreme_l[n] = alpha_extreme[n] - 0.01;
    alpha_extreme_r[n] = alpha_extreme[n] + 0.01;

    double f = function(alpha_extreme_my);
    double f_l = function(alpha_extreme_l);
    double f_r = function(alpha_extreme_r);

//    System.out.println("alpha_l "+ Format.format(alpha_extreme_l));
//    System.out.println("alpha   "+ Format.format(alpha_extreme_my));
//    System.out.println("alpha_r "+ Format.format(alpha_extreme_r));

//    System.out.println("f_l "+ f_l);
//    System.out.println("f   "+ f);
//    System.out.println("f_r "+ f_r);

    if (f_l < f && f_r < f) return false;
    if (f_l > f && f_r > f) return true;
    throw new IllegalArgumentException("Houston, we have a problem!\n" +
                                       "f_l " + f_l + "\n" +
                                       "f   " + f + "\n" +
                                       "f_r " + f_r + "\n");
  }

  private double determineAlphaMin(int n, double min, double max, double[] alpha_min) {
    double alpha_n = extremum_alpha_n(n, alpha_min);

    double[] alpha_extreme = new double[alpha_min.length];
    System.arraycopy(alpha_min, n, alpha_extreme, n, alpha_extreme.length - n);
    alpha_extreme[n] = alpha_n;

    if (isMinimum(n, alpha_extreme)) {
      // A) min <= alpha_n <= max
      if (min <= alpha_n && alpha_n <= max) {
        return alpha_n;
      }
      // B) alpha_n < min
      else if (alpha_n < min) {
        return min;
      }
      // C) alpha_n > max
      else {
        if (alpha_n <= max) throw new IllegalStateException("Should never happen!");
        return max;
      }
    }
    // extremum is maximum
    else {
      if (min <= alpha_n && alpha_n <= max) {
        // A1) min <= alpha_n <= max  && alpha_n - min <= max - alpha_n
        if (alpha_n - min <= max - alpha_n) {
          return max;
        }
        // A2) min <= alpha_n <= max  && alpha_n - min > max - alpha_n
        else {
          return min;
        }
      }
      // B) alpha_n < min
      else if (alpha_n < min) {
        return max;
      }
      // C) alpha_n > max
      else {
        if (alpha_n <= max) throw new IllegalStateException("Should never happen!");
        return min;
      }
    }
  }

  private double determineAlphaMax(int n, double min, double max, double[] alpha_max) {
    double alpha_n = extremum_alpha_n(n, alpha_max);

    double[] alpha_extreme = new double[alpha_max.length];
    System.arraycopy(alpha_max, n, alpha_extreme, n, alpha_extreme.length - n);
    alpha_extreme[n] = alpha_n;

    if (isMinimum(n, alpha_extreme)) {
      if (min <= alpha_n && alpha_n <= max) {
        // A1) min <= alpha_n <= max  && alpha_n - min <= max - alpha_n
        if (alpha_n - min <= max - alpha_n) {
          return max;
        }
        // A2) min <= alpha_n <= max  && alpha_n - min > max - alpha_n
        else {
          return min;
        }
      }
      // B) alpha_n < min
      else if (alpha_n < min) {
        return max;
      }
      // C) alpha_n > max
      else {
        if (alpha_n <= max) throw new IllegalStateException("Should never happen!");
        return min;
      }
    }
    // extremum is maximum
    else {
      // A) min <= alpha_n <= max
      if (min <= alpha_n && alpha_n <= max) {
        return alpha_n;
      }
      // B) alpha_n < min
      else if (alpha_n < min) {
        return min;
      }
      // C) alpha_n > max
      else {
        if (alpha_n <= max) throw new IllegalStateException("Should never happen!");
        return max;
      }
    }
  }

  /**
   * Returns the dimensionality of the feature space.
   *
   * @return the dimensionality of the feature space
   */
  public int getDimensionality() {
    return p.length;
  }

  /**
   * Returns the alpha values of the extremum point in interval [(0,...,0), (Pi,...,Pi)].
   *
   * @return the alpha values of the extremum
   */
  public double[] getGlobalAlphaExtremum() {
    return alphaExtremum;
  }

  /**
   * Returns the extremum of this function in interval [(0,...,0), (Pi,...,Pi)]..
   *
   * @return the extremum
   */
  public double getGlobalExtremum() {
    return function(alphaExtremum);
  }

  /**
   * Returns true, if the extremum in interval [(0,...,0), (Pi,...,Pi)]
   * is a minimum, false if the extremum is a maximum.
   *
   * @return true, if global extremum is aminimum, false if the
   *         global extremum is a maximum
   */
  public boolean isExtremumMinimum() {
    return isExtremumMinimum;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (int d = 0; d < p.length; d++) {
      if (d != 0) {
        result.append(" + \n");
      }
      result.append(Util.format(p[d]));
      for (int j = 0; j < d; j++) {
        result.append(" * sin(a_").append(j + 1).append(")");
      }
      if (d != p.length - 1) {
        result.append(" * cos(a_").append(d + 1).append(")");
      }
    }
    return result.toString();
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

  /**
   * Determines the global extremum of this parameterization function.
   */
  private void determineGlobalExtremum() {
    alphaExtremum = new double[p.length - 1];
    for (int n = alphaExtremum.length - 1; n >= 0; n--) {
      alphaExtremum[n] = extremum_alpha_n(n, alphaExtremum);
    }
    determineIsGlobalExtremumMinumum();
  }

  /**
   * Determines if the global extremum is a minumum or a maximum.
   */
  private void determineIsGlobalExtremumMinumum() {
    Matrix hessian = hessianMatrix(alphaExtremum);
    Matrix minusHessian = hessian.times(-1);
    if (debug) {
      debugFiner("Hessian " + hessian);
    }

    boolean determinantGreaterZero = true;
    boolean minusDeterminantGreaterZero = true;
    for (int i = 0; i < p.length - 1; i++) {
      Matrix a = hessian.getMatrix(0, i, 0, i);
      Matrix minusA = minusHessian.getMatrix(0, i, 0, i);
      double det = a.det();
      double minusDet = minusA.det();
      if (debug) {
        debugFiner("\ndet  A_" + (i + 1) + (i + 1) + " " + det +
                   "\ndet -A_" + (i + 1) + (i + 1) + " " + minusDet);
      }
      determinantGreaterZero &= det > 0;
      minusDeterminantGreaterZero &= minusDet > 0;
    }

    if (determinantGreaterZero && minusDeterminantGreaterZero) {
      throw new IllegalStateException("Should never happen! " + Format.format(p));
    }
    if (!determinantGreaterZero && !minusDeterminantGreaterZero) {
      System.out.println(this);
      System.out.println(Format.format(this.getPointCoordinates()));
      throw new IllegalStateException("Houston, we have a problem!");
    }
    if (determinantGreaterZero) isExtremumMinimum = true;
    else if (minusDeterminantGreaterZero) isExtremumMinimum = false;
  }

  /**
   * Returns the hessian matrix of the given alpha values.
   *
   * @param alpha the alpha values to determine the hessian matrix for
   * @return the hessian matrix of the given alpha values
   */
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

  /**
   * Returns the value of the second order partial derivative of w.r.t. alpha_n, alpha_n
   *
   * @param n     the index of the alpha value
   * @param alpha the alpha values
   * @return the value of the second order partial derivative of w.r.t. alpha_n, alpha_n
   */
  private double secondOrderPartialDerivative(int n, double[] alpha) {
    double h_nn = 0;
    double sinProd = sinusProduct(0, n, alpha);
    for (int j = n; j < p.length; j++) {
      double alpha_j = j == p.length - 1 ? 0 : alpha[j];
      double prod = sinusProduct(n, j, alpha);
      h_nn += -p[j] * prod * Math.cos(alpha_j);
    }
    h_nn *= sinProd;

    if (Math.abs(h_nn) < DELTA) h_nn = 0;
    return h_nn;
  }

  /**
   * Returns the value of the second order partial derivative of w.r.t. alpha_n, alpha_m
   *
   * @param n     the index of the first alpha value
   * @param m     the index of the secondalpha value
   * @param alpha the alpha values
   * @return the value of the second order partial derivative of w.r.t. alpha_n, alpha_m
   */
  private double secondOrderPartialDerivative(int n, int m, double[] alpha) {
    if (m < n) return secondOrderPartialDerivative(m, n, alpha);
    if (n == m) return secondOrderPartialDerivative(n, alpha);

    double h_nm = 0;
    for (int j = m + 1; j < p.length; j++) {
      double alpha_j = j == p.length - 1 ? 0 : alpha[j];
      double prod = p[j] * Math.cos(alpha[m]);
      prod *= sinusProduct(m + 1, j, alpha);
      prod *= Math.cos(alpha_j);
      h_nm += prod;
    }
    h_nm -= p[m] * Math.sin(alpha[m]);
    h_nm *= sinusProduct(0, n, alpha);
    h_nm *= Math.cos(alpha[n]);
    h_nm *= sinusProduct(n + 1, m, alpha);

    if (Math.abs(h_nm) < DELTA) h_nm = 0;
    return h_nm;
  }

  /**
   * Determines the extremum value for alpha_n.
   *
   * @param n     the index of the angle
   * @param alpha the already determined alpha_values
   * @return the extremum value for alpha_n
   */
  private double extremum_alpha_n(int n, double[] alpha) {
    double tan = 0;

    for (int j = n + 1; j < p.length; j++) {
      double alpha_j = j == p.length - 1 ? 0 : alpha[j];
      tan += p[j] * sinusProduct(n + 1, j, alpha) * Math.cos(alpha_j);
    }
    tan /= p[n];

    if (debug) {
      debugFiner("tan alpha_" + (n + 1) + " = " + tan);
    }
    double alpha_n = Math.atan(tan);
    if (alpha_n < 0) {
      alpha_n = Math.PI + alpha_n;
    }
    return alpha_n;
  }
}
