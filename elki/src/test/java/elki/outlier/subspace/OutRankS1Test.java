/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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
package elki.outlier.subspace;

import org.junit.Test;

import elki.clustering.subspace.CLIQUE;
import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the OutRank S1 outlier detection algorithm.
 *
 * @author Erich Schubert
 */
public class OutRankS1Test extends AbstractOutlierAlgorithmTest {
  @Test
  public void testOutRankS1() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(OutRankS1.class) //
        .with(OutRankS1.Par.ALGORITHM_ID, CLIQUE.class) //
        .with(CLIQUE.Par.XSI_ID, 4) //
        .with(CLIQUE.Par.TAU_ID, 0.25) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.90483);
    assertSingleScore(result, 1344, 0.5);
  }
}
