package de.lmu.ifi.dbs.distance;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;

/**
 * Provides a DistanceFunction to compute a Distance between BitVectors based on
 * the number of shared bits.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 *
 * todo implement DatabaseListener
 */
public abstract class FrequencyDependentItemsetDistanceFunction extends
                                                                SharingDependentItemsetDistanceFunction {
  /**
   * Keeps the frequencies of itemset that have already been encountered.
   */
  private Map<BitSet, Integer> frequencies;

  protected FrequencyDependentItemsetDistanceFunction() {
    super();
  }

  /**
   * Sets the database, initializes a new map of frequencies.
   *
   * @see DistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database,
   *      boolean, boolean)
   */
  @Override
  public void setDatabase(Database<BitVector> database, boolean verbose,
                          boolean time) {
    super.setDatabase(database, verbose, time);
    frequencies = new Hashtable<BitSet, Integer>();
  }

  /**
   * Provides the support (percentage) of the given itemset by the currently
   * set database.
   *
   * @param itemset the itemset to compute the support
   * @return the support (percentage) of the given itemset by the currently
   *         set database
   */
  protected double support(BitSet itemset) {
    Integer freq = frequencies.get(itemset);
    double dbSize = getDatabase().size();
    if (freq != null) {
      return freq / dbSize;
    }
    else {
      freq = 0;
      for (Iterator<Integer> dbIter = getDatabase().iterator(); dbIter
      .hasNext();) {
        BitVector bv = getDatabase().get(dbIter.next());
        if (bv.contains(itemset)) {
          freq++;
        }
      }
      frequencies.put(itemset, freq);
      return freq / dbSize;
    }
  }
}
