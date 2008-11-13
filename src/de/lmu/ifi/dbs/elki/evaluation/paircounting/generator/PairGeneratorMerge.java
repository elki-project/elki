package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Merge the output of multiple generators.
 * 
 * @author Erich Schubert
 */
public class PairGeneratorMerge extends PairSortedGenerator {
  /**
   * Generators to merge
   */
  private PairSortedGeneratorInterface[] generators;
  
  /**
   * Set up merging generator.
   * param generators will not be copied!
   * 
   * @param generators array of generators.
   */
  public PairGeneratorMerge(PairSortedGeneratorInterface[] generators) {
    this.generators = generators;
    setCurrent(advance());
  }
  
  /**
   * Advance iterator and return next pair.
   * 
   * This will return the smallest of all the "merged" generator results. 
   */
  @Override
  protected IntIntPair advance() {
    IntIntPair min = null;
    PairSortedGeneratorInterface best = null;
    for (PairSortedGeneratorInterface gen : this.generators) {
      IntIntPair n = gen.current();
      if (n != null && (min == null || n.compareTo(min) < 0)) {
        min = n;
        best = gen;
      }
    }
    // advance best generator
    if (best != null) {
      best.next();
    }
    return min;
  }
}