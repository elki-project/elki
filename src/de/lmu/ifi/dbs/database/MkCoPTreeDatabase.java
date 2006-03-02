package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.mkcop.MkCoPTree;
import de.lmu.ifi.dbs.index.metrical.mtree.mkcop.RkNNStatistic;

import java.util.List;

/**
 * MkCoPTreeDatabase is a database implementation which is supported by a
 * MkCoPTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkCoPTreeDatabase<O extends DatabaseObject, D extends NumberDistance<D>> extends MkMaxTreeDatabase<O, D> {

  /**
   * Empty constructor, creates a new MDkNNTreeDatabase.
   */
  public MkCoPTreeDatabase() {
    super();
  }

  /**
   * Creates a MkCoPTree object for this database.
   */
  public MetricalIndex<O, D> createMetricalIndex() {
    return new MkCoPTree<O,D>(fileName, pageSize, cacheSize, getDistanceFunction(), k);
  }

  /**
   * Creates a MkCoPTree object for this database.
   *
   * @param objects the objects to be indexed
   */
  public MetricalIndex<O, D> createMetricalIndex(List<O> objects) {
    return new MkCoPTree<O,D>(fileName, pageSize, cacheSize, getDistanceFunction(), k, objects);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(MkCoPTreeDatabase.class.getName());
    description.append(" holds all the data in an MkCoPTree-Tree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * Returns the statistic for performed rknn queries.
   *
   * @return the statistic for performed rknn queries
   */
  public RkNNStatistic getRkNNStatistics() {
    return ((MkCoPTree<O,D>) index).getRkNNStatistics();
  }

  /**
   * Clears the values of the statistic for performed rknn queries
   */
  public void clearRkNNStatistics() {
    ((MkCoPTree<O,D>) index).clearRkNNStatistics();
  }

}
