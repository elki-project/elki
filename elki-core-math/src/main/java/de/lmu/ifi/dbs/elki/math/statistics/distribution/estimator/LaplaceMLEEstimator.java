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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LaplaceDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Laplace distribution parameters using Median and mean deviation from
 * median.
 * <p>
 * Reference:
 * <p>
 * R. M. Norton<br>
 * The Double Exponential Distribution: Using Calculus to Find a Maximum
 * Likelihood Estimator<br>
 * The American Statistician 38 (2)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - LaplaceDistribution
 */
@Reference(title = "The Double Exponential Distribution: Using Calculus to Find a Maximum Likelihood Estimator", //
    authors = "R. M. Norton", booktitle = "The American Statistician 38 (2)", //
    url = "https://doi.org/10.2307/2683252", //
    bibkey = "doi:10.2307/2683252")
public class LaplaceMLEEstimator implements DistributionEstimator<LaplaceDistribution> {
  /**
   * Static instance.
   */
  public static final LaplaceMLEEstimator STATIC = new LaplaceMLEEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LaplaceMLEEstimator() {
    // Do not instantiate
  }

  @Override
  public <A> LaplaceDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    int len = adapter.size(data);
    double[] temp = new double[len];
    for(int i = 0; i < len; i++) {
      temp[i] = adapter.getDouble(data, i);
    }
    double location = QuickSelect.median(temp);
    double meandev = 0.;
    for(int i = 0; i < len; i++) {
      meandev += Math.abs(temp[i] - location);
    }
    return new LaplaceDistribution(len / meandev, location);
  }

  @Override
  public Class<? super LaplaceDistribution> getDistributionClass() {
    return LaplaceDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LaplaceMLEEstimator makeInstance() {
      return STATIC;
    }
  }
}
