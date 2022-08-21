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
 * Validate the {@link PairSetsIndex} measures.
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class PairSetsIndexTest extends AbstractClusterEvaluationTest {
  @Test
  public void testPairSetsIndexEquals() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SAMEA.length);
    PairSetsIndex psi = new PairSetsIndex(new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SAMEA), makeClustering(ids.iter(), SAMEB)));

    assertEquals("PSI on an equal example should be 1.", 1, psi.psi, 1e-13);
    assertEquals("Simplified PSI on an equal example should be 1.", psi.simplifiedPSI, 1, 1e-13);
  }

  @Test
  public void tesPairSetsIndexSKLearn() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SKLEARNA.length);
    PairSetsIndex psi = new PairSetsIndex(new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SKLEARNA), makeClustering(ids.iter(), SKLEARNB)));

    assertEquals("PSI value not as expected", .516598639455782, psi.psi, 1e-13);
    assertEquals("Simplified PSI value not as expected", .502380952380952, psi.simplifiedPSI, 1e-13);
  }
}
