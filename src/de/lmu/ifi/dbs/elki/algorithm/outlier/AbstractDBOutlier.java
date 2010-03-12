package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
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
public abstract class AbstractDBOutlier<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, OutlierResult> {
  /**
   * Association ID for DBOD.
   */
  public static final AssociationID<Double> DBOD_SCORE = AssociationID.getOrCreateAssociationID("dbod.score", Double.class);

  /**
   * OptionID for {@link #D_PARAM}
   */
  public static final OptionID D_ID = OptionID.getOrCreateOptionID("dbod.d", "size of the D-neighborhood");

  /**
   * Parameter to specify the size of the D-neighborhood,
   * 
   * <p>
   * Key: {@code -dbod.d}
   * </p>
   */
  private final DistanceParameter<D> D_PARAM = new DistanceParameter<D>(D_ID, getDistanceFunction());

  /**
   * Holds the value of {@link #D_PARAM}.
   */
  private D d;

  /**
   * Constructor, adding options to option handler.
   */
  public AbstractDBOutlier(Parameterization config) {
    super(config);
    // neighborhood size
    if(config.grab(D_PARAM)) {
      d = D_PARAM.getValue();
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    HashMap<Integer, Double> dbodscore = new HashMap<Integer, Double>();
    dbodscore = computeOutlierScores(database, d);

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(DBOD_SCORE, dbodscore);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(dbodscore, true);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult, orderingResult);

    return result;
  }

  /**
   * computes an outlier score for each object of the database.
   */
  protected abstract HashMap<Integer, Double> computeOutlierScores(Database<O> database, D d);
}