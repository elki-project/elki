package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.QMatrix;
import elki.svm.qmatrix.SVR_Q;
import elki.svm.solver.Solver;

public class EpsilonSVR extends AbstractSVR {
  private static final Logging LOG = Logging.getLogger(EpsilonSVR.class);

  protected double p, C;

  public EpsilonSVR(double eps, boolean shrinking, double cache_size, double C, double p) {
    super(eps, shrinking, cache_size);
    this.p = p;
    this.C = C;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size(), l2 = l << 1;
    double[] alpha2 = new double[l2], linear_term = new double[l2];
    for(int i = 0; i < l; i++) {
      final double v = x.value(i);
      linear_term[i] = p - v;
      linear_term[i + l] = p + v;
    }
    byte[] y = new byte[l2];
    Arrays.fill(y, 0, l, ONE);
    Arrays.fill(y, l, l2, MONE);

    QMatrix Q = new SVR_Q(x, cache_size);
    Q.initialize();
    Solver.SolutionInfo si = new Solver().solve(l2, Q, linear_term, y, alpha2, C, C, eps, shrinking);

    // Update alpha
    double sum_alpha = 0;
    for(int i = 0; i < l; i++) {
      si.alpha[i] = alpha2[i] - alpha2[i + l];
      sum_alpha += Math.abs(si.alpha[i]);
    }
    LOG.verbose("nu = " + (sum_alpha / (C * l)));
    return si;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
