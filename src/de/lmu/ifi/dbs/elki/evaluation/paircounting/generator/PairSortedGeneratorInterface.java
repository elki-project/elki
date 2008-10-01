package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import de.lmu.ifi.dbs.elki.utilities.IntIntPair;

/**
 * Pair generator interface.
 * Implementation must return pairs <em>sorted</em>.
 * 
 * Basically this is an Iterator interface, but it deliberately has a current() method,
 * that is useful e.g. for merging.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public interface PairSortedGeneratorInterface {
  /**
   * Return current pair.
   * 
   * @return current pair, null if there are no more pairs
   */
  public IntIntPair current();
  /**
   * Return next pair, advance generator
   * 
   * @return next pair, null if there are no more pairs
   */
  public IntIntPair next();
}