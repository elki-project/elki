package elki.clustering.neighborhood;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.neighborhood.helper.NearestNeighborClosedNeighborhoodSetGenerator;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.clustering.neighborhood.NearestNeighborConsistency;
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
        .with(KMeans.SEED_ID, 7) //
        .with(NearestNeighborClosedNeighborhoodSetGenerator.Par.K_NEIGHBORS, 1) //
        .build().autorun(db);
    // In this lucky random seed, it achieves 1 element better than standard
    // k-means, but do not assume this is always so.
    assertFMeasure(db, result, 1.0);
    assertClusterSizes(result, new int[] { 200, 200, 200, 200, 200 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<NearestNeighborConsistency<NumberVector>>(NearestNeighborConsistency.class) //
        .with(NearestNeighborConsistency.Par.DISTANCE_ID, SquaredEuclideanDistance.STATIC) //
        .with(NearestNeighborConsistency.Par.NUMBER_K, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("1NN-consistency was not enforced?", 1.0, nn1, 1e-15);

    double nn10 = new ELKIBuilder<NearestNeighborConsistency<NumberVector>>(NearestNeighborConsistency.class) //
        .with(NearestNeighborConsistency.Par.DISTANCE_ID, SquaredEuclideanDistance.STATIC) //
        .with(NearestNeighborConsistency.Par.NUMBER_K, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.999, nn10, 1e-15);
  }
}
