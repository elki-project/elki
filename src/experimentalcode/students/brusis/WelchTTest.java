package experimentalcode.students.brusis;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.StudentsTDistribution;

/**
 * Calculates a test statistic according to Welch's t test for two samples
 * Supplies methods for calculating the degrees of freedom according to the
 * Welch-Satterthwaite Equation. Also directly calculates a two-sided p-value for the
 * underlying t-distribution
 * 
 * @author Jan Brusis
 * 
 */
public class WelchTTest implements StatisticalTest {

  public static void main(String... args) {
//    double[] sample1 = { 498, 510, 505, 495, 491, 488, 493, 501, 502, 501 };
//    double[] sample2 = { 495, 510, 507, 500, 498, 492, 498, 501 };
    double[] sample1 = new double[610];
    double[] sample2 = new double[192];
    try {
      BufferedReader reader = new BufferedReader(new FileReader(args[0]));
      String line = reader.readLine();
      String[] split = line.split("\\s");
      for(int i = 0; i<split.length;i++) {
        sample1[i] = Double.parseDouble(split[i]);
      }
      reader = new BufferedReader(new FileReader(args[1]));
      line = reader.readLine();
      split = line.split("\\s");
      for(int i = 0; i<split.length;i++) {
        sample2[i] = Double.parseDouble(split[i]);
      }
      reader.close();
    }
    catch(FileNotFoundException e) {
      // TODO Auto-generated catch block
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    catch(IOException e) {
      // TODO Auto-generated catch block
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }

    new WelchTTest().deviation(sample1, sample2);
  }

  @Override
  /**
   * The deviation as needed for HiCS
   * Returns the contrast value as defined by HiCS
   */
  public double deviation(double[] sample1, double[] sample2) {
    MeanVariance MV1 = new MeanVariance();
    MeanVariance MV2 = new MeanVariance();

    for(double d : sample1) {
      MV1.put(d);
    }
    for(double d : sample2) {
      MV2.put(d);
    }

    double t = calculateTestStatistic(MV1.getMean(), MV2.getMean(), MV1.getSampleVariance(), MV2.getSampleVariance(), sample1.length, sample2.length);
    int v = calculateDG(MV1.getSampleVariance(), MV2.getSampleVariance(), sample1.length, sample2.length);
//     System.out.println("t : "+t+"\tv: "+v+"\tpVal: "+(1-calculatePValue(t, v)));
    return 1 - calculatePValue(t, v);
  }

  /**
   * Calculates the two-sided p-Value of the underlying t-Distribution with v
   * degrees of freedom
   * 
   * @param t Integration limit
   * @param v Degrees of freedom
   * @return p-Value
   */
  public static double calculatePValue(double t, int v) {
    double pVal;
    if(t > 0) {
      pVal = 2 * (1 - StudentsTDistribution.cdf(t, v));
    }
    else {
      pVal = 2 * (1 - StudentsTDistribution.cdf(-t, v)); // Approximation of CDF
                                                         // only works for
                                                         // positive t
    }
    return pVal;
  }

  /**
   * Calculate the statistic of Welch's t test using statistical moments of the provided data samples
   * 
   * @param mean1
   * @param mean2
   * @param var1
   * @param var2
   * @param size1
   * @param size2
   * @return
   */
  public static double calculateTestStatistic(double mean1, double mean2, double var1, double var2, int size1, int size2) {

//    System.err.println("Mean of sample 1: " + mean1);
//    System.err.println("Mean of sample 2: " + mean2);
//    System.err.println("Variance of sample 1: " + var1);
//    System.err.println("Variance of sample 2: " + var2);
    double t = (mean1 - mean2) / Math.sqrt(var1 / size1 + var2 / size2);
    // System.err.println("Test Statistic: " + t);
    return t;
  }

  /**
   * Calculates the degree of freedom according to Welch-Satterthwaite
   * 
   * @param sample1
   * @param sample2
   * @return
   */
  public static int calculateDG(double var1, double var2, int size1, int size2) {
    int v1 = size1 - 1;
    int v2 = size2 - 1;
    double v = Math.pow((var1 / size1) + (var2 / size2), 2) / ((Math.pow(var1, 2) / (Math.pow(size1, 2) * v1)) + (Math.pow(var2, 2) / (Math.pow(size2, 2) * v2)));
    // System.err.println("Degrees of Freedom: " + (int) v);
    return (int) v;
  }

}
