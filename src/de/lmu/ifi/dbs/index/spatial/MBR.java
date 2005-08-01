package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;

/**
 * MBR represents a minmum bounding rectangle in the multidimensional space.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MBR {
  /**
   * The coordinates of the 'lower left' (= minimum) hyper point.
   */
  private double[] min;

  /**
   * The coordinates of the 'upper right' (= maximum) hyper point.
   */
  private double[] max;

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
    return (double[]) min.clone();
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
    return (double[]) max.clone();
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
    double omax = 0.0, omin = 0.0;

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
}
