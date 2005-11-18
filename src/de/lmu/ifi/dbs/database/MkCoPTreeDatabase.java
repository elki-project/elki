package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.mcop.MkCoPTree;
import de.lmu.ifi.dbs.index.metrical.mtree.mcop.RkNNStatistic;

import java.util.List;

/**
 * MkCoPTreeDatabase is a database implementation which is supported by a
 * MkCoPTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkCoPTreeDatabase<O extends MetricalObject> extends MkNNTreeDatabase<O, NumberDistance> {

  /**
   * Empty constructor, creates a new MDkNNTreeDatabase.
   */
  public MkCoPTreeDatabase() {
    super();
  }

  /**
   * Creates a MkCoPTree object for this database.
   */
  public MetricalIndex<O, NumberDistance> createMetricalIndex() {
    return new MkCoPTree<O>(fileName, pageSize, cacheSize, getDistanceFunction(), k);
  }

  /**
   * Creates a MkCoPTree object for this database.
   *
   * @param objects the objects to be indexed
   */
  public MetricalIndex<O, NumberDistance> createMetricalIndex(List<O> objects) {
    return new MkCoPTree<O>(fileName, pageSize, cacheSize, getDistanceFunction(), k, objects);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(MkCoPTreeDatabase.class.getName());
    description.append(" holds all the data in a MkMax-Tree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * Sets the parameters k to the parameters set by the super-class' method.
   * Parameter k is required.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    return super.setParameters(args);
  }

  /**
   * Returns the statistic for performed rknn queries.
   *
   * @return the statistic for performed rknn queries
   */
  public RkNNStatistic getRkNNStatistics() {
    return ((MkCoPTree<O>) index).getRkNNStatistics();
  }

  /**
   * Clears the values of the statistic for performed rknn queries
   */
  public void clearRkNNStatistics() {
    ((MkCoPTree<O>) index).clearRkNNStatistics();
  }

}
