package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.utilities.Util;

import java.io.Serializable;

/**
 * Represents an entry in a LOF-Table.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFEntry implements Serializable {
  /**
   * The sum of the reachability distances between o and its neighbors.
   */
  private double sum1;

  /**
   * For each neighbor p of o: The sum of the reachability distances
   * between p and its neighbors.
   */
  private final double[] sum2Array;

  /**
   * Creates a new entry in a lof table.
   *
   * @param sum1      the sum of the reachability distances between o and its neighbors
   * @param sum2Array for each neighbor p of o: the sum of the reachability distances
   *                  between p and its neighbors
   */
  public LOFEntry(double sum1, double[] sum2Array) {
    this.sum1 = sum1;
    this.sum2Array = sum2Array;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append(sum1);
    for (int i = 0; i < this.sum2Array.length; i++) {
      if (i < this.sum2Array.length - 1)
        result.append(" ").append(this.sum2Array[i]);
    }
    return result.toString();
  }

  /**
   * Returns the sum of the reachability distances between o and its neighbors.
   *
   * @return the sum of the reachability distances between o and its neighbors
   */
  public double getSum1() {
    return sum1;
  }

  /**
   * Sets the sum of the reachability distances between o and its neighbors.
   *
   * @param sum1 the value to be set
   */
  public void setSum1(double sum1) {
    this.sum1 = sum1;
  }

  /**
   * Returns the ith sum2, which is for the ith neighbor p of o
   * the sum of the reachability distances between p and its neighbors
   *
   * @param i the index of the neighbor p
   * @return the ith sum2
   */
  public double getSum2(int i) {
    return sum2Array[i];
  }

  /**
   * Returns the sum2 array, which is for each neighbor p of o
   * the sum of the reachability distances between p and its neighbors.
   *
   * @return sum2 array
   */
  public double[] getSum2Array() {
    return sum2Array;
  }

  /**
   * Returns the local outlier factor
   *
   * @return the local outlier factor
   */
  public double getLOF() {
    double sum_2 = 0.0;
    for (double s2 : sum2Array) {
      sum_2 += 1 / s2;
    }

    return 1 / ((double) sum2Array.length) * sum1 * sum_2;
  }

   /**
   * Inserts the given sum2 value at the specified index in the sum2Array.
    * All elements starting at index are shifted one position right,
    * the (former) last element will be removed.
   * @param index the index in the sum2Array to insert the value in
   * @param sum2  the value to be inserted
   */
  public void insertAndMoveSum2(int index, double sum2) {
    for (int i = index + 1; i < sum2Array.length; i++) {
      sum2Array[i] = sum2Array[i - 1];
    }
    sum2Array[index] = sum2;
  }
}
