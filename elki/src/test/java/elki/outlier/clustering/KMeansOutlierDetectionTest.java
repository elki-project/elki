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
import elki.clustering.kmeans.ShallotKMeans;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the KMeans outlier detection algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KMeansOutlierDetectionTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testKMeansOutlierDistance() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<KMeansOutlierDetection<DoubleVector>>(KMeansOutlierDetection.class) //
        .with(KMeansOutlierDetection.Par.CLUSTERING_ID, ShallotKMeans.class) //
        .with(KMeans.K_ID, 10) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansOutlierDetection.Par.RULE_ID, KMeansOutlierDetection.Rule.DISTANCE) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.8654);
    assertSingleScore(result, 416, 0.0102546);
  }

  @Test
  public void testKMeansOutlierVariance() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<KMeansOutlierDetection<DoubleVector>>(KMeansOutlierDetection.class) //
        .with(KMeansOutlierDetection.Par.CLUSTERING_ID, ShallotKMeans.class) //
        .with(KMeans.K_ID, 10) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansOutlierDetection.Par.RULE_ID, KMeansOutlierDetection.Rule.VARIANCE) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.865);
    assertSingleScore(result, 416, 0.010665);
  }

  @Test
  public void testKMeansOutlierDistanceHighK() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<KMeansOutlierDetection<DoubleVector>>(KMeansOutlierDetection.class) //
        .with(KMeansOutlierDetection.Par.CLUSTERING_ID, ShallotKMeans.class) //
        .with(KMeans.K_ID, 100) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansOutlierDetection.Par.RULE_ID, KMeansOutlierDetection.Rule.DISTANCE) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.77473);
    assertSingleScore(result, 416, 0.00576);
  }

  @Test
  public void testKMeansOutlierSingletons() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<KMeansOutlierDetection<DoubleVector>>(KMeansOutlierDetection.class) //
        .with(KMeansOutlierDetection.Par.CLUSTERING_ID, ShallotKMeans.class) //
        .with(KMeans.K_ID, 100) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansOutlierDetection.Par.RULE_ID, KMeansOutlierDetection.Rule.DISTANCE_SINGLETONS) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.7748);
    assertSingleScore(result, 416, 0.00576);
  }

  @Test
  public void testKMeansOutlierSingletonsVariance() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<KMeansOutlierDetection<DoubleVector>>(KMeansOutlierDetection.class) //
        .with(KMeansOutlierDetection.Par.CLUSTERING_ID, ShallotKMeans.class) //
        .with(KMeans.K_ID, 100) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansOutlierDetection.Par.RULE_ID, KMeansOutlierDetection.Rule.VARIANCE) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.79273);
    assertSingleScore(result, 416, 0.00864);
  }
}
