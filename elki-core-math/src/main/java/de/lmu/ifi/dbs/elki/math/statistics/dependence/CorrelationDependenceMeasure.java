/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Pearson product-moment correlation coefficient.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class CorrelationDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CorrelationDependenceMeasure.class);

  /**
   * Static instance.
   */
  public static final CorrelationDependenceMeasure STATIC = new CorrelationDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected CorrelationDependenceMeasure() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    // Perform two-pass estimation, which is numerically stable and often faster
    // than the Knuth-Welford approach (see PearsonCorrelation class)
    double m1 = 0., m2 = 0;
    for(int i = 0; i < len; i++) {
      m1 += adapter1.getDouble(data1, i);
      m2 += adapter2.getDouble(data2, i);
    }
    m1 /= len;
    m2 /= len;
    // Second pass: variances and covariance
    double v1 = 0., v2 = 0., cov = 0.;
    for(int i = 0; i < len; i++) {
      double d1 = adapter1.getDouble(data1, i) - m1;
      double d2 = adapter2.getDouble(data2, i) - m2;
      v1 += d1 * d1;
      v2 += d2 * d2;
      cov += d1 * d2;
    }
    // Note: we did not normalize by len, as this cancels out.
    return cov / FastMath.sqrt(v1 * v2);
  }

  @Override
  public <A> double[] dependence(NumberArrayAdapter<?, A> adapter, List<? extends A> data) {
    final int dims = data.size();
    final int len = size(adapter, data);
    double[] means = new double[dims];
    // Two passes - often faster due to the lower numerical cost
    // And accurate, don't use sum-of-squares.
    for(int j = 0; j < dims; j++) {
      double m = 0.;
      A da = data.get(j);
      for(int i = 0; i < len; i++) {
        m += adapter.getDouble(da, i);
      }
      means[j] = m / len;
    }
    // Build the covariance matrix, lower triangular half
    double[] vst = new double[dims];
    double[] cov = new double[(dims * (dims - 1)) >> 1];
    double[] buf = new double[dims];
    for(int i = 0; i < len; i++) {
      for(int j = 0; j < dims; j++) {
        buf[j] = adapter.getDouble(data.get(j), i) - means[j];
      }
      for(int y = 0, c = 0; y < dims; y++) {
        for(int x = 0; x < y; x++) {
          cov[c++] += buf[x] * buf[y];
        }
        vst[y] += buf[y] * buf[y];
      }
    }
    // Compute standard deviations (times sqrt(len)!):
    for(int y = 0; y < dims; y++) {
      if(vst[y] == 0.) {
        LOG.warning("Correlation is not well defined for constant attributes.");
      }
      vst[y] = FastMath.sqrt(vst[y]);
    }
    for(int y = 1, c = 0; y < dims; y++) {
      for(int x = 0; x < y; x++) {
        // We don't need to divide by sqrt(len), because it will cancel out with
        // the division we skipped just above.
        cov[c] = cov[c] / (vst[x] * vst[y]);
        c++;
      }
    }
    return cov;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CorrelationDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
