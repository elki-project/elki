package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mkapp;

import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an MkCop-Tree node.
 * Additionally to an entry in an M-Tree conservative approximation of the
 * knn distances is provided.
 *
 * @author Elke Achtert
 */

interface MkAppEntry<D extends NumberDistance<D,N>, N extends Number> extends MTreeEntry<D> {

  /**
   * Returns the approximated value at the specified k.
   *
   * @param k the parameter k of the knn distance
   * @return the approximated value at the specified k
   */
  public double approximatedValueAt(int k);

  /**
   * Returns the polynomial approximation.
   *
   * @return the polynomial approximation
   */
  public PolynomialApproximation getKnnDistanceApproximation();

  /**
   * Sets the polynomial approximation.
   *
   * @param approximation the polynomial approximation to be set
   */
  public void setKnnDistanceApproximation(PolynomialApproximation approximation);
}
