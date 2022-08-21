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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;

/**
 * Validate {@link SetMatchingPurity}.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class SetMatchingPurityTest extends AbstractClusterEvaluationTest {
  @Test
  public void test() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SKLEARNA.length);
    SetMatchingPurity smp = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SKLEARNA), makeClustering(ids.iter(), SKLEARNB)).getSetMatchingPurity();

    assertEquals("Similarity not as expected", 0.705882352941176, smp.purity(), 1e-15);
    assertEquals("Inv. Similarity not as expected", .705882352941176, smp.inversePurity(), 1e-15);
    assertEquals("F1-Measure not as expected", 0.705882352941176, smp.f1Measure(), 1e-15);
    assertEquals("F1-Measure (first) not as expected", 0.707390648567119, smp.fMeasureFirst(), 1e-15);
    assertEquals("F1-Measure (second) not as expected", 0.704374057315234, smp.fMeasureSecond(), 1e-15);
  }
}
