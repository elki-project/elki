package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.EM;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
/**
 * outlier detection algorithm using EM Clustering. 
 * If an object does not belong to any cluster it is supposed to be an outlier. 
 * If the probability for an object to belong to the most probable cluster is still relatively low this object is an outlier. 
 *
 * @author Lisa Reichert
 *
 * @param <V> Vector type
 */
public class EMOutlierDetection<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, OutlierResult>{
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(EMOutlierDetection.class);
  
  /**
   * Inner algorithm.
   */
  EM<V> emClustering;
  
  /**
   * association id to associate the 
   */
  public static final AssociationID<Double> DBOD_MAXCPROB= AssociationID.getOrCreateAssociationID("dbod_maxcprob", Double.class);
  
  /**
   * Provides the result of the algorithm.
   */
  OutlierResult result;

  /**
   * Constructor, adding options to option handler.
   */
  public EMOutlierDetection(Parameterization config) {
    super();
    config = config.descend(this);
    emClustering = EM.parameterize(config);
  }
  
  /**
   * Runs the algorithm in the timed evaluation part.
   */

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    Clustering<EMModel<V>> emresult = emClustering.run(database);
    
    double globmax = 0.0;
    WritableDataStore<Double> emo_score = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    for (DBID id : database) {

      double maxProb = Double.POSITIVE_INFINITY;
      double[] probs = emClustering.getProbClusterIGivenX(id);
      for (double prob : probs){
         maxProb = Math.min(1 - prob, maxProb);
      }
      //logger.debug("maxprob"+ maxProb);
      emo_score.put(id, maxProb);     
      globmax = Math.max(maxProb, globmax);
    }
    AnnotationResult<Double> res1 = new AnnotationFromDataStore<Double>(DBOD_MAXCPROB, emo_score);
    OrderingResult res2 = new OrderingFromDataStore<Double>(emo_score, true);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(0.0, globmax);
    // combine results.
    result = new OutlierResult(meta, res1, res2);
    result.addResult(emresult);
    return result;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}