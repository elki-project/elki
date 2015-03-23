package de.lmu.ifi.dbs.elki.application.experiments;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import java.util.Locale;
import java.util.Random;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.GEDEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.HillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.LMomentsEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.MLEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.MOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.PWMEstimator;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Class for testing the estimation quality of intrinsic dimensionality
 * estimators.
 * 
 * @author Erich Schubert
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
  public void run() throws UnableToComplyException {
    double[][] dists = makeSample();

    ArrayList<IntrinsicDimensionalityEstimator> estimators = new ArrayList<>();
    ArrayList<String> abbreviat = new ArrayList<>();
    estimators.add(GEDEstimator.STATIC);
    abbreviat.add("GED");
    estimators.add(MLEstimator.STATIC);
    abbreviat.add("ML");
    estimators.add(HillEstimator.STATIC);
    abbreviat.add("Hill");
    estimators.add(MOMEstimator.STATIC);
    abbreviat.add("MOM");
    estimators.add(PWMEstimator.STATIC);
    abbreviat.add("PWM");
    estimators.add(LMomentsEstimator.STATIC);
    abbreviat.add("LMM");

    final int digits = (int) Math.ceil(Math.log10(maxk + 1));
    System.out.append(String.format("%" + digits + "s", "k"));
    for(int i = 0; i < estimators.size(); i++) {
      for(String postfix : agg.description()) {
        System.out.format(Locale.ROOT, " %10s", abbreviat.get(i) + "-" + postfix);
      }
    }
    System.out.append(FormatUtil.NEWLINE);
    double[][] v = new double[estimators.size()][samples];
    for(int l = startk; l <= maxk; l++) {
      String kstr = String.format("%0" + digits + "d", l);
      for(int p = 0; p < samples; p++) {
        for(int i = 0; i < estimators.size(); i++) {
          v[i][p] = estimators.get(i).estimate(dists[p], l);
        }
      }
      System.out.append(kstr);
      for(int i = 0; i < estimators.size(); i++) {
        for(double val : agg.aggregate(v[i])) {
          System.out.format(Locale.ROOT, " %10f", val);
        }
      }
      System.out.append(FormatUtil.NEWLINE);
    }
  }

  /**
   * Generate a data sample.
   * 
   * @return Data sample
   */
  protected double[][] makeSample() {
    final Random rnd = this.rnd.getSingleThreadedRandom();
    double[][] dists = new double[samples][maxk + 1];
    final double e = 1. / dim;
    for(int p = 0; p < samples; p++) {
      for(int i = 0; i <= maxk; i++) {
        dists[p][i] = Math.pow(rnd.nextDouble(), e);
      }
      Arrays.sort(dists[p]);
    }
    return dists;
  }

  /**
   * Output format
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static enum OutputFormat {
    TABULAR, TSV
  }

  /**
   * Aggregation methods.
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static enum Aggregate {
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
        return new double[] { avg, Math.sqrt(sqsum) };
      }

      @Override
      String[] description() {
        return new String[] { "Mean", "Stddev" };
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
   *
   * @apiviz.exclude
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
      IntParameter startP = new IntParameter(STARTK_ID, 3);
      if(config.grab(startP)) {
        startk = startP.intValue();
      }
      IntParameter maxkP = new IntParameter(MAXK_ID, 20);
      if(config.grab(maxkP)) {
        maxk = maxkP.intValue();
      }
      IntParameter samplesP = new IntParameter(SAMPLE_ID, 1000);
      if(config.grab(samplesP)) {
        samples = samplesP.intValue();
      }
      RandomParameter seedP = new RandomParameter(SEED_ID);
      if(config.grab(seedP)) {
        rnd = seedP.getValue();
      }
      IntParameter dimP = new IntParameter(DIM_ID);
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
