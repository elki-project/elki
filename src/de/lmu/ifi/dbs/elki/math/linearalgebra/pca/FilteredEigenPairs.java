package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;

import java.util.List;

/**
 * Encapsulates weak and strong eigenpairs that have been filtered out
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
   * Creates a new object that encapsulates weak and strong eigenpairs
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
   * Returns the weak eigenpairs (no copy).
   * @return the weak eigenpairs
   */
  public List<EigenPair> getWeakEigenPairs() {
    return weakEigenPairs;
  }

  /**
   * Counts the strong eigenpairs.
   * @return number of strong eigenpairs
   */
  public int countWeakEigenPairs() {
    return strongEigenPairs.size();
  }

  /**
   * Returns the strong eigenpairs (no copy).
   * @return the strong eigenpairs
   */
  public List<EigenPair> getStrongEigenPairs() {
    return strongEigenPairs;
  }
  
  /**
   * Counts the strong eigenpairs.
   * @return number of strong eigenpairs
   */
  public int countStrongEigenPairs() {
    return strongEigenPairs.size();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return "weak EP: " + weakEigenPairs + "\nstrong EP: " + strongEigenPairs;
  }
}
