package de.lmu.ifi.dbs.algorithm.clustering;


import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.*;

/**
 * DBSCAN provides the DBSCAN algorithm.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DBSCAN<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> implements Clustering<O> {

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>the maximum radius of the neighborhood to " +
                                         "be considerd, must be suitable to the " +
                                         "specified distance function";
  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<minpts>threshold for minumum number of points in the epsilon-" +
                                        "neighborhood of a point";

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
   * parameters provided by super-classes. Since DBSCAN is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public DBSCAN() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    if (isVerbose()) {
      System.out.println();
    }
    try {
      Progress progress = new Progress(database.size());
      resultList = new ArrayList<List<Integer>>();
      noise = new HashSet<Integer>();
      processedIDs = new HashSet<Integer>(database.size());
      getDistanceFunction().setDatabase(database, isVerbose());
      if (isVerbose()) {
        System.out.println("\nClustering:");
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
            System.out.print(Util.status(progress, resultList.size()));
          }
        }
      }
      else {
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
          Integer id = iter.next();
          noise.add(id);
          if (isVerbose()) {
            progress.setProcessed(noise.size());
            System.out.print(Util.status(progress, resultList.size()));
          }
        }
      }

//      if (isVerbose()) {
//        progress.setProcessed(processedIDs.size());
//        System.out.print(Util.status(progress, resultList.size()));
//      }

      Integer[][] resultArray = new Integer[resultList.size() + 1][];
      int i = 0;
      for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
        resultArray[i] = resultListIter.next().toArray(new Integer[0]);
      }

      resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
      result = new ClustersPlusNoise<O>(resultArray, database);
      if (isVerbose()) {
//        progress.setProcessed(processedIDs.size());
//        System.out.print(Util.status(progress, resultList.size()));
        System.out.println();
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }

  /**
   * DBSCAN-function expandCluster. <p/> Border-Objects become members of the
   * first possible cluster.
   *
   * @param database      the database on which the algorithm is run
   * @param startObjectID potential seed of a new potential cluster
   */
  protected void expandCluster(Database<O> database, Integer startObjectID, Progress progress) {
    List<QueryResult<D>> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());

    // startObject is no core-object
    if (seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.print(Util.status(progress, resultList.size()));
      }
      return;
    }

    List<Integer> currentCluster = new ArrayList<Integer>();
    for (QueryResult seed : seeds) {
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
        System.out.print(Util.status(progress, numClusters));
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
    return new Description("DBSCAN", "Density-Based Clustering of Applications with Noise", "Algorithm to find density-connected sets in a database based on the parameters " + "minimumPoints and epsilon (specifying a volume). " + "These two parameters determine a density threshold for clustering.", "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: " + "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. " + "In: Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), " + "Portland, OR, 1996.");
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      // test whether epsilon is compatible with distance function
      getDistanceFunction().valueOf(optionHandler.getOptionValue(EPSILON_P));
      epsilon = optionHandler.getOptionValue(EPSILON_P);
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public ClustersPlusNoise<O> getResult() {
    return result;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(EPSILON_P, epsilon);
    attributeSettings.addSetting(MINPTS_P, Integer.toString(minpts));

    return result;
  }

}
