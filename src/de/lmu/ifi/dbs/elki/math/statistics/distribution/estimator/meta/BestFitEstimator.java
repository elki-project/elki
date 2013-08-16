package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.CauchyMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.EMGOlivierNorbergEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialMedianEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GeneralizedExtremeValueLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GumbelLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GumbelMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LMOMDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LaplaceLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LaplaceMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogGammaLogMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogGammaLogMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogMADDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogMOMDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalBilkovaLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalLogMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalLogMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogisticLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogisticMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.MADDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.MOMDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.SkewGNormalLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformEnhancedMinMaxEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformMinMaxEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.WeibullLMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.WeibullLogMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A meta estimator that will try a number of (inexpensive) estimations, then
 * choose whichever works best.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf MOMDistributionEstimator
 * @apiviz.composedOf MADDistributionEstimator
 * @apiviz.composedOf LMOMDistributionEstimator
 * @apiviz.composedOf LogMOMDistributionEstimator
 * @apiviz.composedOf LogMADDistributionEstimator
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
  private Collection<MOMDistributionEstimator<?>> momests;

  /**
   * Median average deviation from median estimators.
   */
  private Collection<MADDistributionEstimator<?>> madests;

  /**
   * L-Moment estimators.
   */
  private Collection<LMOMDistributionEstimator<?>> lmomests;

  /**
   * Logspace Method of Moments estimators.
   */
  private Collection<LogMOMDistributionEstimator<?>> logmomests;

  /**
   * Logspace Median average deviation from median estimators.
   */
  private Collection<LogMADDistributionEstimator<?>> logmadests;

  /**
   * Constructor. Use static instance instead!
   */
  protected BestFitEstimator() {
    super();
    momests = new ArrayList<>(4);
    momests.add(NormalMOMEstimator.STATIC);
    momests.add(GammaMOMEstimator.STATIC);
    momests.add(ExponentialMOMEstimator.STATIC);
    momests.add(EMGOlivierNorbergEstimator.STATIC);
    madests = new ArrayList<>(9);
    madests.add(NormalMADEstimator.STATIC);
    madests.add(GammaMADEstimator.STATIC);
    madests.add(ExponentialMADEstimator.STATIC);
    madests.add(ExponentialMedianEstimator.STATIC);
    madests.add(LaplaceMADEstimator.STATIC);
    madests.add(GumbelMADEstimator.STATIC);
    madests.add(CauchyMADEstimator.STATIC);
    madests.add(LogisticMADEstimator.STATIC);
    madests.add(UniformMADEstimator.STATIC);
    lmomests = new ArrayList<>(12);
    lmomests.add(NormalLMOMEstimator.STATIC);
    lmomests.add(GammaLMOMEstimator.STATIC);
    lmomests.add(ExponentialLMOMEstimator.STATIC);
    lmomests.add(LaplaceLMOMEstimator.STATIC);
    lmomests.add(GumbelLMOMEstimator.STATIC);
    lmomests.add(LogisticLMOMEstimator.STATIC);
    lmomests.add(LogNormalLMOMEstimator.STATIC);
    lmomests.add(LogNormalBilkovaLMOMEstimator.STATIC);
    lmomests.add(SkewGNormalLMOMEstimator.STATIC);
    lmomests.add(GeneralizedExtremeValueLMOMEstimator.STATIC);
    lmomests.add(WeibullLMOMEstimator.STATIC);
    lmomests.add(UniformLMOMEstimator.STATIC);
    logmomests = new ArrayList<>(2);
    logmomests.add(LogNormalLogMOMEstimator.STATIC);
    logmomests.add(LogGammaLogMOMEstimator.STATIC);
    logmadests = new ArrayList<>(3);
    logmadests.add(LogNormalLogMADEstimator.STATIC);
    logmadests.add(LogGammaLogMADEstimator.STATIC);
    logmadests.add(WeibullLogMADEstimator.STATIC);
  }

  @Override
  public <A> Distribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    int numlmom = 0;
    for (LMOMDistributionEstimator<?> est : lmomests) {
      numlmom = Math.max(numlmom, est.getNumMoments());
    }

    final int len = adapter.size(data);

    // Build various statistics:
    StatisticalMoments mom = new StatisticalMoments(), logmom = new StatisticalMoments();
    double[] x = new double[len], scratch = new double[len], logx = new double[len];

    if (LOG.isVeryVerbose()) {
      LOG.veryverbose("Computing statistical moments and L-Moments.");
    }
    for (int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      x[i] = val;
      mom.put(val);
    }
    // Sort: for L-Moments, but getting the median is now also cheap.
    Arrays.sort(x);
    double[] lmom = (numlmom > 0) ? ProbabilityWeightedMoments.samLMR(x, ArrayLikeUtil.DOUBLEARRAYADAPTER, numlmom) : null;
    final double min = x[0], median = .5 * (x[len >> 1] + x[(len + 1) >> 1]), max = x[len - 1];
    if (LOG.isVeryVerbose()) {
      LOG.veryverbose("Computing statistical moments in logspace.");
    }
    // Build logspace copy:
    double shift = Math.min(0., min - (max - min) * 1e-10);
    for (int i = 0; i < len; i++) {
      double val = x[i] - shift;
      val = val > 0. ? Math.log(val) : Double.NEGATIVE_INFINITY;
      logx[i] = val;
      if (!Double.isInfinite(val) && !Double.isNaN(val)) {
        logmom.put(val);
      }
    }
    double logmedian = .5 * (logx[len >> 1] + logx[(len + 1) >> 1]);
    if (LOG.isVeryVerbose()) {
      LOG.veryverbose("Computing MADs.");
    }
    double mad = computeMAD(x, median, scratch, len);
    double logmad = computeMAD(logx, logmedian, scratch, len);

    Distribution best = null;
    double bestscore = Double.POSITIVE_INFINITY;
    DistributionEstimator<?> bestest = null;

    final int numest = momests.size() + madests.size() + lmomests.size() + logmomests.size() + logmadests.size() + 2;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Estimating distribution.", numest, LOG) : null;
    for (MOMDistributionEstimator<?> est : momests) {
      try {
        Distribution d = est.estimateFromStatisticalMoments(mom);
        double score = testFit(x, scratch, d);
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if (score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      } catch (ArithmeticException e) {
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    for (MADDistributionEstimator<?> est : madests) {
      try {
        Distribution d = est.estimateFromMedianMAD(median, mad);
        double score = testFit(x, scratch, d);
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if (score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      } catch (ArithmeticException e) {
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    for (LMOMDistributionEstimator<?> est : lmomests) {
      try {
        Distribution d = est.estimateFromLMoments(lmom);
        double score = testFit(x, scratch, d);
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if (score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      } catch (ArithmeticException e) {
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    for (LogMOMDistributionEstimator<?> est : logmomests) {
      try {
        Distribution d = est.estimateFromLogStatisticalMoments(logmom, shift);
        double score = testFit(x, scratch, d);
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if (score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      } catch (ArithmeticException e) {
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    for (LogMADDistributionEstimator<?> est : logmadests) {
      try {
        Distribution d = est.estimateFromLogMedianMAD(logmedian, logmad, shift);
        double score = testFit(x, scratch, d);
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if (score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      } catch (ArithmeticException e) {
        if (LOG.isVeryVerbose()) {
          LOG.veryverbose("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    { // Uniform estimators.
      final UniformMinMaxEstimator est = UniformMinMaxEstimator.STATIC;
      Distribution d = est.estimate(min, max);
      double score = testFit(x, scratch, d);
      if (LOG.isVeryVerbose()) {
        LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
      }
      if (score < bestscore) {
        best = d;
        bestscore = score;
        bestest = est;
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    { // Uniform estimators.
      final UniformEnhancedMinMaxEstimator est = UniformEnhancedMinMaxEstimator.STATIC;
      Distribution d = est.estimate(min, max, len);
      double score = testFit(x, scratch, d);
      if (LOG.isVeryVerbose()) {
        LOG.veryverbose(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
      }
      if (score < bestscore) {
        best = d;
        bestscore = score;
        bestest = est;
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
    }

    if (LOG.isVerbose()) {
      LOG.verbose("Best distribution fit: " + bestscore + " " + best.toString() + " via " + bestest);
    }

    return best;
  }

  public double computeMAD(double[] data, double median, double[] scratch, final int len) {
    // Compute LogMAD:
    for (int i = 0; i < len; i++) {
      scratch[i] = Math.abs(data[i] - median);
    }
    double logmad = QuickSelect.median(scratch);
    // Adjust LogMAD if 0:
    if (!(logmad > 0.)) {
      double xmin = Double.POSITIVE_INFINITY;
      for (int i = (len >> 1); i < len; i++) {
        if (scratch[i] > 0. && scratch[i] < xmin) {
          xmin = scratch[i];
        }
      }
      if (!Double.isInfinite(xmin)) {
        logmad = xmin;
      }
    }
    return logmad;
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
  private double testFit(double[] x, double[] test, Distribution dist) throws ArithmeticException {
    for (int i = 0; i < test.length; i++) {
      test[i] = dist.cdf(x[i]);
      if (Double.isNaN(test[i])) {
        throw new ArithmeticException("Got NaN after fitting " + dist.toString());
      }
      if (Double.isInfinite(test[i])) {
        throw new ArithmeticException("Got infinite value after fitting " + dist.toString());
      }
    }
    Arrays.sort(test);
    return KolmogorovSmirnovTest.simpleTest(test, 0., 1.);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected BestFitEstimator makeInstance() {
      return STATIC;
    }
  }
}
