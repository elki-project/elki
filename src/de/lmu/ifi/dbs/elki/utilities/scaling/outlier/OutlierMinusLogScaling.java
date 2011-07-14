package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Scaling function to invert values by computing -1 * Math.log(x)
 * 
 * Useful for example for scaling
 * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.ABOD}, but see
 * {@link MinusLogStandardDeviationScaling} and {@link MinusLogGammaScaling} for
 * more advanced scalings for this algorithm.
 * 
 * @author Erich Schubert
 */
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "Interpreting and Unifying Outlier Scores", booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM), Mesa, AZ, 2011", url = "http://www.dbs.ifi.lmu.de/~zimek/publications/SDM2011/SDM11-outlier-preprint.pdf")
public class OutlierMinusLogScaling implements OutlierScalingFunction {
  /**
   * Maximum value seen, set by {@link #prepare}
   */
  double max = 0.0;

  /**
   * Maximum -log value seen, set by {@link #prepare}
   */
  double mlogmax;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public OutlierMinusLogScaling() {
    super();
  }

  @Override
  public double getScaled(double value) {
    assert (max != 0) : "prepare() was not run prior to using the scaling function.";
    return -Math.log(value / max) / mlogmax;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    MinMax<Double> mm = new MinMax<Double>();
    for(DBID id : ids) {
      double val = or.getScores().getValueFor(id);
      mm.put(val);
    }
    max = mm.getMax();
    mlogmax = -Math.log(mm.getMin() / max);
  }
}