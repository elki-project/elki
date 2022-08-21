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
package elki.outlier.distance;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;
import elki.utilities.referencepoints.*;

/**
 * Tests the ReferenceBasedOutlierDetection algorithm, with different reference
 * point strategies.
 *
 * @author Lucia Cichella
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ReferenceBasedOutlierDetectionTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testReferenceBasedOutlierDetectionGridBased() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Par.K_ID, 11) //
        .with(GridBasedReferencePoints.Par.GRID_ID, 3)//
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.9693703703703);
    assertSingleScore(result, 945, 0.933574455);
  }

  @Test
  public void testReferenceBasedOutlierDetectionStar() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Par.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Par.REFP_ID, StarBasedReferencePoints.class) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.910722222);
    assertSingleScore(result, 945, 0.920950222);
  }

  @Test
  public void testReferenceBasedOutlierDetectionAxis() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Par.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Par.REFP_ID, AxisBasedReferencePoints.class) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.858953703);
    assertSingleScore(result, 945, 0.9193032738);
  }

  @Test
  public void testReferenceBasedOutlierDetectionGenerated() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Par.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Par.REFP_ID, RandomGeneratedReferencePoints.class)//
        .with(RandomGeneratedReferencePoints.Par.N_ID, 15)//
        .with(RandomGeneratedReferencePoints.Par.RANDOM_ID, 0)//
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.8830925);
    assertSingleScore(result, 945, 0.8828635);
  }

  @Test
  public void testReferenceBasedOutlierDetectionSample() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Par.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Par.REFP_ID, RandomSampleReferencePoints.class) //
        .with(RandomSampleReferencePoints.Par.N_ID, 15)//
        .with(RandomSampleReferencePoints.Par.RANDOM_ID, 0)//
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.851018);
    assertSingleScore(result, 945, 0.861747);
  }
}
