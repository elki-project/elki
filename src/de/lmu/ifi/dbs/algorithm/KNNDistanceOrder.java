package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 *
 * @author Arthur Zimek
 */
public class KNNDistanceOrder<O extends DatabaseObject, D extends Distance<D>>
    extends DistanceBasedAlgorithm<O, D> {
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
  public static final String K_D = "the distance of the k-distant object is assessed. k >= 1 (default: "
                                   + DEFAULT_K + ")";

  /**
   * Parameter percentage.
   */
  public static final String PERCENTAGE_P = "percentage";

  /**
   * Default value for percentage.
   */
  public static final double DEFAULT_PERCENTAGE = 1;

  /**
   * Description for parameter percentage.
   */
  public static final String PERCENTAGE_D = "average percentage p, 0 < p <= 1, of distances randomly choosen to be provided in the result (default: "
                                            + DEFAULT_PERCENTAGE + ")";

  /**
   * Holds the parameter k.
   */
  private int k;

  /**
   * Holds the parameter percentage.
   */
  private double percentage;

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
    // parameter k
    IntParameter k = new IntParameter(K_P, K_D, new GreaterConstraint(0));
    k.setDefaultValue(DEFAULT_K);
    optionHandler.put(K_P, k);

    //parameter percentage
    ArrayList<ParameterConstraint<Number>> percentageCons = new ArrayList<ParameterConstraint<Number>>();
    percentageCons.add(new GreaterConstraint(0));
    percentageCons.add(new LessEqualConstraint(1));
    DoubleParameter per = new DoubleParameter(PERCENTAGE_P, PERCENTAGE_D, percentageCons);
    per.setDefaultValue(DEFAULT_PERCENTAGE);
    optionHandler.put(PERCENTAGE_P, per);
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
        knnDistances.add((database.kNNQueryForID(id, k, this
            .getDistanceFunction())).get(k - 1).getDistance());
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
   * Sets the parameter value for parameter k, if specified, additionally to
   * the parameter settings of super classes. Otherwise the default value for
   * k is used.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    //parameter k
    k = (Integer) optionHandler.getOptionValue(K_P);

    //parameter percentage
    percentage = (Double) optionHandler.getOptionValue(PERCENTAGE_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        KNNDistanceOrder.class.getName(),
        "KNN-Distance-Order",
        "Assesses the knn distances for a specified k and orders them.",
        "");
  }

}
