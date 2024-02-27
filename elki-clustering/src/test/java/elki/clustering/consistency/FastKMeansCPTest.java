/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.consistency;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.clustering.internal.NeighborConsistency;
import elki.utilities.ELKIBuilder;

/**
 * Test Fast k-means CP clustering.
 *
 * @author Erich Schubert
 */
public class FastKMeansCPTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testFastKMeansCP() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<FastKMeansCP<DoubleVector>>(FastKMeansCP.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 1) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().autorun(db);
    // With other random seeds, the result is "better", but we need a test where
    // it differs from standard k-means.
    assertFMeasure(db, result, 0.78274);
    assertClusterSizes(result, new int[] { 95, 105, 200, 200, 400 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(MutualNearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("2NN-consistency was not enforced?", 1.0, nn1, 1e-15);

    double nn10 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(MutualNearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.964, nn10, 1e-15);
  }

  @Test
  public void testFastKMeansCPNoise() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<FastKMeansCP<DoubleVector>>(FastKMeansCP.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.9138);
    assertClusterSizes(result, new int[] { 57, 115, 158 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(MutualNearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("1NN-consistency was not enforced?", 1.0, nn1, 1e-15);

    double nn10 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(MutualNearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.95151, nn10, 1e-4);
  }
}
