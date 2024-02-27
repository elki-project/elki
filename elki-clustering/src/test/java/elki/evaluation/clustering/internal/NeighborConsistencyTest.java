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
package elki.evaluation.clustering.internal;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NearestNeighborPredicate;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.clustering.EvaluateClustering.ScoreResult;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;

/**
 * Test the nearest-neighbor consistency measures.
 *
 * @author Niklas Strahmann
 */
public class NeighborConsistencyTest {
  final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  @Test
  public void testEvaluateNearestNeighborConsistencyTestKMeans() {
    // load classes and data
    SquaredEuclideanDistance dist = SquaredEuclideanDistance.STATIC;
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    NeighborConsistency<NumberVector> knnc = new ELKIBuilder<>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, NearestNeighborPredicate.class) //
        .with(NearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, dist) //
        .with(NearestNeighborPredicate.Par.KNN_ID, 2) //
        .build();

    Relation<NumberVector> relation = db.getRelation(dist.getInputTypeRestriction());
    Clustering<?> clusters = new ELKIBuilder<LloydKMeans<NumberVector>>(LloydKMeans.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .build().run(relation);

    knnc.evaluateClustering(clusters, relation);

    It<ScoreResult> it = Metadata.hierarchyOf(clusters).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertNotNull("Not a score result", it.get());
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    EvaluationResult.MeasurementGroup knnConsistency = er.findOrCreateGroup("Distance-based");
    // check measurements
    Iterator<EvaluationResult.Measurement> knncIter = knnConsistency.iterator();
    assertTrue("No knn consistency measurement", knncIter.hasNext());
    EvaluationResult.Measurement m = knncIter.next();
    assertFalse("Too many measurements", knncIter.hasNext());

    assertEquals("kNN consistency not as expected", 0.95, m.getVal(), 1e-15);

    // Mutual kNN variant:
    NeighborConsistency<NumberVector> mknnc = new ELKIBuilder<>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, MutualNearestNeighborPredicate.class) //
        .with(NearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, dist) //
        .with(NearestNeighborPredicate.Par.KNN_ID, 2) //
        .build();
    double mm = mknnc.evaluateClustering(clusters, relation);
    assertEquals("mkNN consistency not as expected", 1.0, mm, 1e-15);
  }

  @Test
  public void testEvaluateNearestNeighborConsistencyAbsolute1NNConsistent() {
    // load classes and data
    SquaredEuclideanDistance dist = SquaredEuclideanDistance.STATIC;
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    NeighborConsistency<NumberVector> knnc = new ELKIBuilder<>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, NearestNeighborPredicate.class) //
        .with(NearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, dist) //
        .with(NearestNeighborPredicate.Par.KNN_ID, 1) //
        .build();

    Relation<NumberVector> relation = db.getRelation(dist.getInputTypeRestriction());
    Clustering<?> clusters = new ELKIBuilder<LloydKMeans<NumberVector>>(LloydKMeans.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .build().run(relation);

    knnc.evaluateClustering(clusters, relation);

    It<ScoreResult> it = Metadata.hierarchyOf(clusters).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertNotNull("Not a score result", it.get());
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    EvaluationResult.MeasurementGroup knnConsistency = er.findOrCreateGroup("Distance-based");
    // check measurements
    Iterator<EvaluationResult.Measurement> knncIter = knnConsistency.iterator();
    assertTrue("No knn consistency measurement", knncIter.hasNext());
    EvaluationResult.Measurement m = knncIter.next();
    assertFalse("Too many measurements", knncIter.hasNext());

    assertEquals("kNN consistency not as expected", 1, m.getVal(), 1e-15);

    // Mutual kNN variant:
    NeighborConsistency<NumberVector> mknnc = new ELKIBuilder<>(NeighborConsistency.class) //
        .with(NeighborConsistency.Par.PREDICATE_ID, MutualNearestNeighborPredicate.class) //
        .with(NearestNeighborPredicate.Par.DISTANCE_FUNCTION_ID, dist) //
        .with(NearestNeighborPredicate.Par.KNN_ID, 2) //
        .build();
    double mm = mknnc.evaluateClustering(clusters, relation);
    assertEquals("mkNN consistency not as expected", 1.0, mm, 1e-15);
  }
}
