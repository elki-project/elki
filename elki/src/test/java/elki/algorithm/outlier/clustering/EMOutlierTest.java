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
package elki.algorithm.outlier.clustering;

import org.junit.Test;

import elki.algorithm.clustering.em.EM;
import elki.algorithm.clustering.kmeans.KMeans;
import elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the EM outlier detection algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class EMOutlierTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testEMOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<EMOutlier<DoubleVector>>(EMOutlier.class) //
        .with(EM.Parameterizer.K_ID, 5) //
        .with(KMeans.SEED_ID, 2) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.5654666);
    testSingleScore(result, 416, 1.8997442e-5);
  }
}
