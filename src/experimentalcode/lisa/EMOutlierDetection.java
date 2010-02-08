package experimentalcode.lisa;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.EM;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
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
   * Inner algorithm.
   */
  EM<V> emClustering = new EM<V>();
  
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
  public EMOutlierDetection() {
    super();
    addParameterizable(emClustering);
  }
  
  /**
   * Calls the super method
   * and sets additionally the values of the parameter
   * {@link #K_PARAM}, {@link #N_PARAM} 
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
      List<String> remainingParameters = super.setParameters(args);
      remainingParameters= emClustering.setParameters(remainingParameters);
      return remainingParameters;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    Clustering<EMModel<V>> emresult = emClustering.run(database);
    
    double globmax = 0.0;
    HashMap<Integer, Double> emo_score = new HashMap<Integer,Double>(database.size());
    for (Integer id : database) {

      double maxProb = Double.POSITIVE_INFINITY;
      double[] probs = emClustering.getProbClusterIGivenX(id);
      for (double prob : probs){
         maxProb = Math.min(1 - prob, maxProb);
      }
      //logger.debug("maxprob"+ maxProb);
      emo_score.put(id, maxProb);     
      globmax = Math.max(maxProb, globmax);
    }
    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(DBOD_MAXCPROB, emo_score);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(emo_score, true);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(0.0, globmax);
    // combine results.
    result = new OutlierResult(meta, res1, res2);
    result.addResult(emresult);
    return result;
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutlierResult getResult() {
    
    return result;
  }
  
}
