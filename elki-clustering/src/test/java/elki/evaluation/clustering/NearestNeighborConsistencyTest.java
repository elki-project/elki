package elki.evaluation.clustering;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.clustering.EvaluateClustering.ScoreResult;
import elki.evaluation.clustering.neighborhood.NearestNeighborConsistency;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;

/**
 * Test the nearest-neighbor consistency measures.
 *
 * @author Niklas Strahmann
 */
public class NearestNeighborConsistencyTest {
  final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  @Test
  public void testEvaluateNearestNeighborConsistencyTestKMeans() {
    // load classes and data
    SquaredEuclideanDistance dist = SquaredEuclideanDistance.STATIC;
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    NearestNeighborConsistency<NumberVector> knnc = new ELKIBuilder<>(NearestNeighborConsistency.class) //
        .with(NearestNeighborConsistency.Par.DISTANCE_ID, dist) //
        .with(NearestNeighborConsistency.Par.NUMBER_K, 2) //
        .build();

    Relation<NumberVector> relation = db.getRelation(dist.getInputTypeRestriction());
    Clustering<?> clusters = new ELKIBuilder<LloydKMeans<NumberVector>>(LloydKMeans.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .build().run(relation);

    // evaluate
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
  }

  @Test
  public void testEvaluateNearestNeighborConsistencyAbsolute1NNConsistent() {
    // load classes and data
    SquaredEuclideanDistance dist = SquaredEuclideanDistance.STATIC;
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    NearestNeighborConsistency<NumberVector> knnc = new ELKIBuilder<>(NearestNeighborConsistency.class) //
        .with(NearestNeighborConsistency.Par.DISTANCE_ID, dist) //
        .with(NearestNeighborConsistency.Par.NUMBER_K, 1) //
        .build();

    Relation<NumberVector> relation = db.getRelation(dist.getInputTypeRestriction());
    Clustering<?> clusters = new ELKIBuilder<LloydKMeans<NumberVector>>(LloydKMeans.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .build().run(relation);

    // evaluate
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
  }
}
