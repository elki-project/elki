package de.lmu.ifi.dbs.index.tree.spatial;

import java.util.Comparator;

/**
 * Compares objects of type SpatialComparable.
 *
 * @author Elke Achtert
 */
public final class SpatialComparator implements Comparator<SpatialComparable> {
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
  private final int compareDimension;

  /**
   * Indicates the comparison value (min or max).
   */
  private final int comparisonValue;

  /**
   * Creates a new spatial comparator with the specified parameters.
   * @param compareDimension the dimension to be set for comparison
   * @param comparisonValue  the comparison value to be set
   */
  public SpatialComparator(int compareDimension, int comparisonValue) {
    this.compareDimension = compareDimension;
    this.comparisonValue = comparisonValue;
  }


  /**
   * Compares the two specified spatial comparables according to
   * the sorting dimension and the comparison value of this Comparator.
   *
   * @param o1 the first spatial comparable
   * @param o2 the second spatial comparable
   * @return a negative integer, zero, or a positive integer as the
   *         first argument is less than, equal to, or greater than the
   *         second.
   */
  public int compare(SpatialComparable o1, SpatialComparable o2) {
    if (comparisonValue == MIN) {
      if (o1.getMin(compareDimension) < o2.getMin(compareDimension))
        return -1;

      if (o1.getMin(compareDimension) > o2.getMin(compareDimension))
        return +1;
    }

    else if (comparisonValue == MAX) {
      if (o1.getMax(compareDimension) < o2.getMax(compareDimension))
        return -1;

      if (o1.getMax(compareDimension) > o2.getMax(compareDimension))
        return +1;
    }

    else
      throw new IllegalArgumentException("No comparison value specified!");

    return 0;
  }
}

