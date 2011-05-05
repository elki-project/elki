package de.lmu.ifi.dbs.elki.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.NumberFormat;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * HyperBoundingBox represents a hyperrectangle in the multidimensional space.
 * 
 * @author Elke Achtert
 */
public class HyperBoundingBox implements SpatialComparable, Externalizable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1;

  /**
   * The coordinates of the 'lower left' (= minimum) hyper point.
   */
  protected double[] min;

  /**
   * The coordinates of the 'upper right' (= maximum) hyper point.
   */
  protected double[] max;

  /**
   * Empty constructor for Externalizable interface.
   */
  public HyperBoundingBox() {
    // nothing to do
  }

  /**
   * Creates a HyperBoundingBox for the given hyper points.
   * 
   * @param min - the coordinates of the minimum hyper point
   * @param max - the coordinates of the maximum hyper point
   */
  public HyperBoundingBox(double[] min, double[] max) {
    if(min.length != max.length) {
      throw new IllegalArgumentException("min/max need same dimensionality");
    }

    this.min = min;
    this.max = max;
  }

  /**
   * Constructor, cloning an existing spatial object.
   * 
   * @param other Object to clone
   */
  public HyperBoundingBox(SpatialComparable other) {
    final int dim = other.getDimensionality();
    this.min = new double[dim];
    this.max = new double[dim];
    for(int i = 0; i < dim; i++) {
      this.min[i] = other.getMin(i);
      this.max[i] = other.getMax(i);
    }
  }

  /**
   * Returns a clone of the minimum hyper point.
   * 
   * @return the minimum hyper point
   * 
   * @deprecated Use SpatialUtil.getMin(mbr)?
   */
  @Deprecated
  public double[] getMin() {
    return min.clone();
  }

  /**
   * Returns the coordinate at the specified dimension of the minimum hyper
   * point
   * 
   * @param dimension the dimension for which the coordinate should be returned,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @return the coordinate at the specified dimension of the minimum hyper
   *         point
   */
  @Override
  public double getMin(int dimension) {
    return min[dimension - 1];
  }

  /**
   * Returns a clone of the maximum hyper point.
   * 
   * @return the maximum hyper point
   * 
   * @deprecated Use SpatialUtil.getMin(mbr)?
   */
  @Deprecated
  public double[] getMax() {
    return max.clone();
  }

  /**
   * Returns the coordinate at the specified dimension of the maximum hyper
   * point
   * 
   * @param dimension the dimension for which the coordinate should be returned,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @return the coordinate at the specified dimension of the maximum hyper
   *         point
   */
  @Override
  public double getMax(int dimension) {
    return max[dimension - 1];
  }

  /**
   * Returns the dimensionality of this HyperBoundingBox.
   * 
   * @return the dimensionality of this HyperBoundingBox
   */
  @Override
  public int getDimensionality() {
    return min.length;
  }

  /**
   * Returns a String representation of the HyperBoundingBox.
   * 
   * @return a string representation of this hyper bounding box
   */
  @Override
  public String toString() {
    return "[Min(" + FormatUtil.format(min, ",", 10) + "), Max(" + FormatUtil.format(max, ",", 10) + ")]";
  }

  /**
   * Returns a String representation of the HyperBoundingBox.
   * 
   * @param nf number format for output accuracy
   * @param pre the prefix of each line
   * @return a string representation of this hyper bounding box
   */
  public String toString(String pre, NumberFormat nf) {
    return pre + "[Min(" + FormatUtil.format(min, ",", nf) + "), Max(" + FormatUtil.format(max, ",", nf) + ")]";
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
   * The object implements the writeExternal method to save its contents by
   * calling the methods of DataOutput for its primitive values or calling the
   * writeObject method of ObjectOutput for objects, strings, and arrays.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe the data
   *             layout of this Externalizable object. List the sequence of
   *             element types and, if possible, relate the element to a
   *             public/protected field and/or method of this Externalizable
   *             class.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    int dim = getDimensionality();
    out.writeInt(dim);

    for(double aMin : min) {
      out.writeDouble(aMin);
    }

    for(double aMax : max) {
      out.writeDouble(aMax);
    }
  }

  /**
   * The object implements the readExternal method to restore its contents by
   * calling the methods of DataInput for primitive types and readObject for
   * objects, strings and arrays. The readExternal method must read the values
   * in the same sequence and with the same types as were written by
   * writeExternal.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int dim = in.readInt();
    min = new double[dim];
    max = new double[dim];

    for(int i = 0; i < min.length; i++) {
      min[i] = in.readDouble();
    }

    for(int i = 0; i < max.length; i++) {
      max[i] = in.readDouble();
    }
  }
}