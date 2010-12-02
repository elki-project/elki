package experimentalcode.shared.outlier.scaling;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Tries to fit a sigmoid to the outlier scores and use it to convert the values
 * to probability estimates in the range of 0.0 to 1.0
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. Gao, P.-N. Tan", title = "Converting Output Scores from Outlier Detection Algorithms into Probability Estimates", booktitle = "Proc. Sixth International Conference on Data Mining, 2006. ICDM'06.", url = "http://dx.doi.org/10.1109/ICDM.2006.43")
public class SigmoidOutlierScalingFunction implements OutlierScalingFunction {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SigmoidOutlierScalingFunction.class);
  
  /**
   * Sigmoid parameter
   */
  double Afinal;

  /**
   * Sigmoid parameter
   */
  double Bfinal;

  @Override
  public void prepare(Database<?> db, OutlierResult or) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    for(DBID id : db) {
      double val = or.getScores().getValueFor(id);
      mv.put(val);
    }
    double a = 1.0;
    double b = - mv.getMean();
    int iter = 0;

    ArrayDBIDs ids = DBIDUtil.ensureArray(db.getIDs());
    BitSet t = new BitSet(ids.size());
    boolean changing = true;
    while(changing) {
      changing = false;
      // E-Step
      for(int i = 0; i < ids.size(); i++) {
        double val = or.getScores().getValueFor(ids.get(i));
        double targ = a * val + b;
        if(targ > 0) {
          if (!t.get(i)) { 
            t.set(i);
            changing = true;
          }
        }
        else {
          if (t.get(i)) {
            t.clear(i);
            changing = true;
          }
        }
      }
      if (!changing) {
        break;
      }
      //logger.debugFine("Number of outliers in sigmoid: " + t.cardinality());
      // M-Step
      // Implementation based on:<br />
      // H.-T. Lin, C.-J. Lin, R. C. Weng:<br />
      // A Note on Platt’s Probabilistic Outputs for Support Vector Machines
      {
        double[] newab = MStepLevenbergMarquardt(a, b, ids, t, or.getScores());
        a = newab[0];
        b = newab[1];
      }

      iter++;
      if(iter > 100) {
        logger.warning("Max iterations met in sigmoid fitting.");
        break;
      }
    }
    Afinal = a;
    Bfinal = b;
    logger.debugFine("A = "+Afinal+" B = "+Bfinal);
  }

  /**
   * M-Step using a modified Levenberg-Marquardt method.
   * 
   * <p>
   * Implementation based on:<br />
   * H.-T. Lin, C.-J. Lin, R. C. Weng:<br />
   * A Note on Platt’s Probabilistic Outputs for Support Vector Machines
   * </p>
   * 
   * @param a A parameter
   * @param b B parameter
   * @param ids Ids to process
   * @param t Bitset containing the assignment
   * @param scores Scores
   * @return new values for A and B.
   */
  private final double[] MStepLevenbergMarquardt(double a, double b, ArrayDBIDs ids, BitSet t, AnnotationResult<Double> scores) {
    final int prior1 = t.cardinality();
    final int prior0 = ids.size() - prior1;

    final int maxiter = 10;
    final double minstep = 1e-8;
    final double sigma = 1e-12;
    // target value for "set" objects
    final double loTarget = (prior1 + 1.0) / (prior1 + 2.0);
    // target value for "unset" objects
    final double hiTarget = 1.0 / (prior0 + 2.0);
    // t[i] := t.get(i) ? hiTarget : loTarget.

    // Reset, or continue with previous values?
    //a = 0.0;
    //b = Math.log((prior0 + 1.0) / (prior1 + 1.0));
    double fval = 0.0;
    for(int i = 0; i < ids.size(); i++) {
      final double val = scores.getValueFor(ids.get(i));
      final double fApB = val * a + b;
      final double ti = t.get(i) ? hiTarget : loTarget;
      if(fApB >= 0) {
        fval += ti * fApB + Math.log(1 + Math.exp(-fApB));
      }
      else {
        fval += (ti - 1) * fApB + Math.log(1 + Math.exp(fApB));
      }
    }
    for(int it = 0; it < maxiter; it++) {
      //logger.debugFinest("Iter: " + it + "a: " + a + " b: " + b);
      // Update Gradient and Hessian (use H’ = H + sigma I)
      double h11 = sigma;
      double h22 = sigma;
      double h21 = 0.0;
      double g1 = 0.0;
      double g2 = 0.0;
      for(int i = 0; i < ids.size(); i++) {
        final double val = scores.getValueFor(ids.get(i));
        final double fApB = val * a + b;
        final double p;
        final double q;
        if(fApB >= 0) {
          p = Math.exp(-fApB) / (1.0 + Math.exp(-fApB));
          q = 1.0 / (1.0 + Math.exp(-fApB));
        }
        else {
          p = 1.0 / (1.0 + Math.exp(fApB));
          q = Math.exp(fApB) / (1.0 + Math.exp(fApB));
        }
        final double d2 = p * q;
        h11 += val * val * d2;
        h22 += d2;
        h21 += val * d2;
        final double d1 = (t.get(i) ? hiTarget : loTarget) - p;
        g1 += val * d1;
        g2 += d1;
      }
      // Stop condition
      if(Math.abs(g1) < 1e-5 && Math.abs(g2) < 1e-5) {
        break;
      }
      // Compute modified Newton directions
      final double det = h11 * h22 - h21 * h21;
      final double dA = -(h22 * g1 - h21 * g2) / det;
      final double dB = -(-h21 * g1 + h11 * g2) / det;
      final double gd = g1 * dA + g2 * dB;
      double stepsize = 1.0;
      while(stepsize >= minstep) { // Line search
        final double newA = a + stepsize * dA;
        final double newB = b + stepsize * dB;
        double newf = 0.0;
        for(int i = 0; i < ids.size(); i++) {
          final double val = scores.getValueFor(ids.get(i));
          final double fApB = val * newA + newB;
          final double ti = t.get(i) ? hiTarget : loTarget;
          if(fApB >= 0) {
            newf += ti * fApB + Math.log(1 + Math.exp(-fApB));
          }
          else {
            newf += (ti - 1) * fApB + Math.log(1 + Math.exp(fApB));
          }
        }
        if(newf < fval + 0.0001 * stepsize * gd) {
          a = newA;
          b = newB;
          fval = newf;
          break; // Sufficient decrease satisfied
        }
        else {
          stepsize /= 2.0;
        }
        if(stepsize < minstep) {
          logger.debug("Minstep hit.");
          break;
        }
      }
      if(it + 1 >= maxiter) {
        logger.debug("Maximum iterations hit.");
        break;
      }
    }
    return new double[] { a, b };
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
    return 1.0 / (1 + Math.exp(-Afinal * value - Bfinal));
  }
}
