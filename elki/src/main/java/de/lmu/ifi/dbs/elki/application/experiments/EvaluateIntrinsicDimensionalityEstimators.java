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
package de.lmu.ifi.dbs.elki.application.experiments;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.AggregatedHillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.GEDEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.HillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.LMomentsEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.MOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.PWM2Estimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.PWMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.RVEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.ZipfEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Class for testing the estimation quality of intrinsic dimensionality
 * estimators.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class EvaluateIntrinsicDimensionalityEstimators extends AbstractApplication {
  /**
   * Benchmark parameters.
   */
  int startk = 3, maxk = 10, samples = 1000, dim = 5;

  /**
   * Aggregation method.
   */
  Aggregate agg;

  /**
   * Output format parameter.
   */
  OutputFormat format;

  /**
   * Random generator.
   */
  RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param startk Start value of k
   * @param maxk Maximum value of k
   * @param samples Number of samples
   * @param dim Number of dimensions
   * @param agg Aggregation method
   * @param format Output format
   * @param rnd Random seed.
   */
  public EvaluateIntrinsicDimensionalityEstimators(int startk, int maxk, int samples, int dim, Aggregate agg, OutputFormat format, RandomFactory rnd) {
    this.startk = startk;
    this.maxk = maxk;
    this.samples = samples;
    this.dim = dim;
    this.agg = agg;
    this.format = format;
    this.rnd = rnd;
  }

  @Override
  public void run() {
    ArrayList<String> abbreviat = new ArrayList<>();
    ArrayList<IntrinsicDimensionalityEstimator> estimators = new ArrayList<>();
    // Hill estimator
    abbreviat.add("Hill");
    estimators.add(HillEstimator.STATIC);
    // Method of Moments
    abbreviat.add("MoM");
    estimators.add(MOMEstimator.STATIC);
    // Regularly varying functions estimator.
    abbreviat.add("RV");
    estimators.add(RVEstimator.STATIC);
    // Aggregated hill estimator
    abbreviat.add("AggHi");
    estimators.add(AggregatedHillEstimator.STATIC);
    // Zipf estimator (qq-estimator)
    abbreviat.add("Zipf");
    estimators.add(ZipfEstimator.STATIC);
    // Generalized expansion dimension
    abbreviat.add("GED");
    estimators.add(GEDEstimator.STATIC);
    // L-Moments based
    abbreviat.add("LMM");
    estimators.add(LMomentsEstimator.STATIC);
    // Probability weighted moments, using first moment only.
    abbreviat.add("PWM");
    estimators.add(PWMEstimator.STATIC);
    // Probability weighted moments, using second moment.
    abbreviat.add("PWM2");
    estimators.add(PWM2Estimator.STATIC);

    PrintStream out = System.out; // TODO: add output file parameter?
    final int digits = (int) FastMath.ceil(FastMath.log10(maxk + 1));
    switch(format){
    case TABULAR:
      out.append(String.format("%" + digits + "s", "k"));
      for(int i = 0; i < estimators.size(); i++) {
        for(String postfix : agg.description()) {
          out.format(Locale.ROOT, " %10s", abbreviat.get(i) + "-" + postfix);
        }
      }
      out.append(FormatUtil.NEWLINE);
      break;
    case TSV:
      out.append("k");
      for(int i = 0; i < estimators.size(); i++) {
        for(String postfix : agg.description()) {
          out.append('\t').append(abbreviat.get(i)).append('-').append(postfix);
        }
      }
      out.append(FormatUtil.NEWLINE);
      break;
    }
    double[][] v = new double[estimators.size()][samples];
    for(int l = startk; l <= maxk; l++) {
      for(int p = 0; p < samples; p++) {
        // Prefer independent samples.
        double[] dists = makeSample(l);
        for(int i = 0; i < estimators.size(); i++) {
          IntrinsicDimensionalityEstimator est = estimators.get(i);
          v[i][p] = est.estimate(dists, l);
        }
      }
      switch(format){
      case TABULAR:
        out.append(String.format("%0" + digits + "d", l));
        for(int i = 0; i < estimators.size(); i++) {
          for(double val : agg.aggregate(v[i])) {
            out.format(Locale.ROOT, " %10f", val);
          }
        }
        out.append(FormatUtil.NEWLINE);
        break;
      case TSV:
        out.append(FormatUtil.NF.format(l));
        for(int i = 0; i < estimators.size(); i++) {
          for(double val : agg.aggregate(v[i])) {
            out.append('\t');
            out.append(FormatUtil.NF.format(val));
          }
        }
        out.append(FormatUtil.NEWLINE);
        break;
      }
    }
  }

  /**
   * Generate a data sample.
   *
   * @param maxk Number of entries.
   * @return Data sample
   */
  protected double[] makeSample(int maxk) {
    final Random rnd = this.rnd.getSingleThreadedRandom();
    double[] dists = new double[maxk + 1];
    final double e = 1. / dim;
    for(int i = 0; i <= maxk; i++) {
      dists[i] = FastMath.pow(rnd.nextDouble(), e);
    }
    Arrays.sort(dists);
    return dists;
  }

  /**
   * Output format
   *
   * @author Erich Schubert
   */
  enum OutputFormat {
    TABULAR, TSV
  }

  /**
   * Aggregation methods.
   *
   * @author Erich Schubert
   */
  enum Aggregate {
    /**
     * Aggregate using the mean only.
     */
    MEAN {
      @Override
      double[] aggregate(double[] data) {
        double avg = 0.;
        for(double val : data) {
          avg += val;
        }
        return new double[] { avg / data.length };
      }

      @Override
      String[] description() {
        return new String[] { "Mean" };
      }
    },
    /**
     * Aggregate as mean and standard deviation.
     */
    MEAN_STDDEV {
      @Override
      double[] aggregate(double[] data) {
        double avg = 0.;
        for(double val : data) {
          avg += val;
        }
        avg /= data.length;
        double sqsum = 0.;
        for(double val : data) {
          double v = val - avg;
          sqsum += v * v;
        }
        sqsum /= data.length;
        return new double[] { avg, FastMath.sqrt(sqsum) };
      }

      @Override
      String[] description() {
        return new String[] { "Mean", "Stddev" };
      }
    },
    /**
     * Aggregate as mean and standard deviation.
     */
    MEAN_STDDEV_MIN_MAX {
      @Override
      double[] aggregate(double[] data) {
        double avg = 0.;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for(double val : data) {
          avg += val;
          min = (val < min) ? val : min;
          max = (val > max) ? val : max;
        }
        avg /= data.length;
        double sqsum = 0.;
        for(double val : data) {
          double v = val - avg;
          sqsum += v * v;
        }
        sqsum /= data.length;
        return new double[] { avg, FastMath.sqrt(sqsum), min, max };
      }

      @Override
      String[] description() {
        return new String[] { "Mean", "Stddev", "Min", "Max" };
      }
    },
    /**
     * Harmonic mean.
     */
    HMEAN {
      @Override
      double[] aggregate(double[] data) {
        double avg = 0.;
        for(double val : data) {
          avg += 1. / val;
        }
        return new double[] { data.length / avg };
      }

      @Override
      String[] description() {
        return new String[] { "HMean" };
      }
    },
    /**
     * Aggregate using median.
     */
    MEDIAN {
      @Override
      double[] aggregate(double[] data) {
        double med = QuickSelect.median(data);
        return new double[] { med };
      }

      @Override
      String[] description() {
        return new String[] { "Median" };
      }
    },
    /**
     * Aggregate using median and MAD.
     */
    MED_MAD {
      @Override
      double[] aggregate(double[] data) {
        double med = QuickSelect.median(data);
        double[] devs = new double[data.length];
        for(int i = 0; i < data.length; i++) {
          devs[i] = Math.abs(data[i] - med);
        }
        double mad = QuickSelect.median(devs);
        return new double[] { med, mad };
      }

      @Override
      String[] description() {
        return new String[] { "Med", "Mad" };
      }
    },
    /**
     * Aggregate using median and MAD.
     */
    MED_MAD_MIN_MAX {
      @Override
      double[] aggregate(double[] data) {
        double med = QuickSelect.median(data);
        double[] devs = new double[data.length];
        double min = med, max = med;
        for(int i = 0; i < data.length; i++) {
          double v = data[i];
          min = (v < min) ? v : min;
          max = (v > max) ? v : max;
          devs[i] = Math.abs(v - med);
        }
        double mad = QuickSelect.median(devs);
        return new double[] { med, mad, min, max };
      }

      @Override
      String[] description() {
        return new String[] { "Med", "Mad", "Min", "Max" };
      }
    },
    /**
     * Selected quantiles.
     */
    QUANTILES {
      @Override
      double[] aggregate(double[] data) {
        final double[] quants = { 0, .1, .25, .5, .75, .9, 1. };
        final int l = data.length;
        Arrays.sort(data); // QuickSelect would do, actually
        double[] ret = new double[quants.length];
        for(int i = 0; i < quants.length; i++) {
          final double dleft = (l - 1) * quants[i];
          final int ileft = (int) FastMath.floor(dleft);
          final double err = dleft - ileft;
          if(err < Double.MIN_NORMAL) {
            ret[i] = data[ileft];
          }
          else {
            ret[i] = data[ileft] + (data[ileft + 1] - data[ileft]) * err;
          }
        }
        return ret;
      }

      @Override
      String[] description() {
        return new String[] { "Min", "Q10", "Q25", "Med", "Q75", "Q90", "Max" };
      }
    },
    // Last alternative.
    ;
    /**
     * Aggregate values.
     *
     * @param data Data to aggregate.
     * @return Aggregated values.
     */
    abstract double[] aggregate(double[] data);

    /**
     * Descriptions of the aggregate values.
     *
     * @return Descriptions
     */
    abstract String[] description();
  }

  /**
   * Main method
   */
  public static void main(String[] args) {
    runCLIApplication(EvaluateIntrinsicDimensionalityEstimators.class, args);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Initial neighborhood size.
     */
    public static final OptionID STARTK_ID = new OptionID("mink", "Minimum value of k.");

    /**
     * Final neighborhood size.
     */
    public static final OptionID MAXK_ID = new OptionID("maxk", "Maximum value of k.");

    /**
     * Samples size.
     */
    public static final OptionID SAMPLE_ID = new OptionID("sample", "Sample size for averaging.");

    /**
     * Dimensionality.
     */
    public static final OptionID DIM_ID = new OptionID("dim", "Dimensionality.");

    /**
     * Random seed.
     */
    public static final OptionID SEED_ID = new OptionID("seed", "Random seed.");

    /**
     * Aggregation method.
     */
    public static final OptionID AGGREGATE_ID = new OptionID("aggregation", "Aggregation method.");

    /**
     * Output format.
     */
    public static final OptionID FORMAT_ID = new OptionID("output-format", "Output format (ascii, or tab separated).");

    /**
     * Benchmark parameters.
     */
    int startk = 3, maxk = 10, samples = 1000, dim = 5;

    /**
     * Aggregation method.
     */
    Aggregate agg;

    /**
     * Output format parameter.
     */
    OutputFormat format;

    /**
     * Random generator.
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter startP = new IntParameter(STARTK_ID, 3) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(startP)) {
        startk = startP.intValue();
      }
      IntParameter maxkP = new IntParameter(MAXK_ID, 20) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(maxkP)) {
        maxk = maxkP.intValue();
      }
      IntParameter samplesP = new IntParameter(SAMPLE_ID, 1000) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(samplesP)) {
        samples = samplesP.intValue();
      }
      RandomParameter seedP = new RandomParameter(SEED_ID);
      if(config.grab(seedP)) {
        rnd = seedP.getValue();
      }
      IntParameter dimP = new IntParameter(DIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(dimP)) {
        dim = dimP.intValue();
      }
      EnumParameter<Aggregate> aggP = new EnumParameter<>(AGGREGATE_ID, Aggregate.class, Aggregate.MED_MAD);
      if(config.grab(aggP)) {
        agg = aggP.getValue();
      }
      EnumParameter<OutputFormat> formatP = new EnumParameter<>(FORMAT_ID, OutputFormat.class, OutputFormat.TABULAR);
      if(config.grab(formatP)) {
        format = formatP.getValue();
      }
    }

    @Override
    protected EvaluateIntrinsicDimensionalityEstimators makeInstance() {
      return new EvaluateIntrinsicDimensionalityEstimators(startk, maxk, samples, dim, agg, format, rnd);
    }
  }
}
