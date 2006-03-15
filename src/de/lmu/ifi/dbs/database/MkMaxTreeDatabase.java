package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.mkmax.MkMaxTree;
import de.lmu.ifi.dbs.index.metrical.mtree.mkmax.ReversekNNStatistic;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * MkMaxTreeDatabase is a database implementation which is supported by a
 * MkMaxTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkMaxTreeDatabase<O extends DatabaseObject, D extends Distance<D>> extends MetricalIndexDatabase<O, D> {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>positive integer specifying the maximal number k of " +
                                   "k nearest neighbors to be supported.";

  /**
   * Parameter k.
   */
  int k;

  /**
   * Empty constructor, creates a new MkMaxTreeDatabase.
   */
  public MkMaxTreeDatabase() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Creates a MkMaxTree object for this database.
   */
  public MetricalIndex<O, D> createMetricalIndex() {
    return new MkMaxTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction(), k);
  }

  /**
   * Creates a MkMaxTree object for this database.
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
    description.append(MkMaxTreeDatabase.class.getName());
    description.append(" holds all the data in a MkMaxTree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * Sets the parameters k to the parameters set by the super-class' method.
   * Parameter k is required.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    try {
      k = Integer.parseInt(optionHandler.getOptionValue(K_P));
      if (k <= 0)
        throw new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D, e);
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(K_P, Integer.toString(k));
    return attributeSettings;
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
