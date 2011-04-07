package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Gamma;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1],
 * by assuming a Gamma distribution on the data and evaluating the Gamma CDF.
 * 
 * @author Erich Schubert
 * 
 */
public class MinusLogGammaScaling extends OutlierGammaScaling {
  /**
   * Maximum value seen
   */
  double max;
  
  /**
   * Minimum value (after log step, so maximum again)
   */
  double mlogmax;
  
  /**
   * Constructor.
   */
  public MinusLogGammaScaling() {
    super(false);
  }

  @Override
  protected double preScale(double score) {
    return - Math.log(score / max) / mlogmax;
  }

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    meta = or.getOutlierMeta();
    // Determine Minimum and Maximum.
    MinMax<Double> mm = new MinMax<Double>();
    for(DBID id : ids) {
      double score = or.getScores().getValueFor(id);
      mm.put(score);
    }
    max = mm.getMax();
    mlogmax = - Math.log(mm.getMin() / max);
    // with the prescaling, do Gamma Scaling.
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
    k = (mean*mean) / var;
    theta = var / mean;
    try {
      atmean = Gamma.regularizedGammaP(k, mean/theta);
    }
    catch(MathException e) {
      LoggingUtil.exception(e);
    }
    //logger.warning("Mean:"+mean+" Var:"+var+" Theta: "+theta+" k: "+k+" valatmean"+atmean);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MinusLogGammaScaling makeInstance() {
      return new MinusLogGammaScaling();
    }
  }
}