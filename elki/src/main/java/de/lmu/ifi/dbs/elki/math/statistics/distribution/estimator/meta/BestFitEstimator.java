package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.CauchyMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.EMGOlivierNorbergEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.ExponentialMedianEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GeneralizedExtremeValueLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GeneralizedLogisticAlternateLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GeneralizedParetoLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GumbelLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GumbelMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LMMDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LaplaceLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LaplaceMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogGammaLogMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogGammaLogMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogLogisticMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogMADDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogMOMDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalBilkovaLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalLogMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogNormalLogMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogisticLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.LogisticMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.MADDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.MOMDistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.RayleighLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.RayleighMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.SkewGNormalLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformEnhancedMinMaxEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformLMMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformMADEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformMinMaxEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.InverseGaussianMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.WeibullLMMEstimator;
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
 * @since 0.6.0
 * 
 * @apiviz.uses MOMDistributionEstimator
 * @apiviz.uses MADDistributionEstimator
 * @apiviz.uses LMMDistributionEstimator
 * @apiviz.uses LogMOMDistributionEstimator
 * @apiviz.uses LogMADDistributionEstimator
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
  private Collection<LMMDistributionEstimator<?>> lmmests;

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
    momests = new ArrayList<>(5);
    momests.add(NormalMOMEstimator.STATIC);
    momests.add(GammaMOMEstimator.STATIC);
    momests.add(InverseGaussianMOMEstimator.STATIC);
    momests.add(ExponentialMOMEstimator.STATIC);
    momests.add(EMGOlivierNorbergEstimator.STATIC);
    madests = new ArrayList<>(11);
    madests.add(NormalMADEstimator.STATIC);
    madests.add(GammaMADEstimator.STATIC);
    madests.add(ExponentialMADEstimator.STATIC);
    madests.add(ExponentialMedianEstimator.STATIC);
    madests.add(LaplaceMADEstimator.STATIC);
    madests.add(GumbelMADEstimator.STATIC);
    madests.add(CauchyMADEstimator.STATIC);
    madests.add(LogisticMADEstimator.STATIC);
    madests.add(LogLogisticMADEstimator.STATIC);
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
    logmadests.add(LogGammaLogMADEstimator.STATIC);
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
    StatisticalMoments mom = new StatisticalMoments(), logmom = new StatisticalMoments();
    double[] x = new double[len], scratch = new double[len], logx = new double[len];

    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Computing statistical moments and L-Moments.");
    }
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      x[i] = val;
      mom.put(val);
    }
    if(mom.getMax() <= mom.getMin()) {
      LOG.warning("Constant distribution detected. Cannot fit.");
      return new UniformDistribution(mom.getMin() - 1., mom.getMax() + 1.);
    }
    // Sort: for L-Moments, but getting the median is now also cheap.
    Arrays.sort(x);
    double[] lmm;
    try {
      lmm = (numlmm > 0) ? ProbabilityWeightedMoments.samLMR(x, ArrayLikeUtil.DOUBLEARRAYADAPTER, numlmm) : null;
    }
    catch(ArithmeticException e) {
      lmm = null;
    }
    final double min = x[0], median = .5 * (x[len >> 1] + x[(len + 1) >> 1]), max = x[len - 1];
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Computing statistical moments in logspace.");
    }
    // Build logspace copy:
    double shift = Math.min(0., min - (max - min) * 1e-10);
    for(int i = 0; i < len; i++) {
      double val = x[i] - shift;
      val = val > 0. ? Math.log(val) : Double.NEGATIVE_INFINITY;
      logx[i] = val;
      if(!Double.isInfinite(val) && !Double.isNaN(val)) {
        logmom.put(val);
      }
    }
    double logmedian = .5 * (logx[len >> 1] + logx[(len + 1) >> 1]);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Computing MADs.");
    }
    double mad = computeMAD(x, median, scratch, len);
    double logmad = computeMAD(logx, logmedian, scratch, len);

    Distribution best = null;
    double bestscore = Double.POSITIVE_INFINITY;
    DistributionEstimator<?> bestest = null;

    final int numest = momests.size() + madests.size() + lmmests.size() + logmomests.size() + logmadests.size() + 2;
    FiniteProgress prog = LOG.isDebuggingFine() ? new FiniteProgress("Finding best matching distribution", numest, LOG) : null;
    for(MOMDistributionEstimator<?> est : momests) {
      try {
        Distribution d = est.estimateFromStatisticalMoments(mom);
        double score = testFit(x, scratch, d);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if(score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      LOG.incrementProcessed(prog);
    }
    for(MADDistributionEstimator<?> est : madests) {
      try {
        Distribution d = est.estimateFromMedianMAD(median, mad);
        double score = testFit(x, scratch, d);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if(score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      LOG.incrementProcessed(prog);
    }
    for(LMMDistributionEstimator<?> est : lmmests) {
      if(lmm != null) {
        try {
          Distribution d = est.estimateFromLMoments(lmm);
          double score = testFit(x, scratch, d);
          if(LOG.isDebuggingFine()) {
            LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
          }
          if(score < bestscore) {
            best = d;
            bestscore = score;
            bestest = est;
          }
        }
        catch(ArithmeticException e) {
          if(LOG.isDebuggingFine()) {
            LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
          }
        }
      }
      LOG.incrementProcessed(prog);
    }
    for(LogMOMDistributionEstimator<?> est : logmomests) {
      try {
        Distribution d = est.estimateFromLogStatisticalMoments(logmom, shift);
        double score = testFit(x, scratch, d);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if(score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      LOG.incrementProcessed(prog);
    }
    for(LogMADDistributionEstimator<?> est : logmadests) {
      try {
        Distribution d = est.estimateFromLogMedianMAD(logmedian, logmad, shift);
        double score = testFit(x, scratch, d);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if(score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      LOG.incrementProcessed(prog);
    }
    { // Uniform estimators.
      final UniformMinMaxEstimator est = UniformMinMaxEstimator.STATIC;
      try {
        Distribution d = est.estimate(min, max);
        double score = testFit(x, scratch, d);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if(score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      LOG.incrementProcessed(prog);
    }
    { // Uniform estimators.
      final UniformEnhancedMinMaxEstimator est = UniformEnhancedMinMaxEstimator.STATIC;
      try {
        Distribution d = est.estimate(min, max, len);
        double score = testFit(x, scratch, d);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(est.getClass().getSimpleName() + ": " + score + " " + d.toString());
        }
        if(score < bestscore) {
          best = d;
          bestscore = score;
          bestest = est;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Fitting distribution " + est.getClass().getSimpleName() + " failed: " + e.getMessage());
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    if(LOG.isVeryVerbose()) {
      LOG.veryverbose("Best distribution fit: " + bestscore + " " + best.toString() + " via " + bestest);
    }

    return best;
  }

  public double computeMAD(double[] data, double median, double[] scratch, final int len) {
    // Compute LogMAD:
    for(int i = 0; i < len; i++) {
      scratch[i] = Math.abs(data[i] - median);
    }
    double logmad = QuickSelect.median(scratch);
    // Adjust LogMAD if 0:
    if(!(logmad > 0.)) {
      double xmin = Double.POSITIVE_INFINITY;
      for(int i = (len >> 1); i < len; i++) {
        if(scratch[i] > 0. && scratch[i] < xmin) {
          xmin = scratch[i];
        }
      }
      if(!Double.isInfinite(xmin)) {
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
    for(int i = 0; i < test.length; i++) {
      test[i] = dist.cdf(x[i]);
      if(test[i] > 1.) {
        test[i] = 1.;
      }
      if(test[i] < 0.) {
        test[i] = 0.;
      }
      if(Double.isNaN(test[i])) {
        throw new ArithmeticException("Got NaN after fitting " + dist.toString());
      }
    }
    // Should actually be sorted already...
    Arrays.sort(test);
    return KolmogorovSmirnovTest.simpleTest(test);
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
