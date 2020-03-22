package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.CachedQMatrix;
import elki.svm.qmatrix.QMatrix;
import elki.svm.qmatrix.R2_Qq;
import elki.svm.solver.Solver;

/**
 * R2q variant
 */
public class R2q extends AbstractSVR {
  private static final Logging LOG = Logging.getLogger(R2q.class);

  private double C;

  public R2q(double eps, boolean shrinking, double cache_size, double C) {
    super(eps, shrinking, cache_size);
    this.C = C;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size();
    QMatrix Q = new CachedQMatrix(l, cache_size, new R2_Qq(x, C));
    Q.initialize();
    double[] QD = Q.get_QD();

    double[] alpha = new double[l];
    alpha[0] = 1; // all others are 0.

    byte[] ones = new byte[l];
    Arrays.fill(ones, ONE);

    double[] linear_term = QD.clone();
    for(int i = 0; i < l; i++) {
      linear_term[i] = -0.5 * linear_term[i]; // 1./C is already in QD.
    }

    Solver.SolutionInfo si = new Solver().solve(l, Q, linear_term, ones, alpha, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps, shrinking);

    LOG.verbose("R^2 = " + (-2 * si.obj));
    return si;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
