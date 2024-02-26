package elki.clustering.neighborhood;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.NearestNeighborClosedNeighborhoodSetGenerator;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.clustering.neighborhood.MutualNeighborConsistency;
import elki.utilities.ELKIBuilder;

/**
 * Test ENFORCE clustering.
 *
 * @author Erich Schubert
 */
public class KMeansCPTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeansCP() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KMeansCP<DoubleVector>>(KMeansCP.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 1) //
        .with(NearestNeighborClosedNeighborhoodSetGenerator.Par.K_NEIGHBORS, 1) //
        .build().autorun(db);
    // With other random seeds, the result is "better", but we need a test where
    // it differs from standard k-means.
    assertFMeasure(db, result, 0.78274);
    assertClusterSizes(result, new int[] { 95, 105, 200, 200, 400 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<MutualNeighborConsistency<NumberVector>>(MutualNeighborConsistency.class) //
        .with(MutualNeighborConsistency.Par.DISTANCE_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNeighborConsistency.Par.NUMBER_K, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("2NN-consistency was not enforced?", 1.0, nn1, 1e-15);

    double nn10 = new ELKIBuilder<MutualNeighborConsistency<NumberVector>>(MutualNeighborConsistency.class) //
        .with(MutualNeighborConsistency.Par.DISTANCE_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNeighborConsistency.Par.NUMBER_K, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.964, nn10, 1e-15);
  }

  @Test
  public void testKMeansCPNoise() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<KMeansCP<DoubleVector>>(KMeansCP.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 1) //
        .with(MutualNeighborClosedNeighborhoodSetGenerator.Par.K_NEIGHBORS, 1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.9138);
    assertClusterSizes(result, new int[] { 57, 115, 158 });

    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double nn1 = new ELKIBuilder<MutualNeighborConsistency<NumberVector>>(MutualNeighborConsistency.class) //
        .with(MutualNeighborConsistency.Par.DISTANCE_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNeighborConsistency.Par.NUMBER_K, 1) //
        .build().evaluateClustering(result, rel);
    assertEquals("1NN-consistency was not enforced?", 1.0, nn1, 1e-15);

    double nn10 = new ELKIBuilder<MutualNeighborConsistency<NumberVector>>(MutualNeighborConsistency.class) //
        .with(MutualNeighborConsistency.Par.DISTANCE_ID, SquaredEuclideanDistance.STATIC) //
        .with(MutualNeighborConsistency.Par.NUMBER_K, 10) //
        .build().evaluateClustering(result, rel);
    assertEquals("10NN-consistency not as expected?", 0.95151, nn10, 1e-4);
  }
}
