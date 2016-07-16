package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Naive parameter estimation via least squares.
 * 
 * TODO: this doesn't seem to work very well yet. Buggy?
 * 
 * TODO: the naming is misleading: while it uses some method of moments, it
 * doesn't use "the" statistical moments.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has WeibullDistribution - - estimates
 */
public class WeibullLogMOMEstimator implements DistributionEstimator<WeibullDistribution> {
  /**
   * The naive least-squares estimator.
   */
  public static final WeibullLogMOMEstimator STATIC = new WeibullLogMOMEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private WeibullLogMOMEstimator() {
    super();
  }

  @Override
  public <A> WeibullDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    double beta1 = 0.0, beta3 = 0.0;
    MeanVariance mvlogx = new MeanVariance();
    int size = adapter.size(data);
    double size1 = size + 1.;
    for (int i = 0; i < size; i++) {
      final double val = adapter.getDouble(data, i);
      if (!(val > 0)) {
        throw new ArithmeticException("Cannot least squares fit weibull to a data set which includes non-positive values: " + val);
      }
      final double yi = Math.log(-Math.log((size - i) / size1));
      final double logxi = Math.log(val);
      beta1 += yi * logxi;
      beta3 += yi;
      mvlogx.put(logxi);
    }
    double k = (beta1 / size - beta3 / size * mvlogx.getMean()) / mvlogx.getSampleVariance();
    double lambda = 1. / Math.exp(beta3 / size - k * mvlogx.getMean());

    return new WeibullDistribution(k, lambda);
  }

  @Override
  public Class<? super WeibullDistribution> getDistributionClass() {
    return WeibullDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
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
    protected WeibullLogMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
