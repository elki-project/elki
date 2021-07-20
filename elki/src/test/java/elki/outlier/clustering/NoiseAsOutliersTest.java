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
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.KMeansMinusMinus;
import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Test the noise as outliers algorithm
 *
 * @author Erich Schubert
 */
public class NoiseAsOutliersTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testKMeansMinusMinusOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<NoiseAsOutliers>(NoiseAsOutliers.class) //
        .with(Algorithm.Utils.ALGORITHM_ID, KMeansMinusMinus.class) //
        .with(KMeansMinusMinus.Par.RATE_ID, 0.1) //
        .with(KMeansMinusMinus.Par.NOISE_FLAG_ID) //
        .with(KMeans.K_ID, 10) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.765);
    assertSingleScore(result, 416, 1.0);
  }
}
