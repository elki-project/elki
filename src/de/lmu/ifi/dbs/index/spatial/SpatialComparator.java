package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Comparator;

/**
 * Compares objects of type Entry.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public final class SpatialComparator implements Comparator {
  /**
   * Indicates the comparison of the min values of the entries' MBRs.
   */
  public static final int MIN = 1;

  /**
   * Indicates the comparison of the max values of the entries' MBRs.
   */
  public static final int MAX = 2;

  /**
   * The dimension for comparison.
   */
  private int compareDimension = 0;

  /**
   * Indicates the comparison value of the MBRs.
   */
  private int comparisonValue = -1;


  /**
   * Sets the dimension for comparison.
   *
   * @param dim the dimension to be set
   */
  public void setCompareDimension(final int dim) {
    compareDimension = dim;
  }

  /**
   * Sets the comparison value.
   *
   * @param compValue the comparison value to be set
   */
  public void setComparisonValue(final int compValue) {
    comparisonValue = compValue;
  }

  /**
   * Compares the two specified objects according to
   * the sorting dimension and the comparison value of this Comparator.
   *
   * @param o1 the first entry
   * @param o2 the second entry
   * @return a negative integer, zero, or a positive integer as the
   *         first argument is less than, equal to, or greater than the
   *         second.
   */
  public int compare(final Object o1, final Object o2) {
    if (o1 instanceof Entry && o2 instanceof Entry) {
      return compare(((Entry) o1).getMBR(), ((Entry) o2).getMBR());
    }

    if (o1 instanceof SpatialNode && o2 instanceof SpatialNode) {
      return compare(((SpatialNode) o1).mbr(), ((SpatialNode) o2).mbr());
    }

    if (o1 instanceof RealVector && o2 instanceof RealVector) {
      return compare(Util.unbox(((RealVector) o1).getValues()),
                     Util.unbox(((RealVector) o2).getValues()));
    }

    throw new IllegalArgumentException("Unknown objects!");
  }

  /**
   * Compares the two specified MBRs according to
   * the sorting dimension and the comparison value of this Comparator.
   *
   * @param mbr1 the first MBR
   * @param mbr2 the second MBR
   * @return a negative integer, zero, or a positive integer as the
   *         first argument is less than, equal to, or greater than the
   *         second.
   */
  private int compare(final MBR mbr1, final MBR mbr2) {
    if (comparisonValue == MIN) {
      if (mbr1.getMin(compareDimension) <
          mbr2.getMin(compareDimension))
        return -1;
      if (mbr1.getMin(compareDimension) >
          mbr2.getMin(compareDimension))
        return +1;
    }
    else if (comparisonValue == MAX) {
      if (mbr1.getMax(compareDimension) <
          mbr2.getMax(compareDimension))
        return -1;
      if (mbr1.getMax(compareDimension) >
          mbr2.getMax(compareDimension))
        return +1;
    }
    else
      throw new IllegalArgumentException("No comparison value specified!");

    return 0;
  }

  /**
   * Compares the two specified double arrays according to
   * the sorting dimension and the comparison value of this Comparator.
   *
   * @param values1 the first double array
   * @param values2 the second double array
   * @return a negative integer, zero, or a positive integer as the
   *         first argument is less than, equal to, or greater than the
   *         second.
   */
  private int compare(final double[] values1, final double[] values2) {
    if (values1[compareDimension - 1] <
        values2[compareDimension - 1])
      return -1;

    if (values1[compareDimension - 1] >
        values2[compareDimension - 1])
      return +1;

    return 0;
  }
}

