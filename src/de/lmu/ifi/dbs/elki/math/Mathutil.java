package de.lmu.ifi.dbs.elki.math;

/**
 * A collection of math related utility functions.
 */
public class Mathutil {

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
}
