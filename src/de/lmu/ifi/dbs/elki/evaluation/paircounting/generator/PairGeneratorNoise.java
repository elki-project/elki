package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.IntIntPair;

/**
 * Generator for noise points.
 * 
 * This generator will generate pairs (a,a) for all elements a in the given list.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public class PairGeneratorNoise extends PairSortedGenerator {
  /**
   * Ids to use
   */
  private int[] ids;
  /**
   * Current position.
   */
  private int pos;
  
  /**
   * Create new generator for noise pairs.
   * 
   * Note: this will <em>not make a copy of param ids</em>, and ids will be sorted.
   * 
   * @param ids array to use as source.
   */
  public PairGeneratorNoise(int[] ids) {
    this.ids = ids;
    Arrays.sort(this.ids);
    
    pos = 0;
    if (ids.length > 0)
      setCurrent(new IntIntPair(ids[pos], ids[pos]));
  }
  
  /**
   * Advance iterator and return new pair.
   */
  protected IntIntPair advance() {
    if (current() == null) return null;
    pos++;
    if (pos >= ids.length)
      return null;
    else
      return new IntIntPair(ids[pos],ids[pos]);
  }
}