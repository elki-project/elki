package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.DimensionsSelectingEuklideanDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

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
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SUBCLU<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V> implements Clustering<V> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to AbstractDimensionsSelectingDoubleDistanceFunction.
     * <p>Key: (@code -epsilon) </p>
     */
    public static final PatternParameter EPSILON_PARAM = new PatternParameter("epsilon",
                                                                              "the maximum radius of the neighborhood " +
                                                                              "to be considered, must be suitable to " +
                                                                              AbstractDimensionsSelectingDoubleDistanceFunction.class.getName());

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be greater than 0.
     * <p>Key: (@code -minpts) </p>
     */
    public static final IntParameter MINPTS_PARAM = new IntParameter("minpts",
                                                                     "threshold for minimum number of points in the " +
                                                                     "epsilon-neighborhood of a point",
                                                                     new GreaterConstraint(0));


    /**
     * The default distance function.
     */
    public static final Class DEFAULT_DISTANCE_FUNCTION = DimensionsSelectingEuklideanDistanceFunction.class;

    /**
     * The distance function to determine the distance between database objects.
     * <p>Default value: DimensionsSelectingEuklideanDistanceFunction </p>
     * <p>Key: (@code -distancefunction) </p>
     */
    // noinspection unchecked
    public static ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction> DISTANCEFUNCTION_PARAM = new ClassParameter("distancefunction",
                                                                                                                                "the distance function to determine the distance between database objects "
                                                                                                                                + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(AbstractDimensionsSelectingDoubleDistanceFunction.class)
                                                                                                                                + ". Default: " + DEFAULT_DISTANCE_FUNCTION.getName(),
                                                                                                                                AbstractDimensionsSelectingDoubleDistanceFunction.class);

    static {
        DISTANCEFUNCTION_PARAM.setDefaultValue(DEFAULT_DISTANCE_FUNCTION.getName());
    }

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
        optionHandler.put(EPSILON_PARAM);

        // parameter minpts
        optionHandler.put(MINPTS_PARAM);

        // distance function
        optionHandler.put(DISTANCEFUNCTION_PARAM);

        // global constraint epsilon <-> distance function
        // noinspection unchecked
        GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(EPSILON_PARAM, DISTANCEFUNCTION_PARAM);
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
            e.printStackTrace();
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
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // distance function
        try {
            // noinspection unchecked
            distanceFunction = Util.instantiate(AbstractDimensionsSelectingDoubleDistanceFunction.class, DISTANCEFUNCTION_PARAM.getValue());
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(DISTANCEFUNCTION_PARAM.getName(),
                                                   DISTANCEFUNCTION_PARAM.getValue(),
                                                   DISTANCEFUNCTION_PARAM.getDescription(),
                                                   e);
        }
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
        parameters.add(OptionHandler.OPTION_PREFIX + DISTANCEFUNCTION_PARAM.getName());
        parameters.add(distanceFunction.getClass().toString());

        // selected dimensions for distance function
        parameters.add(OptionHandler.OPTION_PREFIX + AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_P);
        parameters.add(Util.parseSelectedBits(selectedDimensions, ","));

        // aditional distance function paramaters
        String[] distanceFunctionParams = distanceFunction.getParameters();
        for (String param : distanceFunctionParams) {
            parameters.add(param);
        }

        // epsilon
        parameters.add(OptionHandler.OPTION_PREFIX + EPSILON_PARAM.getName());
        parameters.add(EPSILON_PARAM.getValue());

        // minpts
        parameters.add(OptionHandler.OPTION_PREFIX + MINPTS_PARAM.getName());
        parameters.add(MINPTS_PARAM.getValue().toString());

        dbscan.setParameters(parameters.toArray(new String[parameters.size()]));
        return dbscan;
    }
}
