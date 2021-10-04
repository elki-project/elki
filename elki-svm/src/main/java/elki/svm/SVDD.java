package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.CachedQMatrix;
import elki.svm.qmatrix.Kernel;
import elki.svm.qmatrix.QMatrix;
import elki.svm.solver.Solver;

/**
 * Support Vector Data Description.
 * <p>
 * Note: R2 variant is SVDD with C=2.
 */
public class SVDD extends AbstractSVR {
  private static final Logging LOG = Logging.getLogger(SVDD.class);

  private double C;

  public SVDD(double eps, boolean shrinking, double cache_size, double C) {
    super(eps, shrinking, cache_size);
    this.C = C;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size();
    double invl = 1.0 / l;
    QMatrix Q = new CachedQMatrix(l, cache_size, new Kernel(x));
    Q.initialize();
    double[] QD = Q.get_QD();
    double[] alpha = new double[l];

    double r_square = 0.0;
    Solver.SolutionInfo si;
    if(C > invl) {
      double[] linear_term = QD.clone();
      for(int i = 0; i < l; i++) {
        linear_term[i] *= -0.5;
      }

      double sum_alpha = 1;
      for(int i = 0; i < l; i++) {
        alpha[i] = Math.min(C, sum_alpha);
        sum_alpha -= alpha[i];
      }
      byte[] ones = new byte[l];
      Arrays.fill(ones, ONE);
      si = new Solver().solve(l, Q, linear_term, ones, alpha, C, C, eps, shrinking);

      // \bar{R} = 2(obj-rho) + sum K_{ii}*alpha_i
      // because rho = (a^Ta - \bar{R})/2
      r_square = 2 * (si.obj - si.rho);
      for(int i = 0; i < l; i++) {
        r_square += alpha[i] * QD[i];
      }
    }
    else {
      double rho = 0, obj = 0;
      // rho = aTa/2 = sum sum Q_ij /l/l/2
      // obj = 0.5*(-sum Q_ii + sum sum Q_ij /l)*C
      // 0.5 for consistency with C > 1/l, where dual is divided by 2
      for(int i = 0; i < l; i++) {
        alpha[i] = invl;
        obj -= QD[i] * 0.5;
        rho += QD[i] * 0.5;
        for(int j = i + 1; j < l; j++) {
          rho += x.similarity(i, j);
        }
      }
      si = new Solver.SolutionInfo();
      si.obj = (obj + rho / l) * C;
      si.rho = rho / (l * l);
      si.alpha = alpha;
    }

    si.r_square = r_square;
    LOG.verbose("R^2 = " + r_square);
    if(C > 1) {
      LOG.verbose("Warning: Note that after C > 1, all models are the same.");
    }
    if(C <= invl) {
      LOG.verbose("Warning: R^* = 0 for C <= 1/#instances.");
    }
    return si;
  }

  public interface RadiusAcceptor{
    void setRSquare(double r_square);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
