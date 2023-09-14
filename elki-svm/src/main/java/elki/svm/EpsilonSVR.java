/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.qmatrix.QMatrix;
import elki.svm.qmatrix.SVR_Q;
import elki.svm.solver.Solver;

/**
 * Epsilon Support Vector Regression Machine with epsilon-insensitive loss function.
 */
public class EpsilonSVR extends AbstractSVR {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(EpsilonSVR.class);

  /**
   * Epsilon in epsilon-loss function
   */
  protected double p;
  
  /**
   * Regularization parameter
   */
  protected double C;

  /**
   * Constructor.
   * 
   * @param eps Optimizer tolerance (<b>not</b> the epsilon in the loss function)
   * @param shrinking Use shrinking
   * @param cache_size Cache size
   * @param C Regularization parameter
   * @param p Epsilon in epsilon-loss function
   */
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
