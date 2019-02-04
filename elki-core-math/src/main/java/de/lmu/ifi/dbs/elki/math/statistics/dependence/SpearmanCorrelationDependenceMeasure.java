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

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Spearman rank-correlation coefficient, also known as Spearmans Rho.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SpearmanCorrelationDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final SpearmanCorrelationDependenceMeasure STATIC = new SpearmanCorrelationDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected SpearmanCorrelationDependenceMeasure() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    double[] ranks1 = computeNormalizedRanks(adapter1, data1, len);
    double[] ranks2 = computeNormalizedRanks(adapter2, data2, len);

    // Second pass: variances and covariance
    double v1 = 0., v2 = 0., cov = 0.;
    for(int i = 0; i < len; i++) {
      double d1 = ranks1[i] - .5, d2 = ranks2[i] - .5;
      v1 += d1 * d1;
      v2 += d2 * d2;
      cov += d1 * d2;
    }
    // Note: we did not normalize by len, as this cancels out.
    return cov / FastMath.sqrt(v1 * v2);
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SpearmanCorrelationDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
