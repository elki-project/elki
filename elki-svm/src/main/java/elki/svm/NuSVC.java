package elki.svm;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.CachedQMatrix;
import elki.svm.qmatrix.QMatrix;
import elki.svm.qmatrix.SVC_Q;
import elki.svm.solver.NuSolver;
import elki.svm.solver.Solver;

/**
 * Nu Support Vector Classification Machine.
 * <p>
 * Nu-SVM are a slightly different regularization method, where the nu parameter
 * controls the desired amount of support vectors.
 */
public class NuSVC extends AbstractSVC {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NuSVC.class);

  /**
   * Nu regularization parameter
   */
  protected double nu;

  /**
   * Constructor.
   * 
   * @param tol Optimizer tolerance
   * @param shrinking Use shrinking
   * @param cache_size Cache size
   * @param nu Nu regularization parameter
   * @param probability Estimate probabilities
   */
  public NuSVC(double tol, boolean shrinking, double cache_size, double nu, boolean probability) {
    super(tol, shrinking, cache_size, probability);
    this.nu = nu;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size();
    byte[] y = new byte[l];
    for(int i = 0; i < l; i++) {
      y[i] = x.value(i) > 0 ? ONE : MONE;
    }
    double sum_pos = nu * l * .5, sum_neg = nu * l * .5;

    double[] alpha = new double[l];
    for(int i = 0; i < l; i++) {
      if(y[i] == ONE) {
        alpha[i] = sum_pos < 1.0 ? sum_pos : 1.0;
        sum_pos -= alpha[i];
      }
      else {
        alpha[i] = sum_neg < 1.0 ? sum_neg : 1.0;
        sum_neg -= alpha[i];
      }
    }

    double[] zeros = new double[l];

    QMatrix Q = new CachedQMatrix(l, cache_size, new SVC_Q(x, y));
    Q.initialize();
    NuSolver solver = new NuSolver();
    Solver.SolutionInfo si = solver.solve(l, Q, zeros, y, alpha, 1., 1., tol, shrinking);
    final double ir = 1 / solver.r;
    LOG.verbose("C = " + ir);

    for(int i = 0; i < l; i++) {
      si.alpha[i] *= y[i] * ir;
    }

    si.rho *= ir;
    si.obj *= ir * ir;
    si.upper_bound_p = si.upper_bound_n = ir;
    return si;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
