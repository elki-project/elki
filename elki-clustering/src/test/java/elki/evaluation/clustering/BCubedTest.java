/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import static org.junit.Assert.*;

import org.junit.Test;

import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;

/**
 * Validate {@link BCubed} based measures with the SkLearn example
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class BCubedTest extends AbstractClusterEvaluationTest {
  /**
   * Test {@link BCubed} with SkLearn example.
   */
  @Test
  public void testBCubed() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SKLEARNA.length);
    BCubed bc = new ClusterContingencyTable(true, false, makeClustering(ids.iter(), SKLEARNA), makeClustering(ids.iter(), SKLEARNB)).getBCubed();

    assertEquals("BCubed precision not as expected", 0.57843137254902, bc.precision(), 1e-15);
    assertEquals("BCubed Recall not as expected", 0.584313725490196, bc.recall(), 1e-15);
    assertEquals("BCubed F1-Measure not as expected", 0.5813576695433655, bc.f1Measure(), 1e-15);
  }
}
