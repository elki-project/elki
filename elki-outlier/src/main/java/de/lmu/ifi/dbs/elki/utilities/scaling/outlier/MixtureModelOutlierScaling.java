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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Tries to fit a mixture model (exponential for inliers and gaussian for
 * outliers) to the outlier score distribution.
 * <p>
 * Note: we found this method to often fail, and fit the normal distribution to
 * the inliers instead of the outliers, yielding reversed results.
 * <p>
 * Reference:
 * <p>
 * J. Gao, P.-N. Tan<br>
 * Converting Output Scores from Outlier Detection Algorithms into Probability
 * Estimates<br>
 * Proc. Sixth International Conference on Data Mining, 2006. ICDM'06.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "J. Gao, P.-N. Tan", //
    title = "Converting Output Scores from Outlier Detection Algorithms into Probability Estimates", //
    booktitle = "Proc. Sixth International Conference on Data Mining, 2006. ICDM'06.", //
    url = "https://doi.org/10.1109/ICDM.2006.43", //
    bibkey = "DBLP:conf/icdm/GaoT06")
@Alias("de.lmu.ifi.dbs.elki.utilities.scaling.outlier.MixtureModelOutlierScalingFunction")
public class MixtureModelOutlierScaling implements OutlierScaling {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MixtureModelOutlierScaling.class);

  /**
   * Parameter mu of the gaussian distribution (outliers)
   */
  protected double mu;

  /**
   * Parameter sigma of the gaussian distribution (outliers)
   */
  protected double sigma;

  /**
   * Parameter lambda of the exponential distribution (inliers)
   */
  protected double lambda;

  /**
   * Mixing parameter alpha
   */
  protected double alpha;

  /**
   * Precomputed static value
   */
  public static final double ONEBYSQRT2PI = 1.0 / MathUtil.SQRTTWOPI;

  /**
   * Convergence parameter
   */
  private static final double DELTA = 0.0001;

  /**
   * Compute p_i (Gaussian distribution, outliers)
   * 
   * @param f value
   * @param mu Mu parameter
   * @param sigma Sigma parameter
   * @return probability
   */
  protected static double calcP_i(double f, double mu, double sigma) {
    final double fmu = f - mu;
    return ONEBYSQRT2PI / sigma * FastMath.exp(fmu * fmu / (-2 * sigma * sigma));
  }

  /**
   * Compute q_i (Exponential distribution, inliers)
   * 
   * @param f value
   * @param lambda Lambda parameter
   * @return probability
   */
  protected static double calcQ_i(double f, double lambda) {
    return lambda * FastMath.exp(-lambda * f);
  }

  /**
   * Compute the a posterior probability for the given parameters.
   * 
   * @param f value
   * @param alpha Alpha (mixing) parameter
   * @param mu Mu (for gaussian)
   * @param sigma Sigma (for gaussian)
   * @param lambda Lambda (for exponential)
   * @return Probability
   */
  protected static double calcPosterior(double f, double alpha, double mu, double sigma, double lambda) {
    final double pi = calcP_i(f, mu, sigma);
    final double qi = calcQ_i(f, lambda);
    return (alpha * pi) / (alpha * pi + (1.0 - alpha) * qi);
  }

  @Override
  public void prepare(OutlierResult or) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double val = scores.doubleValue(id);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mv.put(val);
      }
    }
    double curMu = mv.getMean() * 2.;
    if(curMu == 0) {
      curMu = Double.MIN_NORMAL;
    }
    double curSigma = Math.max(mv.getSampleStddev(), Double.MIN_NORMAL);
    double curLambda = Math.min(1.0 / curMu, Double.MAX_VALUE);
    double curAlpha = 0.05;

    DBIDs ids = scores.getDBIDs();
    // TODO: stop condition!
    int iter = 0;
    while(true) {
      // E and M-Steps
      // Sum of weights for both distributions
      double otisum = 0.0, itisum = 0.0;
      // Weighted sum for both distributions
      double owsum = 0.0, iwsum = 0.0;
      // Weighted deviation from previous mean (Gaussian only)
      double osqsum = 0.0;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        double val = scores.doubleValue(it);
        // E-Step: estimate outlier probability
        double ti = calcPosterior(val, curAlpha, curMu, curSigma, curLambda);
        // M-Step
        otisum += ti;
        itisum += 1 - ti;
        owsum += ti * val;
        iwsum += (1 - ti) * val;
        osqsum += ti * val * val; // (val - curMu) * (val - curMu);
      }
      if(otisum <= 0.0 || owsum <= 0.0) {
        LOG.warning("MixtureModel Outlier Scaling converged to extreme.");
        break;
      }
      double newMu = owsum / otisum;
      double newSigma = Math.max(FastMath.sqrt(osqsum / otisum - newMu * newMu), Double.MIN_NORMAL);
      double newLambda = Math.min(itisum / iwsum, Double.MAX_VALUE);
      double newAlpha = otisum / ids.size();
      // converged?
      if(Math.abs(newMu - curMu) < DELTA //
          && Math.abs(newSigma - curSigma) < DELTA //
          && Math.abs(newLambda - curLambda) < DELTA //
          && Math.abs(newAlpha - curAlpha) < DELTA) {
        break;
      }
      if(newSigma <= 0.0 || newAlpha <= 0.0) {
        LOG.warning("MixtureModel Outlier Scaling converged to extreme.");
        break;
      }
      curMu = newMu;
      curSigma = newSigma;
      curLambda = newLambda;
      curAlpha = newAlpha;

      iter++;
      if(iter > 100) {
        LOG.warning("Max iterations met in mixture model fitting.");
        break;
      }
    }
    mu = curMu;
    sigma = curSigma;
    lambda = curLambda;
    alpha = curAlpha;
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    final int size = adapter.size(array);
    for(int i = 0; i < size; i++) {
      double val = adapter.getDouble(array, i);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mv.put(val);
      }
    }
    double curMu = mv.getMean() * 2.;
    if(curMu == 0) {
      curMu = Double.MIN_NORMAL;
    }
    double curSigma = Math.max(mv.getSampleStddev(), Double.MIN_NORMAL);
    double curLambda = Math.min(1.0 / curMu, Double.MAX_VALUE);
    double curAlpha = 0.05;

    // TODO: stop condition!
    int iter = 0;
    while(true) {
      // E and M-Steps
      // Sum of weights for both distributions
      double otisum = 0.0, itisum = 0.0;
      // Weighted sum for both distributions
      double owsum = 0.0, iwsum = 0.0;
      // Weighted deviation from previous mean (Gaussian only)
      double osqsum = 0.0;
      for(int i = 0; i < size; i++) {
        double val = adapter.getDouble(array, i);
        // E-Step
        double ti = calcPosterior(val, curAlpha, curMu, curSigma, curLambda);
        // M-Step
        otisum += ti;
        itisum += 1 - ti;
        owsum += ti * val;
        iwsum += (1 - ti) * val;
        osqsum += ti * val * val; // (val - curMu) * (val - curMu);
      }
      if(otisum <= 0.0 || owsum <= 0.0) {
        LOG.warning("MixtureModel Outlier Scaling converged to extreme.");
        break;
      }
      double newMu = owsum / otisum;
      double newSigma = Math.max(FastMath.sqrt(osqsum / otisum - newMu * newMu), Double.MIN_NORMAL);
      double newLambda = Math.min(itisum / iwsum, Double.MAX_VALUE);
      double newAlpha = otisum / size;
      // converged?
      if(Math.abs(newMu - curMu) < DELTA //
          && Math.abs(newSigma - curSigma) < DELTA //
          && Math.abs(newLambda - curLambda) < DELTA //
          && Math.abs(newAlpha - curAlpha) < DELTA) {
        break;
      }
      if(newSigma <= 0.0 || newAlpha <= 0.0) {
        LOG.warning("MixtureModel Outlier Scaling converged to extreme.");
        break;
      }
      curMu = newMu;
      curSigma = newSigma;
      curLambda = newLambda;
      curAlpha = newAlpha;

      iter++;
      if(iter > 100) {
        LOG.warning("Max iterations met in mixture model fitting.");
        break;
      }
    }
    mu = curMu;
    sigma = curSigma;
    lambda = curLambda;
    alpha = curAlpha;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getScaled(double value) {
    final double val = calcPosterior(value, alpha, mu, sigma, lambda);
    // Work around issues with unstable convergence.
    return val > 0 ? val : 0;
  }
}
