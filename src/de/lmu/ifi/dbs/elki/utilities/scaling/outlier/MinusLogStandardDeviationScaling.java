package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1].
 * 
 * Transformation is done using the formula max(0, erf(lambda * (x - mean) /
 * (stddev * sqrt(2))))
 * 
 * Where mean can be fixed to a given value, and stddev is then computed against
 * this mean.
 * 
 * @author Erich Schubert
 */
@Reference(authors="H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title="Interpreting and Unifying Outlier Scores", booktitle="Proc. 11th SIAM International Conference on Data Mining (SDM), Mesa, AZ, 2011", url="http://www.dbs.ifi.lmu.de/~zimek/publications/SDM2011/SDM11-outlier-preprint.pdf")
public class MinusLogStandardDeviationScaling extends StandardDeviationScaling {
  /**
   * Constructor.
   * 
   * @param fixedmean
   * @param lambda
   */
  public MinusLogStandardDeviationScaling(Double fixedmean, Double lambda) {
    super(fixedmean, lambda);
  }

  @Override
  public double getScaled(double value) {
    assert (factor != 0) : "prepare() was not run prior to using the scaling function.";
    final double mlogv = -Math.log(value);
    if(mlogv < mean || Double.isNaN(mlogv)) {
      return 0.0;
    }
    return Math.max(0.0, ErrorFunctions.erf((mlogv - mean) / factor));
  }

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    if(fixedmean == null) {
      MeanVariance mv = new MeanVariance();
      for(DBID id : ids) {
        double val = -Math.log(or.getScores().get(id));
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          mv.put(val);
        }
      }
      mean = mv.getMean();
      factor = lambda * mv.getSampleStddev() * MathUtil.SQRT2;
    }
    else {
      mean = fixedmean;
      double sqsum = 0;
      int cnt = 0;
      for(DBID id : ids) {
        double val = -Math.log(or.getScores().get(id));
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          sqsum += (val - mean) * (val - mean);
          cnt += 1;
        }
      }
      factor = lambda * Math.sqrt(sqsum / cnt) * MathUtil.SQRT2;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends StandardDeviationScaling.Parameterizer {
    @Override
    protected MinusLogStandardDeviationScaling makeInstance() {
      return new MinusLogStandardDeviationScaling(fixedmean, lambda);
    }
  }
}