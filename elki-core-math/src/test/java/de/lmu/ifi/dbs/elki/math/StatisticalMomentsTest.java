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
package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test to check our statistical moments class for correctness.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class StatisticalMomentsTest {
  @Test
  public void testTiny() {
    double[] data = new double[] { 222, 1122, 45444 };
    StatisticalMoments m = new StatisticalMoments();
    for(double d : data) {
      m.put(d);
    }
    // Validated with R package "e1071" and scipy.
    // Looks as we might be closer to scipys result!
    // skewness(c(222,1122,45444), type = 1) = 0.70614285367114104
    // scipy.stats.skew([222, 1122, 45444], bias=True) = 0.7061428536711412
    assertEquals("Naive skewness does not match.", 0.70614285367114104, m.getNaiveSkewness(), 1e-15);
    // skewness(c(222,1122,45444), type = 2) = 1.7296896770071024
    // scipy.stats.skew([222, 1122, 45444], bias=False) = 1.7296896770071026
    assertEquals("Sample skewness does not match.", 1.7296896770071009, m.getSampleSkewness(), 1e-15);
    // Type 3 skewness is not in ELKI.

    // kurtosis(c(222,1122,45444), type = 1) = -1.5
    assertEquals("Naive kurtosis does not match.", -1.5, m.getNaiveExcessKurtosis(), 1e-15);
    assertEquals("Naive kurtosis does not match.", -1.5 + 3.0, m.getNaiveKurtosis(), 1e-15);
    // type = 2 needs 4 observations - so do we!

    assertEquals("No toString", -1, m.toString().indexOf('@'));
  }

  @Test
  public void testGamma() {
    double[] data = new double[] { // 100 Gamma(5,5) distributed random values.
        0.6388530607016163, 0.9782442336324599, 0.8882176966052615, 0.7849646645745156, 2.0633275427120497, //
        0.9545195282320902, 1.092390620469477, 0.7258265676258062, 1.378026049091643, 0.8310918468931533, //
        0.9495339766440013, 0.7126363476416752, 0.5608700366395489, 0.31408795273142415, 1.4909322345449254, //
        0.46625313007571856, 1.1870054487101156, 0.7640843133327799, 2.235582041645692, 1.5812487824715005, //
        1.512895069952923, 0.606517358771931, 1.1961880366469766, 0.7854465499137, 0.8084781536051177, //
        0.7615405607968533, 1.6614435713861262, 1.4869842072109933, 0.43465864880894045, 2.007341464773332, //
        0.33478515273215137, 1.9093706181977907, 0.3539177300096471, 0.44888559667524086, 1.0249621186169293, //
        0.38784905783431306, 0.7632290555534638, 0.9941038353388855, 0.6358745449186729, 0.5750658437507069, //
        0.9565742499910938, 1.2221328658541637, 0.9389920544737386, 1.21461175894598, 1.5713316877580166, //
        0.3470088915002977, 0.4419291224056868, 2.343465732285217, 1.0612049261527454, 0.9960347794653911, //
        0.5433216826433932, 1.006345176861023, 1.034059277904943, 0.7199916430119295, 1.218878192200847, //
        1.0023101563986212, 1.093784991451296, 0.9748868952634213, 1.81732410812918, 0.5259404003069172, //
        1.5179842038671814, 0.7461497981977685, 0.47530253061596, 0.810558795121139, 0.9383514512691828, //
        0.42371144806092004, 0.8698616597894346, 0.4541860955288436, 0.3136304450065802, 0.8799371863836344, //
        0.9854734113277808, 0.8651302802119106, 0.7981146204118504, 0.5255964404563404, 0.8570668629471919, //
        1.0294303406520284, 1.4592029352371467, 1.1034285928922425, 1.0042662401564635, 0.6443339212869743, //
        1.4096313764251664, 0.9328112988095885, 2.0874718257265483, 1.063702918067901, 0.5885221900074733, //
        1.216856018361022, 0.5572986786142953, 0.6620626132872955, 0.7357210048560157, 1.5174141129183254, //
        1.2293542801457167, 1.490026433254134, 1.4307462549666328, 1.381520418446136, 0.48400651328811123, //
        0.8448303085270435, 1.1514319532981445, 0.8944580168473978, 0.5003052782124537, 1.1457096934062219 };
    StatisticalMoments m = new StatisticalMoments();
    for(double d : data) {
      m.put(d);
    }
    // Validated with R package "e1071" and scipy:
    // Looks as we might be closer to scipys result!
    // skewness(data, type = 1) = 0.8553563631726689
    // scipy.stats.skew(data, bias=True) = 0.8553563631726712
    assertEquals("Naive skewness does not match.", 0.8553563631726689, m.getNaiveSkewness(), 1e-14);
    // skewness(data, type = 2) = 0.868437587353074
    // scipy.stats.skew(data, bias=False) = 0.868437587353077
    assertEquals("Sample skewness does not match.", 0.868437587353074, m.getSampleSkewness(), 1e-14);
    // Type 3 skewness is not in ELKI.

    // kurtosis(data, type = 1) = 0.4778717586207377
    // scipy.stats.kurtosis(data, fisher=True) = 0.4778717586207377
    assertEquals("Naive kurtosis does not match.", 0.4778717586207377, m.getNaiveExcessKurtosis(), 1e-14);
    assertEquals("Naive kurtosis does not match.", 0.4778717586207377 + 3.0, m.getNaiveKurtosis(), 1e-14);
    // kurtosis(data, type = 2) = 0.565141985530060675
    // scipy.stats.kurtosis(data, fisher=True, bias=False) = 0.5651419855300603
    // scipy.stats.kurtosis(data, fisher=True, bias=False) = 0.5651419855300603
    // scipy.stats.kurtosis(data, fisher=False, bias=False) = 3.5651419855300603
    assertEquals("Sample kurtosis does not match.", 0.565141985530060675, m.getSampleExcessKurtosis(), 1e-14);
    assertEquals("Sample kurtosis does not match.", 3.565141985530060675, m.getSampleKurtosis(), 1e-14);

    // numpy.array(data).mean() = 0.98342960290360215
    assertEquals("Mean does not match.", 0.98342960290360215, m.getMean(), 1e-15);
    // numpy.array(data).std() = 0.44953213330440578
    assertEquals("Naive stddev does not match.", 0.44953213330440578, m.getNaiveStddev(), 1e-15);
    // numpy.array(data).std(ddof=1) = 0.45179679314507293
    assertEquals("Sample stddev does not match.", 0.45179679314507293, m.getSampleStddev(), 1e-15);
    // numpy.array(data).min() = 0.31363044500658022
    assertEquals("Min does not match.", 0.31363044500658022, m.getMin(), 1e-16);
    // numpy.array(data).max() = 2.343465732285217
    assertEquals("Max does not match.", 2.343465732285217, m.getMax(), 1e-16);
  }

  // TODO: Actually test the moments...
  @Test
  public void combine() {
    StatisticalMoments m1 = new StatisticalMoments();
    StatisticalMoments m2 = new StatisticalMoments();
    m1.put(new double[] { 1, 2, 3 });
    m2.put(new double[] { 4, 5, 6, 7 });
    StatisticalMoments m3 = new StatisticalMoments(m1);
    m3.put(m2);
    assertEquals("First mean", 2, m1.getMean(), 0.);
    assertEquals("First std", 1, m1.getSampleStddev(), 0.);
    assertEquals("First skew", 0, m1.getNaiveSkewness(), 0.);
    assertEquals("First kurt", 1.5, m1.getNaiveKurtosis(), 0.);
    assertEquals("First kurt", -1.5, m1.getNaiveExcessKurtosis(), 0.);
    assertEquals("Second mean", 5.5, m2.getMean(), 0.);
    assertEquals("Second std", Math.sqrt(1.25), m2.getNaiveStddev(), 0.);
    assertEquals("Second skew", 0, m2.getNaiveSkewness(), 0.);
    assertEquals("Second kurt", 1.64, m2.getNaiveKurtosis(), 0.);
    assertEquals("Third mean", 4, m3.getMean(), 0.);
    assertEquals("Third std", 4., m3.getNaiveVariance(), 0.);
    assertEquals("Third skew", 0, m3.getNaiveSkewness(), 0.);
    assertEquals("Third kurt", 1.75, m3.getNaiveKurtosis(), 0.);
    // m2.put(new double[] { 1, 2, 3 }, new double[] { 4, 2, 1 });
    m2.put(new double[] { 1, 2, 3 }, new double[] { 4, 2, 1 });
    assertEquals("Fourth mean", 3.0, m2.getMean(), 0);
    assertEquals("Fourth weight", 11, m2.getCount(), 0);
    assertEquals("Fourth stddev", 4.8, m2.getSampleVariance(), 0);
    assertEquals("Fourth skew", 0.658231136582482, m2.getNaiveSkewness(), 1e-15);
    assertEquals("Fourth kurt", 6.015625 / 3, m2.getNaiveKurtosis(), 0.);

    m2.reset();
    assertEquals("Reset", 0., m2.getCount(), 0);
  }

  public void testEmpty1() {
    assertTrue(Double.isNaN(new StatisticalMoments().put(new double[0]).getNaiveSkewness()));
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty2() {
    new StatisticalMoments().put(new double[0]).getSampleSkewness();
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty3() {
    new StatisticalMoments().put(new double[0]).getNaiveExcessKurtosis();
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty4() {
    new StatisticalMoments().put(new double[0]).getSampleExcessKurtosis();
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty5() {
    new StatisticalMoments().put(new double[0]).getNaiveKurtosis();
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty6() {
    new StatisticalMoments().put(new double[0]).getSampleKurtosis();
  }
}
