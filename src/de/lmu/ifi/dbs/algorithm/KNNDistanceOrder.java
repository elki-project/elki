package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KNNDistanceOrder<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Default value for k.
   */
  public static final int DEFAULT_K = 1;

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>the distance of the k-distant object is assessed. k >= 1 (default: " + DEFAULT_K + ")";

  /**
   * Parameter percentage.
   */
  public static final String PERCENTAGE_P = "percent";

  /**
   * Default value for percentage.
   */
  public static final double DEFAULT_PERCENTAGE = 1;

  /**
   * Description for parameter percentage.
   */
  public static final String PERCENTAGE_D = "<double>average percentage p, 0 < p <= 1, of distances randomly choosen to be provided in the result (default: " + DEFAULT_PERCENTAGE + ")";

  /**
   * Holds the parameter k.
   */
  private int k = DEFAULT_K;

  /**
   * Holds the parameter percentage.
   */
  private double percentage = DEFAULT_PERCENTAGE;

  /**
   * Holds the result.
   */
  private KNNDistanceOrderResult<O, D> result;

  /**
   * Provides an algorithm to order the kNN-distances for all objects of the
   * database.
   */
  public KNNDistanceOrder() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    parameterToDescription.put(PERCENTAGE_P + OptionHandler.EXPECTS_VALUE, PERCENTAGE_D);
    optionHandler = new OptionHandler(parameterToDescription, KNNDistanceOrder.class.getName());
  }

  /**
   * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
   */
  protected
  @Override
  void runInTime(Database<O> database) throws IllegalStateException {
    Random random = new Random();
    List<D> knnDistances = new ArrayList<D>();
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if (random.nextDouble() < percentage) {
        knnDistances.add((database.kNNQueryForID(id, k, this.getDistanceFunction())).get(k - 1).getDistance());
      }
    }
    Collections.sort(knnDistances, Collections.reverseOrder());
    result = new KNNDistanceOrderResult<O, D>(database, knnDistances);
  }

  /**
   * @see Algorithm#getResult()
   */
  public Result<O> getResult() {
    return result;
  }

  /**
   * Adds the value of k to the attribute settings as provided by the super
   * class.
   *
   * @see Algorithm#getAttributeSettings()
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(K_P, Integer.toString(k));
    mySettings.addSetting(PERCENTAGE_P, Double.toString(percentage));

    return attributeSettings;
  }

  /**
   * Sets the parameter value for parameter k, if specified, additionally to
   * the parameter settings of super classes. Otherwise the default value for
   * k is used.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    if (optionHandler.isSet(K_P)) {
      String kString = optionHandler.getOptionValue(K_P);
      try {
        k = Integer.parseInt(kString);
        if (k <= 0) {
          throw new WrongParameterValueException(K_P, kString, K_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(K_P, kString, K_D, e);
      }
    }

    if (optionHandler.isSet(PERCENTAGE_P)) {
      String percentageString = optionHandler.getOptionValue(PERCENTAGE_P);
      try {
        percentage = Double.parseDouble(percentageString);
        if (percentage <= 0 || percentage > 1) {
          throw new WrongParameterValueException(PERCENTAGE_P, percentageString, PERCENTAGE_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(PERCENTAGE_P, percentageString, PERCENTAGE_D, e);
      }
    }
    return remainingParameters;
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(KNNDistanceOrder.class.getName(), "KNN-Distance-Order", "Assesses the knn distances for a specified k and orders them.", "");
  }

}
