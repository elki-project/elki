package de.lmu.ifi.dbs.index.metrical.mtree.mkcop;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ApproximationLine implements Externalizable {

  private double m;
  private double t;
  private int k_0;

  /**
   * Empty constructor for serialization purposes.
   */
  public ApproximationLine() {
  }

  public ApproximationLine(int k_0, double m, double t) {
    this.k_0 = k_0;
    this.m = m;
    this.t = t;
  }

  public double getM() {
    return m;
  }

  public double getT() {
    return t;
  }

  public int getK_0() {
    return k_0;
  }

  public double getValueAt(int k) {
    if (k < k_0) return Double.POSITIVE_INFINITY;
//    System.out.println(k_0 + " getValueAt " + k + ": m * log(k) + t = " + m + " * log(" + k + ") + " + t + " = " + (m * Math.log(k) + t));
    return m * Math.log(k) + t;
  }

  public <O extends MetricalObject, D extends NumberDistance<D>> D getApproximatedKnnDistance(int k, DistanceFunction<O, D> distanceFunction) {
//    System.out.println("k_0 " + k_0 +  " k = " + k);
    if (k < k_0)
      return distanceFunction.nullDistance();
    return distanceFunction.valueOf("" + Math.exp(getValueAt(k)));
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
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ApproximationLine that = (ApproximationLine) o;

    if (Double.compare(that.m, m) != 0) return false;
    return Double.compare(that.t, t) == 0;
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result;
    long temp;
    temp = m != +0.0d ? Double.doubleToLongBits(m) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = t != +0.0d ? Double.doubleToLongBits(t) : 0L;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return "m = " + m + ", t = " + t + " k_0 " + k_0;
  }
}
