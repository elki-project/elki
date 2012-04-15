package experimentalcode.students.brusis;

import java.util.Arrays;

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

/**
 * Class that tests two given real-valued data samples on whether they might have originated from the same underlying distribution 
 * using the Kolmogorov-Smirnov test statistic that compares the two empirical cumulative distribution functions
 * 
 * @author Jan Brusis
 *
 */
public class KolmogorovSmirnovTest implements StatisticalTest {

  @Override
  public double deviation(double[] fullSample, double[] conditionalSample) {

    return calculateTestStatistic(fullSample, conditionalSample, calculatePercentages(fullSample), calculatePercentages(conditionalSample));
  }

  /**
   * For every value x in the sample, calculates the percentage of values <= x
   * @param sample array with the sample values
   * @return array with the percentages
   */
  public double[] calculatePercentages(double[] sample) {
    Arrays.sort(sample);
    int size = sample.length;
    double[] F = new double[size];
    double x = sample[0];

    for(int i = 0; i < size; i++) {
      x = sample[i];
      int count = 1;
      for(int j = i + 1; j < size; j++) {
        if(sample[j] == x) {
          count++;
        }
        else
          break;
      }

      for(int j = 0; j < count; j++) {
        if(i == 0) {
          F[j] = ((double) count) / size;
        }
        else if(j == 0) {
          F[i] = ((double) count) / size + F[i - 1];
        }
        else {
          F[j + i] = F[j + i - 1];
        }
      }
      i = count + i - 1;
    }

    return F;
  }

  /**
   * Calculates the maximum distance between the two empirical CDFs of two data samples
   * @param sample1 first data sample
   * @param sample2 second data sample
   * @param f1 array of percentages for first sample
   * @param f2 array of percentages for second sample
   * @return the largest distance between both functions
   */
  public double calculateTestStatistic(double[] sample1, double[] sample2, double[] f1, double[] f2) {

    int index1 = 0;
    int index2 = 0;
    double maximum = f1[index1];

    for(index1 = 0; index1 < sample1.length; index1++) {

      if(index2 == sample2.length) {
        maximum = Math.max(maximum, Math.abs(f1[index1] - 1));
      }
      else {
        maximum = Math.max(maximum, Math.abs(f1[index1] - f2[index2]));
      }

      for(int j = index2; j < sample2.length; j++) {
        if(index1 == sample1.length - 1) {
          maximum = Math.max(maximum, Math.abs(f2[j] - 1));
          continue;
        }
        if(sample2[j] < sample1[index1 + 1]) {
          maximum = Math.max(maximum, Math.abs(f1[index1] - f2[j]));
          index2++;
        }
        else {
          if(index2 == 0) {
            break;
          }
          index2--;
          break;
        }
      }
    }

    return maximum;
  }

//  public static void main(String... args) {
//    double[] x1 = { 15, 17, 5, 10, 18 };
//    double[] x2 = { 6, 7, 13, 9, 6, 9 };
//    double[] x3 = { -2.209, -1.900, -0.782, -0.592, -0.419, -0.148, 0.040, 0.348, 0.602, 1.204 };
//    double[] x4 = { -1.093, -1.089, -0.983, -0.359, -0.044, 0.084, 0.119, 0.226, 0.431, 1.221, 1.247, 1.533 };
//
//    KolmogorovSmirnovTest test = new KolmogorovSmirnovTest();
//    double[] F1 = test.calculatePercentages(x1);
//    double[] F2 = test.calculatePercentages(x2);
//    double[] F3 = test.calculatePercentages(x3);
//    double[] F4 = test.calculatePercentages(x4);
//
//    System.out.println("D: " + test.calculateTestStatistic(x4, x3, F4, F3));
//  }

}
