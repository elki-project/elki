package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;

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
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * outlier detection algorithm using EM Clustering. If an object does not belong
 * to any cluster it is supposed to be an outlier. If the probability for an
 * object to belong to the most probable cluster is still relatively low this
 * object is an outlier.
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector type
 */
@Title("EM Outlier: Outlier Detection based on the generic EM clustering")
@Description("The outlier score assigned is based on the highest cluster probability obtained from EM clustering.")
public class EMOutlier<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * Inner algorithm.
   */
  private EM<V> emClustering;

  /**
   * association id to associate
   */
  public static final AssociationID<Double> EMOD_MAXCPROB = AssociationID.getOrCreateAssociationID("emod_maxcprob", Double.class);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EMOutlier(Parameterization config) {
    super(config);
    emClustering = new EM<V>(config);
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    Clustering<EMModel<V>> emresult = emClustering.run(database);

    double globmax = 0.0;
    HashMap<Integer, Double> emo_score = new HashMap<Integer, Double>(database.size());
    for(Integer id : database) {
      double maxProb = Double.POSITIVE_INFINITY;
      double[] probs = emClustering.getProbClusterIGivenX(id);
      for(double prob : probs) {
        maxProb = Math.min(1 - prob, maxProb);
      }
      // logger.debug("maxprob"+ maxProb);
      emo_score.put(id, maxProb);
      globmax = Math.max(maxProb, globmax);
    }
    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(EMOD_MAXCPROB, emo_score);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(emo_score, true);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(0.0, globmax);
    // combine results.
    OutlierResult result = new OutlierResult(meta, res1, res2);
    result.addResult(emresult);
    return result;
  }
}