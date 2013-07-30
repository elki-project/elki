package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Robust parameter estimation for the Gamma distribution.
 * 
 * Based on the Median and Median absolute deviation from Median (MAD).
 * 
 * Reference:
 * <p>
 * J. Chen and H. Rubin<br />
 * Bounds for the difference between median and mean of Gamma and Poisson
 * distributions<br />
 * In: Statist. Probab. Lett., 4 , 281–283.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has GammaDistribution - - estimates
 */
@Reference(authors = "J. Chen. H. Rubin", title = "Bounds for the difference between median and mean of Gamma and Poisson distributions", booktitle = "Statist. Probab. Lett., 4")
public class GammaMADEstimator implements DistributionEstimator<GammaDistribution> {
  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static final GammaMADEstimator STATIC = new GammaMADEstimator();

  /**
   * Private constructor.
   */
  private GammaMADEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public <A> GammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // TODO: detect pre-sorted data?
    final int len = adapter.size(data);
    // Modifiable copy:
    double[] x = new double[len];
    for (int i = 0; i < len; i++) {
      x[i] = adapter.getDouble(data, i);
    }
    double median = QuickSelect.median(x);
    if (median < Double.MIN_NORMAL) {
      throw new ArithmeticException("Cannot estimate Gamma parameters on a distribution with zero median.");
    }
    // Compute deviations:
    for (int i = 0; i < len; i++) {
      x[i] = Math.abs(x[i] - median);
    }
    double mad = QuickSelect.median(x);
    if (mad < Double.MIN_NORMAL) {
      throw new ArithmeticException("Cannot estimate Gamma parameters on a distribution with zero MAD.");
    }

    final double theta = (mad * mad) / median;
    final double k = median / theta;
    return new GammaDistribution(k, 1 / theta);
  }

  @Override
  public Class<? super GammaDistribution> getDistributionClass() {
    return GammaDistribution.class;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GammaMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
