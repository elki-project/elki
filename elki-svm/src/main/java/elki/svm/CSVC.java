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
import elki.svm.qmatrix.QMatrix;
import elki.svm.qmatrix.SVC_Q;
import elki.svm.solver.Solver;

/**
 * Regularized SVM based classification (C-SVC, C-SVM).
 */
public class CSVC extends AbstractSVC {
  private static final Logging LOG = Logging.getLogger(CSVC.class);

  double Cp = 1., Cn = 1.;

  public CSVC(double eps, boolean shrinking, double cache_size) {
    super(eps, shrinking, cache_size);
  }

  @Override
  public void set_weights(double Cp, double Cn) {
    this.Cp = Cp;
    this.Cn = Cn;
  }

  @Override
  protected Solver.SolutionInfo solve(DataSet x) {
    final int l = x.size();
    double[] alpha = new double[l];
    double[] minus_ones = new double[l];
    Arrays.fill(minus_ones, -1);
    byte[] y = new byte[l];
    for(int i = 0; i < l; i++) {
      y[i] = x.value(i) > 0 ? ONE : MONE;
    }
    QMatrix Q = new CachedQMatrix(l, cache_size, new SVC_Q(x, y));
    Q.initialize();
    Solver.SolutionInfo si = new Solver().solve(l, Q, minus_ones, y, alpha, Cp, Cn, eps, shrinking);

    if(Cp == Cn && LOG.isVerbose()) {
      double sum_alpha = 0;
      for(int i = 0; i < l; i++) {
        sum_alpha += si.alpha[i];
      }

      LOG.verbose("nu = " + sum_alpha / (Cp * l));
    }

    for(int i = 0; i < l; i++) {
      si.alpha[i] *= y[i];
    }
    return si;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
