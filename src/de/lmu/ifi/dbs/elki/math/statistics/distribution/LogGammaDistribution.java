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

import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Gamma Distribution, with random generation and density functions.
 * 
 * @author Erich Schubert
 */
public class LogGammaDistribution implements DistributionWithRandom {
  /**
   * Static estimation, using iterative refinement.
   */
  public static final ChoiWetteEstimator CHOI_WETTE_ESTIMATOR = new ChoiWetteEstimator();

  /**
   * Static estimation using just the mean and variance.
   */
  public static final NaiveEstimator NAIVE_ESTIMATOR = new NaiveEstimator();

  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static final MADEstimator MAD_ESTIMATOR = new MADEstimator();

  /**
   * Alpha == k.
   */
  private final double k;

  /**
   * Theta == 1 / Beta.
   */
  private final double theta;

  /**
   * Translation offset.
   */
  private final double shift;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param shift Location offset
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param random Random generator
   */
  public LogGammaDistribution(double k, double theta, double shift, Random random) {
    super();
    if (!(k > 0.0) || !(theta > 0.0)) { // Note: also tests for NaNs!
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution: " + k + " " + theta);
    }

    this.k = k;
    this.theta = theta;
    this.shift = shift;
    this.random = random;
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param shift Location offset
   */
  public LogGammaDistribution(double k, double theta, double shift) {
    this(k, theta, shift, new Random());
  }

  @Override
  public double pdf(double val) {
    return pdf(val, k, theta, shift);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, k, theta, shift);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, k, theta, shift);
  }

  @Override
  public double nextRandom() {
    return Math.exp(GammaDistribution.nextRandom(k, theta, random)) + shift;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "LogGammaDistribution(k=" + k + ", theta=" + theta + ", shift=" + shift + ")";
  }

  /**
   * @return the value of k
   */
  public double getK() {
    return k;
  }

  /**
   * @return the standard deviation
   */
  public double getTheta() {
    return theta;
  }

  /**
   * The CDF, static version.
   * 
   * @param val Value
   * @param k Shape k
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return cdf value
   */
  public static double cdf(double val, double k, double theta, double shift) {
    if (val <= shift) {
      return 0.0;
    }
    return GammaDistribution.regularizedGammaP(k, Math.log(val - shift) * theta);
  }

  /**
   * The log CDF, static version.
   * 
   * @param val Value
   * @param k Shape k
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return cdf value
   */
  public static double logcdf(double val, double k, double theta, double shift) {
    if (val <= shift) {
      return 0.0;
    }
    return GammaDistribution.logregularizedGammaP(k, Math.log(val - shift) * theta);
  }

  /**
   * LogGamma distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return probability density
   */
  public static double pdf(double x, double k, double theta, double shift) {
    if (x <= shift) {
      return 0.0;
    }
    return GammaDistribution.pdf(Math.log(x - shift), k, theta);
  }

  /**
   * LogGamma distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return probability density
   */
  public static double logpdf(double x, double k, double theta, double shift) {
    if (x <= shift) {
      return 0.0;
    }
    return GammaDistribution.logpdf(Math.log(x - shift), k, theta);
  }

  /**
   * Compute probit (inverse cdf) for LogGamma distributions.
   * 
   * @param p Probability
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return Probit for Gamma distribution
   */
  public static double quantile(double p, double k, double theta, double shift) {
    return Math.exp(GammaDistribution.pdf(p, k, theta)) + shift;
  }

  /**
   * Simple parameter estimation for the Gamma distribution.
   * 
   * This is a very naive estimation, based on the mean and variance only.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has LogGammaDistribution - - estimates
   */
  public static class NaiveEstimator implements DistributionEstimator<LogGammaDistribution> {
    /**
     * Private constructor.
     */
    private NaiveEstimator() {
      // Do not instantiate - use static class
    }

    @Override
    public <A> LogGammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      final int len = adapter.size(data);
      double shift = Double.MAX_VALUE;
      for (int i = 0; i < len; i++) {
        shift = Math.min(shift, adapter.getDouble(data, i));
      }
      shift -= 1; // So no negative values arise after log
      MeanVariance mv = new MeanVariance();
      for (int i = 0; i < len; i++) {
        final double val = adapter.getDouble(data, i) - shift;
        mv.put(val > 1 ? Math.log(val) : 0);
      }
      return estimate(mv, shift);
    }

    /**
     * Simple parameter estimation for Gamma distribution.
     * 
     * @param mv Mean and Variance
     * @param shift Shift
     * @return LogGamma distribution
     */
    private LogGammaDistribution estimate(MeanVariance mv, double shift) {
      final double mu = mv.getMean();
      final double var = mv.getSampleVariance();
      if (!(mu > 0.) || !(var > 0.)) {
        throw new ArithmeticException("Cannot estimate LogGamma parameters on a distribution with zero mean or variance: " + mv.toString());
      }
      final double theta = var / mu;
      final double k = mu / theta;
      return new LogGammaDistribution(k, 1 / theta, shift);
    }

    @Override
    public Class<? super LogGammaDistribution> getDistributionClass() {
      return LogGammaDistribution.class;
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
   * Robust parameter estimation for the LogGamma distribution.
   * 
   * A modified algorithm for LogGamma distributions.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has LogGammaDistribution - - estimates
   */
  public static class MADEstimator implements DistributionEstimator<LogGammaDistribution> {
    /**
     * Private constructor.
     */
    private MADEstimator() {
      // Do not instantiate - use static class
    }

    @Override
    public <A> LogGammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      final int len = adapter.size(data);
      // Modifiable copy:
      double[] x = new double[len];
      double shift = Double.MAX_VALUE;
      for (int i = 0; i < len; i++) {
        x[i] = adapter.getDouble(data, i);
        shift = Math.min(shift, x[i]);
      }
      shift -= 1; // So no negative values arise after log
      for (int i = 0; i < len; i++) {
        final double val = x[i] - shift;
        if (val > 1.) {
          x[i] = Math.log(val);
        } else {
          x[i] = 0.;
        }
      }
      double median = QuickSelect.median(x);
      if (!(median > 0)) {
        median = Double.MIN_NORMAL;
      }
      // Compute deviations:
      for (int i = 0; i < len; i++) {
        x[i] = Math.abs(x[i] - median);
      }
      double mad = QuickSelect.median(x);
      if (!(mad > 0)) {
        throw new ArithmeticException("Cannot estimate LogGamma parameters on a distribution with zero MAD.");
      }

      final double theta = (mad * mad) / median;
      final double k = median / theta;
      return new LogGammaDistribution(k, 1 / theta, shift);
    }

    @Override
    public Class<? super LogGammaDistribution> getDistributionClass() {
      return LogGammaDistribution.class;
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

  /**
   * Estimate distribution parameters using the method by Choi and Wette.
   * 
   * A modified algorithm for LogGamma distributions.
   * 
   * Reference:
   * <p>
   * Maximum likelihood estimation of the parameters of the gamma distribution
   * and their bias<br />
   * S. C. Choi, R. Wette<br />
   * in: Technometrics
   * </p>
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has LogGammaDistribution - - estimates
   */
  @Reference(title = "Maximum likelihood estimation of the parameters of the gamma distribution and their bias", authors = "S. C. Choi, R. Wette", booktitle = "Technometrics", url = "http://www.jstor.org/stable/10.2307/1266892")
  public static class ChoiWetteEstimator implements DistributionEstimator<LogGammaDistribution> {
    /**
     * Private constructor.
     */
    private ChoiWetteEstimator() {
      // Do not instantiate - use static class
    }

    @Override
    public <A> LogGammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      final int len = adapter.size(data);
      double shift = Double.MAX_VALUE;
      for (int i = 0; i < len; i++) {
        shift = Math.min(shift, adapter.getDouble(data, i));
      }
      shift -= 1; // So no negative values arise after log
      double meanx = 0, meanlogx = 0;
      for (int i = 0; i < len; i++) {
        final double shifted = adapter.getDouble(data, i) - shift;
        final double val = shifted > 1 ? Math.log(shifted) : 1.;
        final double logx = (val > 0) ? Math.log(val) : meanlogx;
        final double deltax = val - meanx;
        final double deltalogx = logx - meanlogx;
        meanx += deltax / (i + 1.);
        meanlogx += deltalogx / (i + 1.);
      }
      if (!(meanx > 0)) {
        throw new ArithmeticException("Cannot estimate LogGamma distribution with mean ");
      }
      // Initial approximation
      final double logmeanx = Math.log(meanx);
      final double diff = logmeanx - meanlogx;
      double k = (3 - diff + Math.sqrt((diff - 3) * (diff - 3) + 24 * diff)) / (12 * diff);

      // Refine via newton iteration, based on Choi and Wette equation
      while (true) {
        double kdelta = (Math.log(k) - GammaDistribution.digamma(k) - diff) / (1 / k - GammaDistribution.trigamma(k));
        if (Math.abs(kdelta) < 1E-8 || Double.isNaN(kdelta)) {
          break;
        }
        k += kdelta;
      }
      if (!(k > 0)) {
        throw new ArithmeticException("LogGamma estimation failed: k <= 0.");
      }
      // Estimate theta:
      final double theta = k / meanx;
      return new LogGammaDistribution(k, theta, shift);
    }

    @Override
    public Class<? super LogGammaDistribution> getDistributionClass() {
      return LogGammaDistribution.class;
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
      protected ChoiWetteEstimator makeInstance() {
        return CHOI_WETTE_ESTIMATOR;
      }
    }
  }
}
