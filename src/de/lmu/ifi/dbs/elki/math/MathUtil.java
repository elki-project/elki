package de.lmu.ifi.dbs.elki.math;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * A collection of math related utility functions.
 */
public class MathUtil {

   /** 
    * Computes the square root of the sum of the squared arguments
    * without under or overflow.
    * 
    * @param a first cathetus
    * @param b second cathetus
    * @return {@code sqrt(a<sup>2</sup> + b<sup>2</sup>)}
    */
   public static double hypotenuse(double a, double b) {
      double r;
      if (Math.abs(a) > Math.abs(b)) {
         r = b/a;
         r = Math.abs(a)*Math.sqrt(1+r*r);
      } else if (b != 0) {
         r = a/b;
         r = Math.abs(b)*Math.sqrt(1+r*r);
      } else {
         r = 0.0;
      }
      return r;
   }

  public static double mahalanobisDistance(Matrix weightMatrix, Vector o1_minus_o2) {
    double sqrDist = o1_minus_o2.transpose().times(weightMatrix).times(o1_minus_o2).get(0, 0);
  
    if (sqrDist < 0 && Math.abs(sqrDist) < 0.000000001) {
      sqrDist = Math.abs(sqrDist);
    }
    double dist = Math.sqrt(sqrDist);
    return dist;
  }
}
