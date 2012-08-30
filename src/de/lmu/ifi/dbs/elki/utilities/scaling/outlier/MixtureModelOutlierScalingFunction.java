package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Tries to fit a mixture model (exponential for inliers and gaussian for
 * outliers) to the outlier score distribution.
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. Gao, P.-N. Tan", title = "Converting Output Scores from Outlier Detection Algorithms into Probability Estimates", booktitle = "Proc. Sixth International Conference on Data Mining, 2006. ICDM'06.", url = "http://dx.doi.org/10.1109/ICDM.2006.43")
public class MixtureModelOutlierScalingFunction implements OutlierScalingFunction {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MixtureModelOutlierScalingFunction.class);
  
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
    return ONEBYSQRT2PI / sigma * Math.exp(fmu * fmu / (-2 * sigma * sigma));
  }

  /**
   * Compute q_i (Exponential distribution, inliers)
   * 
   * @param f value
   * @param lambda Lambda parameter
   * @return probability
   */
  protected static double calcQ_i(double f, double lambda) {
    return lambda * Math.exp(-lambda * f);
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
    final double val = (alpha * pi) / (alpha * pi + (1.0 - alpha) * qi);
    return val;
  }

  @Override
  public void prepare(OutlierResult or) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    Relation<Double> scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double val = scores.get(id);
      if(!Double.isNaN(val) && !Double.isInfinite(val)) {
        mv.put(val);
      }
    }
    double curMu = mv.getMean() * 2;
    if(curMu == 0) {
      curMu = Double.MIN_NORMAL;
    }
    double curSigma = Math.max(mv.getSampleStddev(), Double.MIN_NORMAL);
    double curLambda = Math.min(1.0 / curMu, Double.MAX_VALUE);
    double curAlpha = 0.05;

    ArrayDBIDs ids = DBIDUtil.ensureArray(or.getScores().getDBIDs());
    // TODO: stop condition!
    int iter = 0;
    // logger.debugFine("iter #-1 mu = " + curMu + " sigma = " + curSigma +
    // " lambda = " + curLambda + " alpha = " + curAlpha);
    while(true) {
      // E and M-Steps
      // Sum of weights
      double tisum = 0.0;
      // Weighted sum
      double wsum = 0.0;
      // Weighted deviation from previous mean
      double sqsum = 0.0;
      for(int i = 0; i < ids.size(); i++) {
        double val = or.getScores().get(ids.get(i));
        // E-Step
        double ti = calcPosterior(val, curAlpha, curMu, curSigma, curLambda);
        // M-Step
        tisum += ti;
        wsum += ti * val;
        sqsum += ti * val * val; // (val - curMu) * (val - curMu);
      }
      if(tisum <= 0.0 || wsum <= 0.0) {
        logger.warning("MixtureModel Outlier Scaling converged to extreme.");
        break;
      }
      double newMu = wsum / tisum;
      double newSigma = Math.max(Math.sqrt(sqsum / tisum - newMu * newMu), Double.MIN_NORMAL);
      double newLambda = Math.min(tisum / wsum, Double.MAX_VALUE);
      double newAlpha = tisum / ids.size();
      // converged?
      {
        boolean changed = false;
        if(Math.abs(newMu - curMu) > DELTA) {
          changed = true;
        }
        if(Math.abs(newSigma - curSigma) > DELTA) {
          changed = true;
        }
        if(Math.abs(newLambda - curLambda) > DELTA) {
          changed = true;
        }
        if(Math.abs(newAlpha - curAlpha) > DELTA) {
          changed = true;
        }
        if(!changed) {
          break;
        }
      }
      if(newSigma <= 0.0 || newAlpha <= 0.0) {
        logger.warning("MixtureModel Outlier Scaling converged to extreme.");
        break;
      }
      // logger.debugFine("iter #"+iter+" mu = " + newMu + " sigma = " +
      // newSigma + " lambda = " + newLambda + " alpha = " + newAlpha);
      curMu = newMu;
      curSigma = newSigma;
      curLambda = newLambda;
      curAlpha = newAlpha;

      iter++;
      if(iter > 100) {
        logger.warning("Max iterations met in mixture model fitting.");
        break;
      }
    }
    mu = curMu;
    sigma = curSigma;
    lambda = curLambda;
    alpha = curAlpha;
    // logger.debugFine("mu = " + mu + " sigma = " + sigma + " lambda = " +
    // lambda + " alpha = " + alpha);
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
    final double val = 1.0 - calcPosterior(value, alpha, mu, sigma, lambda);
    // Work around issues with unstable convergence.
    if(Double.isNaN(val)) {
      return 0.0;
    }
    return val;
  }
}