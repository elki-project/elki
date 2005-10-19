package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides a DistanceFunction to compute
 * a Distance between BitVectors based on the number of shared bits.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class FrequencyDependentItemsetDistanceFunction extends SharingDependentItemsetDistanceFunction {
  /**
   * Keeps the frequencies of itemset that have already been encountered.
   */
  private Map<BitSet, Integer> frequencies;

  /**
   * The database to compute itemset frequencies.
   */
  private Database<BitVector> database;

  /**
   * Sets the database, initializes a new map of frequencies.
   *
   * @see SharingDependentItemsetDistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database, boolean)
   */
  @Override
  public void setDatabase(Database<BitVector> database, boolean verbose) {
    frequencies = new Hashtable<BitSet, Integer>();
    this.database = database;
  }


  /**
   * Provides the support (percentage) of the given itemset
   * by the currently set database.
   *
   * @param itemset the itemset to compute the support
   * @return the support (percentage) of the given itemset
   *         by the currently set database
   */
  protected double support(BitSet itemset) {
    Integer freq = frequencies.get(itemset);
    double dbSize = database.size();
    if (freq != null) {
      return freq / dbSize;
    }
    else {
      freq = 0;
      for (Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();) {
        BitVector bv = database.get(dbIter.next());
        if (bv.contains(itemset)) {
          freq++;
        }
      }
      frequencies.put(itemset, freq);
      return freq / dbSize;
    }
  }
}
