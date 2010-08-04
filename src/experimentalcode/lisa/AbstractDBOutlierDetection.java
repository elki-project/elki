package experimentalcode.lisa;


import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
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
 * <p>Reference: 
 *  E.M. Knorr, R.
 * T. Ng: Algorithms for Mining Distance-Based Outliers in Large Datasets, In:
 * Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998.
 * 
 * 
 * @author Lisa Reichert
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public abstract class AbstractDBOutlierDetection<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, MultiResult> {
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
  private final DistanceParameter<D> D_PARAM = new DistanceParameter<D>(D_ID, getDistanceFunction().getDistanceFactory());

  /**
   * Holds the value of {@link #D_PARAM}.
   */
  private D d;

 
  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * Constructor, adding options to option handler.
   */
  public AbstractDBOutlierDetection(Parameterization config) {
    super(config);
    config = config.descend(this);
    // neighborhood size
    if (config.grab(D_PARAM)) {
      d = D_PARAM.getValue();
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    if(logger.isVerbose()) {
      logger.verbose("computing outlier flag");
    }
    
    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);
    WritableDataStore<Double> dbodscore = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    dbodscore = computeOutlierScores(database, distFunc, d);

    
    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>(DBOD_SCORE, dbodscore);
    OrderingResult orderingResult = new OrderingFromDataStore<Double>(dbodscore, true);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    this.result = new OutlierResult(scoreMeta, scoreResult, orderingResult);
 
    return result;

  }
  /**
   * computes an outlier score for each object of the database.
   * @param distFunc 
   */
  protected abstract WritableDataStore<Double> computeOutlierScores(Database<O> database, DistanceQuery<O, D> distFunc, D d);

}