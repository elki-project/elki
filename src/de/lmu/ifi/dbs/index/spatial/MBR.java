package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.utilities.Util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * MBR represents a minmum bounding rectangle in the multidimensional space.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MBR implements Externalizable {
  /**
   * The coordinates of the 'lower left' (= minimum) hyper point.
   */
  private double[] min;

  /**
   * The coordinates of the 'upper right' (= maximum) hyper point.
   */
  private double[] max;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MBR() {
  }

  /**
   * Creates a MBR for the given hyper points.
   *
   * @param min - the coordinates of the minimum hyper point
   * @param max - the coordinates of the maximum hyper point
   */
  public MBR(double[] min, double[] max) {
    if (min.length != max.length)
      throw new IllegalArgumentException("min/max need same dimensionality");

    this.min = min;
    this.max = max;
  }

  /**
   * Returns a clone of the minimum hyper point.
   *
   * @return the minimum hyper point
   */
  public double[] getMin() {
    return min.clone();
  }

  /**
   * Returns the coordinate at the specified dimension of the minimum hyper point
   *
   * @param dimension the dimension for which the coordinate should be returned,
   *                  where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @return the coordinate at the specified dimension of the minimum hyper point
   */
  public double getMin(int dimension) {
    return min[dimension - 1];
  }

  /**
   * Returns a clone of the maximum hyper point.
   *
   * @return the maximum hyper point
   */
  public double[] getMax() {
    return max.clone();
  }

  /**
   * Returns the coordinate at the specified dimension of the maximum hyper point
   *
   * @param dimension the dimension for which the coordinate should be returned,
   *                  where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @return the coordinate at the specified dimension of the maximum hyper point
   */
  public double getMax(int dimension) {
    return max[dimension - 1];
  }

  /**
   * Returns the dimensionality of this MBR.
   *
   * @return the dimensionality of this MBR
   */
  public int getDimensionality() {
    return min.length;
  }

  /**
   * Returns true if this MBR and the given MBR intersect, false otherwise.
   *
   * @param mbr the MBR to be tested for intersection
   * @return true if this MBR and the given MBR intersect, false otherwise
   */
  public boolean intersects(MBR mbr) {
    if (this.getDimensionality() != mbr.getDimensionality())
      throw new IllegalArgumentException("This MBR and the given MBR need same dimensionality");

    boolean intersect = true;
    for (int i = 0; i < min.length; i++) {
      if (this.min[i] > mbr.max[i] || this.max[i] < mbr.min[i]) {
        intersect = false;
        break;
      }
    }
    return intersect;
  }

  /**
   * Retuns true if this MBR contains the given MBR, false otherwise.
   *
   * @param mbr the MBR to be tested for containment
   * @return true if this MBR contains the given MBR, false otherwise
   */
  public boolean contains(MBR mbr) {
    if (this.getDimensionality() != mbr.getDimensionality())
      throw new IllegalArgumentException("This MBR and the given MBR need same dimensionality");

    boolean contains = true;
    for (int i = 0; i < min.length; i++) {
      if (this.min[i] > mbr.min[i] || this.max[i] < mbr.max[i]) {
        contains = false;
        break;
      }
    }
    return contains;
  }

  /**
   * Computes the volume of this MBR.
   *
   * @return the volume of this MBR
   */
  public double volume() {
    double vol = 1;
    for (int i = 0; i < min.length; i++) {
      vol *= max[i] - min[i];
    }
    return vol;
  }

  /**
   * Computes the perimeter of this MBR.
   *
   * @return the perimeter of this MBR
   */
  public double perimeter() {
    double perimeter = 0;
    for (int i = 0; i < min.length; i++) {
      perimeter += max[i] - min[i];
    }
    return perimeter;
  }

  /**
   * Computes the volume of the overlapping box between this MBR and the given MBR
   * and return the relation between the volume of the overlapping box and the volume of both MBRs.
   *
   * @param mbr the MBR for which the intersection volume with this MBR should be computed
   * @return the relation between the volume of the overlapping box and the volume of this MBR
   *         and the given MBR
   */
  public double overlap(MBR mbr) {
    if (this.getDimensionality() != mbr.getDimensionality())
      throw new IllegalArgumentException("This MBR and the given MBR need same dimensionality");

    // the maximal and minimal value of the overlap box.
    double omax, omin;

    // the overlap volume
    double overlap = 1.0;

    for (int i = 0; i < min.length; i++) {
      // The maximal value of that overlap box in the current
      // dimension is the minimum of the max values.
      omax = Math.min(max[i], mbr.max[i]);
      // The minimal value is the maximum of the min values.
      omin = Math.max(min[i], mbr.min[i]);

      // if omax <= omin in any dimension, the overlap box has a volume of zero
      if (omax <= omin) {
        return 0.0;
      }

      overlap *= omax - omin;
    }

    return overlap / (volume() + mbr.volume());
  }

  /**
   * Computes the union MBR of this MBR and the given MBR.
   *
   * @param mbr the MBR to be united with this MBR
   * @return the union MBR of this MBR and the given MBR
   */
  public MBR union(MBR mbr) {
    if (this.getDimensionality() != mbr.getDimensionality())
      throw new IllegalArgumentException("This MBR and the given MBR need same dimensionality");

    double[] min = new double[this.min.length];
    double[] max = new double[this.max.length];

    for (int i = 0; i < this.min.length; i++) {
      min[i] = Math.min(this.min[i], mbr.min[i]);
      max[i] = Math.max(this.max[i], mbr.max[i]);
    }
    return new MBR(min, max);
  }

  /**
   * Retuns a String representation of the MBR.
   *
   * @return String
   */
  public String toString() {
    return "MBR: Min(" + Util.format(min) + "), Max(" + Util.format(max) + ")";
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object obj) {
    MBR mbr = (MBR) obj;
    return Arrays.equals(min, mbr.min) && Arrays.equals(max, mbr.max);
  }

  /**
   * @see Object#hashCode()
   */
  public int hashCode() {
    return 29 * Arrays.hashCode(min) + Arrays.hashCode(max);
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe
   * the data layout of this Externalizable object.
   * List the sequence of element types and, if possible,
   * relate the element to a public/protected field and/or
   * method of this Externalizable class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    int dim = getDimensionality();
    out.writeInt(dim);

    for (double aMin : min) {
      out.writeDouble(aMin);
    }

    for (double aMax : max) {
      out.writeDouble(aMax);
    }
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int dim = in.readInt();
    min = new double[dim];
    max = new double[dim];

    for (int i=0; i<min.length; i++) {
      min[i] = in.readDouble();
    }

    for (int i=0; i<max.length; i++) {
      max[i] = in.readDouble();
    }
  }
}
