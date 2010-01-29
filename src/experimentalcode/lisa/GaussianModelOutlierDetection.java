package experimentalcode.lisa;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Outlier have smallest GMOD_PROB: the outlier scores is the
 * <em>probability density</em> of the assumed distribution.
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector type
 */
public class GaussianModelOutlierDetection<V extends NumberVector<V, Double>> extends AbstractAlgorithm<V, OutlierResult> {

  OutlierResult result;

  public static final AssociationID<Double> GMOD_PROB = AssociationID.getOrCreateAssociationID("gmod.prob", Double.class);

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    MinMax<Double> mm = new MinMax<Double>();
    // resulting scores
    HashMap<Integer, Double> oscores = new HashMap<Integer, Double>(database.size());

    // Compute mean and covariance Matrix
    V mean = DatabaseUtil.centroid(database);
    // debugFine(mean.toString());
    Matrix covarianceMatrix = DatabaseUtil.covarianceMatrix(database, mean);
    // debugFine(covarianceMatrix.toString());
    Matrix covarianceTransposed = covarianceMatrix.inverse();

    // Normalization factors for Gaussian PDF
    final double fakt = (1.0 / (Math.sqrt(Math.pow(2 * Math.PI, database.dimensionality()) * covarianceMatrix.det())));

    // for each object compute Mahalanobis distance
    for(Integer id : database) {
      V x = database.get(id);
      Vector x_minus_mean = x.minus(mean).getColumnVector();
      // Gaussian PDF
      final double mDist = x_minus_mean.transposeTimes(covarianceTransposed).times(x_minus_mean).get(0, 0);
      final double prob = fakt * Math.exp(-mDist / 2.0);

      mm.put(prob);
      oscores.put(id, prob);
    }

    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), 0.0, Double.POSITIVE_INFINITY /*
                                                                                                                  * ,
                                                                                                                  * 1.0
                                                                                                                  */);
    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(GMOD_PROB, oscores);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(oscores);
    result = new OutlierResult(meta, res1, res2);
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
