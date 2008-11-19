package de.lmu.ifi.dbs.elki.math;

/**
 * Some mathematical error functions. Numerical approximations.
 * 
 * Currently, this class contains the erf and erfc error functions (Gaussian = normal distribution)
 * 
 * {@see <a href="http://en.wikipedia.org/wiki/Error_function">Wikipedia on Error functions</a>}
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 */
public final class ErrorFunctions {
  // Loosely based on http://www.netlib.org/specfun/erf
  static final double a[] = { 1.85777706184603153e-1, 3.16112374387056560e+0, 1.13864154151050156E+2, 3.77485237685302021e+2, 3.20937758913846947e+3 };

  static final double b[] = { 1.00000000000000000e00, 2.36012909523441209e01, 2.44024637934444173e02, 1.28261652607737228e03, 2.84423683343917062e03 };

  static final double c[] = { 2.15311535474403846e-8, 5.64188496988670089e-1, 8.88314979438837594e00, 6.61191906371416295e01, 2.98635138197400131e02, 8.81952221241769090e02, 1.71204761263407058e03, 2.05107837782607147e03, 1.23033935479799725E03 };

  static final double d[] = { 1.00000000000000000e00, 1.57449261107098347e01, 1.17693950891312499e02, 5.37181101862009858e02, 1.62138957456669019e03, 3.29079923573345963e03, 4.36261909014324716e03, 3.43936767414372164e03, 1.23033935480374942e03 };

  static final double p[] = { 1.63153871373020978e-2, 3.05326634961232344e-1, 3.60344899949804439e-1, 1.25781726111229246e-1, 1.60837851487422766e-2, 6.58749161529837803e-4 };

  static final double q[] = { 1.00000000000000000e00, 2.56852019228982242e00, 1.87295284992346047e00, 5.27905102951428412e-1, 6.05183413124413191e-2, 2.33520497626869185e-3 };

  static final double onebysqrtpi = 1 / Math.sqrt(Math.PI);

  /**
   * Complementary error function for Gaussian distributions = Normal distributions.
   * 
   * {@see <a href="http://en.wikipedia.org/wiki/Error_function">Wikipedia on Error functions</a>}
   * 
   * Numerical approximation using taylor series.
   * Implementation loosely based on http://www.netlib.org/specfun/erf
   * 
   * @param x parameter value
   * @return erfc(x)
   */
  public static final double erfc(double x) {
    if(Double.isNaN(x))
      return Double.NaN;
    if(Double.isInfinite(x))
      return (x < 0.0) ? 2 : 0;

    double result = Double.NaN;
    double absx = Math.abs(x);
    // First approximation interval
    if(absx < 0.46875) {
      double z = x * x;
      result = 1 - x * ((((a[0] * z + a[1]) * z + a[2]) * z + a[3]) * z + a[4]) / ((((b[0] * z + b[1]) * z + b[2]) * z + b[3]) * z + b[4]);
    }
    // Second approximation interval
    else if(absx < 4.0) {
      double z = absx;
      result = ((((((((c[0] * z + c[1]) * z + c[2]) * z + c[3]) * z + c[4]) * z + c[5]) * z + c[6]) * z + c[7]) * z + c[8]) / ((((((((d[0] * z + d[1]) * z + d[2]) * z + d[3]) * z + d[4]) * z + d[5]) * z + d[6]) * z + d[7]) * z + d[8]);
      double rounded = Math.round(result * 16.0) / 16.0;
      double del = (absx - rounded) * (absx + rounded);
      result = Math.exp(-rounded * rounded) * Math.exp(-del) * result;
      if(x < 0.0)
        result = 2.0 - result;
    }
    // Third approximation interval
    else {
      double z = 1.0 / (absx * absx);
      result = z * (((((p[0] * z + p[1]) * z + p[2]) * z + p[3]) * z + p[4]) * z + p[5]) / (((((q[0] * z + q[1]) * z + q[2]) * z + q[3]) * z + q[4]) * z + q[5]);
      result = (onebysqrtpi - result) / absx;
      double rounded = Math.round(result * 16.0) / 16.0;
      double del = (absx - rounded) * (absx + rounded);
      result = Math.exp(-rounded * rounded) * Math.exp(-del) * result;
      if(x < 0.0)
        result = 2.0 - result;
    }
    return result;
  }

  /**
   * Error function for Gaussian distributions = Normal distributions.
   * 
   * {@see <a href="http://en.wikipedia.org/wiki/Error_function">Wikipedia on Error functions</a>}
   * 
   * Numerical approximation using taylor series.
   * Implementation loosely based on http://www.netlib.org/specfun/erf
   * 
   * @param x parameter value
   * @return erf(x)
   */
  public static final double erf(double x) {
    if (x >= 0) return 1 - erfc(x);
    else        return erfc(x) - 1;
  }
}
