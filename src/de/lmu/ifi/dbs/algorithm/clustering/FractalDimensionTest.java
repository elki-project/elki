package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.FractalDimensionTestResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.FractalDimensionBasedDistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * todo Arthur comment all
 *
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class FractalDimensionTest<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {

    /**
     * @todo arthur: constraints, default value, description
     * Parameter that specifies the first id.
     * <p>Key: {@code -fractaldimensiontest.id1} </p>
     */
    private final IntParameter ID1_PARAM = new IntParameter(OptionID.FRACTAL_DIMENSION_TEST_ID1);

    /**
     * @todo arthur: constraints, default value, description
     * Parameter that specifies the second id.
     * <p>Key: {@code -fractaldimensiontest.id2} </p>
     */
    private final IntParameter ID2_PARAM = new IntParameter(OptionID.FRACTAL_DIMENSION_TEST_ID1);

    private FractalDimensionTestResult<V> result;

    //private IntParameter supporters = new IntParameter("supporters", "number of supporters");

    private int id1;

    private int id2;

    //private int k;

    private FractalDimensionBasedDistanceFunction<V> distanceFunction = new FractalDimensionBasedDistanceFunction<V>();
    //private EuklideanDistanceFunction<V> distanceFunction = new EuklideanDistanceFunction<V>();

    public FractalDimensionTest() {
        super();
        addOption(ID1_PARAM);
        addOption(ID2_PARAM);
        //optionHandler.put(supporters);
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    @SuppressWarnings("unchecked")
    protected void runInTime(Database<V> database) throws IllegalStateException {
        distanceFunction.setDatabase(database, true, false);
        List<Integer> suppID1 = (List<Integer>) database.getAssociation(AssociationID.NEIGHBORS, id1);
        List<Integer> suppID2 = (List<Integer>) database.getAssociation(AssociationID.NEIGHBORS, id2);
        V o1 = database.get(id1);
        V o2 = database.get(id2);
        V centroid = o1.plus(o2).multiplicate(0.5);
        KNNList<DoubleDistance> knnList = new KNNList<DoubleDistance>(distanceFunction.getPreprocessor().getK(), distanceFunction.infiniteDistance());
        for (Integer id : suppID1) {
            knnList.add(new QueryResult<DoubleDistance>(id, distanceFunction.STANDARD_DOUBLE_DISTANCE_FUNCTION.distance(id, centroid)));
        }
        for (Integer id : suppID2) {
            knnList.add(new QueryResult<DoubleDistance>(id, distanceFunction.STANDARD_DOUBLE_DISTANCE_FUNCTION.distance(id, centroid)));
        }
        List<Integer> suppCentroid = knnList.idsToList();
        result = new FractalDimensionTestResult<V>(database, id1, id2, suppID1, suppID2, centroid, suppCentroid);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        id1 = getParameterValue(ID1_PARAM);
        id2 = getParameterValue(ID2_PARAM);
        //k = getParameterValue(supporters);

        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("FracClusTest", "FracClusTest", "", "");
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public FractalDimensionTestResult<V> getResult() {
        return result;
    }

}
