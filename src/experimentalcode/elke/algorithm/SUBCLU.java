package experimentalcode.elke.algorithm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * <p>
 * Implementation of the SUBCLU algorithm, an algorithm to
 * detect arbitrarily shaped and positioned clusters
 * in subspaces.</p>
 * SUBCLU delivers for each subspace the same clusters
 * DBSCAN would have found, when applied to this
 * subspace separately.
 * 
 * <p>
 * Reference:
 * <br>K. Kailing, H.-P. Kriegel, P. Kroeger:
 * Density connected Subspace Clustering for High Dimensional Data.
 * <br>In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.
 * </p>
 *
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Algorithm
 * @param <D> the type of Distance used
 */
// todo elke implementation
public class SUBCLU<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>,V> {

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "subclu.distancefunction",
        "Distance function to determine the distance between database objects."
    );

    /**
     * The distance function to determine the distance between database objects.
     * <p>Default value: {@link DimensionsSelectingEuclideanDistanceFunction} </p>
     * <p>Key: {@code -subclu.distancefunction} </p>
     */
    private final ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>>(
            DISTANCE_FUNCTION_ID,
            AbstractDimensionsSelectingDoubleDistanceFunction.class,
            DimensionsSelectingEuclideanDistanceFunction.class.getName());

    /**
     * Holds the instance of the distance function specified by {@link #DISTANCE_FUNCTION_PARAM}.
     */
    private AbstractDimensionsSelectingDoubleDistanceFunction<V> distanceFunction;

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "subclu.epsilon",
        "The maximum radius of the neighborhood to be considered."
    );

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link AbstractDimensionsSelectingDoubleDistanceFunction}.
     * <p>Key: {@code -subclu.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);

    /**
     * Holds the value of {@link #EPSILON_PARAM}.
     */
    private String epsilon;

    /**
     * OptionID for {@link #MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "subclu.minpts",
        "Threshold for minimum number of points in the epsilon-neighborhood of a point."
    );

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
     * Holds the value of {@link #MINPTS_PARAM}.
     */
    private int minpts;

    /**
     * Holds the result;
     */
    private Clustering<Model> result;

    /**
     * Provides the SUBCLU algorithm,
     * adding parameters
     * {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM}, and {@link #DISTANCE_FUNCTION_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public SUBCLU() {
        super();
        //this.debug = true;

        // parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter minpts
        addOption(MINPTS_PARAM);

        // distance function
        addOption(DISTANCE_FUNCTION_PARAM);

        // global constraint epsilon <-> distance function
        optionHandler.setGlobalParameterConstraint(
            new GlobalDistanceFunctionPatternConstraint<AbstractDimensionsSelectingDoubleDistanceFunction<V>>(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM));
    }


    /**
     * Performs the SUBCLU algorithm on the given database.
     *
     */
    @Override
    protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
        try {
            int dimensionality = database.dimensionality();
            // 1. Generate all 1-D clusters
            if (logger.isVerbose()) {
              logger.verbose("*** Step 1: Generate all 1-D clusters ***");
            }
            for (int d = 0; d < dimensionality; d++) {
                BitSet selectedDimensions = new BitSet();
                selectedDimensions.set(d);
                DBSCAN<V, D> dbscan = initDBSCAN(selectedDimensions);
                dbscan.run(database);
                Clustering<Model> clusters = dbscan.getResult();
                if (logger.isDebugging()) {
                  logger.debugFine(d + " clusters: " + clusters);
                }
                // FIXME: INCOMPLETE IMPLEMENTATION
            }
        }
        catch (ParameterException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public Clustering<Model> getResult() {
        return result;
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
                "DBSCAN would have found, when applied to this subspace seperately. ",
            "K. Kailing, H.-P. Kriegel, P. Kroeger: " +
                "Density connected Subspace Clustering for High Dimensional Data. " +
                "In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.");
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameters
     * {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM}, and {@link #DISTANCE_FUNCTION_PARAM}
     * and instantiates {@link #distanceFunction} according to the value of parameter
     * {@link #DISTANCE_FUNCTION_PARAM}.
     * The remaining parameters are passed to the {@link #distanceFunction}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon
        epsilon = EPSILON_PARAM.getValue();
        // minpts
        minpts = MINPTS_PARAM.getValue();

        // distance function
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        addParameterizable(distanceFunction);
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        
        rememberParametersExcept(args, remainingParameters);
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
        OptionUtil.addParameter(parameters, DISTANCE_FUNCTION_PARAM, distanceFunction.getClass().getName());

        // selected dimensions for distance function
        parameters.add(OptionHandler.OPTION_PREFIX + AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_ID.getName());
        parameters.add(Util.parseSelectedBits(selectedDimensions, ","));

        // additional distance function parameters
        String[] distanceFunctionParams = distanceFunction.getParameters();
        for (String param : distanceFunctionParams) {
            parameters.add(param);
        }

        // epsilon
        OptionUtil.addParameter(parameters, EPSILON_PARAM, epsilon);

        // minpts
        OptionUtil.addParameter(parameters, MINPTS_PARAM, Integer.toString(minpts));

        dbscan.setParameters(parameters.toArray(new String[parameters.size()]));
        return dbscan;
    }
}
