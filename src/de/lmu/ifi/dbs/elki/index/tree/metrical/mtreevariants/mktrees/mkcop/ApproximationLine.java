package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Provides an approximation for knn-distances line consisting of
 * incline m, axes intercept t and a start value for k.
 *
 * @author Elke Achtert 
 */
public class ApproximationLine implements Externalizable {
  private static final long serialVersionUID = 1;

  /**
   * The incline.
   */
  private double m;

  /**
   * The axes intercept.
   */
  private double t;

  /**
   * The start value for k.
   */
  private int k_0;

  /**
   * Empty constructor for serialization purposes.
   */
  public ApproximationLine() {
	  // empty constructor
  }

  /**
   * Provides an approximation for knn-distances line consisting of
   * incline m, axes intercept t and a start value for k.
   *
   * @param k_0 the start value for k
   * @param m   the incline
   * @param t   the axes intercept
   */
  public ApproximationLine(int k_0, double m, double t) {
    this.k_0 = k_0;
    this.m = m;
    this.t = t;
  }

  /**
   * Returns the incline.
   *
   * @return the incline
   */
  public double getM() {
    return m;
  }

  /**
   * Returns the axes intercept.
   *
   * @return the axes intercept
   */
  public double getT() {
    return t;
  }

  /**
   * Returns the start value for k.
   *
   * @return the start value for k
   */
  public int getK_0() {
    return k_0;
  }

  /**
   * Returns the function value of the approximation line
   * at the specified k.
   *
   * @param k the value for which the function value of the approximation line should be returned
   * @return the function value of the approximation line
   *         at the specified k
   */
  public double getValueAt(int k) {
    if (k < k_0) return Double.POSITIVE_INFINITY;
    return m * StrictMath.log(k) + t;
  }

  /**
   * Returns the approximated knn-distance at the specified k.
   *
   * @param <O> Object type
   * @param <D> Distance type
   * @param <N> Number type
   * @param k                the value for which the knn-distance should be returned
   * @param distanceFunction the distance function
   * @return the approximated knn-distance at the specified k
   */
  public <O extends DatabaseObject, D extends NumberDistance<D,N>, N extends Number> D getApproximatedKnnDistance(int k, DistanceFunction<O, D> distanceFunction) {
    if (k < k_0)
      return distanceFunction.nullDistance();
    return distanceFunction.valueOf("" + StrictMath.exp(getValueAt(k)));
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(m);
    out.writeDouble(t);
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   */
  public void readExternal(ObjectInput in) throws IOException {
    m = in.readDouble();
    t = in.readDouble();
  }

  /**
   * Returns true</code> if this object is the same as the o
   * argument; <code>false</code> otherwise.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj
   *         argument; <code>false</code> otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ApproximationLine that = (ApproximationLine) o;

    return Double.compare(that.m, m) == 0 &&
           Double.compare(that.t, t) == 0;
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = m != 0.0d ? Double.doubleToLongBits(m) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = t != 0.0d ? Double.doubleToLongBits(t) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return "m = " + m + ", t = " + t + " k_0 " + k_0;
  }
}
