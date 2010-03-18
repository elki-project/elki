package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
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
@Title("KNN-Distance-Order")
@Description("Assesses the knn distances for a specified k and orders them.")
public class KNNDistanceOrder<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, KNNDistanceOrderResult<D>> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knndistanceorder.k", "Specifies the distance of the k-distant object to be assessed.");

  /**
   * Parameter to specify the distance of the k-distant object to be assessed,
   * must be an integer greater than 0.
   * <p>
   * Default value: {@code 1}
   * </p>
   * <p>
   * Key: {@code -knndistanceorder.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), 1);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * OptionID for {@link #PERCENTAGE_PARAM}
   */
  public static final OptionID PERCENTAGE_ID = OptionID.getOrCreateOptionID("knndistanceorder.percentage", "The average percentage of distances randomly choosen to be provided in the result.");

  /**
   * Parameter to specify the average percentage of distances randomly choosen
   * to be provided in the result, must be a double greater than 0 and less than
   * or equal to 1.
   * <p>
   * Default value: {@code 1.0}
   * </p>
   * <p>
   * Key: {@code -knndistanceorder.percentage}
   * </p>
   */
  public final DoubleParameter PERCENTAGE_PARAM = new DoubleParameter(PERCENTAGE_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE), 1.0);

  /**
   * Holds the value of {@link #PERCENTAGE_PARAM}.
   */
  private double percentage;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public KNNDistanceOrder(Parameterization config) {
    super(config);
    // parameter k
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // parameter percentage
    if(config.grab(PERCENTAGE_PARAM)) {
      percentage = PERCENTAGE_PARAM.getValue();
    }
  }

  /**
   * Provides an order of the kNN-distances for all objects within the specified
   * database.
   */
  @Override
  protected KNNDistanceOrderResult<D> runInTime(Database<O> database) throws IllegalStateException {
    Random random = new Random();
    List<D> knnDistances = new ArrayList<D>();
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if(random.nextDouble() < percentage) {
        knnDistances.add((database.kNNQueryForID(id, k, this.getDistanceFunction())).get(k - 1).getDistance());
      }
    }
    Collections.sort(knnDistances, Collections.reverseOrder());
    return new KNNDistanceOrderResult<D>(knnDistances);
  }
}
