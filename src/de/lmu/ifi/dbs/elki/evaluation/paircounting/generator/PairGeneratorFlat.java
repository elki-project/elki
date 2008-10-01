/**
 * 
 */
package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.IntIntPair;

/**
 * Generate {@link IntIntPair}s for a flat cluster.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public class PairGeneratorFlat extends PairSortedGenerator {
  /**
   * Set of ids to use
   */
  private int[] ids;
  /**
   * First position
   */
  private int pos1;
  /**
   * Second position
   */
  private int pos2;
  
  /**
   * Generate all pairs (a,b) from a set of integers.
   * 
   * Note: this will <em>not</em> copy ids, and it will sort this array.
   * If you want to work on a copy, pass a copy.
   * 
   * @param ids
   */
  public PairGeneratorFlat(int[] ids) {
    this.ids = ids;
    Arrays.sort(this.ids);
    
    pos1 = 0; pos2 = 0;
    if (ids.length > 0)
      setCurrent(new IntIntPair(ids[pos1], ids[pos2]));
  }
  
  /**
   * Advance iterator.
   */
  protected IntIntPair advance() {
    if (current() == null) return null;
    pos2++;
    if (pos2 >= ids.length) {
      pos2 = 0; pos1++;
    }
    if (pos1 >= ids.length)
      return null;
    else
      return new IntIntPair(ids[pos1],ids[pos2]);
  }
}