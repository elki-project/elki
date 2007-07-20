package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * DBSCAN provides the DBSCAN algorithm.
 * 
 * @author Arthur Zimek 
 */
public class DBSCAN<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> implements Clustering<O> {

	/**
	 * Parameter for epsilon.
	 */
	public static final String EPSILON_P = "epsilon";

	/**
	 * Description for parameter epsilon.
	 */
	public static final String EPSILON_D = "the maximum radius of the neighborhood to be considered, must be suitable to the distance function specified";

	/**
	 * Parameter minimum points.
	 */
	public static final String MINPTS_P = "minpts";

	/**
	 * Description for parameter minimum points.
	 */
	public static final String MINPTS_D = "threshold for minimum number of points in the epsilon-neighborhood of a point";

	/**
	 * Epsilon.
	 */
	protected String epsilon;

	/**
	 * Minimum points.
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
	 * Sets epsilon and minimum points to the optionhandler additionally to the
	 * parameters provided by super-classes.
	 */
	public DBSCAN() {
		super();
		PatternParameter eps = new PatternParameter(EPSILON_P, EPSILON_D);
		optionHandler.put(EPSILON_P, eps);
		// global constraint
		try {
			// noinspection unchecked
			GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(eps, (ClassParameter<DistanceFunction<?,?>>) optionHandler.getOption(DISTANCE_FUNCTION_P));
			optionHandler.setGlobalParameterConstraint(gpc);
		} catch (UnusedParameterException e) {
			verbose("Could not instantiate global parameter constraint concerning parameter " + EPSILON_P + " and " + DISTANCE_FUNCTION_P + " because parameter " + DISTANCE_FUNCTION_P + " is not specified! " + e.getMessage());
		}

		optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
	}

	/**
	 * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
	 */
	@Override
    protected void runInTime(Database<O> database) {
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
		} else {
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
	 * @param database
	 *            the database on which the algorithm is run
	 * @param startObjectID
	 *            potential seed of a new potential cluster
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
			} else if (noise.contains(nextID)) {
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
		} else {
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
		return new Description("DBSCAN", "Density-Based Clustering of Applications with Noise", "Algorithm to find density-connected sets in a database based on the parameters " + MINPTS_P + " and " + EPSILON_P + " (specifying a volume). " + "These two parameters determine a density threshold for clustering.", "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: " + "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. " + "In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996.");
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

		epsilon = (String) optionHandler.getOptionValue(EPSILON_P);

		// minpts
		minpts = (Integer) optionHandler.getOptionValue(MINPTS_P);

		setParameters(args, remainingParameters);
		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
	 */
	public ClustersPlusNoise<O> getResult() {
		return result;
	}

	public Option<?>[] getOptions() {
		return optionHandler.getOptions();
	}

	public String getEpsilon() {
		return epsilon;
	}

}