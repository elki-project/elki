package de.lmu.ifi.dbs.elki.math.linearalgebra.fitting;

/**
 * Gaussian function for parameter fitting
 * 
 * Based mostly on fgauss in "Numerical Recpies in C".
 * However we've removed some small optimizations at the benefit of having
 * easier to use parameters. Instead of position, amplitude and width used
 * in the book, we use the traditional Gaussian parameters mean, standard deviation
 * and a linear scaling factor (which is mostly useful when combining multiple distributions)
 * The cost are some additional computations such as a square root.
 * This could of course have been handled by an appropriate wrapper instead.
 * 
 * They are also arranged differently: the book uses
 *   amplitude, position, width
 * whereas we use
 *   mean, stddev, scaling
 *   
 * The function also can use a mixture of gaussians, just use an appropriate number
 * of parameters (which obviously needs to be a multiple of 3)
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public class GaussianFittingFunction implements FittingFunction {
  /**
   * precomputed constant value of Sqrt(2*PI)
   */
  static final double Sqrt2PI = Math.sqrt(2 * Math.PI);
  /**
   * compute the mixture of Gaussians at the given position
   */
  public FittingFunctionResult eval(double x, double[] params) {
    int len = params.length;
    
    // We always need triples: (mean, stddev, scaling)
    assert (len % 3) == 0;

    double y = 0.0;
    double[] gradients = new double[len];
    
    // Mostly according to:
    // Numerical Recipes in C: The Art of Scientific Computing
    for (int i = 0; i < params.length; i+=3) {
      // Standardized Gaussian parameter (centered, scaled by stddev)
      double stdpar = (x - params[i]) / params[i+1];
      double e = Math.exp(-.5 * stdpar * stdpar);
      double localy = params[i+2]/(params[i+1] * Sqrt2PI) * e;
      double fac = localy * 2.0 * stdpar;
      
      y += localy;
      //// 1+ offsets at the beginning since we use [0] to return the y value!
      // mean gradient
      gradients[i] = localy * stdpar;
      // stddev gradient
      gradients[i+1] = ((x-params[i])*(x-params[i])-params[i+1]*params[i+1]) * localy / (params[i+1] * params[i+1]);
      // amplitude gradient
      gradients[i+2] = e / (params[i+1] * Sqrt2PI);
    }
    
    return new FittingFunctionResult(y, gradients);
  }
}
