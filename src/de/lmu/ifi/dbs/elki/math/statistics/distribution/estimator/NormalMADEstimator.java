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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimator using Medians. More robust to outliers, and just slightly more
 * expensive (needs to copy the data for partial sorting to find the median).
 * 
 * References:
 * <p>
 * F. R. Hampel<br />
 * The Influence Curve and Its Role in Robust Estimation<br />
 * in: Journal of the American Statistical Association, June 1974, Vol. 69, No.
 * 346
 * </p>
 * <p>
 * P. J. Rousseeuw, C. Croux<br />
 * Alternatives to the Median Absolute Deviation<br />
 * in: Journal of the American Statistical Association, December 1993, Vol. 88,
 * No. 424, Theory and Methods
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has NormalDistribution - - estimates
 */
@Reference(authors = "F. R. Hampel", title = "The Influence Curve and Its Role in Robust Estimation", booktitle = "Journal of the American Statistical Association, June 1974, Vol. 69, No. 346", url = "http://www.jstor.org/stable/10.2307/2285666")
public class NormalMADEstimator implements DistributionEstimator<NormalDistribution> {
  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static NormalMADEstimator STATIC = new NormalMADEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private NormalMADEstimator() {
    super();
  }

  @Override
  public <A> NormalDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // TODO: detect pre-sorted data?
    final int len = adapter.size(data);
    // Modifiable copy:
    double[] x = new double[len];
    for (int i = 0; i < len; i++) {
      x[i] = adapter.getDouble(data, i);
    }
    double median = QuickSelect.median(x);
    // Compute absolute deviations:
    for (int i = 0; i < len; i++) {
      x[i] = Math.abs(x[i] - median);
    }
    double mdev = QuickSelect.median(x);
    // Fallback if we have more than 50% ties to next largest.
    if (!(mdev > 0.)) {
      double min = Double.POSITIVE_INFINITY;
      for (double xi : x) {
        if (xi > 0. && xi < min) {
          min = xi;
        }
      }
      if (min < Double.POSITIVE_INFINITY) {
        mdev = min;
      } else {
        mdev = 1.0; // Maybe all constant. No real value.
      }
    }
    // The scaling factor is for consistency
    return new NormalDistribution(median, NormalDistribution.ONEBYPHIINV075 * mdev);
  }

  @Override
  public Class<? super NormalDistribution> getDistributionClass() {
    return NormalDistribution.class;
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
    protected NormalMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
