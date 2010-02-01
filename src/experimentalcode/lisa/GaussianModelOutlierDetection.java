package experimentalcode.lisa;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Outlier have smallest GMOD_PROB: the outlier scores is the
 * <em>probability density</em> of the assumed distribution.
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector type
 */
public class GaussianModelOutlierDetection<V extends NumberVector<V, Double>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * OptionID for {@link #INVERT_FLAG}
   */
  public static final OptionID INVERT_ID = OptionID.getOrCreateOptionID("gaussod.invert", "Invert the value range to [0:1], with 1 being outliers instead of 0.");

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -gaussod.invert}
   * </p>
   */
  private final Flag INVERT_FLAG = new Flag(INVERT_ID);

  /**
   * Invert the result
   */
  private boolean invert = false;

  OutlierResult result;

  public static final AssociationID<Double> GMOD_PROB = AssociationID.getOrCreateAssociationID("gmod.prob", Double.class);

  public GaussianModelOutlierDetection() {
    super();
    addOption(INVERT_FLAG);
  }

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

    final OutlierScoreMeta meta;
    if (invert) {
      double max = mm.getMax() != 0 ? mm.getMax() : 1.;
      for (Entry<Integer, Double> entry : oscores.entrySet()) {
        entry.setValue((max - entry.getValue()) / max);
      }
      meta = new BasicOutlierScoreMeta(0.0, 1.0);
    } else {
      meta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), 0.0, Double.POSITIVE_INFINITY );
    }
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

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    invert = INVERT_FLAG.getValue();

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
