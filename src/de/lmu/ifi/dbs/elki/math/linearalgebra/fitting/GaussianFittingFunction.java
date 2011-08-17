package de.lmu.ifi.dbs.elki.math.linearalgebra.fitting;

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Gaussian function for parameter fitting
 * 
 * Based loosely on fgauss in the book "Numerical Recipies". <br />
 * We did not bother to implement all optimizations at the benefit of having
 * easier to use parameters. Instead of position, amplitude and width used in
 * the book, we use the traditional Gaussian parameters mean, standard deviation
 * and a linear scaling factor (which is mostly useful when combining multiple
 * distributions) The cost are some additional computations such as a square
 * root and probably a slight loss in precision. This could of course have been
 * handled by an appropriate wrapper instead.
 * 
 * Due to their license, we cannot use their code, but we have to implement the
 * mathematics ourselves. We hope the loss in precision isn't big.
 * 
 * They are also arranged differently: the book uses
 * 
 * <pre>
 * amplitude, position, width
 * </pre>
 * 
 * whereas we use
 * 
 * <pre>
 * mean, stddev, scaling
 * </pre>
 * 
 * But we're obviously using essentially the same mathematics.
 * 
 * The function also can use a mixture of gaussians, just use an appropriate
 * number of parameters (which obviously needs to be a multiple of 3)
 * 
 * @author Erich Schubert
 */
public class GaussianFittingFunction implements FittingFunction {
  /**
   * compute the mixture of Gaussians at the given position
   */
  @Override
  public FittingFunctionResult eval(double x, double[] params) {
    int len = params.length;

    // We always need triples: (mean, stddev, scaling)
    assert (len % 3) == 0;

    double y = 0.0;
    double[] gradients = new double[len];

    // Loosely based on the book:
    // Numerical Recipes in C: The Art of Scientific Computing
    // Due to their license, we cannot use their code, but we have to implement
    // the mathematics ourselves. We hope the loss in precision is not too big.
    for(int i = 0; i < params.length; i += 3) {
      // Standardized Gaussian parameter (centered, scaled by stddev)
      double stdpar = (x - params[i]) / params[i + 1];
      double e = Math.exp(-.5 * stdpar * stdpar);
      double localy = params[i + 2] / (params[i + 1] * MathUtil.SQRTTWOPI) * e;

      y += localy;
      // mean gradient
      gradients[i] = localy * stdpar;
      // stddev gradient
      gradients[i + 1] = (stdpar * stdpar - 1.0) * localy;
      // amplitude gradient
      gradients[i + 2] = e / (params[i + 1] * MathUtil.SQRTTWOPI);
    }

    return new FittingFunctionResult(y, gradients);
  }
}