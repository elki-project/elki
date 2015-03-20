package experimentalcode.erich.intrinsicdimensionality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.GEDEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.HillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.LMomentsEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.MLEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.MOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.PWMEstimator;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

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

public class TestIntrinsicDimensionality {
  static int startk = 2, maxk = 10, samples = 1000, dim = 5;

  public static void main(String[] args) {
    Random rnd = new Random();
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
      System.out.format(Locale.ROOT, " %10s %10s", abbreviat.get(i) + "-Avg", abbreviat.get(i) + "-Dev");
    }
    System.out.append(FormatUtil.NEWLINE);
    double[][] dists = new double[samples][maxk + 1];
    final double e = 1. / dim;
    for(int p = 0; p < samples; p++) {
      for(int i = 0; i <= maxk; i++) {
        dists[p][i] = Math.pow(rnd.nextDouble(), e);
      }
      Arrays.sort(dists[p]);
    }
    PartialArrayAdapter ad = new PartialArrayAdapter();
    double[][] v = new double[estimators.size()][samples];
    for(int l = startk; l <= maxk; l++) {
      String kstr = String.format("%0" + digits + "d", l);
      ad.max = l;
      for(int p = 0; p < samples; p++) {
        for(int i = 0; i < estimators.size(); i++) {
          v[i][p] = estimators.get(i).estimate(dists[p], ad);
        }
      }
      System.out.append(kstr);
      for(int i = 0; i < estimators.size(); i++) {
        double mean = median(v[i]); // samples / sum[i];
        double std = mad(v[i], mean);
        System.out.format(Locale.ROOT, " %10f %10f", mean, std);
      }
      System.out.append(FormatUtil.NEWLINE);
    }
  }

  static class PartialArrayAdapter implements NumberArrayAdapter<Double, double[]> {
    int max;

    @Override
    public int size(double[] array) {
      return max;
    }

    @Override
    public Double get(double[] array, int off) throws IndexOutOfBoundsException {
      return array[off];
    }

    @Override
    public double getDouble(double[] array, int off) throws IndexOutOfBoundsException {
      return array[off];
    }

    @Override
    public float getFloat(double[] array, int off) throws IndexOutOfBoundsException {
      return (float) array[off];
    }

    @Override
    public int getInteger(double[] array, int off) throws IndexOutOfBoundsException {
      return (int) array[off];
    }

    @Override
    public short getShort(double[] array, int off) throws IndexOutOfBoundsException {
      return (short) array[off];
    }

    @Override
    public long getLong(double[] array, int off) throws IndexOutOfBoundsException {
      return (long) array[off];
    }

    @Override
    public byte getByte(double[] array, int off) throws IndexOutOfBoundsException {
      return (byte) array[off];
    }
  }

  private static double median(double[] v) {
    return QuickSelect.median(v);
  }

  private static double mean(double[] v) {
    double sum = 0.;
    for(double val : v) {
      sum += val;
    }
    return sum / v.length;
  }

  private static double gmean(double[] v) {
    double sum = 0.;
    for(double val : v) {
      sum += 1. / val;
    }
    return v.length / sum;
  }

  private static double stddev(double[] v, double mean, int bias) {
    double sum = 0.;
    for(double val : v) {
      val = val - mean;
      sum += val * val;
    }
    sum /= (v.length - bias);
    return (sum > 0) ? Math.sqrt(sum) : 0.;
  }

  private static double avdev(double[] v, double mean, int bias) {
    double sum = 0.;
    for(double val : v) {
      val = val - mean;
      sum += Math.abs(val);
    }
    sum /= (v.length - bias);
    return sum;
  }

  private static double mad(double[] v, double mean) {
    double[] devs = new double[v.length];
    for(int i = 0; i < v.length; i++) {
      devs[i] = Math.abs(v[i] - mean);
    }
    return QuickSelect.median(devs);
  }

}
