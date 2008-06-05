package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract superclass for preprocessor of algorithms extending
 * the ProjectedDBSCAN alghorithm.
 *
 * @author Arthur Zimek
 *         todo parameter
 */
public abstract class ProjectedDBSCANPreprocessor<D extends Distance<D>, V extends RealVector<V, ?>> extends AbstractParameterizable implements Preprocessor<V> {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -epsilon} </p>
     */
    public static final PatternParameter EPSILON_PARAM = new PatternParameter("epsilon",
        "the maximum radius of the neighborhood " +
        "to be considered, must be suitable to " +
        LocallyWeightedDistanceFunction.class.getName());


    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.PROJECTED_DBSCAN_MINPTS,
        new GreaterConstraint(0));

    /**
     * The default range query distance function.
     */
    public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

    /**
     * Parameter for range query distance function.
     */
    public static final String DISTANCE_FUNCTION_P = "distancefunction";

    /**
     * Description for parameter range query distance function.
     */
    public static final String DISTANCE_FUNCTION_D = "the distance function to determine the neighbors for variance analysis "
                                                     + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class)
                                                     + ". Default: "
                                                     + DEFAULT_DISTANCE_FUNCTION;

    /**
     * Contains the value of parameter epsilon;
     */
    private String epsilon;

    /**
     * The distance function for the variance analysis.
     */
    protected DistanceFunction<V, D> rangeQueryDistanceFunction;

    /**
     * Holds the value of parameter minpts.
     */
    private int minpts;

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    protected ProjectedDBSCANPreprocessor() {
        super();
        //parameter epsilon
        addOption(EPSILON_PARAM);

        //parameter minpts
        addOption(MINPTS_PARAM);

        // parameter range query distance function
        // noinspection unchecked
        ClassParameter<DistanceFunction<V, D>> distance = new ClassParameter(DISTANCE_FUNCTION_P, DISTANCE_FUNCTION_D, DistanceFunction.class);
        distance.setDefaultValue(DEFAULT_DISTANCE_FUNCTION);
        optionHandler.put(distance);

        // global constraint epsilon <-> distancefunction
        GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint<DistanceFunction<V, D>>(EPSILON_PARAM, distance);
        optionHandler.setGlobalParameterConstraint(gpc);
    }

    /**
     * @see Preprocessor#run(de.lmu.ifi.dbs.database.Database,boolean,boolean)
     */
    public void run(Database<V> database, boolean verbose, boolean time) {
        if (database == null) {
            throw new IllegalArgumentException("Database must not be null!");
        }

        long start = System.currentTimeMillis();
        rangeQueryDistanceFunction.setDatabase(database, verbose, time);

        Progress progress = new Progress(this.getClass().getName(), database.size());
        if (verbose) {
            verbose("Preprocessing:");
        }
        Iterator<Integer> it = database.iterator();
        int processed = 1;
        while (it.hasNext()) {
            Integer id = it.next();
            List<QueryResult<D>> neighbors = database.rangeQuery(id, epsilon, rangeQueryDistanceFunction);

            if (neighbors.size() >= minpts) {
                runVarianceAnalysis(id, neighbors, database);
            }
            else {
                QueryResult<D> firstQR = neighbors.get(0);
                neighbors = new ArrayList<QueryResult<D>>();
                neighbors.add(firstQR);
                runVarianceAnalysis(id, neighbors, database);
            }

            progress.setProcessed(processed++);
            if (verbose) {
                progress(progress);
            }
        }
        if (verbose) {
            verbose("");
        }

        long end = System.currentTimeMillis();
        if (time) {
            long elapsedTime = end - start;
            verbose(this.getClass().getName() + " runtime: "
                    + elapsedTime + " milliseconds.");
        }
    }

    /**
     * This method implements the type of variance analysis to be computed for a given point.
     * <p/>
     * Example1: for 4C, this method should implement a PCA for the given point.
     * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
     *
     * @param id        the given point
     * @param neighbors the neighbors as query results of the given point
     * @param database  the database for which the preprocessing is performed
     */
    protected abstract void runVarianceAnalysis(Integer id, List<QueryResult<D>> neighbors, Database<V> database);

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // range query distance function
        String className = (String) optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
        try {
            // noinspection unchecked
            rangeQueryDistanceFunction = Util.instantiate(DistanceFunction.class, className);
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(DISTANCE_FUNCTION_P, className, DISTANCE_FUNCTION_D, e);
        }

        remainingParameters = rangeQueryDistanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        // epsilon
        epsilon = getParameterValue(EPSILON_PARAM);

        // minpts
        minpts = getParameterValue(MINPTS_PARAM);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(rangeQueryDistanceFunction.getAttributeSettings());
        return attributeSettings;
    }

}