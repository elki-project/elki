package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Normalization used by HeDES
 * 
 * @author Erich Schubert
 */
@Reference(authors = "H. V. Nguyen, H. H. Ang, V. Gopalkrishnan", title = "Mining Outliers with Ensemble of Heterogeneous Detectors on Random Subspaces", booktitle = "Proc. 15th International Conference on Database Systems for Advanced Applications (DASFAA 2010)", url = "http://dx.doi.org/10.1007/978-3-642-12026-8_29")
public class HeDESNormalizationOutlierScaling implements OutlierScalingFunction {
  /**
   * Mean
   */
  double mean;

  /**
   * Standard deviation
   */
  double stddev;

  /**
   * Minimum after scaling
   */
  double scaledmin;

  /**
   * Maximum after scaling
   */
  double scaledmax;

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    MeanVariance mv = new MeanVariance();
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBID id : ids) {
      double val = or.getScores().get(id);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mv.put(val);
        minmax.put(val);
      }
    }

    mean = mv.getMean();
    stddev = mv.getSampleStddev();
    scaledmax = getScaled(minmax.getMax());
    scaledmin = getScaled(minmax.getMin());
  }

  @Override
  public double getMax() {
    return scaledmax;
  }

  @Override
  public double getMin() {
    return scaledmin;
  }

  @Override
  public double getScaled(double value) {
    assert (stddev > 0 || (value == mean)) : "prepare() was not run prior to using the scaling function.";
    return (value - mean) / stddev;
  }
}