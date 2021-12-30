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
package elki.outlier.clustering;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.hierarchical.HDBSCANLinearMemory;
import elki.clustering.hierarchical.extraction.HDBSCANHierarchyExtraction;
import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the GLOSH outlier detection algorithm.
 *
 * @author Braulio V.S. Vinces
 */
public class GLOSHTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testGLOSHOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<GLOSH>(GLOSH.class) //
        .with(HDBSCANHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 2) //
        .with(Algorithm.Utils.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Par.MIN_PTS_ID, 2) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.5326);
    assertSingleScore(result, 416, 0.9967879136406594);
  }
}
