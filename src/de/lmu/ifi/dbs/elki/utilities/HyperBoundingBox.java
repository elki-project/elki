package de.lmu.ifi.dbs.elki.utilities;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.text.NumberFormat;

/**
 * HyperBoundingBox represents a hyperrectangle in the multidimensional space.
 *
 * @author Elke Achtert
 */
public class HyperBoundingBox extends AbstractLoggable implements Externalizable {
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
  public HyperBoundingBox() {
    super(LoggingConfiguration.DEBUG);
  }

  /**
   * Creates a HyperBoundingBox for the given hyper points.
   *
   * @param min - the coordinates of the minimum hyper point
   * @param max - the coordinates of the maximum hyper point
   */
  public HyperBoundingBox(double[] min, double[] max) {
    super(LoggingConfiguration.DEBUG);
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
   * Returns the dimensionality of this HyperBoundingBox.
   *
   * @return the dimensionality of this HyperBoundingBox
   */
  public int getDimensionality() {
    return min.length;
  }

  /**
   * Returns true if this HyperBoundingBox and the given HyperBoundingBox intersect, false otherwise.
   *
   * @param box the HyperBoundingBox to be tested for intersection
   * @return true if this HyperBoundingBox and the given HyperBoundingBox intersect, false otherwise
   */
  public boolean intersects(HyperBoundingBox box) {
    if (this.getDimensionality() != box.getDimensionality())
      throw new IllegalArgumentException("This HyperBoundingBox and the given HyperBoundingBox need same dimensionality");

    boolean intersect = true;
    for (int i = 0; i < min.length; i++) {
      if (this.min[i] > box.max[i] || this.max[i] < box.min[i]) {
        intersect = false;
        break;
      }
    }
    return intersect;
  }

  /**
   * Returns true if this HyperBoundingBox contains the given HyperBoundingBox, false otherwise.
   *
   * @param box the HyperBoundingBox to be tested for containment
   * @return true if this HyperBoundingBox contains the given HyperBoundingBox, false otherwise
   */
  public boolean contains(HyperBoundingBox box) {
    if (this.getDimensionality() != box.getDimensionality())
      throw new IllegalArgumentException("This HyperBoundingBox and the given HyperBoundingBox need same dimensionality");

    boolean contains = true;
    for (int i = 0; i < min.length; i++) {
      if (this.min[i] > box.min[i] || this.max[i] < box.max[i]) {
        contains = false;
        break;
      }
    }
    return contains;
  }

  /**
   * Returns true if this HyperBoundingBox contains the given point, false otherwise.
   *
   * @param point the point to be tested for containment
   * @return true if this HyperBoundingBox contains the given point, false otherwise
   */
  public boolean contains(double[] point) {
    if (this.getDimensionality() != point.length)
      throw new IllegalArgumentException("This HyperBoundingBox and the given point need same dimensionality");

    boolean contains = true;
    for (int i = 0; i < min.length; i++) {
      if (this.min[i] > point[i] || this.max[i] < point[i]) {
        contains = false;
        break;
      }
    }
    return contains;
  }

  /**
   * Computes the volume of this HyperBoundingBox
   *
   * @return the volume of this HyperBoundingBox
   */
  public double volume() {
    double vol = 1;
    for (int i = 0; i < min.length; i++) {
      vol *= max[i] - min[i];
    }
    return vol;
  }

  /**
   * Computes the perimeter of this HyperBoundingBox.
   *
   * @return the perimeter of this HyperBoundingBox
   */
  public double perimeter() {
    double perimeter = 0;
    for (int i = 0; i < min.length; i++) {
      perimeter += max[i] - min[i];
    }
    return perimeter;
  }

  /**
   * Computes the volume of the overlapping box between this HyperBoundingBox
   * and the given HyperBoundingBox
   * and return the relation between the volume of the overlapping box and the volume
   * of both HyperBoundingBoxes.
   *
   * @param box the HyperBoundingBox for which the intersection volume with this HyperBoundingBox should be computed
   * @return the relation between the volume of the overlapping box
   *         and the volume of this HyperBoundingBox
   *         and the given HyperBoundingBox
   */
  public double overlap(HyperBoundingBox box) {
    if (this.getDimensionality() != box.getDimensionality())
      throw new IllegalArgumentException("This HyperBoundingBox and the given HyperBoundingBox need same dimensionality");

    // the maximal and minimal value of the overlap box.
    double omax, omin;

    // the overlap volume
    double overlap = 1.0;

    for (int i = 0; i < min.length; i++) {
      // The maximal value of that overlap box in the current
      // dimension is the minimum of the max values.
      omax = Math.min(max[i], box.max[i]);
      // The minimal value is the maximum of the min values.
      omin = Math.max(min[i], box.min[i]);

      // if omax <= omin in any dimension, the overlap box has a volume of zero
      if (omax <= omin) {
        return 0.0;
      }

      overlap *= omax - omin;
    }

    return overlap / (volume() + box.volume());
  }

  /**
   * Computes the union HyperBoundingBox of this HyperBoundingBox and the given HyperBoundingBox.
   *
   * @param box the HyperBoundingBox to be united with this HyperBoundingBox
   * @return the union HyperBoundingBox of this HyperBoundingBox and the given HyperBoundingBox
   */
  public HyperBoundingBox union(HyperBoundingBox box) {
    if (this.getDimensionality() != box.getDimensionality())
      throw new IllegalArgumentException("This HyperBoundingBox and the given HyperBoundingBox need same dimensionality");

    double[] min = new double[this.min.length];
    double[] max = new double[this.max.length];

    for (int i = 0; i < this.min.length; i++) {
      min[i] = Math.min(this.min[i], box.min[i]);
      max[i] = Math.max(this.max[i], box.max[i]);
    }
    return new HyperBoundingBox(min, max);
  }

  /**
   * Returns the centroid of this HyperBoundingBox.
   *
   * @return the centroid of this HyperBoundingBox
   */
  public double[] centroid() {
    double[] centroid = new double[getDimensionality()];
    for (int d = 0; d < getDimensionality(); d++) {
      centroid[d] = (max[d] + min[d]) / 2.0;
    }
    return centroid;
  }

  /**
   * Returns the centroid of the specified values of this HyperBoundingBox.
   *
   * @param start the start dimension to be considered
   * @param end   the end dimension to be considered
   * @return the centroid of the specified values of this HyperBoundingBox
   */
  public double[] centroid(int start, int end) {
    double[] centroid = new double[end - start + 1];
    for (int d = start - 1; d < end; d++) {
      centroid[d - start + 1] = (max[d] + min[d]) / 2.0;
    }
    return centroid;
  }

  /**
   * Returns a String representation of the HyperBoundingBox.
   *
   * @return a string representation of this hyper bounding box
   */
  @Override
  public String toString() {
    return "[Min(" + Util.format(min, ",", 10) + "), Max(" + Util.format(max, ",", 10) + ")]";
  }

  /**
   * Returns a String representation of the HyperBoundingBox.
   *
   * @param nf  number format for output accuracy
   * @param pre the prefix of each line
   * @return a string representation of this hyper bounding box
   */
  public String toString(String pre, NumberFormat nf) {
    return pre + "[Min(" + Util.format(min, ",", nf) + "), Max(" + Util.format(max, ",", nf) + ")]";
  }

  /**
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    HyperBoundingBox box = (HyperBoundingBox) obj;
    return Arrays.equals(min, box.min) && Arrays.equals(max, box.max);
  }

  /**
   * @see Object#hashCode()
   */
  @Override
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

    for (int i = 0; i < min.length; i++) {
      min[i] = in.readDouble();
    }

    for (int i = 0; i < max.length; i++) {
      max[i] = in.readDouble();
    }
  }
}
