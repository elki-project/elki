package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Implement the common functionality of caching the current result in a base class.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 */
public abstract class PairSortedGenerator implements PairSortedGeneratorInterface {
  /**
   * Current pair
   */
  private IntIntPair cur = null;

  /**
   * Set the current pair.
   * 
   * @param cur new current pair.
   */
  protected final void setCurrent(IntIntPair cur) {
    this.cur = cur;
  }
  
  /**
   * Return current pair.
   * 
   * Marked as final to avoid bad implementations.
   * If you intend to override this, just implement the interface!
   */
  @Override
  public final IntIntPair current() {
    return cur;
  }

  /**
   * Return next pair.
   * 
   * Marked as final to avoid bad implementations.
   * If you intend to override this, just implement the interface!
   */
  @Override
  public final IntIntPair next() {
    setCurrent(advance());
    return current();
  }
  
  /**
   * Main advance method.
   */
  protected abstract IntIntPair advance();
}