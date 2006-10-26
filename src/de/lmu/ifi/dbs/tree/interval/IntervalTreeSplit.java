package de.lmu.ifi.dbs.tree.interval;

import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

import java.util.List;
import java.util.Set;

/**
 * Provides necessary methods for splitting a node in a interval tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface IntervalTreeSplit {
  /**
   * Returns the list of ids which would be associated with the
   * subtree representing the specified interval. If the split criterion is not met,
   * null will be returned.
   *
   * @param parentIDs     the ids belonging to the parent subtree of the given interval
   * @param childInterval the interval represented by the new subtree
   * @param childLevel    the level of the new subtree
   * @return the list of ids which would be associated with the
   *         subtree representing the specified interval or null
   */
  Set<Integer> split(Set<Integer> parentIDs, HyperBoundingBox childInterval, int childLevel);
}
