package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.CachedQMatrix;
import elki.svm.qmatrix.QMatrix;
import elki.svm.qmatrix.R2_Qq;
import elki.svm.solver.Solver;

/**
 * R^2 L2SVM variant, similar to SVDD.
 */
public class R2q extends AbstractOCSV {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(R2q.class);

  /**
   * Regularization
   */
  private double C;

  /**
   * Constructor.
   * 
   * @param tol Optimizer tolerance
   * @param shrinking Use shrinking
   * @param cache_size Cache size
   * @param C Regularization parameter
   * @param probability Estimate probabilities
   */
  public R2q(double tol, boolean shrinking, double cache_size, double C, boolean probability) {
    super(tol, shrinking, cache_size, probability);
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

    Solver.SolutionInfo si = new Solver().solve(l, Q, linear_term, ones, alpha, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, tol, shrinking);

    LOG.verbose("R^2 = " + (-2 * si.obj));
    return si;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
