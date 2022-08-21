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
import elki.svm.qmatrix.CachedQMatrix;
import elki.svm.qmatrix.Kernel;
import elki.svm.qmatrix.QMatrix;
import elki.svm.solver.Solver;

/**
 * One-class classification is similar to regression.
 */
public class OneClassSVM extends AbstractSVR {
  private static final Logging LOG = Logging.getLogger(OneClassSVM.class);

  protected double nu;

  public OneClassSVM(double eps, boolean shrinking, double cache_size, double nu) {
    super(eps, shrinking, cache_size);
    this.nu = nu;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size();
    double[] zeros = new double[l];
    double[] alpha = new double[l];
    final int n = (int) (nu * l); // # of alpha's at upper bound
    Arrays.fill(alpha, 0, n, 1);
    if(n < l) {
      alpha[n] = nu * l - n;
    }
    byte[] ones = new byte[l];
    Arrays.fill(ones, ONE);

    QMatrix Q = new CachedQMatrix(l, cache_size, new Kernel(x));
    Q.initialize();
    return new Solver().solve(l, Q, zeros, ones, alpha, 1.0, 1.0, eps, shrinking);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
