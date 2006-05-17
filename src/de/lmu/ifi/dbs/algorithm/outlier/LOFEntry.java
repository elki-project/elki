package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DoubleDistance;

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
  private final DoubleDistance sum1;

  /**
   * For each neighbor p of o: The sum of the reachability distances
   * between p and its neighbors.
   */
  private final DoubleDistance[] sum2Array;

  /**
   * The number of k nearest neigbors to be considerd.
   */
  private final int k;

  /**
   * Creates a new entry in a lof table.
   *
   * @param sum1      the sum of the reachability distances between o and its neighbors
   * @param sum2Array for each neighbor p of o: the sum of the reachability distances
   *                  between p and its neighbors
   * @param k         the number of k nearest neigbors to be considerd
   */
  public LOFEntry(DoubleDistance sum1, DoubleDistance[] sum2Array, int k) {
    this.sum1 = sum1;
    this.sum2Array = sum2Array;
    this.k = k;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer sum2 = new StringBuffer();
    sum2.append("[");
    for (int i = 0; i < this.sum2Array.length; i++) {
      if (i < this.sum2Array.length - 1)
        sum2.append(this.sum2Array[i]).append(", ");
      else
        sum2.append(this.sum2Array[i]).append("] ");
    }
    return "(" + sum1 + ", " + sum2 + ")";
  }

  /**
   * Returns the sum of the reachability distances between o and its neighbors.
   *
   * @return the sum of the reachability distances between o and its neighbors
   */
  public Distance getSum1() {
    return sum1;
  }

  /**
   * Returns the ith sum2, which is for the ith neighbor p of o
   * the sum of the reachability distances between p and its neighbors
   *
   * @param i the index of the neighbor p
   * @return the ith sum2
   */
  public Distance getSum2(int i) {
    return sum2Array[i];
  }

  /**
   * Returns the sum2 array, which is for each neighbor p of o
   * the sum of the reachability distances between p and its neighbors.
   *
   * @return sum2 array
   */
  public Distance[] getSum2Array() {
    return sum2Array;
  }

  /**
   * Returns the local outlier factor
   *
   * @return the local outlier factor
   */
  public double getLOF() {
    double sum_1 = sum1.getDoubleValue();
    double sum_2 = 0;
    for (DoubleDistance s2 : sum2Array) {
      sum_2 += 1 / s2.getDoubleValue();
    }
    return 1 / k * sum_1 * sum_2;
  }
}
