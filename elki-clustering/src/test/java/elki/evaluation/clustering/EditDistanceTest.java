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
 * Validate {@link EditDistance} with respect to the SkLearn Example.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class EditDistanceTest extends AbstractClusterEvaluationTest {
  /**
   * Validate {@link EditDistance} with the SkLearn example
   */
  @Test
  public void testEditDistance() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SKLEARNA.length);
    EditDistance ed = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SKLEARNA), makeClustering(ids.iter(), SKLEARNB)).getEdit();

    assertEquals("EditDistance operations (first) not as expected", 8, ed.editOperationsFirst());
    assertEquals("EditDistance operations (second) as expected", 8, ed.editOperationsSecond());
    assertEquals("EditDistance operations (base) as expected", 17, ed.editOperationsBaseline());
    assertEquals("EditDistance (first) not as expected", 1 - (8 / 17.), ed.editDistanceFirst(), 0.);
    assertEquals("EditDistance (second) not as expected", 1 - (8 / 17.), ed.editDistanceSecond(), 0.);
    assertEquals("EditDistance F1-Measure not as expected", 1 - (8 / 17.), ed.f1Measure(), 0.);
  }
}
