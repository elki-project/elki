package elki.clustering.neighborhood;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDs;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MutualClosedNeighborhoodSetGeneratorTest {
    final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

    @Test
    public void testAmountOfClosedNeighborhoodSets2MN(){
        int kNeighbors = 2;
        NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
        ClosedNeighborhoodSetGenerator<DoubleVector> ncsGenerator =  new MutualNeighborClosedNeighborhoodSetGenerator.Factory<DoubleVector>(kNeighbors, distance).instantiate();

        Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);

        DBIDs[] result = ncsGenerator.getClosedNeighborhoods(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));

        assertEquals("An unexpected amount of neighborhood sets were found.",6, result.length);
    }

    @Test
    public void testAmountOfClosedNeighborhoodSets3MN(){
        int kNeighbors = 3;
        NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
        ClosedNeighborhoodSetGenerator<DoubleVector> ncsGenerator =  new MutualNeighborClosedNeighborhoodSetGenerator.Factory<DoubleVector>(kNeighbors, distance).instantiate();

        Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);

        DBIDs[] result = ncsGenerator.getClosedNeighborhoods(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));

        assertEquals("An unexpected amount of neighborhood sets were found.",4, result.length);
    }

    @Test
    public void testAmountOfClosedNeighborhoodSets4MN(){
        int kNeighbors = 4;
        NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
        ClosedNeighborhoodSetGenerator<DoubleVector> ncsGenerator =  new MutualNeighborClosedNeighborhoodSetGenerator.Factory<DoubleVector>(kNeighbors, distance).instantiate();

        Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);

        DBIDs[] result = ncsGenerator.getClosedNeighborhoods(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));

        assertEquals("An unexpected amount of neighborhood sets were found.",3, result.length);
    }

    @Test
    public void testAmountOfClosedNeighborhoodSets5MN(){
        int kNeighbors = 5;
        NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
        ClosedNeighborhoodSetGenerator<DoubleVector> ncsGenerator =  new MutualNeighborClosedNeighborhoodSetGenerator.Factory<DoubleVector>(kNeighbors, distance).instantiate();

        Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);

        DBIDs[] result = ncsGenerator.getClosedNeighborhoods(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));

        assertEquals("An unexpected amount of neighborhood sets were found.",2, result.length);
    }
}
