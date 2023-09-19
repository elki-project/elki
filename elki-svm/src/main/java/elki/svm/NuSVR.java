package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.SVR_Q;
import elki.svm.solver.NuSolver;
import elki.svm.solver.Solver;

/**
 * Nu Support Vector Regression Machine.
 * <p>
 * Here, nu replaces the epsilon-insensitive loss (parameter p in {@link EpsilonSVR}}).
 */
public class NuSVR extends AbstractSVR {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NuSVR.class);

  /**
   * Nu regularization parameter
   */
  protected double nu;

  /**
   * Classic regularization
   */
  protected double C;

  /**
   * Constructor.
   * 
   * @param tol Optimizer tolerance
   * @param shrinking Use shrinking
   * @param cache_size Cache size
   * @param C Regularization parameter
   * @param nu Nu regularization
   * @param probability Estimate probabilities
   */
  public NuSVR(double tol, boolean shrinking, double cache_size, double C, double nu, boolean probability) {
    super(tol, shrinking, cache_size, probability);
    this.nu = nu;
    this.C = C;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size(), l2 = 2 * l;
    double[] alpha2 = new double[l2];
    double[] linear_term = new double[l2];
    byte[] y = new byte[l2];
    Arrays.fill(y, 0, l, ONE);
    Arrays.fill(y, l, l2, MONE);

    double sum = C * nu * l * 0.5;
    for(int i = 0; i < l; i++) {
      sum -= alpha2[i] = alpha2[i + l] = sum < C ? sum : C;
      linear_term[i] = -(linear_term[i + l] = y[i]);
    }

    SVR_Q Q = new SVR_Q(x, cache_size);
    Q.initialize();
    NuSolver solver = new NuSolver();
    Solver.SolutionInfo si = solver.solve(l2, Q, linear_term, y, alpha2, C, C, tol, shrinking);

    LOG.verbose("epsilon = " + (-solver.r));

    for(int i = 0; i < l; i++) {
      si.alpha[i] = alpha2[i] - alpha2[i + l];
    }
    return si;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
