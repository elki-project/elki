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
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Parameter estimation via median and median absolute deviation from median
 * (MAD).
 * 
 * Reference:
 * <p>
 * Robust Estimators for Transformed Location Scale Families<br />
 * D. J. Olive
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has WeibullDistribution - - estimates
 */
@Reference(title = "Robust Estimators for Transformed Location Scale Families", authors = "D. J. Olive", booktitle = "")
public class WeibullLogMADEstimator implements DistributionEstimator<WeibullDistribution> {
  /**
   * The more robust median based estimator.
   */
  public static final WeibullLogMADEstimator STATIC = new WeibullLogMADEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private WeibullLogMADEstimator() {
    super();
  }

  @Override
  public <A> WeibullDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    int size = adapter.size(data);
    double[] logx = new double[size];
    for (int i = 0; i < size; i++) {
      final double val = adapter.getDouble(data, i);
      if (!(val > 0)) {
        throw new ArithmeticException("Cannot least squares fit weibull to a data set which includes non-positive values: " + val);
      }
      logx[i] = Math.log(val);
    }
    double med = QuickSelect.median(logx);
    for (int i = 0; i < size; i++) {
      logx[i] = Math.abs(logx[i] - med);
    }
    double mad = QuickSelect.median(logx);
    // Work around degenerate cases:
    if (!(mad > 0.)) {
      double min = Double.POSITIVE_INFINITY;
      for (double val : logx) {
        if (val > 0 && val < min) {
          min = val;
        }
      }
      if (min < Double.POSITIVE_INFINITY) {
        mad = min;
      } else {
        mad = 1.;
      }
    }
    double isigma = 1.30370 / mad;
    double lambda = Math.exp(isigma * med - MathUtil.LOGLOG2);

    return new WeibullDistribution(isigma, lambda);
  }

  @Override
  public Class<? super WeibullDistribution> getDistributionClass() {
    return WeibullDistribution.class;
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
    protected WeibullLogMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
