package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Gamma;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1]
 * by assuming a Gamma distribution on the values.
 * 
 * @author Erich Schubert
 */
public class OutlierGammaScaling implements OutlierScalingFunction {
  /**
   * Option to normalize data before fitting the gamma curve.
   */
  private static final OptionID NORMALIZE_ID = OptionID.getOrCreateOptionID("gammascale.normalize", "Regularize scores before using Gamma scaling.");

  /**
   * Normalization flag.
   * 
   * <pre>
   * -gammascale.normalize
   * </pre>
   */
  Flag NORMALIZE_FLAG = new Flag(NORMALIZE_ID);

  /**
   * Gamma parameter k
   */
  double k;

  /**
   * Gamma parameter theta
   */
  double theta;

  /**
   * Score at the mean, for cut-off.
   */
  double atmean = 0.5;

  /**
   * Store flag to Normalize data before curve fitting.
   */
  boolean normalize = false;

  /**
   * Keep a reference to the outlier score meta, for normalization.
   */
  OutlierScoreMeta meta = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OutlierGammaScaling(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(NORMALIZE_FLAG)) {
      normalize = NORMALIZE_FLAG.getValue();
    }
  }

  @Override
  public double getScaled(double value) {
    try {
      value = preScale(value);
      if(Double.isNaN(value) || Double.isInfinite(value)) {
        return 1.0;
      }
      return Math.max(0, (Gamma.regularizedGammaP(k, value / theta) - atmean) / (1 - atmean));
    }
    catch(MathException e) {
      LoggingUtil.exception(e);
      return 1.0;
    }
  }

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    meta = or.getOutlierMeta();
    MeanVariance mv = new MeanVariance();
    for(DBID id : ids) {
      double score = or.getScores().getValueFor(id);
      score = preScale(score);
      if(!Double.isNaN(score) && !Double.isInfinite(score)) {
        mv.put(score);
      }
    }
    final double mean = mv.getMean();
    final double var = mv.getSampleVariance();
    k = (mean * mean) / var;
    theta = var / mean;
    try {
      atmean = Gamma.regularizedGammaP(k, mean / theta);
    }
    catch(MathException e) {
      LoggingUtil.exception(e);
    }
    // logger.warning("Mean:"+mean+" Var:"+var+" Theta: "+theta+" k: "+k+" valatmean"+atmean);
  }

  /**
   * Normalize data if necessary.
   * 
   * Note: this is overridden by {@link MinusLogGammaScaling}!
   * 
   * @param score Original score
   * @return Normalized score.
   */
  protected double preScale(double score) {
    if(normalize) {
      score = meta.normalizeScore(score);
    }
    return score;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }
}