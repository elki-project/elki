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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.*;

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
        .with(ReferenceBasedOutlierDetection.Parameterizer.K_ID, 11) //
        .with(GridBasedReferencePoints.Parameterizer.GRID_ID, 3)//
        .build().run(db);
    testAUC(db, "Noise", result, 0.9693703703703);
    testSingleScore(result, 945, 0.933574455);
  }

  @Test
  public void testReferenceBasedOutlierDetectionStar() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.REFP_ID, StarBasedReferencePoints.class) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.910722222);
    testSingleScore(result, 945, 0.920950222);
  }

  @Test
  public void testReferenceBasedOutlierDetectionAxis() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.REFP_ID, AxisBasedReferencePoints.class) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.858953703);
    testSingleScore(result, 945, 0.9193032738);
  }

  @Test
  public void testReferenceBasedOutlierDetectionGenerated() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.REFP_ID, RandomGeneratedReferencePoints.class)//
        .with(RandomGeneratedReferencePoints.Parameterizer.N_ID, 15)//
        .with(RandomGeneratedReferencePoints.Parameterizer.RANDOM_ID, 0)//
        .build().run(db);
    testAUC(db, "Noise", result, 0.878203703);
    testSingleScore(result, 945, 0.910430564);
  }

  @Test
  public void testReferenceBasedOutlierDetectionSample() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(ReferenceBasedOutlierDetection.class) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.K_ID, 11) //
        .with(ReferenceBasedOutlierDetection.Parameterizer.REFP_ID, RandomSampleReferencePoints.class) //
        .with(RandomSampleReferencePoints.Parameterizer.N_ID, 15)//
        .with(RandomSampleReferencePoints.Parameterizer.RANDOM_ID, 0)//
        .build().run(db);
    testAUC(db, "Noise", result, 0.829814814);
    testSingleScore(result, 945, 0.846881387);
  }
}
