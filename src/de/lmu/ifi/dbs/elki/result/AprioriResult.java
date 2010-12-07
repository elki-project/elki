package de.lmu.ifi.dbs.elki.result;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Result class for Apriori Algorithm.
 * 
 * @author Erich Schubert
 *
 */
public class AprioriResult extends BasicResult {
  /**
   * The frequent itemsets.
   */
  private List<BitSet> solution;

  /**
   * The supports of all itemsets.
   */
  private Map<BitSet, Integer> supports;

  /**
   * Constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param solution Frequent itemsets
   * @param supports Supports for the itemsets
   */
  public AprioriResult(String name, String shortname, List<BitSet> solution, Map<BitSet, Integer> supports) {
    super(name, shortname);
    this.solution = solution;
    this.supports = supports;
  }

  /**
   * Returns the frequent item sets.
   *
   * @return the frequent item sets.
   */
  public List<BitSet> getSolution() {
      return solution;
  }

  /**
   * Returns the frequencies of the frequent item sets.
   *
   * @return the frequencies of the frequent item sets
   */
  public Map<BitSet, Integer> getSupports() {
      return supports;
  }
  
  // TODO: text writer for AprioriResult!
}
