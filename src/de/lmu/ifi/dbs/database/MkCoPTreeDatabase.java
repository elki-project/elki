package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.mcop.MkCoPTree;

import java.util.List;

/**
 * MDkNNTreeDatabase is a database implementation which is supported by a
 * MDkNNTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkCoPTreeDatabase<O extends MetricalObject> extends MkNNTreeDatabase<O, DoubleDistance> {

  /**
   * Empty constructor, creates a new MDkNNTreeDatabase.
   */
  public MkCoPTreeDatabase() {
    super();
  }

  /**
   * Creates a metrical index object for this database.
   */
  public MetricalIndex<O, DoubleDistance> createMetricalIndex() {
    return new MkCoPTree<O>(fileName, pageSize, cacheSize, getDistanceFunction(), k);
  }

  /**
   * Creates a metrical index object for this database.
   *
   * @param objects the objects to be indexed
   */
  public MetricalIndex<O, DoubleDistance> createMetricalIndex(List<O> objects) {
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


}
