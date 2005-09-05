package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.ClustersPlusNoise;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.distance.Distance;

import java.util.*;

/**
 * DBSCAN provides the DBSCAN algorithm.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DBSCAN<T extends MetricalObject> extends DistanceBasedAlgorithm<T> {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

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
  private List<List<Integer>> resultList;

  /**
   * Provides the result of the algorithm.
   */
  protected Result result;

  /**
   * Holds a set of noise.
   */
  private Set<Integer> noise;

  /**
   * Holds a set of processed ids.
   */
  private Set<Integer> processedIDs;

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
  public void run(Database<T> database) throws IllegalStateException {
    long start = System.currentTimeMillis();
    try {
      Progress progress = new Progress(database.size());
      resultList = new ArrayList<List<Integer>>();
      noise = new HashSet<Integer>();
      processedIDs = new HashSet<Integer>(database.size());
      getDistanceFunction().setDatabase(database, isVerbose());

      for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
        Integer id = iter.next();
        if (!processedIDs.contains(id)) {
          expandCluster(database, id, progress);
        }
      }

      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.println("\r" + progress.toString() + " Number of clusters: " + resultList.size() + ".                           ");
      }
      
      Integer[][] resultArray = new Integer[resultList.size() + 1][];
      int i = 0;
      for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
        resultArray[i] = resultListIter.next().toArray(new Integer[0]);
      }
      resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
      result = new ClustersPlusNoise<T>(resultArray, database, getDistanceFunction().valueOf(epsilon), minpts);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    long end = System.currentTimeMillis();
    if (isTime()) {
      long elapsedTime = end - start;
      System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  /**
   * DBSCAN-function expandCluster. <p/> Border-Objects become members of the
   * first possible cluster.
   *
   * @param database      the database on which the algorithm is run
   * @param startObjectID potential seed of a new potential cluster
   * @return boolean true if a cluster was extended successfully
   */
  protected boolean expandCluster(Database<T> database, Integer startObjectID, Progress progress) {
    Set<Integer> processedIDsOLD = new HashSet<Integer>(processedIDs);
    Set<Integer> noiseOLD = new HashSet<Integer>(noise);

    List<QueryResult> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());
    if (seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.print("\r" + progress.toString() + " Number of clusters: " + resultList.size() + ".                           ");
      }
      return false;
    }
    else {
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
        List<QueryResult> neighborhood = database.rangeQuery(o, epsilon, getDistanceFunction());
        if (neighborhood.size() >= minpts) {
          for (QueryResult neighbor : neighborhood) {
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
          int numClusters = currentCluster.size() > minpts? resultList.size() + 1 : resultList.size();
          System.out.print("\r" + progress.toString() + " Number of clusters: " + numClusters + ".                           ");
        }
      }
      if (currentCluster.size() >= minpts) {
        resultList.add(currentCluster);
        return true;
      }
      else {
        processedIDs = processedIDsOLD;
        noise = noiseOLD;
        noise.add(startObjectID);
        processedIDs.add(startObjectID);
        return false;
      }
    }
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("DBSCAN", "Density-Based Clustering of Applications with Noise", "Algorithm to find density-connected sets in a database based on the parameters minimumPoints and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.", "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. In: Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996.");
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
  public Result getResult() {
    return result;
  }

  /**
   * Returns the epsilon parameter.
   * @return the epsilon parameter
   */
  public Distance getEpsilon() {
    return getDistanceFunction().valueOf(epsilon);
  }

  /**
   * Returns the minpts parameter.
   * @return the minpts parameter
   */
  public int getMinpts() {
    return minpts;
  }
}
