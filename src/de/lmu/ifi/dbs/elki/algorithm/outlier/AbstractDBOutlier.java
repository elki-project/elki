package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;

/**
 * Simple distance based outlier detection algorithms.
 * 
 * <p>
 * Reference: E.M. Knorr, R. T. Ng: Algorithms for Mining Distance-Based
 * Outliers in Large Datasets, In: Procs Int. Conf. on Very Large Databases
 * (VLDB'98), New York, USA, 1998.
 * 
 * @author Lisa Reichert
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public abstract class AbstractDBOutlier<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> {
  /**
   * Association ID for DBOD.
   */
  public static final AssociationID<Double> DBOD_SCORE = AssociationID.getOrCreateAssociationID("dbod.score", Double.class);

  /**
   * Parameter to specify the size of the D-neighborhood
   */
  public static final OptionID D_ID = OptionID.getOrCreateOptionID("dbod.d", "size of the D-neighborhood");

  /**
   * Holds the value of {@link #D_ID}.
   */
  private D d;

  /**
   * Constructor with actual parameters.
   * 
   * @param distanceFunction distance function to use
   * @param d d value
   */
  public AbstractDBOutlier(DistanceFunction<? super O, D> distanceFunction, D d) {
    super(distanceFunction);
    this.d = d;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);

    DataStore<Double> dbodscore = computeOutlierScores(database, distFunc, d);

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>(DBOD_SCORE, dbodscore);
    OrderingResult orderingResult = new OrderingFromDataStore<Double>(dbodscore, true);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult, orderingResult);

    return result;
  }

  /**
   * computes an outlier score for each object of the database.
   */
  protected abstract DataStore<Double> computeOutlierScores(Database<O> database, DistanceQuery<O, D> distFunc, D d);

  /**
   * Grab the 'd' configuration option.
   * 
   * @param config Parameterization
   * @return d Parameter
   */
  protected static <D extends Distance<D>> D getParameterD(Parameterization config, DistanceFunction<?, D> distanceFunction) {
    final D distanceFactory = (distanceFunction != null) ? distanceFunction.getDistanceFactory() : null;
    final DistanceParameter<D> param = new DistanceParameter<D>(D_ID, distanceFactory);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }
}