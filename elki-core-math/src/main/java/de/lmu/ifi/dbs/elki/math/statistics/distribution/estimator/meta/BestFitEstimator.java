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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.*;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * A meta estimator that will try a number of (inexpensive) estimations, then
 * choose whichever works best.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - MOMDistributionEstimator
 * @assoc - - - MADDistributionEstimator
 * @assoc - - - LMMDistributionEstimator
 * @assoc - - - LogMOMDistributionEstimator
 * @assoc - - - LogMADDistributionEstimator
 */
public class BestFitEstimator implements DistributionEstimator<Distribution> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BestFitEstimator.class);

  /**
   * Static instance.
   */
  public static final BestFitEstimator STATIC = new BestFitEstimator();

  /**
   * Mean and variance based estimators.
   */
  protected Collection<MOMDistributionEstimator<?>> momests;

  /**
   * Median average deviation from median estimators.
   */
  protected Collection<MADDistributionEstimator<?>> madests;

  /**
   * L-Moment estimators.
   */
  protected Collection<LMMDistributionEstimator<?>> lmmests;

  /**
   * Logspace Method of Moments estimators.
   */
  protected Collection<LogMOMDistributionEstimator<?>> logmomests;

  /**
   * Logspace Median average deviation from median estimators.
   */
  protected Collection<LogMADDistributionEstimator<?>> logmadests;

  /**
   * Constructor. Use static instance instead!
   */
  protected BestFitEstimator() {
    super();
    momests = new ArrayList<>(5);
    momests.add(NormalMOMEstimator.STATIC);
    momests.add(GammaMOMEstimator.STATIC);
    momests.add(InverseGaussianMOMEstimator.STATIC);
    momests.add(ExponentialMOMEstimator.STATIC);
    momests.add(EMGOlivierNorbergEstimator.STATIC);
    madests = new ArrayList<>(10);
    madests.add(NormalMADEstimator.STATIC);
    madests.add(ExponentialMADEstimator.STATIC);
    madests.add(ExponentialMedianEstimator.STATIC);
    madests.add(LaplaceMADEstimator.STATIC);
    madests.add(GumbelMADEstimator.STATIC);
    madests.add(CauchyMADEstimator.STATIC);
    madests.add(LogisticMADEstimator.STATIC);
    madests.add(RayleighMADEstimator.STATIC);
    madests.add(UniformMADEstimator.STATIC);
    lmmests = new ArrayList<>(15);
    lmmests.add(NormalLMMEstimator.STATIC);
    lmmests.add(GammaLMMEstimator.STATIC);
    lmmests.add(ExponentialLMMEstimator.STATIC);
    lmmests.add(LaplaceLMMEstimator.STATIC);
    lmmests.add(GumbelLMMEstimator.STATIC);
    lmmests.add(LogisticLMMEstimator.STATIC);
    lmmests.add(GeneralizedLogisticAlternateLMMEstimator.STATIC);
    lmmests.add(LogNormalLMMEstimator.STATIC);
    lmmests.add(LogNormalBilkovaLMMEstimator.STATIC);
    lmmests.add(SkewGNormalLMMEstimator.STATIC);
    lmmests.add(GeneralizedExtremeValueLMMEstimator.STATIC);
    lmmests.add(GeneralizedParetoLMMEstimator.STATIC);
    lmmests.add(RayleighLMMEstimator.STATIC);
    lmmests.add(WeibullLMMEstimator.STATIC);
    lmmests.add(UniformLMMEstimator.STATIC);
    logmomests = new ArrayList<>(2);
    logmomests.add(LogNormalLogMOMEstimator.STATIC);
    logmomests.add(LogGammaLogMOMEstimator.STATIC);
    logmadests = new ArrayList<>(3);
    logmadests.add(LogNormalLogMADEstimator.STATIC);
    logmadests.add(LogLogisticMADEstimator.STATIC);
    logmadests.add(WeibullLogMADEstimator.STATIC);
  }

  @Override
  public <A> Distribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    int numlmm = 0;
    for(LMMDistributionEstimator<?> est : lmmests) {
      numlmm = Math.max(numlmm, est.getNumMoments());
    }

    final int len = adapter.size(data);

    // Build various statistics:
    StatisticalMoments mom = new StatisticalMoments(),
        logmom = new StatisticalMoments();
    double[] x = new double[len], scratch = new double[len],
        logx = new double[len];

    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Computing statistical moments and L-Moments.");
    }
    for(int i = 0; i < len; i++) {
      final double val = x[i] = adapter.getDouble(data, i);
      if(Double.NEGATIVE_INFINITY < val && val < Double.POSITIVE_INFINITY) {
        mom.put(val);
      }
    }
    if(mom.getMax() <= mom.getMin()) {
      LOG.warning("Constant distribution detected. Cannot fit.");
      return new UniformDistribution(mom.getMin() - 1., mom.getMax() + 1.);
    }
    // Sort: for L-Moments, but getting the median is now also cheap.
    Arrays.sort(x);
    double[] lmm;
    try {
      lmm = (numlmm > 0) ? ProbabilityWeightedMoments.samLMR(x, DoubleArrayAdapter.STATIC, numlmm) : null;
    }
    catch(ArithmeticException e) {
      lmm = null;
    }
    final double min = x[0], median = .5 * (x[len >> 1] + x[(len + 1) >> 1]),
        max = x[len - 1];
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Computing statistical moments in logspace.");
    }
    // Build logspace copy:
    double shift = Math.min(0., min - (max - min) * 1e-10);
    for(int i = 0; i < len; i++) {
      double val = x[i] - shift;
      val = val > 0. ? FastMath.log(val) : Double.NEGATIVE_INFINITY;
      logx[i] = val;
      if(Double.NEGATIVE_INFINITY < val && val < Double.POSITIVE_INFINITY) {
        logmom.put(val);
      }
    }
    double logmedian = .5 * (logx[len >> 1] + logx[(len + 1) >> 1]);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Computing MADs.");
    }
    double mad = MADDistributionEstimator.computeMAD(x, len, median, scratch);
    double logmad = MADDistributionEstimator.computeMAD(logx, len, logmedian, scratch);

    BestFit best = new BestFit(x, scratch);
    for(MOMDistributionEstimator<?> est : momests) {
      try {
        best.test(est, est.estimateFromStatisticalMoments(mom));
      }
      catch(ArithmeticException e) {
        warnIfDebugging(e, est);
      }
    }
    for(MADDistributionEstimator<?> est : madests) {
      try {
        best.test(est, est.estimateFromMedianMAD(median, mad));
      }
      catch(ArithmeticException e) {
        warnIfDebugging(e, est);
      }
    }
    for(LMMDistributionEstimator<?> est : lmmests) {
      if(lmm != null) {
        try {
          best.test(est, est.estimateFromLMoments(lmm));
        }
        catch(ArithmeticException e) {
          warnIfDebugging(e, est);
        }
      }
    }
    for(LogMOMDistributionEstimator<?> est : logmomests) {
      try {
        best.test(est, est.estimateFromLogStatisticalMoments(logmom, shift));
      }
      catch(ArithmeticException e) {
        warnIfDebugging(e, est);
      }
    }
    for(LogMADDistributionEstimator<?> est : logmadests) {
      try {
        best.test(est, est.estimateFromLogMedianMAD(logmedian, logmad, shift));
      }
      catch(ArithmeticException e) {
        warnIfDebugging(e, est);
      }
    }
    { // Uniform estimators.
      final UniformMinMaxEstimator est = UniformMinMaxEstimator.STATIC;
      best.test(est, est.estimate(min, max));
    }
    { // Uniform estimators.
      final UniformEnhancedMinMaxEstimator est = UniformEnhancedMinMaxEstimator.STATIC;
      best.test(est, est.estimate(min, max, len));
    }

    if(LOG.isVeryVerbose()) {
      LOG.veryverbose("Best distribution fit: " + best.score + " " + best.toString() + " via " + best.est);
    }
    return best.dist;
  }

  /**
   * Class to track the best fit.
   *
   * @hidden
   *
   * @author Erich Schubert
   */
  private static class BestFit {
    /**
     * Best match.
     */
    Distribution dist = null;

    /**
     * Best score.
     */
    double score = Double.POSITIVE_INFINITY;

    /**
     * Best estimator.
     */
    DistributionEstimator<?> est = null;

    /**
     * Input data
     */
    private double[] x;

    /**
     * Scratch space
     */
    private double[] scratch;

    /**
     * Constructor.
     *
     * @param x Data
     * @param scratch Scratch array of the same size
     */
    public BestFit(double[] x, double[] scratch) {
      this.x = x;
      this.scratch = scratch;
    }

    /**
     * Test the quality of a fit.
     * 
     * @param x Input data
     * @param test Scratch space for testing (will be overwritten!)
     * @param dist Distribution
     * @return K-S-Test score
     * @throws ArithmeticException
     */
    private static double testFit(double[] x, double[] test, Distribution dist) throws ArithmeticException {
      for(int i = 0; i < test.length; i++) {
        double v = dist.cdf(x[i]);
        if(Double.isNaN(v)) {
          throw new ArithmeticException("Got NaN after fitting " + dist.toString());
        }
        test[i] = (v >= 1.) ? 1 : (v <= 0.) ? 0. : v;
      }
      Arrays.sort(test); // Supposedly not necessary, but to be safe.
      return KolmogorovSmirnovTest.simpleTest(test);
    }

    /**
     * Test the goodness of fit, keep the best.
     * 
     * @param est Estimator
     * @param d Distribution
     */
    public void test(DistributionEstimator<?> est, Distribution d) {
      double score = testFit(x, scratch, d);
      if(LOG.isDebuggingFine()) {
        LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
      }
      if(score < this.score) {
        this.dist = d;
        this.score = score;
        this.est = est;
      }
    }
  }

  /**
   * Warn on arithmetic errors, if debug logging is enabled
   *
   * @param e Error
   * @param est Estimator
   */
  private void warnIfDebugging(ArithmeticException e, DistributionEstimator<?> est) {
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
    }
  }

  @Override
  public Class<? super Distribution> getDistributionClass() {
    return Distribution.class; // No guarantees, sorry.
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
    protected BestFitEstimator makeInstance() {
      return STATIC;
    }
  }
}
