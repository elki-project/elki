package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.preprocessing.ProjectedDBSCANPreprocessor;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Provides an abstract algorithm requiring a VarianceAnalysisPreprocessor.
 *
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
public abstract class ProjectedDBSCAN<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V> {

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCEFUNCTION_ID = OptionID.getOrCreateOptionID(
        "projdbscan.distancefunction",
        "Classname of the distance function to determine the distance between database objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(AbstractLocallyWeightedDistanceFunction.class) + "."
    );

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID DBSCAN_EPSILON_ID = OptionID.getOrCreateOptionID(
        "projdbscan.epsilon",
        "The maximum radius of the neighborhood to be considered."
    );

    /**
     * OptionID for {@link #LAMBDA_PARAM}
     */
    public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID(
        "projdbscan.lambda",
        "The intrinsic dimensionality of the clusters to find."
    );

    /**
     * OptionID for {@link #MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "projdbscan.minpts",
        "Threshold for minimum number of points in " +
            "the epsilon-neighborhood of a point."
    );

    /**
     * Parameter to specify the distance function to determine the distance between database objects,
     * must extend {@link de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractLocallyWeightedDistanceFunction}.
     * <p>Key: {@code -projdbscan.distancefunction} </p>
     * <p>Default value: {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction} </p>
     */
    protected final ClassParameter<AbstractLocallyWeightedDistanceFunction> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<AbstractLocallyWeightedDistanceFunction>(
            DISTANCEFUNCTION_ID,
            AbstractLocallyWeightedDistanceFunction.class,
            LocallyWeightedDistanceFunction.class.getName());


    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -projdbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(DBSCAN_EPSILON_ID);

    /**
     * Parameter to specify the intrinsic dimensionality of the clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.lambda} </p>
     */
    private final IntParameter LAMBDA_PARAM = new IntParameter(
        LAMBDA_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Epsilon.
     */
    protected String epsilon;

    /**
     * Minimum points.
     */
    protected int minpts;

    /**
     * Keeps lambda.
     */
    private int lambda;

    /**
     * Holds a list of clusters found.
     */
    private List<List<Integer>> resultList;

    /**
     * Provides the result of the algorithm.
     */
    private ClustersPlusNoise<V> result;

    /**
     * Holds a set of noise.
     */
    private Set<Integer> noise;

    /**
     * Holds a set of processed ids.
     */
    private Set<Integer> processedIDs;

    /**
     * The distance function.
     */
    private AbstractLocallyWeightedDistanceFunction<V, ?> distanceFunction;

    /**
     * Provides the abstract algorithm for variance analysis based DBSCAN.
     */
    protected ProjectedDBSCAN() {
        super();

        // parameter epsilon
        addOption(EPSILON_PARAM);

        // minpts
        addOption(MINPTS_PARAM);

        // lambda
        addOption(LAMBDA_PARAM);

        // parameter distance function
        addOption(DISTANCE_FUNCTION_PARAM);

        // global parameter constraint epsilon <-> distance function
        // noinspection unchecked
        GlobalParameterConstraint con = new GlobalDistanceFunctionPatternConstraint(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM);
        optionHandler.setGlobalParameterConstraint(con);
    }

    /**
     * @see AbstractAlgorithm#runInTime(Database)
     */
    @Override
    protected void runInTime(Database<V> database) throws IllegalStateException {
        if (isVerbose()) {
            verbose("");
        }
        try {
            Progress progress = new Progress("Clustering", database.size());
            resultList = new ArrayList<List<Integer>>();
            noise = new HashSet<Integer>();
            processedIDs = new HashSet<Integer>(database.size());
            distanceFunction.setDatabase(database, isVerbose(), isTime());
            if (isVerbose()) {
                verbose("\nClustering:");
            }
            if (database.size() >= minpts) {
                for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
                    Integer id = iter.next();
                    if (!processedIDs.contains(id)) {
                        expandCluster(database, id, progress);
                        if (processedIDs.size() == database.size() && noise.size() == 0) {
                            break;
                        }
                    }
                    if (isVerbose()) {
                        progress.setProcessed(processedIDs.size());
                        progress(progress, resultList.size());
                    }
                }
            }
            else {
                for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
                    Integer id = iter.next();
                    noise.add(id);
                    if (isVerbose()) {
                        progress.setProcessed(processedIDs.size());
                        progress(progress, resultList.size());
                    }
                }
            }

            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                progress(progress, resultList.size());
            }

