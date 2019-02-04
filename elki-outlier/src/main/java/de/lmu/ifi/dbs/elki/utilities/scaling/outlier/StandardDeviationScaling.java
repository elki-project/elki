/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1].
 * <p>
 * Transformation is done using the formula
 * \(\max\{0, \mathrm{erf}(\lambda \frac{x-\mu}{\sigma\sqrt{2}})\}\)
 * <p>
 * Where mean can be fixed to a given value, and stddev is then computed against
 * this mean.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Interpreting and Unifying Outlier Scores<br>
 * Proc. 11th SIAM International Conference on Data Mining (SDM 2011)
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Interpreting and Unifying Outlier Scores", //
    booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM 2011)", //
    url = "https://doi.org/10.1137/1.9781611972818.2", //
    bibkey = "DBLP:conf/sdm/KriegelKSZ11")
public class StandardDeviationScaling implements OutlierScaling {
  /**
   * Field storing the fixed mean to use
   */
  protected double fixedmean = Double.NaN;

  /**
   * Field storing the lambda value
   */
  protected double lambda;

  /**
   * Mean to use
   */
  double mean;

  /**
   * Scaling factor to use (usually: Lambda * Stddev * Sqrt(2))
   */
  double factor;

  /**
   * Constructor.
   * 
   * @param fixedmean Fixed mean
   * @param lambda Scaling factor lambda
   */
  public StandardDeviationScaling(double fixedmean, double lambda) {
    super();
    this.fixedmean = fixedmean;
    this.lambda = lambda;
  }

  /**
   * Constructor.
   */
  public StandardDeviationScaling() {
    this(Double.NaN, 1.0);
  }

  @Override
  public double getScaled(double value) {
    assert (factor != 0) : "prepare() was not run prior to using the scaling function.";
    return value <= mean ? 0 : Math.max(0, NormalDistribution.erf((value - mean) / factor));
  }

  @Override
  public void prepare(OutlierResult or) {
    DoubleRelation scores = or.getScores();
    if(Double.isNaN(fixedmean)) {
      MeanVariance mv = new MeanVariance();
      for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
        final double val = scores.doubleValue(id);
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          mv.put(val);
        }
      }
      mean = mv.getMean();
      factor = lambda * mv.getSampleStddev() * MathUtil.SQRT2;
    }
    else {
      mean = fixedmean;
      Mean sqsum = new Mean();
      for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
        final double val = scores.doubleValue(id) - mean;
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          sqsum.put(val * val);
        }
      }
      factor = lambda * FastMath.sqrt(sqsum.getMean()) * MathUtil.SQRT2;
    }
    factor = factor > 0 ? factor : Double.MIN_NORMAL;
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    if(Double.isNaN(fixedmean)) {
      MeanVariance mv = new MeanVariance();
      final int size = adapter.size(array);
      for(int i = 0; i < size; i++) {
        double val = adapter.getDouble(array, i);
        if(!Double.isInfinite(val)) {
          mv.put(val);
        }
      }
      mean = mv.getMean();
      factor = lambda * mv.getSampleStddev() * MathUtil.SQRT2;
    }
    else {
      mean = fixedmean;
      Mean sqsum = new Mean();
      final int size = adapter.size(array);
      for(int i = 0; i < size; i++) {
        double val = adapter.getDouble(array, i);
        if(!Double.isInfinite(val)) {
          sqsum.put((val - mean) * (val - mean));
        }
      }
      factor = lambda * FastMath.sqrt(sqsum.getMean()) * MathUtil.SQRT2;
    }
    factor = factor > 0 ? factor : Double.MIN_NORMAL;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify a fixed mean to use.
     */
    public static final OptionID MEAN_ID = new OptionID("stddevscale.mean", "Fixed mean to use in standard deviation scaling.");

    /**
     * Parameter to specify the lambda value
     */
    public static final OptionID LAMBDA_ID = new OptionID("stddevscale.lambda", "Significance level to use for error function.");

    /**
     * Field storing the fixed mean to use
     */
    protected double fixedmean = Double.NaN;

    /**
     * Field storing the lambda value
     */
    protected double lambda;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter meanP = new DoubleParameter(MEAN_ID) //
          .setOptional(true);
      if(config.grab(meanP)) {
        fixedmean = meanP.getValue();
      }
      DoubleParameter lambdaP = new DoubleParameter(LAMBDA_ID, 3.0);
      if(config.grab(lambdaP)) {
        lambda = lambdaP.getValue();
      }
    }

    @Override
    protected StandardDeviationScaling makeInstance() {
      return new StandardDeviationScaling(fixedmean, lambda);
    }
  }
}
