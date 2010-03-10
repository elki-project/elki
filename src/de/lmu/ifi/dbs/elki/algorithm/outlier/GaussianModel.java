package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Outlier have smallest GMOD_PROB: the outlier scores is the
 * <em>probability density</em> of the assumed distribution.
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector type
 */
public class GaussianModel<V extends NumberVector<V, Double>> extends AbstractAlgorithm<V, OutlierResult> {
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

  /**
   * Store the last result
   */
  private OutlierResult result;

  /**
   * Association ID for the Gaussian model outlier probability
   */
  public static final AssociationID<Double> GMOD_PROB = AssociationID.getOrCreateAssociationID("gmod.prob", Double.class);

  /**
   * Constructor.
   * 
   * @param config Parameterization
   */
  public GaussianModel(Parameterization config) {
    super(config);
    if(config.grab(INVERT_FLAG)) {
      invert = INVERT_FLAG.getValue();
    }
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
    if(invert) {
      double max = mm.getMax() != 0 ? mm.getMax() : 1.;
      for(Entry<Integer, Double> entry : oscores.entrySet()) {
        entry.setValue((max - entry.getValue()) / max);
      }
      meta = new BasicOutlierScoreMeta(0.0, 1.0);
    }
    else {
      meta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), 0.0, Double.POSITIVE_INFINITY);
    }
    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(GMOD_PROB, oscores);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(oscores);
    result = new OutlierResult(meta, res1, res2);
    return result;
  }

  @Override
  public Description getDescription() {
    return new Description("Gaussian Model Outlier", "Gaussian Model Outlier Detection", "Fit a multivariate gaussian model onto the data, and use the PDF to compute an outlier score.");
  }

  @Override
  public OutlierResult getResult() {
    return result;
  }
}