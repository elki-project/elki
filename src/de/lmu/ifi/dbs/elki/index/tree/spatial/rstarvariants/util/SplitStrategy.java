package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Generic interface for split strategies.
 * 
 * @author Erich Schubert
 * 
 * @param <B> Base type that can be processed by this strategy
 */
public interface SplitStrategy<B> {
  /**
   * Split a page
   * 
   * @param <E> Actual data type
   * @param entries Entries to split
   * @param minEntries Minimum number of entries in each part
   * @return Pair containing the two sets of objects
   */
  public <E extends B> Pair<List<E>, List<E>> split(List<E> entries, int minEntries);
}
