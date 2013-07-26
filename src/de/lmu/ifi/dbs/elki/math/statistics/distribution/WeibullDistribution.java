package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Weibull distribution.
 * 
 * @author Erich Schubert
 */
public class WeibullDistribution implements DistributionWithRandom {
  /**
   * The naive least-squares estimator.
   */
  public static final NaiveEstimator NAIVE_ESTIMATOR = new NaiveEstimator();

  /**
   * The more robust median based estimator.
   */
  public static final MADEstimator MAD_ESTIMATOR = new MADEstimator();

  /**
   * Shift offset.
   */
  double theta = 0.0;

  /**
   * Shape parameter k.
   */
  double k;

  /**
   * Lambda parameter.
   */
  double lambda;

  /**
   * Random number generator.
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param k Shape parameter
   * @param lambda Scale parameter
   */
  public WeibullDistribution(double k, double lambda) {
    this(k, lambda, 0.0, null);
  }

  /**
   * Constructor.
   * 
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   */
  public WeibullDistribution(double k, double lambda, double theta) {
    this(k, lambda, theta, null);
  }

  /**
   * Constructor.
   * 
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param random Random number generator
   */
  public WeibullDistribution(double k, double lambda, Random random) {
    this(k, lambda, 0.0, random);
  }

  /**
   * Constructor.
   * 
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @param random Random number generator
   */
  public WeibullDistribution(double k, double lambda, double theta, Random random) {
    super();
    this.k = k;
    this.lambda = lambda;
    this.theta = theta;
    this.random = random;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, k, lambda, theta);
  }

  /**
   * PDF of Weibull distribution
   * 
   * @param x Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return PDF at position x.
   */
  public static double pdf(double x, double k, double lambda, double theta) {
    if (x > theta) {
      double xl = (x - theta) / lambda;
      return k / lambda * Math.pow(xl, k - 1) * Math.exp(-Math.pow(xl, k));
    } else {
      return 0.;
    }
  }

  /**
   * CDF of Weibull distribution
   * 
   * @param val Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return CDF at position x.
   */
  public static double cdf(double val, double k, double lambda, double theta) {
    if (val > theta) {
      return 1.0 - Math.exp(-Math.pow((val - theta) / lambda, k));
    } else {
      return 0.0;
    }
  }

  @Override
  public double cdf(double val) {
    return cdf(val, k, lambda, theta);
  }

  /**
   * Quantile function of Weibull distribution
   * 
   * @param val Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double k, double lambda, double theta) {
    if (val < 0.0 || val > 1.0) {
      return Double.NaN;
    } else if (val == 0) {
      return 0.0;
    } else if (val == 1) {
      return Double.POSITIVE_INFINITY;
    } else {
      return theta + lambda * Math.pow(-Math.log(1.0 - val), 1.0 / k);
    }
  }

  @Override
  public double quantile(double val) {
    return quantile(val, k, lambda, theta);
  }

  @Override
  public double nextRandom() {
    return theta + lambda * Math.pow(-Math.log(1 - random.nextDouble()), 1. / k);
  }

  @Override
  public String toString() {
    return "WeibullDistribution(" + k + ", " + lambda + ", " + theta + ")";
  }

  /**
   * Naive parameter estimation via least squares.
   * 
   * TODO: this doesn't seem to work very well yet. Buggy?
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has WeibullDistribution - - estimates
   */
  public static class NaiveEstimator implements DistributionEstimator<WeibullDistribution> {
    /**
     * Private constructor, use static instance!
     */
    private NaiveEstimator() {
      // Do not instantiate
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

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected NaiveEstimator makeInstance() {
        return NAIVE_ESTIMATOR;
      }
    }
  }

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
  public static class MADEstimator implements DistributionEstimator<WeibullDistribution> {
    /**
     * Private constructor, use static instance!
     */
    private MADEstimator() {
      // Do not instantiate
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
      protected MADEstimator makeInstance() {
        return MAD_ESTIMATOR;
      }
    }
  }
}
