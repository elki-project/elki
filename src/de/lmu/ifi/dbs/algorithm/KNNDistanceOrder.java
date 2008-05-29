package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.*;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 *
 * @author Arthur Zimek
 */
public class KNNDistanceOrder<O extends DatabaseObject, D extends Distance<D>>
    extends DistanceBasedAlgorithm<O, D> {

    /**
     * Parameter to specify the distance of the k-distant object to be assessed,
     * must be greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -k} </p>
     */
    public static final IntParameter K_PARAM = new IntParameter("k",
        "<int>the distance of the k-distant object is assessed. k >= 1 (default: 1)",
        new GreaterConstraint(0));

    //public static final DoubleParameter per = new DoubleParameter(PERCENTAGE_P, PERCENTAGE_D, percentageCons);

    /**
     * Parameter percentage.
     */
    public static final String PERCENTAGE_P = "percentage";

    /**
     * Default value for percentage.
     */
    public static final double DEFAULT_PERCENTAGE = 1;

    /**
     * Description for parameter percentage.
     */
    public static final String PERCENTAGE_D = "average percentage p, 0 < p <= 1, of distances randomly choosen to be provided in the result (default: "
        + DEFAULT_PERCENTAGE + ")";

    /**
     * Holds the parameter percentage.
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
        optionHandler.put(K_PARAM);

        //parameter percentage
        ArrayList<ParameterConstraint<Number>> percentageCons = new ArrayList<ParameterConstraint<Number>>();
        percentageCons.add(new GreaterConstraint(0));
        percentageCons.add(new LessEqualConstraint(1));
        DoubleParameter per = new DoubleParameter(PERCENTAGE_P, PERCENTAGE_D, percentageCons);
        per.setDefaultValue(DEFAULT_PERCENTAGE);
        optionHandler.put(per);
    }

    /**
     * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    protected
    @Override
    void runInTime(Database<O> database) throws IllegalStateException {
        int k;
        try {
            k = K_PARAM.getValue();
        }
        catch (UnusedParameterException e) {
            throw new IllegalStateException(e);
        }

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
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        //parameter percentage
        percentage = (Double) optionHandler.getOptionValue(PERCENTAGE_P);

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
