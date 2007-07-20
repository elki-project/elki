package de.lmu.ifi.dbs.varianceanalysis;

import java.util.List;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;

/**
 * Encapsulates weak and stromg eigenpairs that have been filtered out
 * by an eigenpair filter.
 *
 * @author Elke Achtert 
 */
public class FilteredEigenPairs {
  /**
   * The weak eigenpairs.
   */
  private final List<EigenPair> weakEigenPairs;

  /**
   * The strong eigenpairs.
   */
  private final List<EigenPair> strongEigenPairs;

  /**
   * Creates a new object that encapsulates weak and stromg eigenpairs
   * that have been filtered out by an eigenpair filter
   *
   * @param weakEigenPairs the weak eigenpairs
   * @param strongEigenPairs the strong eigenpairs
   */
  public FilteredEigenPairs(List<EigenPair> weakEigenPairs, List<EigenPair> strongEigenPairs) {
    this.weakEigenPairs = weakEigenPairs;
    this.strongEigenPairs = strongEigenPairs;
  }

  /**
   * Returns the weak eigenpairs.
   * @return the weak eigenpairs
   */
  public List<EigenPair> getWeakEigenPairs() {
    return weakEigenPairs;
  }

  /**
   * Returns the strong eigenpairs.
   * @return the strong eigenpairs
   */
  public List<EigenPair> getStrongEigenPairs() {
    return strongEigenPairs;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return "weak EP: " + weakEigenPairs + "\nstrong EP: " + strongEigenPairs;
  }
}
