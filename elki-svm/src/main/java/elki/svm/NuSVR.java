package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.SVR_Q;
import elki.svm.solver.NuSolver;
import elki.svm.solver.Solver;

public class NuSVR extends AbstractSVR {
  private static final Logging LOG = Logging.getLogger(NuSVR.class);

  protected double nu, C;

  public NuSVR(double eps, boolean shrinking, double cache_size, double C, double nu) {
    super(eps, shrinking, cache_size);
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
    Solver.SolutionInfo si = solver.solve(l2, Q, linear_term, y, alpha2, C, C, eps, shrinking);

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
