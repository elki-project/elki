package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * DBSCAN provides the DBSCAN algorithm, an algorithm to find density-connected
 * sets in a database.
 * <p>
 * Reference: <br>
 * M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: A Density-Based Algorithm for
 * Discovering Clusters in Large Spatial Databases with Noise. <br>
 * In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96),
 * Portland, OR, 1996.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used
 */
@Title("DBSCAN: Density-Based Clustering of Applications with Noise")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minpts' and 'epsilon' (specifying a volume). " + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu", title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996")
public class DBSCAN<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN#EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("dbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   * <p>
   * Key: {@code -dbscan.epsilon}
   * </p>
   */
  private final DistanceParameter<D> EPSILON_PARAM;

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  private D epsilon;

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN#MINPTS_PARAM}
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("dbscan.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -dbscan.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

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
  protected Clustering<Model> result;

  /**
   * Holds a set of noise.
   */
  protected Set<Integer> noise;

  /**
   * Holds a set of processed ids.
   */
  protected Set<Integer> processedIDs;

  /**
   * Provides the DBSCAN algorithm, adding parameters {@link #EPSILON_PARAM} and
   * {@link #MINPTS_PARAM} to the option handler additionally to parameters of
   * super class.
   */
  public DBSCAN(Parameterization config) {
    super(config);
    // parameter epsilon
    EPSILON_PARAM = new DistanceParameter<D>(EPSILON_ID, getDistanceFunction());

    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }

    // parameter minpts
    if(config.grab(MINPTS_PARAM)) {
      minpts = MINPTS_PARAM.getValue();
    }
  }

  /**
   * Performs the DBSCAN algorithm on the given database.
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    FiniteProgress objprog = logger.isVerbose() ? new FiniteProgress("Processing objects", database.size()) : null;
    IndefiniteProgress clusprog = logger.isVerbose() ? new IndefiniteProgress("Number of clusters") : null;
    resultList = new ArrayList<List<Integer>>();
    noise = new HashSet<Integer>();
    processedIDs = new HashSet<Integer>(database.size());
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    if(logger.isVerbose()) {
      logger.verbose("Clustering:");
    }
    if(database.size() >= minpts) {
      for(Integer id : database) {
        if(!processedIDs.contains(id)) {
          expandCluster(database, id, objprog, clusprog);
          if(processedIDs.size() == database.size()) {
            break;
          }
        }
        if(objprog != null && clusprog != null && logger.isVerbose()) {
          objprog.setProcessed(processedIDs.size());
          clusprog.setProcessed(resultList.size());
          logger.progress(objprog);
          logger.progress(clusprog);
        }
      }
    }
    else {
      for(Integer id : database) {
        noise.add(id);
        if(objprog != null && clusprog != null && logger.isVerbose()) {
          objprog.setProcessed(noise.size());
          clusprog.setProcessed(resultList.size());
          logger.progress(objprog);
          logger.progress(clusprog);
        }
      }
    }
    // Signal that the progress has completed.
    if(clusprog != null) {
      clusprog.setCompleted();
      logger.progress(clusprog);
    }

    result = new Clustering<Model>();
    for(List<Integer> res : resultList) {
      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(res);
      Cluster<Model> c = new Cluster<Model>(group, ClusterModel.CLUSTER);
      result.addCluster(c);
    }

    DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Set<Integer>>(noise);
    Cluster<Model> n = new Cluster<Model>(group, true, ClusterModel.CLUSTER);
    result.addCluster(n);

    return result;
  }

  /**
   * DBSCAN-function expandCluster.
   * <p/>
   * Border-Objects become members of the first possible cluster.
   * 
   * @param database the database on which the algorithm is run
   * @param startObjectID potential seed of a new potential cluster
   * @param objprog the progress object for logging the current status
   */
  protected void expandCluster(Database<O> database, Integer startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    List<DistanceResultPair<D>> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());

    // startObject is no core-object
    if(seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null && logger.isVerbose()) {
        objprog.setProcessed(processedIDs.size());
        clusprog.setProcessed(resultList.size());
        logger.progress(objprog);
        logger.progress(clusprog);
      }
      return;
    }

    // try to expand the cluster
    List<Integer> currentCluster = new ArrayList<Integer>();
    for(DistanceResultPair<D> seed : seeds) {
      Integer nextID = seed.getID();
      if(!processedIDs.contains(nextID)) {
        currentCluster.add(nextID);
        processedIDs.add(nextID);
      }
      else if(noise.contains(nextID)) {
        currentCluster.add(nextID);
        noise.remove(nextID);
      }
    }
    seeds.remove(0);

    while(seeds.size() > 0) {
      Integer o = seeds.remove(0).getID();
      List<DistanceResultPair<D>> neighborhood = database.rangeQuery(o, epsilon, getDistanceFunction());

      if(neighborhood.size() >= minpts) {
        for(DistanceResultPair<D> neighbor : neighborhood) {
          Integer p = neighbor.getID();
          boolean inNoise = noise.contains(p);
          boolean unclassified = !processedIDs.contains(p);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(neighbor);
            }
            currentCluster.add(p);
            processedIDs.add(p);
            if(inNoise) {
              noise.remove(p);
            }
          }
        }
      }

      if(processedIDs.size() == database.size() && noise.size() == 0) {
        break;
      }

      if(objprog != null && clusprog != null && logger.isVerbose()) {
        objprog.setProcessed(processedIDs.size());
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters);
        logger.progress(objprog);
        logger.progress(clusprog);
      }
    }
    if(currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      for(Integer id : currentCluster) {
        noise.add(id);
      }
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }
  }

  public Clustering<Model> getResult() {
    return result;
  }
}