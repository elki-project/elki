package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DimensionsSelectingEuklideanDistanceFunction;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Implementation of the SUBCLU algorithm, an algorithm to
 * detect arbitrarily shaped and positioned clusters
 * in subspaces.
 * SUBCLU delivers for each subspace the same clusters
 * DBSCAN would have found, when applied to this
 * subspace seperately.
 *
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Algorithm
 * @param <D> the type of Distance used
 * todo implementation
 */
public class SUBCLU<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V> implements Clustering<V> {

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "subclu.distancefunction",
        "Classname of the distance function to determine the distance between database objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(AbstractDimensionsSelectingDoubleDistanceFunction.class) + "."
    );

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "subclu.epsilon",
        "The maximum radius of the neighborhood to be considered."
    );

    /**
     * OptionID for {@link #MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "subclu.minpts",
        "Threshold for minimum number of points in the epsilon-neighborhood of a point."
    );

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link AbstractDimensionsSelectingDoubleDistanceFunction}.
     * <p>Key: {@code -subclu.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -subclu.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * The distance function to determine the distance between database objects.
     * <p>Default value: {@link DimensionsSelectingEuklideanDistanceFunction} </p>
     * <p>Key: {@code -subclu.distancefunction} </p>
     */
    private final ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction>(
            DISTANCE_FUNCTION_ID,
            AbstractDimensionsSelectingDoubleDistanceFunction.class,
            DimensionsSelectingEuklideanDistanceFunction.class.getName());

    /**
     * The maximum radius of the neighborhood to be considered.
     */
    private String epsilon;

    /**
     * The threshold for minimum number of points in the
     * epsilon-neighborhood of a point.
     */
    private int minpts;

    /**
     * The distance function.
     */
    private AbstractDimensionsSelectingDoubleDistanceFunction<V> distanceFunction;

    /**
     * Sets epsilon and minimum points to the optionhandler additionally to the
     * parameters provided by super-classes.
     */
    public SUBCLU() {
        super();
        this.debug = true;

        // parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter minpts
        addOption(MINPTS_PARAM);

        // distance function
        addOption(DISTANCE_FUNCTION_PARAM);

        // global constraint epsilon <-> distance function
        // noinspection unchecked
        GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);
    }


    /**
     * Performs the SUBCLU algorithm on the given database.
     *
     * @param database the database to run the algorithm on
     * @throws IllegalStateException if the algorithm has not been initialized
     *                               properly (e.g. the setParameters(String[]) method has been failed
     *                               to be called).
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        try {
            int dimensionality = database.dimensionality();
            // 1. Generate all 1-D clusters
            if (isVerbose()) {
                verbose("*** Step 1: Generate all 1-D clusters ***");
            }
            for (int d = 0; d < dimensionality; d++) {
                BitSet selectedDimensions = new BitSet();
                selectedDimensions.set(d);
                DBSCAN<V, D> dbscan = initDBSCAN(selectedDimensions);
                dbscan.run(database);
                ClustersPlusNoise<V> clusters = dbscan.getResult();
                if (debug) {
                    debugFine(d + " clusters: " + clusters);
                }
            }
        }
        catch (ParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public ClusteringResult<V> getResult() {
        // todo
        return null;
    }

    /**
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    public Description getDescription() {
        return new Description("SUBCLU",
            "Density connected Subspace Clustering",
            "Algorithm to detect arbitrarily shaped and positioned clusters " +
                "in subspaces. SUBCLU delivers for each subspace the same clusters " +
                "DBSCAN would have found, when applied to this subspace seperately.. ",
            "K. Kailing, H.-P. Kriegel, P. Kroeger: " +
                "Density connected Subspace Clustering for High Dimensional Data. " +
                "In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.");
    }

    /**
     * Sets the parameters epsilon and minpts additionally to the parameters set
     * by the super-class' method. Both epsilon and minpts are required
     * parameters.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon
        epsilon = getParameterValue(EPSILON_PARAM);
        // minpts
        minpts = getParameterValue(MINPTS_PARAM);

        // distance function
        // noinspection unchecked
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Initializes the DBSCAN algorithm
     *
     * @param selectedDimensions the dimensions to be considered for distance computation
     * @return an instance of the DBSCAN algorithm
     * @throws ParameterException in case of wrong parameter-setting
     */
    private DBSCAN<V, D> initDBSCAN(BitSet selectedDimensions) throws ParameterException {
        DBSCAN<V, D> dbscan = new DBSCAN<V, D>();
        List<String> parameters = new ArrayList<String>();

        // distance function
        Util.addParameter(parameters, DISTANCE_FUNCTION_PARAM, distanceFunction.getClass().getName());

        // selected dimensions for distance function
        parameters.add(OptionHandler.OPTION_PREFIX + AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_P);
        parameters.add(Util.parseSelectedBits(selectedDimensions, ","));

        // aditional distance function paramaters
        String[] distanceFunctionParams = distanceFunction.getParameters();
        for (String param : distanceFunctionParams) {
            parameters.add(param);
        }

        // epsilon
        Util.addParameter(parameters, EPSILON_PARAM, epsilon);

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(minpts));

        dbscan.setParameters(parameters.toArray(new String[parameters.size()]));
        return dbscan;
    }
}