            Integer[][] resultArray = new Integer[resultList.size() + 1][];
            int i = 0;
            for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
                resultArray[i] = resultListIter.next().toArray(new Integer[0]);
            }

            resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
            result = new ClustersPlusNoise<V>(resultArray, database);
            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                progress(progress, resultList.size());
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * ExpandCluster function of DBSCAN.
     *
     * @param database      the database to run the algorithm on
     * @param startObjectID the object id of the database object to start the expansion with
     * @param progress      the progress object for logging the current status
     */
    protected void expandCluster(Database<V> database, Integer startObjectID, Progress progress) {
        String label = database.getAssociation(AssociationID.LABEL, startObjectID);
        Integer corrDim = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, startObjectID);

        if (this.debug) {
            debugFine("\nEXPAND CLUSTER id = " + startObjectID + " " + label + " " + corrDim + "\n#clusters: " + resultList.size());

        }

        // euclidean epsilon neighborhood < minpts OR local dimensionality >
        // lambda -> noise
        if (corrDim == null || corrDim > lambda) {
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                progress(progress, resultList.size());
            }
            return;
        }

        // compute weighted epsilon neighborhood
        List<QueryResult<DoubleDistance>> seeds = database.rangeQuery(startObjectID, epsilon, distanceFunction);
        // neighbors < minPts -> noise
        if (seeds.size() < minpts) {
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                progress(progress, resultList.size());
            }
            return;
        }

        // try to expand the cluster
        List<Integer> currentCluster = new ArrayList<Integer>();
        for (QueryResult<DoubleDistance> seed : seeds) {
            Integer nextID = seed.getID();

            Integer nextID_corrDim = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, nextID);
            // nextID is not reachable from start object
            if (nextID_corrDim > lambda)
                continue;

            if (!processedIDs.contains(nextID)) {
                currentCluster.add(nextID);
                processedIDs.add(nextID);
            }
            else if (noise.contains(nextID)) {
                currentCluster.add(nextID);
                noise.remove(nextID);
            }
        }
        seeds.remove(0);

        while (seeds.size() > 0) {
            Integer q = seeds.remove(0).getID();
            Integer corrDim_q = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, q);
            // q forms no lambda-dim hyperplane
            if (corrDim_q > lambda)
                continue;

            List<QueryResult<DoubleDistance>> reachables = database.rangeQuery(q, epsilon, distanceFunction);
            if (reachables.size() > minpts) {
                for (QueryResult<DoubleDistance> r : reachables) {
                    Integer corrDim_r = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, r.getID());
                    // r is not reachable from q
                    if (corrDim_r > lambda)
                        continue;

                    boolean inNoise = noise.contains(r.getID());
                    boolean unclassified = !processedIDs.contains(r.getID());
                    if (inNoise || unclassified) {
                        if (unclassified) {
                            seeds.add(r);
                        }
                        currentCluster.add(r.getID());
                        processedIDs.add(r.getID());
                        if (inNoise) {
                            noise.remove(r.getID());
                        }
                        if (isVerbose()) {
                            progress.setProcessed(processedIDs.size());
                            int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
                            progress(progress, numClusters);
                        }
                    }
                }
            }

            if (processedIDs.size() == database.size() && noise.size() == 0) {
                break;
            }
        }

        if (currentCluster.size() >= minpts) {
            resultList.add(currentCluster);
        }
        else {
            for (Integer id : currentCluster) {
                noise.add(id);
            }
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
        }

        if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            progress(progress, resultList.size());
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    @SuppressWarnings("unchecked")
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // distance function
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();

        // epsilon
        epsilon = getParameterValue(EPSILON_PARAM);

        // minpts
        minpts = getParameterValue(MINPTS_PARAM);

        // lambda
        lambda = getParameterValue(LAMBDA_PARAM);

        // parameters for the distance function
        String[] distanceFunctionParameters = new String[remainingParameters.length + 5];
        System.arraycopy(remainingParameters, 0, distanceFunctionParameters, 5, remainingParameters.length);

        // omit preprocessing flag
        distanceFunctionParameters[0] = OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F;
        // preprocessor
        distanceFunctionParameters[1] = OptionHandler.OPTION_PREFIX + PreprocessorHandler.PREPROCESSOR_CLASS_P;
        distanceFunctionParameters[2] = preprocessorClass().getName();
        // preprocessor epsilon
        distanceFunctionParameters[3] = OptionHandler.OPTION_PREFIX + ProjectedDBSCANPreprocessor.EPSILON_PARAM.getName();
        distanceFunctionParameters[4] = epsilon;
        // preprocessor minpts
        Util.addParameter(distanceFunctionParameters, MINPTS_PARAM, Integer.toString(minpts));

        distanceFunction.setParameters(distanceFunctionParameters);

        setParameters(args, remainingParameters);
        return remainingParameters;

    }

    /**
     * @see Algorithm#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(distanceFunction.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Returns the class actually used as
     * {@link ProjectedDBSCANPreprocessor VarianceAnalysisPreprocessor}.
     *
     * @return the class actually used as
     *         {@link ProjectedDBSCANPreprocessor VarianceAnalysisPreprocessor}
     */
    public abstract Class<?> preprocessorClass();

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
     */
    public ClustersPlusNoise<V> getResult() {
        return result;
    }
}