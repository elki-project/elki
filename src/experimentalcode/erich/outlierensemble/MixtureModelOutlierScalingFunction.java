package experimentalcode.erich.outlierensemble;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Tries to fit a mixture model (exponential for inliers and gaussian for
 * outliers) to the outlier score distribution.
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. Gao, P.-N. Tan", title = "Converting Output Scores from Outlier Detection Algorithms into Probability Estimates", booktitle = "Proc. Sixth International Conference on Data Mining, 2006. ICDM'06.", url = "http://dx.doi.org/10.1109/ICDM.2006.43")
public class MixtureModelOutlierScalingFunction extends AbstractLoggable implements OutlierScalingFunction {
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
  public final static double ONEBYSQRT2PI = 1.0 / Math.sqrt(2 * Math.PI);

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
    return (alpha * pi) / (alpha * pi + (1.0 - alpha) * qi);
  }

  @Override
  public void prepare(Database<?> db, OutlierResult or) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    for(DBID id : db) {
      double val = or.getScores().getValueFor(id);
      mv.put(val);
    }
    double curMu = mv.getMean() * 2;
    double curSigma = mv.getStddev();
    double curLambda = 1.0 / curMu;
    double curAlpha = 0.05;

    ArrayDBIDs ids = DBIDUtil.ensureArray(db.getIDs());
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
        double val = or.getScores().getValueFor(ids.get(i));
        // E-Step
        double ti = calcPosterior(val, curAlpha, curMu, curSigma, curLambda);
        // M-Step
        tisum += ti;
        wsum += ti * val;
        sqsum += ti * val * val; // (val - curMu) * (val - curMu);
      }
      if(tisum <= 0.0 || wsum <= 0.0) {
        logger.warning("MixtureModel Outlier Scaling converged to 'no outliers'.");
        break;
      }
      double newMu = wsum / tisum;
      double newSigma = Math.sqrt(sqsum / tisum - newMu * newMu);
      double newLambda = tisum / wsum;
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
        logger.warning("MixtureModel Outlier Scaling converged to 'no outliers'.");
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
    return calcPosterior(value, alpha, mu, sigma, lambda);
  }
}