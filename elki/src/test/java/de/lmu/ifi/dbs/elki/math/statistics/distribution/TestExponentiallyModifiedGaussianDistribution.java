package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Unit test for the Normal distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 */
public class TestExponentiallyModifiedGaussianDistribution extends AbstractDistributionTest implements JUnit4Test {
  public static final double[] P_CDFPDF = { //
  1e-10, 1e-05, 0.1, 0.1234567, 0.2, 0.2718281828459045, 0.3, 0.3141592653589793, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.718281828459045, 3.141592653589793 //
  };

  public static final double[] GNUR_EXGAUSS_PDF_1_3_05 = { //
  8.47499457587128773861e-02, // 0.000000
  8.47501509788143592150e-02, // 0.000010
  8.67850439453969474402e-02, // 0.100000
  8.72571793172293719287e-02, // 0.123457
  8.87828480833198901978e-02, // 0.200000
  9.01922765508967883008e-02, // 0.271828
  9.07388117440050406826e-02, // 0.300000
  9.10121196636441698313e-02, // 0.314159
  9.26484294233294591869e-02, // 0.400000
  9.45072549271919470915e-02, // 0.500000
  9.63109197365605573804e-02, // 0.600000
  9.80551512388879997761e-02, // 0.700000
  9.97357907145074162880e-02, // 0.800000
  1.01348810967098704183e-01, // 0.900000
  1.02890333488697346964e-01, // 1.000000
  1.04356645052008520369e-01, // 1.100000
  1.05744213625967600767e-01, // 1.200000
  1.07049703514534744198e-01, // 1.300000
  1.08269989623592619021e-01, // 1.400000
  1.09402170766494374887e-01, // 1.500000
  1.10443581925230030483e-01, // 1.600000
  1.11391805391289122618e-01, // 1.700000
  1.12244680718037018186e-01, // 1.800000
  1.13000313424705317589e-01, // 1.900000
  1.13657082400870648731e-01, // 2.000000
  1.15388290567157214550e-01, // 2.718282
  1.13959589524871160449e-01, // 3.141593
  };

  public static final double[] GNUR_EXGAUSS_CDF_1_3_05 = { //
  1.99941448676917293836e-01, // 0.000000
  1.99942296168926059163e-01, // 0.000010
  2.08518489920253430325e-01, // 0.100000
  2.10559722098245583055e-01, // 0.123457
  2.17297214297385354875e-01, // 0.200000
  2.23725077521963966465e-01, // 0.271828
  2.26273664808885194288e-01, // 0.300000
  2.27560395746384258597e-01, // 0.314159
  2.35443431714238121666e-01, // 0.400000
  2.44801657534712419073e-01, // 0.500000
  2.54343043903264842687e-01, // 0.600000
  2.64061860245194957031e-01, // 0.700000
  2.73951954270620134935e-01, // 0.800000
  2.84006764248710408260e-01, // 0.900000
  2.94219333022605278316e-01, // 1.000000
  3.04582323713075031613e-01, // 1.100000
  3.15088037048429803200e-01, // 1.200000
  3.25728430247959499511e-01, // 1.300000
  3.36495137376428721243e-01, // 1.400000
  3.47379491077914881458e-01, // 1.500000
  3.58372545588643010017e-01, // 1.600000
  3.69465100920526368089e-01, // 1.700000
  3.80647728099900828358e-01, // 1.800000
  3.91910795339542039617e-01, // 1.900000
  4.03244495016495063666e-01, // 2.000000
  4.85820022244521587673e-01, // 2.718282
  5.34425248272120123616e-01, // 3.141593
  };

  @Test
  public void testPDF() {
    checkPDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), P_CDFPDF, GNUR_EXGAUSS_PDF_1_3_05, 1e-13);
  }

  @Test
  public void testCDF() {
    checkCDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), P_CDFPDF, GNUR_EXGAUSS_CDF_1_3_05, 1e-12);
  }
}
