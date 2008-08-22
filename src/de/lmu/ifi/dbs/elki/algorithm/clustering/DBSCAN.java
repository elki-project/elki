package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * DBSCAN provides the DBSCAN algorithm,
 * an algorithm to find density-connected sets in a database.
 * <p>Reference:
 * <br>M. Ester, H.-P. Kriegel, J. Sander, and X. Xu:
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise.
 * <br>In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used
 */
public class DBSCAN<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> implements Clustering<O> {
    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN#EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "dbscan.epsilon",
        "The maximum radius of the neighborhood to be considered."
    );

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to the distance function specified.
     * <p>Key: {@code -dbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);

    /**
     * Holds the value of {@link #EPSILON_PARAM}.
     */
    private String epsilon;

    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN#MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "dbscan.minpts",
        " Threshold for minimum number of points in the epsilon-neighborhood of a point."
    );

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -dbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Holds the value of {@link #MINPTS_PARAM}.
     */
    protected int minpts;

    /**
     * Holds a list of clusters found.
     */
    protected List<List<Integer>> resultList;

    /**
     * Provides the result of the algorithm.
     */
    protected ClustersPlusNoise<O> result;

    /**
     * Holds a set of noise.
     */
    protected Set<Integer> noise;

    /**
     * Holds a set of processed ids.
     */
    protected Set<Integer> processedIDs;

    /**
     * Provides the DBSCAN algorithm,
     * adding parameters {@link #EPSILON_PARAM} and
     * {@link #MINPTS_PARAM} to the option handler
     * additionally to parameters of super class.
     */
    @SuppressWarnings("unchecked")
    public DBSCAN() {
        super();
        // parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter minpts
        addOption(MINPTS_PARAM);

        // global constraint
        // noinspection unchecked
        GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);
    }

    /**
     * Performs the DBSCAN algorithm on the given database.
     *
     * @see de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
     */
    @Override
    protected void runInTime(Database<O> database) throws IllegalStateException {
        Progress progress = new Progress("Clustering", database.size());
        resultList = new ArrayList<List<Integer>>();
        noise = new HashSet<Integer>();
        processedIDs = new HashSet<Integer>(database.size());
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        if (isVerbose()) {
            verbose("Clustering:");
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
                    progress.setProcessed(noise.size());
                    progress(progress, resultList.size());
                }
            }
        }

        Integer[][] resultArray = new Integer[resultList.size() + 1][];
        int i = 0;
        for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
            resultArray[i] = resultListIter.next().toArray(new Integer[0]);
        }

        resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
        result = new ClustersPlusNoise<O>(resultArray, database);
        if (isVerbose()) {
            verbose("");
        }
    }

    /**
     * DBSCAN-function expandCluster. <p/> Border-Objects become members of the
     * first possible cluster.
     *
     * @param database      the database on which the algorithm is run
     * @param startObjectID potential seed of a new potential cluster
     * @param progress      the progress object for logging the current status
     */
    protected void expandCluster(Database<O> database, Integer startObjectID, Progress progress) {
        List<QueryResult<D>> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());

        // startObject is no core-object
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
        for (QueryResult<D> seed : seeds) {
            Integer nextID = seed.getID();
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
            Integer o = seeds.remove(0).getID();
            List<QueryResult<D>> neighborhood = database.rangeQuery(o, epsilon, getDistanceFunction());

            if (neighborhood.size() >= minpts) {
                for (QueryResult<D> neighbor : neighborhood) {
                    Integer p = neighbor.getID();
                    boolean inNoise = noise.contains(p);
                    boolean unclassified = !processedIDs.contains(p);
                    if (inNoise || unclassified) {
                        if (unclassified) {
                            seeds.add(neighbor);
                        }
                        currentCluster.add(p);
                        processedIDs.add(p);
                        if (inNoise) {
                            noise.remove(p);
                        }
                    }
                }
            }

            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
                progress(progress, numClusters);
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
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("DBSCAN", "Density-Based Clustering of Applications with Noise",
            "Algorithm to find density-connected sets in a database based on the parameters " +
                MINPTS_PARAM.getName() + " and " + EPSILON_PARAM.getName() + " (specifying a volume). " +
                "These two parameters determine a density threshold for clustering.",
            "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: " +
                "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. " +
                "In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996.");
    }

    /**
     * Calls the super method
     * and sets additionally the values of the parameters
     * {@link #EPSILON_PARAM} and {@link #MINPTS_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon, minpts
        epsilon = getParameterValue(EPSILON_PARAM);
        minpts = getParameterValue(MINPTS_PARAM);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
     */
    public ClustersPlusNoise<O> getResult() {
        return result;
    }
}