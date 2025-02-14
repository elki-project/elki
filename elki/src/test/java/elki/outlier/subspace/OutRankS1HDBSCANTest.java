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

import elki.clustering.hierarchical.AbstractHDBSCAN;
import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the OutRank S1 outlier detection algorithm for HDBSCAN*.
 *
 * @author Erich Schubert
 */
public class OutRankS1HDBSCANTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testOutRankS1HDBSCAN() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(OutRankS1HDBSCAN.class) //
        .with(OutRankS1HDBSCAN.Par.SUBSPACES_ID, 100) //
        .with(OutRankS1HDBSCAN.Par.SEED_ID, 1) //
        .with(AbstractHDBSCAN.Par.MIN_PTS_ID, 10) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.5679);
    assertSingleScore(result, 1344, 1.0);
  }
}
