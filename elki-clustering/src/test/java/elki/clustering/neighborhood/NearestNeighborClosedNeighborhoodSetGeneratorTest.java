package elki.clustering.neighborhood;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.NearestNeighborClosedNeighborhoodSetGenerator;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDs;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;

/**
 * Test nearest neighbor neighborhood sets.
 * 
 * @author Niklas Strahmann
 */
public class NearestNeighborClosedNeighborhoodSetGeneratorTest {
  final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  @Test
  public void testAmountOfClosedNeighborhoodSets2NN() {
    int kNeighbors = 2;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoodSetGenerator<DoubleVector> ncsGenerator = new NearestNeighborClosedNeighborhoodSetGenerator<>(kNeighbors, distance);
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    List<DBIDs> result = ncsGenerator.getClosedNeighborhoods(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 2, result.size());
  }

  @Test
  public void testAmountOfClosedNeighborhoodSets1NN() {
    int kNeighbors = 1;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoodSetGenerator<DoubleVector> ncsGenerator = new NearestNeighborClosedNeighborhoodSetGenerator<>(kNeighbors, distance);
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    List<DBIDs> result = ncsGenerator.getClosedNeighborhoods(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 3, result.size());
  }
}
