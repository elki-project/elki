package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.mkmax.MkMaxTree;
import de.lmu.ifi.dbs.index.metrical.mtree.mkmax.ReversekNNStatistic;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * MkNNTreeDatabase is a database implementation which is supported by a
 * MkNNTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkNNTreeDatabase<O extends DatabaseObject, D extends Distance<D>> extends MetricalIndexDatabase<O, D> {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<int>k";

  /**
   * Parameter k.
   */
  int k;


  /**
   * Empty constructor, creates a new MDkNNTreeDatabase.
   */
  public MkNNTreeDatabase() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Creates a MkNNTree object for this database.
   */
  public MetricalIndex<O, D> createMetricalIndex() {
    return new MkMaxTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction(), k);
  }

  /**
   * Creates a MkNNTree object for this database.
   *
   * @param objects the objects to be indexed
   */
  public MetricalIndex<O, D> createMetricalIndex(List<O> objects) {
    return new MkMaxTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction(), k, objects);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(MkNNTreeDatabase.class.getName());
    description.append(" holds all the data in a MDkNNTree index structure.\n");
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
    String[] remainingParameters = super.setParameters(args);
    try {
      getDistanceFunction().valueOf(optionHandler.getOptionValue(K_P));
      k = Integer.parseInt(optionHandler.getOptionValue(K_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * Returns the statistic for performed rknn queries.
   *
   * @return the statistic for performed rknn queries
   */
  public ReversekNNStatistic getRkNNStatistics() {
    return ((MkMaxTree<O, D>) index).getRkNNStatistics();
  }

  /**
   * Clears the values of the statistic for performed rknn queries
   */
  public void clearRkNNStatistics() {
    ((MkMaxTree<O, D>) index).clearRkNNStatistics();
  }


}
