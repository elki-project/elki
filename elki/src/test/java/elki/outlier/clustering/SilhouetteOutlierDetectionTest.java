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
package elki.outlier.clustering;

import org.junit.Test;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.HamerlyKMeans;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the Silhouette outlier detection algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SilhouetteOutlierDetectionTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testSilhouetteOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<SilhouetteOutlierDetection<DoubleVector>>(SilhouetteOutlierDetection.class) //
        .with(SilhouetteOutlierDetection.Par.CLUSTERING_ID, HamerlyKMeans.class) //
        .with(KMeans.K_ID, 10) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.73073);
    assertSingleScore(result, 416, 0.4743337);
  }

  @Test
  public void testSilhouetteOutlierDetectionOneCluster() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<SilhouetteOutlierDetection<DoubleVector>>(SilhouetteOutlierDetection.class) //
        .with(SilhouetteOutlierDetection.Par.CLUSTERING_ID, HamerlyKMeans.class) //
        .with(KMeans.K_ID, 1) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.5);
    assertSingleScore(result, 416, 0.0);
  }
}
