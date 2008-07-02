package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.algorithm.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public class KNNDistanceOrder<O extends DatabaseObject, D extends Distance<D>>
    extends DistanceBasedAlgorithm<O, D> {

    /**
     * Parameter to specify the distance of the k-distant object to be assessed,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -knndistanceorder.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(OptionID.KNN_DISTANCE_ORDER_K,
        new GreaterConstraint(0), 1);

    /**
     * Parameter to specify the average percentage of distances randomly choosen to be provided in the result,
     * must be a double greater than 0 and less than or equal to 1.
     * <p>Default value: {@code 1.0} </p>
     * <p>Key: {@code -knndistanceorder.percentage} </p>
     */
    public final DoubleParameter PERCENTAGE_PARAM =
        new DoubleParameter(OptionID.KNN_DISTANCE_ORDER_PERCENTAGE,
            new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE),
            1.0);

    /**
     * Holds the value of parameter k.
     */
    private int k;

    /**
     * Holds the value of parameter percentage.
     */
    private double percentage;

    /**
     * Holds the result.
     */
    private KNNDistanceOrderResult<O, D> result;

    /**
     * Provides an algorithm to order the kNN-distances for all objects of the
     * database.
     */
    public KNNDistanceOrder() {
        super();
        // parameter k
        addOption(K_PARAM);

        //parameter percentage
        addOption(PERCENTAGE_PARAM);
    }

    /**
     * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
     */
    @Override
    protected void runInTime(Database<O> database) throws IllegalStateException {
        Random random = new Random();
        List<D> knnDistances = new ArrayList<D>();
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            if (random.nextDouble() < percentage) {
                knnDistances.add((database.kNNQueryForID(id, k, this.getDistanceFunction())).get(k - 1).getDistance());
            }
        }
        Collections.sort(knnDistances, Collections.reverseOrder());
        result = new KNNDistanceOrderResult<O, D>(database, knnDistances);
    }

    /**
     * @see Algorithm#getResult()
     */
    public Result<O> getResult() {
        return result;
    }

    /**
     * Sets the parameter value for parameter k, if specified, additionally to
     * the parameter settings of super classes. Otherwise the default value for
     * k is used.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        //k and percentage
        k = getParameterValue(K_PARAM);
        percentage = getParameterValue(PERCENTAGE_PARAM);

        return remainingParameters;
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description(
            KNNDistanceOrder.class.getName(),
            "KNN-Distance-Order",
            "Assesses the knn distances for a specified k and orders them.",
            "");
    }

}
