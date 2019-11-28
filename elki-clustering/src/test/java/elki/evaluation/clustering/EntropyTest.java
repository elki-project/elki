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
package elki.evaluation.clustering;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;

/**
 * Validate {@link Entropy} based Measures with an equal example
 * and the SkLearn example
 * 
 * @author Erich Schubert
 */
public class EntropyTest extends AbstractClusterEvaluationTest {

  /**
   * Validate {@link Entropy} based Measures with an equal example
   * 
   */
  @Test
  public void testIdentical() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SAMEA.length);
    Entropy e = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SAMEA), makeClustering(ids.iter(), SAMEB)).getEntropy();
    assertEquals("MI not as expected", e.upperBoundMI(), e.mutualInformation(), 1e-15);
    assertEquals("Joint NMI not as expected", 1, e.jointNMI(), 1e-15);
    assertEquals("minNMI not as expected", 1, e.minNMI(), 1e-15);
    assertEquals("maxNMI not as expected", 1, e.maxNMI(), 1e-15);
    assertEquals("Arithmetic NMI not as expected", 1, e.arithmeticNMI(), 1e-15);
    assertEquals("Geometric NMI not as expected", 1, e.geometricNMI(), 1e-15);
    assertEquals("EMI not as expected", 0.5441, e.expectedMutualInformation(), 1e-5);
    assertEquals("AMI not as expected", 1, e.adjustedMaxMI(), 1e-15);
  }

  /**
   * Validate the large path of the {@link Entropy} algorithm with an equal
   * example
   */
  @Test
  public void testIdenticalLarge() {
    int[] la = repeat(SAMEA, 10_000);
    int[] lb = repeat(SAMEB, 10_000);
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(la.length);
    Entropy e = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), la), makeClustering(ids.iter(), lb)).getEntropy();
    assertEquals("MI not as expected", e.upperBoundMI(), e.mutualInformation(), 1e-15);
    assertEquals("Joint NMI not as expected", 1, e.jointNMI(), 1e-15);
    assertEquals("minNMI not as expected", 1, e.minNMI(), 1e-15);
    assertEquals("maxNMI not as expected", 1, e.maxNMI(), 1e-15);
    assertEquals("Arithmetic NMI not as expected", 1, e.arithmeticNMI(), 1e-15);
    assertEquals("Geometric NMI not as expected", 1, e.geometricNMI(), 1e-15);
    assertEquals("AMI not as expected", 1, e.adjustedMaxMI(), 1e-15);
  }

  /**
   * Validate {@link Entropy} based Measures with the SkLearn example
   */
  @Test
  public void testSklearn() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SKLEARNA.length);
    Entropy e = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SKLEARNA), makeClustering(ids.iter(), SKLEARNB)).getEntropy();
    assertEquals("MI not as expected", 0.41022, e.mutualInformation(), 1e-5);
    assertEquals("EMI not as expected", 0.15042, e.expectedMutualInformation(), 1e-5);
    assertEquals("AMI not as expected", 0.27821, e.adjustedArithmeticMI(), 1e-5);
  }

  /**
   * Validate the large path of the {@link Entropy} algorithm with the SkLearn
   * example
   */
  @Test
  public void testSklearnLarge() {
    // From sklearn unit test
    int[] la = repeat(SKLEARNA, 10_000);
    int[] lb = repeat(SKLEARNB, 10_000);
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(la.length);
    Entropy e = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), la), makeClustering(ids.iter(), lb)).getEntropy();
    assertEquals("MI not as expected", 0.41022, e.mutualInformation(), 1e-5);
  }

}
