package elki.evaluation.clustering;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.clustering.EvaluateClustering.ScoreResult;
import elki.evaluation.clustering.neighborhood.NearestNeighborConsistency;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.random.RandomFactory;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class NearestNeighborConsistencyTest {
    final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

    @Test
    public void testEvaluateNearestNeighborConsistencyTestKMeans(){
        // load classes and data
        EuclideanDistance dist = EuclideanDistance.STATIC;
        Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
        NearestNeighborConsistency<NumberVector> knnc = new ELKIBuilder<>(NearestNeighborConsistency.class)
                .with(NearestNeighborConsistency.Par.DISTANCE_ID, dist)
                .with(NearestNeighborConsistency.Par.NUMBER_K, 2).build();

        LloydKMeans<NumberVector> clusteringAlgorithm = new LloydKMeans<>(dist, 3, 20, new RandomlyChosen<>(new RandomFactory(12341234L)) );
        Clustering<?> clusters = clusteringAlgorithm.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
        Relation<NumberVector> relation = db.getRelation(dist.getInputTypeRestriction());

        //evaluate
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

        assertEquals("kNN consistency not as expected", 0.975, m.getVal(), 1e-15);


    }

    @Test
    public void testEvaluateNearestNeighborConsistencyAbsolute1NNConsistent(){
        // load classes and data
        EuclideanDistance dist = EuclideanDistance.STATIC;
        Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
        NearestNeighborConsistency<NumberVector> knnc = new ELKIBuilder<>(NearestNeighborConsistency.class)
                .with(NearestNeighborConsistency.Par.DISTANCE_ID, dist)
                .with(NearestNeighborConsistency.Par.NUMBER_K, 1).build();

        LloydKMeans<NumberVector> clusteringAlgorithm = new LloydKMeans<>(dist, 3, 20, new RandomlyChosen<>(new RandomFactory(12341234L)) );
        Clustering<?> clusters = clusteringAlgorithm.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
        Relation<NumberVector> relation = db.getRelation(dist.getInputTypeRestriction());

        //evaluate
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
