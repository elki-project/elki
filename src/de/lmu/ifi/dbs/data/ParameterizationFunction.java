package de.lmu.ifi.dbs.data;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.output.Format;

/**
 * A parameterization function decribes all lines in a
 * d-dimensional feature space intersecting in one point p.
 * A single line in d-dimensional space is uniquely determined by a
 * translation vector p and (d-1) angles alpha_i belonging to the normal vector n.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ParameterizationFunction extends DoubleVector {
  /**
   * A small number to handle numbers near 0 as 0.
   */
  public static final double DELTA = 1E-10;

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
    super(p);
//    this.debug = true;
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
   * @return a new DoubleVector with the specified values
   * @see de.lmu.ifi.dbs.data.RealVector#newInstance(double[])
   */
  public RealVector<Double> newInstance(double[] values) {
    return new ParameterizationFunction(values);
  }

  /**
   * Computes the function value at <code>alpha</code>.
   *
   * @param alpha the values of the d-1 angles
   * @return the function value at alpha
   */
  public double function(double[] alpha) {
    int d = getDimensionality();
    if (alpha.length != d - 1) {
      throw new IllegalArgumentException("Parameter alpha must have a " +
                                         "dimensionality of " + (d - 1) +
                                         ", read: " + alpha.length);
    }

    double result = 0;
    for (int i = 0; i < d; i++) {
      double alpha_i = i == d - 1 ? 0 : alpha[i];
      result += getValue(i + 1) * sinusProduct(0, i, alpha) * Math.cos(alpha_i);
    }
    return result;
  }

  public HyperBoundingBox determineAlphaMinMax(HyperBoundingBox box) {
    int dim = getDimensionality();
    if (box.getDimensionality() != dim - 1) {
      throw new IllegalArgumentException("Box needs to have dimensionality d=" + (dim - 1) +
                                         ", read: " + box.getDimensionality());
    }

    double[] alpha_min = new double[dim - 1];
    double[] alpha_max = new double[dim - 1];

    if (box.contains(new HyperBoundingBox(alphaExtremum, alphaExtremum))) {
      if (isExtremumMinimum) {
        alpha_min = alphaExtremum;
        for (int d = dim - 2; d >= 0; d--) {
          alpha_max[d] = determineAlphaMax(d, box.getMin(d + 1), box.getMax(d + 1), alpha_max, box);
        }
      }
      else {
        alpha_max = alphaExtremum;
        for (int d = dim - 2; d >= 0; d--) {
          alpha_min[d] = determineAlphaMin(d, box.getMin(d + 1), box.getMax(d + 1), alpha_min, box);
        }
      }
    }
    else {
      for (int d = dim - 2; d >= 0; d--) {
        alpha_min[d] = determineAlphaMin(d, box.getMin(d + 1), box.getMax(d + 1), alpha_min, box);
        alpha_max[d] = determineAlphaMax(d, box.getMin(d + 1), box.getMax(d + 1), alpha_max, box);
      }
    }


    return new HyperBoundingBox(alpha_min, alpha_max);
  }

  private boolean isMinimum(int n, double[] alpha_extreme, HyperBoundingBox box) {
    if (n == alpha_extreme.length - 1) {
      return isExtremumMinimum;
    }
    double[] alpha_extreme_l = new double[alpha_extreme.length];
    double[] alpha_extreme_r = new double[alpha_extreme.length];
    double[] alpha_extreme_my = new double[alpha_extreme.length];

    System.arraycopy(alpha_extreme, 0, alpha_extreme_l, 0, alpha_extreme.length);
    System.arraycopy(alpha_extreme, 0, alpha_extreme_r, 0, alpha_extreme.length);
    System.arraycopy(alpha_extreme, 0, alpha_extreme_my, 0, alpha_extreme.length);

    double[] centroid = box.centroid();
    for (int i = 0; i < n; i++) {
      alpha_extreme_l[i] = centroid[i];
      alpha_extreme_r[i] = centroid[i];
      alpha_extreme_my[i] = centroid[i];
    }

    double interval = box.getMax(n + 1) - box.getMin(n + 1);
    alpha_extreme_l[n] = Math.random() * interval + box.getMin(n + 1);
    alpha_extreme_r[n] = Math.random() * interval + box.getMin(n + 1);

    double f = function(alpha_extreme_my);
    double f_l = function(alpha_extreme_l);
    double f_r = function(alpha_extreme_r);

//    if (f_l < f && f_r < f) return false;
//    if (f_l > f && f_r > f) return true;
//    if (Math.abs(f_l - f) < 0.000000000001 && Math.abs(f_r - f) < 0.000000000001) return isExtremumMinimum;

    if (f_l < f) return false;
    if (f_l > f) return true;
    if (Math.abs(f_l - f) < 0.000000000001) return isExtremumMinimum;
//    return isExtremumMinimum;

throw new IllegalArgumentException("Houston, we have a problem!\n" +
    getID() + "\n" +
    this + "\n" +
    "f_l " + f_l + "\n" +
    "f   " + f + "\n" +
    "f_r " + f_r + "\n" +
    "p " + getColumnVector() + "\n" +
    "box min " + Format.format(box.getMin()) + "\n" +
    "box max " + Format.format(box.getMax()) + "\n" +
    "alpha   " + Format.format(alpha_extreme_my) + "\n" +
    "alpha_l " + Format.format(alpha_extreme_l) + "\n" +
    "alpha_r " + Format.format(alpha_extreme_r) + "\n" +
    "n " + n);

  }

  private double determineAlphaMin(int n, double min, double max, double[] alpha_min, HyperBoundingBox box) {
    double alpha_n = extremum_alpha_n(n, alpha_min);

    double[] alpha_extreme = new double[alpha_min.length];
    System.arraycopy(alpha_min, n, alpha_extreme, n, alpha_extreme.length - n);
    alpha_extreme[n] = alpha_n;

    if (isMinimum(n, alpha_extreme, box)) {
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

  private double determineAlphaMax(int n, double min, double max, double[] alpha_max, HyperBoundingBox box) {
    double alpha_n = extremum_alpha_n(n, alpha_max);

    double[] alpha_extreme = new double[alpha_max.length];
    System.arraycopy(alpha_max, n, alpha_extreme, n, alpha_extreme.length - n);
    alpha_extreme[n] = alpha_n;

    if (isMinimum(n, alpha_extreme, box)) {
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
    for (int d = 0; d < getDimensionality(); d++) {
      if (d != 0) {
        result.append(" + \n");
      }
      result.append(Util.format(getValue(d + 1)));
      for (int j = 0; j < d; j++) {
        result.append(" * sin(a_").append(j + 1).append(")");
      }
      if (d != getDimensionality() - 1) {
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
    alphaExtremum = new double[getDimensionality() - 1];
    for (int n = alphaExtremum.length - 1; n >= 0; n--) {
      alphaExtremum[n] = extremum_alpha_n(n, alphaExtremum);
      if (Double.isNaN(alphaExtremum[n])) {
        throw new IllegalStateException("Houston, we have a problem!" +
                                        "\n" + this +
                                        "\n" + this.getColumnVector() +
                                        "\n" + Format.format(alphaExtremum));
      }
    }

    determineIsGlobalExtremumMinumum();
  }

  /**
   * Determines if the global extremum is a minumum or a maximum.
   */
  private void determineIsGlobalExtremumMinumum() {
    double f = function(alphaExtremum);
    double[] alpha_1 = new double[alphaExtremum.length];
    double[] alpha_2 = new double[alphaExtremum.length];
    System.arraycopy(alphaExtremum, 0, alpha_1, 0, alphaExtremum.length);
    System.arraycopy(alphaExtremum, 0, alpha_2, 0, alphaExtremum.length);
    double x1 = Math.random() * Math.PI;
    double x2 = Math.random() * Math.PI;
    alpha_1[0] = x1;
    alpha_2[0] = x2;

    double f1 = function(alpha_1);
    double f2 = function(alpha_2);

    if (f1 < f && f2 < f) isExtremumMinimum = false;
    else if (f1 > f && f2 > f) isExtremumMinimum = true;
    else throw new IllegalStateException("Houston, we have a problem:" +
                                         "\n" + this +
                                         "\nid " + this.getID() +
                                         " \n" + Format.format(alphaExtremum) +
                                         "\nf " + f +
                                         "\nf1 " + f1 +
                                         "\nf2 " + f2);

    /*
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
      throw new IllegalStateException("Houston, we have a problem: |D|>0 && |-D|>0" +
                                      "\n" + this +
                                      "\n" + Format.format(this.getPointCoordinates()));
    }
    if (!determinantGreaterZero && !minusDeterminantGreaterZero) {

      throw new IllegalStateException("Houston, we have a problem: |D|<0 && |-D|<0" +
                                      "\n" + this +
                                      "\n" + Format.format(this.getPointCoordinates()) +
                                      " \n" + Format.format(alphaExtremum));
    }
    if (determinantGreaterZero) isExtremumMinimum = true;
    else if (minusDeterminantGreaterZero) isExtremumMinimum = false;
    */
  }

  /**
   * Returns the hessian matrix of the given alpha values.
   *
   * @param alpha the alpha values to determine the hessian matrix for
   * @return the hessian matrix of the given alpha values
   */
  private Matrix hessianMatrix(double[] alpha) {
    int dim = getDimensionality();
    Matrix h = new Matrix(dim - 1, dim - 1);

    // diagonal
    for (int n = 0; n < dim - 1; n++) {
      double h_nn = secondOrderPartialDerivative(n, alpha);
      h.set(n, n, h_nn);
    }

    // other
    for (int n = 0; n < dim - 1; n++) {
      for (int m = n + 1; m < dim - 1; m++) {
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
    int dim = getDimensionality();
    double h_nn = 0;
    double sinProd = sinusProduct(0, n, alpha);
    for (int j = n; j < dim; j++) {
      double alpha_j = j == dim - 1 ? 0 : alpha[j];
      double prod = sinusProduct(n, j, alpha);
      h_nn += -getValue(j + 1) * prod * Math.cos(alpha_j);
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
    int dim = getDimensionality();
    if (m < n) return secondOrderPartialDerivative(m, n, alpha);
    if (n == m) return secondOrderPartialDerivative(n, alpha);

    double h_nm = 0;
    for (int j = m + 1; j < dim; j++) {
      double alpha_j = j == dim - 1 ? 0 : alpha[j];
      double prod = getValue(j + 1) * Math.cos(alpha[m]);
      prod *= sinusProduct(m + 1, j, alpha);
      prod *= Math.cos(alpha_j);
      h_nm += prod;
    }
    h_nm -= getValue(m + 1) * Math.sin(alpha[m]);
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
    if (getValue(n + 1) == 0) {
      return 0.5 * Math.PI;
    }

    double tan = 0;
    for (int j = n + 1; j < getDimensionality(); j++) {
      double alpha_j = j == getDimensionality() - 1 ? 0 : alpha[j];
      tan += getValue(j + 1) * sinusProduct(n + 1, j, alpha) * Math.cos(alpha_j);
    }
    tan /= getValue(n + 1);

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
