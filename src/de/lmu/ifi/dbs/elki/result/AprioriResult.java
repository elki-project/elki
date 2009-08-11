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
public class AprioriResult implements Result {
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
   * @param solution
   * @param supports
   */
  public AprioriResult(List<BitSet> solution, Map<BitSet, Integer> supports) {
    super();
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
  
  @Override
  public String getName() {
    return "apriori";
  }
  
  // TODO: text writer for AprioriResult!
}
