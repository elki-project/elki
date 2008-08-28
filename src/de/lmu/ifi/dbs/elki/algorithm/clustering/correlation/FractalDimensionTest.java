package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.FractalDimensionTestResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.FractalDimensionBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.KNNList;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
// todo arthur comment all
public class FractalDimensionTest<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {

    /**
     * OptionID for {@link #ID1_PARAM}
     */
    public static final OptionID ID1_ID = OptionID.getOrCreateOptionID(
        "fractaldimensiontest.id1",
        "The first id."
    );

    /**
     * OptionID for {@link #ID2_PARAM}
     */
    public static final OptionID ID2_ID = OptionID.getOrCreateOptionID(
        "fractaldimensiontest.id2",
        "The second id."
    );

    /**
     * Parameter that specifies the first id.
     * <p>Key: {@code -fractaldimensiontest.id1} </p>
     */
    // todo arthur: constraints, default value, description
    private final IntParameter ID1_PARAM = new IntParameter(ID1_ID);

    /**
     * Holds the value of {@link #ID1_PARAM}.
     */
    private int id1;

    /**
     * Parameter that specifies the second id.
     * <p>Key: {@code -fractaldimensiontest.id2} </p>
     */
    // todo arthur: constraints, default value, description
    private final IntParameter ID2_PARAM = new IntParameter(ID1_ID);

    /**
     * Holds the value of {@link #ID2_PARAM}.
     */
    private int id2;

    /**
     * Holds the result.
     */
    private FractalDimensionTestResult<V> result;

    //private IntParameter supporters = new IntParameter("supporters", "number of supporters");

    //private int k;

    private FractalDimensionBasedDistanceFunction<V> distanceFunction = new FractalDimensionBasedDistanceFunction<V>();

    /**
     * Adds parameters
     * {@link #ID1_PARAM} and {@link #ID2_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public FractalDimensionTest() {
        super();
        addOption(ID1_PARAM);
        addOption(ID2_PARAM);
        //optionHandler.put(supporters);
    }

    protected void runInTime(Database<V> database) throws IllegalStateException {
        distanceFunction.setDatabase(database, true, false);
        List<Integer> suppID1 = database.getAssociation(AssociationID.NEIGHBOR_IDS, id1);
        List<Integer> suppID2 = database.getAssociation(AssociationID.NEIGHBOR_IDS, id2);
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
     * Calls the super method
     * and sets additionally the values of the parameters
     * {@link #ID1_PARAM} and {@link #ID2_PARAM}.
     * The remaining parameters are passed to {@link #distanceFunction}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        id1 = ID1_PARAM.getValue();
        id2 = ID2_PARAM.getValue();
        //k = getParameterValue(supporters);

        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }


    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #distanceFunction}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(distanceFunction.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Calls the super method
     * and appends the parameter description of
     * the {@link #distanceFunction}.
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // distance function
        description.append(Description.NEWLINE);
        description.append(distanceFunction.parameterDescription());

        return description.toString();
    }

    public Description getDescription() {
        return new Description("FracClusTest", "FracClusTest", "", "");
    }

    public FractalDimensionTestResult<V> getResult() {
        return result;
    }

}
