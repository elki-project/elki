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

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NearestNeighborPredicate;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
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
 * Test ENFORCE clustering.
 *
 * @author Erich Schubert
 */
public class ENFORCETest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeansENFORCE() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<ENFORCE<DoubleVector>>(ENFORCE.class) //
        .with(Algorithm.Utils.ALGORITHM_ID, LloydKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 1) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.7832);
    assertClusterSizes(result, new int[] { 89, 111, 200, 200, 400 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, NearestNeighborPredicate.class) //
        .with(NearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(NearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("1NN-consistency was not enforced?", 0.998, nn1, 1e-15);

    double nn10 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, MutualNearestNeighborPredicate.class) //
        .with(MutualNearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.964, nn10, 1e-15);
  }

  @Test
  public void testKMeansENFORCENoise() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<ENFORCE<DoubleVector>>(ENFORCE.class) //
        .with(Algorithm.Utils.ALGORITHM_ID, LloydKMeans.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.9138);
    assertClusterSizes(result, new int[] { 57, 115, 158 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, NearestNeighborPredicate.class) //
        .with(NearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(NearestNeighborPredicate.Par.KNN_ID, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("1NN-consistency was not enforced?", 0.9848, nn1, 1e-4);

    double nn10 = new ELKIBuilder<NeighborConsistency<NumberVector>>(NeighborConsistency.class) //
        .with(MutualNearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNearestNeighborPredicate.Par.KNN_ID, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.95151, nn10, 1e-4);
  }
}
