package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.IntegerDistance;
import de.lmu.ifi.dbs.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * Shared nearest neighbor clustering.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used for the preprocessing of the shared nearest neighbors neighborhood lists
 */
public class SNNClustering<O extends DatabaseObject, D extends Distance<D>> extends AbstractAlgorithm<O> implements Clustering<O> {

  /**
   * Parameter for epsilon.
   */
  public static final IntParameter EPSILON_PARAM = new IntParameter("epsilon", "the minimum SNN density", new GreaterConstraint(0));

  /**
   * Parameter minimum points.
   */
  public static final IntParameter MINPTS_PARAM = new IntParameter("minpts", "threshold for minimum number of points in the epsilon-SNN-neighborhood of a point", new GreaterConstraint(0));

  /**
   * Epsilon.
   */
  private IntegerDistance epsilon;

  /**
   * Minimum points.
   */
  private int minpts;

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

  private SharedNearestNeighborSimilarityFunction<O, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<O, D>();

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes.
   */
  public SNNClustering() {
    super();
    addOption(EPSILON_PARAM);
    addOption(MINPTS_PARAM);
  }

  /**
   * Performs the SNN clustering algorithm on the given database.
   *
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
   */
  @Override
  protected void runInTime(Database<O> database) {
    Progress progress = new Progress("Clustering", database.size());
    resultList = new ArrayList<List<Integer>>();
    noise = new HashSet<Integer>();
    processedIDs = new HashSet<Integer>(database.size());
    similarityFunction.setDatabase(database, isVerbose(), isTime());
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

  protected List<Integer> findSNNNeighbors(Database<O> database, Integer queryObject) {
    List<Integer> neighbors = new LinkedList<Integer>();
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if (similarityFunction.similarity(queryObject, id).compareTo(epsilon) >= 0) {
        neighbors.add(id);
      }
    }
    return neighbors;
  }

  /**
   * DBSCAN-function expandCluster adapted to SNN criterion.<p/>
   * <p/>
   * Border-Objects become members of the
   * first possible cluster.
   *
   * @param database      the database on which the algorithm is run
   * @param startObjectID potential seed of a new potential cluster
   * @param progress      the progress object to report about the progress of clustering
   */
  protected void expandCluster(Database<O> database, Integer startObjectID, Progress progress) {
    List<Integer> seeds = findSNNNeighbors(database, startObjectID);

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
    for (Integer seed : seeds) {
      if (!processedIDs.contains(seed)) {
        currentCluster.add(seed);
        processedIDs.add(seed);
      }
      else if (noise.contains(seed)) {
        currentCluster.add(seed);
        noise.remove(seed);
      }
    }
    seeds.remove(0);

    while (seeds.size() > 0) {
      Integer o = seeds.remove(0);
      List<Integer> neighborhood = findSNNNeighbors(database, o);

      if (neighborhood.size() >= minpts) {
        for (Integer p : neighborhood) {
          boolean inNoise = noise.contains(p);
          boolean unclassified = !processedIDs.contains(p);
          if (inNoise || unclassified) {
            if (unclassified) {
              seeds.add(p);
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
    return new Description("SNN", "Shared Nearest Neighbor Clustering", "Algorithm to find shared-nearest-neighbors-density-connected sets in a database based on the parameters minPts and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.", "L. Ert\u00F6z, M. Steinbach, V. Kumar: Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High Dimensional Data. In: Proc. of SIAM Data Mining (SDM), 2003");
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

    epsilon = new IntegerDistance(getParameterValue(EPSILON_PARAM));

    // minpts
    minpts = getParameterValue(MINPTS_PARAM);

    remainingParameters = similarityFunction.setParameters(remainingParameters);
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
    return this.getOptions();
  }

  public IntegerDistance getEpsilon() {
    return epsilon;
  }

  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(similarityFunction.getAttributeSettings());
    return attributeSettings;
    }

    
}