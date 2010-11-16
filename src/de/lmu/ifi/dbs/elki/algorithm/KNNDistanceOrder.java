package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// TODO: redundant to kNN outlier detection?
@Title("KNN-Distance-Order")
@Description("Assesses the knn distances for a specified k and orders them.")
public class KNNDistanceOrder<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, KNNDistanceOrderResult<D>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNDistanceOrder.class);

  /**
   * Parameter to specify the distance of the k-distant object to be assessed,
   * must be an integer greater than 0.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knndistanceorder.k", "Specifies the distance of the k-distant object to be assessed.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Parameter to specify the average percentage of distances randomly chosen to
   * be provided in the result, must be a double greater than 0 and less than or
   * equal to 1.
   */
  public static final OptionID PERCENTAGE_ID = OptionID.getOrCreateOptionID("knndistanceorder.percentage", "The average percentage of distances randomly choosen to be provided in the result.");

  /**
   * Holds the value of {@link #PERCENTAGE_ID}.
   */
  private double percentage;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k k Parameter
   * @param percentage percentage parameter
   */
  public KNNDistanceOrder(DistanceFunction<O, D> distanceFunction, int k, double percentage) {
    super(distanceFunction);
    this.k = k;
    this.percentage = percentage;
  }

  /**
   * Provides an order of the kNN-distances for all objects within the specified
   * database.
   */
  @Override
  protected KNNDistanceOrderResult<D> runInTime(Database<O> database) throws IllegalStateException {
    KNNQuery.Instance<O, D> knnQuery = database.getKNNQuery(this.getDistanceFunction(), k);
    final Random random = new Random();
    List<D> knnDistances = new ArrayList<D>();
    for(Iterator<DBID> iter = database.iterator(); iter.hasNext();) {
      DBID id = iter.next();
      if(random.nextDouble() < percentage) {
        // FIXME: what if less than k objects returned?
        knnDistances.add(knnQuery.getForDBID(id).get(k - 1).getDistance());
      }
    }
    Collections.sort(knnDistances, Collections.reverseOrder());
    return new KNNDistanceOrderResult<D>("kNN distance order", "knn-order", knnDistances);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> KNNDistanceOrder<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);

    double percentage = getParameterPercentage(config);

    if(config.hasErrors()) {
      return null;
    }
    return new KNNDistanceOrder<O, D>(distanceFunction, k, percentage);
  }

  /**
   * Get the percentage parameter.
   * 
   * @param config Parameterization
   * @return percentage parameter
   */
  private static double getParameterPercentage(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(PERCENTAGE_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE), 1.0);
    if(config.grab(param)) {
      return param.getValue();
    }
    return Double.NaN;
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(0), 1);
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }
}