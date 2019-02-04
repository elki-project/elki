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

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Tries to fit a sigmoid to the outlier scores and use it to convert the values
 * to probability estimates in the range of 0.0 to 1.0
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
@Alias("de.lmu.ifi.dbs.elki.utilities.scaling.outlier.SigmoidOutlierScalingFunction")
public class SigmoidOutlierScaling implements OutlierScaling {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SigmoidOutlierScaling.class);

  /**
   * Sigmoid parameter
   */
  double Afinal;

  /**
   * Sigmoid parameter
   */
  double Bfinal;

  @Override
  public void prepare(OutlierResult or) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      final double val = scores.doubleValue(id);
      if(Double.isFinite(val)) {
        mv.put(val);
      }
    }
    double a = 1.0, b = -mv.getMean();
    int iter = 0;

    ArrayDBIDs ids = DBIDUtil.ensureArray(or.getScores().getDBIDs());
    DBIDArrayIter it = ids.iter();
    long[] t = BitsUtil.zero(ids.size());
    boolean changing = true;
    while(changing) {
      changing = false;
      // E-Step
      it.seek(0);
      for(int i = 0; i < ids.size(); i++, it.advance()) {
        double val = or.getScores().doubleValue(it);
        double targ = a * val + b;
        if(targ > 0) {
          if(!BitsUtil.get(t, i)) {
            BitsUtil.setI(t, i);
            changing = true;
          }
        }
        else {
          if(BitsUtil.get(t, i)) {
            BitsUtil.clearI(t, i);
            changing = true;
          }
        }
      }
      if(!changing) {
        break;
      }
      // M-Step
      {
        double[] newab = MStepLevenbergMarquardt(a, b, ids, t, or.getScores());
        a = newab[0];
        b = newab[1];
      }

      iter++;
      if(iter > 100) {
        LOG.warning("Max iterations met in sigmoid fitting.");
        break;
      }
    }
    Afinal = a;
    Bfinal = b;
    LOG.debugFine("A = " + Afinal + " B = " + Bfinal);
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    // Initial parameters - are these defaults sounds?
    MeanVariance mv = new MeanVariance();
    final int size = adapter.size(array);
    for(int i = 0; i < size; i++) {
      double val = adapter.getDouble(array, i);
      if(Double.isFinite(val)) {
        mv.put(val);
      }
    }
    double a = 1.0, b = -mv.getMean();
    int iter = 0;

    long[] t = BitsUtil.zero(size);
    boolean changing = true;
    while(changing) {
      changing = false;
      // E-Step
      for(int i = 0; i < size; i++) {
        double val = adapter.getDouble(array, i);
        double targ = a * val + b;
        if(targ > 0) {
          if(!BitsUtil.get(t, i)) {
            BitsUtil.setI(t, i);
            changing = true;
          }
        }
        else {
          if(BitsUtil.get(t, i)) {
            BitsUtil.clearI(t, i);
            changing = true;
          }
        }
      }
      if(!changing) {
        break;
      }
      // M-Step
      {
        double[] newab = MStepLevenbergMarquardt(a, b, t, array, adapter);
        a = newab[0];
        b = newab[1];
      }

      iter++;
      if(iter > 100) {
        LOG.warning("Max iterations met in sigmoid fitting.");
        break;
      }
    }
    Afinal = a;
    Bfinal = b;
    LOG.debugFine("A = " + Afinal + " B = " + Bfinal);
  }

  /**
   * M-Step using a modified Levenberg-Marquardt method.
   * <p>
   * Implementation based on:<br>
   * H.-T. Lin, C.-J. Lin, R. C. Weng:<br>
   * A Note on Platt’s Probabilistic Outputs for Support Vector Machines
   * 
   * @param a A parameter
   * @param b B parameter
   * @param ids Ids to process
   * @param t Bitset containing the assignment
   * @param scores Scores
   * @return new values for A and B.
   */
  private double[] MStepLevenbergMarquardt(double a, double b, ArrayDBIDs ids, long[] t, DoubleRelation scores) {
    final int prior1 = BitsUtil.cardinality(t);
    final int prior0 = ids.size() - prior1;

    final int maxiter = 10;
    final double minstep = 1e-8, sigma = 1e-12;
    // target value for "set" objects
    final double loTarget = (prior1 + 1.0) / (prior1 + 2.0);
    // target value for "unset" objects
    final double hiTarget = 1.0 / (prior0 + 2.0);
    // t[i] := t.get(i) ? hiTarget : loTarget.

    // Reset, or continue with previous values?
    // a = 0.0;
    // b = FastMath.log((prior0 + 1.0) / (prior1 + 1.0));
    double fval = 0.0;
    DBIDArrayIter iter = ids.iter();
    for(int i = 0; i < ids.size(); i++, iter.advance()) {
      final double val = scores.doubleValue(iter);
      final double fApB = val * a + b;
      final double ti = BitsUtil.get(t, i) ? hiTarget : loTarget;
      if(fApB >= 0) {
        fval += ti * fApB + FastMath.log(1 + FastMath.exp(-fApB));
      }
      else {
        fval += (ti - 1) * fApB + FastMath.log(1 + FastMath.exp(fApB));
      }
    }
    for(int it = 0; it < maxiter; it++) {
      // Update Gradient and Hessian (use H’ = H + sigma I)
      double h11 = sigma, h22 = sigma, h21 = 0.0;
      double g1 = 0.0, g2 = 0.0;
      iter.seek(0);
      for(int i = 0; i < ids.size(); i++, iter.advance()) {
        final double val = scores.doubleValue(iter);
        final double fApB = val * a + b;
        final double p, q;
        if(fApB >= 0) {
          p = FastMath.exp(-fApB) / (1.0 + FastMath.exp(-fApB));
          q = 1.0 / (1.0 + FastMath.exp(-fApB));
        }
        else {
          p = 1.0 / (1.0 + FastMath.exp(fApB));
          q = FastMath.exp(fApB) / (1.0 + FastMath.exp(fApB));
        }
        final double d2 = p * q;
        h11 += val * val * d2;
        h22 += d2;
        h21 += val * d2;
        final double d1 = (BitsUtil.get(t, i) ? hiTarget : loTarget) - p;
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
        iter.seek(0);
        for(int i = 0; i < ids.size(); i++, iter.advance()) {
          final double val = scores.doubleValue(iter);
          final double fApB = val * newA + newB;
          final double ti = BitsUtil.get(t, i) ? hiTarget : loTarget;
          if(fApB >= 0) {
            newf += ti * fApB + FastMath.log(1 + FastMath.exp(-fApB));
          }
          else {
            newf += (ti - 1) * fApB + FastMath.log(1 + FastMath.exp(fApB));
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
          LOG.debug("Minstep hit.");
          break;
        }
      }
    }
    return new double[] { a, b };
  }

  /**
   * M-Step using a modified Levenberg-Marquardt method.
   * <p>
   * Implementation based on:
   * <p>
   * H.-T. Lin, C.-J. Lin, R. C. Weng:<br>
   * A Note on Platt’s Probabilistic Outputs for Support Vector Machines
   *
   * @param a A parameter
   * @param b B parameter
   * @param t Bitset containing the assignment
   * @param array Score array
   * @param adapter Array adapter
   * @return new values for A and B.
   */
  private <A> double[] MStepLevenbergMarquardt(double a, double b, long[] t, A array, NumberArrayAdapter<?, A> adapter) {
    final int size = adapter.size(array);
    final int prior1 = BitsUtil.cardinality(t);
    final int prior0 = size - prior1;

    final int maxiter = 10;
    final double minstep = 1e-8, sigma = 1e-12;
    // target value for "set" objects
    final double loTarget = (prior1 + 1.0) / (prior1 + 2.0);
    // target value for "unset" objects
    final double hiTarget = 1.0 / (prior0 + 2.0);
    // t[i] := t.get(i) ? hiTarget : loTarget.

    // Reset, or continue with previous values?
    // a = 0.0;
    // b = FastMath.log((prior0 + 1.0) / (prior1 + 1.0));
    double fval = 0.0;
    for(int i = 0; i < size; i++) {
      final double val = adapter.getDouble(array, i);
      final double fApB = val * a + b;
      final double ti = BitsUtil.get(t, i) ? hiTarget : loTarget;
      if(fApB >= 0) {
        fval += ti * fApB + FastMath.log(1 + FastMath.exp(-fApB));
      }
      else {
        fval += (ti - 1) * fApB + FastMath.log(1 + FastMath.exp(fApB));
      }
    }
    for(int it = 0; it < maxiter; it++) {
      // Update Gradient and Hessian (use H’ = H + sigma I)
      double h11 = sigma, h22 = sigma, h21 = 0.0;
      double g1 = 0.0, g2 = 0.0;
      for(int i = 0; i < size; i++) {
        final double val = adapter.getDouble(array, i);
        final double fApB = val * a + b;
        final double p, q;
        if(fApB >= 0) {
          p = FastMath.exp(-fApB) / (1.0 + FastMath.exp(-fApB));
          q = 1.0 / (1.0 + FastMath.exp(-fApB));
        }
        else {
          p = 1.0 / (1.0 + FastMath.exp(fApB));
          q = FastMath.exp(fApB) / (1.0 + FastMath.exp(fApB));
        }
        final double d2 = p * q;
        h11 += val * val * d2;
        h22 += d2;
        h21 += val * d2;
        final double d1 = (BitsUtil.get(t, i) ? hiTarget : loTarget) - p;
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
        for(int i = 0; i < size; i++) {
          final double val = adapter.getDouble(array, i);
          final double fApB = val * newA + newB;
          final double ti = BitsUtil.get(t, i) ? hiTarget : loTarget;
          if(fApB >= 0) {
            newf += ti * fApB + FastMath.log(1 + FastMath.exp(-fApB));
          }
          else {
            newf += (ti - 1) * fApB + FastMath.log(1 + FastMath.exp(fApB));
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
          LOG.debug("Minstep hit.");
          break;
        }
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
    return 1.0 / (1 + FastMath.exp(-Afinal * value - Bfinal));
  }
}
