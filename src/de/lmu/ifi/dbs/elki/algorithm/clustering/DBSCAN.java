package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
@Reference(authors = "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu", title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996", url="http://dx.doi.org/10.1145/93605.98741")
public class DBSCAN<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("dbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private D epsilon;

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("dbscan.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  protected int minpts;

  /**
   * Holds a list of clusters found.
   */
  protected List<ModifiableDBIDs> resultList;

  /**
   * Holds a set of noise.
   */
  protected ModifiableDBIDs noise;

  /**
   * Holds a set of processed ids.
   */
  protected ModifiableDBIDs processedIDs;

  /**
   * Constructor with parameters.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts parameter
   */
  public DBSCAN(DistanceFunction<O, D> distanceFunction, D epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the DBSCAN algorithm on the given database.
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);
    
    FiniteProgress objprog = logger.isVerbose() ? new FiniteProgress("Processing objects", database.size(), logger) : null;
    IndefiniteProgress clusprog = logger.isVerbose() ? new IndefiniteProgress("Number of clusters", logger) : null;
    resultList = new ArrayList<ModifiableDBIDs>();
    noise = DBIDUtil.newHashSet();
    processedIDs = DBIDUtil.newHashSet(database.size());
    if(database.size() >= minpts) {
      for(DBID id : database) {
        if(!processedIDs.contains(id)) {
          expandCluster(database, distFunc, id, objprog, clusprog);
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
        }
        if(processedIDs.size() == database.size()) {
          break;
        }
      }
    }
    else {
      for(DBID id : database) {
        noise.add(id);
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(noise.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
        }
      }
    }
    // Finish progress logging
    if (objprog != null) {
      objprog.ensureCompleted(logger);
    }
    if(clusprog != null) {
      clusprog.setCompleted(logger);
    }

    Clustering<Model> result = new Clustering<Model>();
    for(ModifiableDBIDs res : resultList) {
      Cluster<Model> c = new Cluster<Model>(res, ClusterModel.CLUSTER);
      result.addCluster(c);
    }

    Cluster<Model> n = new Cluster<Model>(noise, true, ClusterModel.CLUSTER);
    result.addCluster(n);

    return result;
  }

  /**
   * DBSCAN-function expandCluster.
   * <p/>
   * Border-Objects become members of the first possible cluster.
   * 
   * @param database the database on which the algorithm is run
   * @param distFunc The distance function
   * @param startObjectID potential seed of a new potential cluster
   * @param objprog the progress object for logging the current status
   */
  protected void expandCluster(Database<O> database, DistanceQuery<O, D> distFunc, DBID startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    List<DistanceResultPair<D>> seeds = database.rangeQuery(startObjectID, epsilon, distFunc);

    // startObject is no core-object
    if(seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), logger);
        clusprog.setProcessed(resultList.size(), logger);
      }
      return;
    }

    // try to expand the cluster
    ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(DistanceResultPair<D> seed : seeds) {
      DBID nextID = seed.getID();
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
      DBID o = seeds.remove(0).getID();
      List<DistanceResultPair<D>> neighborhood = database.rangeQuery(o, epsilon, distFunc);

      if(neighborhood.size() >= minpts) {
        for(DistanceResultPair<D> neighbor : neighborhood) {
          DBID p = neighbor.getID();
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

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), logger);
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters, logger);
      }
    }
    if(currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      for(DBID id : currentCluster) {
        noise.add(id);
      }
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return Clustering Algorithm
   */
  public static <O extends DatabaseObject, D extends Distance<D>> DBSCAN<O, D> parameterize(Parameterization config) {
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    D epsilon = getParameterEpsilon(config, distanceFunction);
    int minpts = getParameterMinpts(config);
    if(config.hasErrors()) {
      return null;
    }
    return new DBSCAN<O, D>(distanceFunction, epsilon, minpts);
  }

  /**
   * Get the epsilon parameter value.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param config Parameterization
   * @param distanceFunction distance function (for factory)
   * @return Epsilon value
   */
  protected static <O extends DatabaseObject, D extends Distance<D>> D getParameterEpsilon(Parameterization config, DistanceFunction<O, D> distanceFunction) {
    final DistanceParameter<D> param = new DistanceParameter<D>(EPSILON_ID, distanceFunction);
    if (config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  /**
   * Get the minPts parameter value.
   * 
   * @param config Parameterization
   * @return minpts parameter value
   */
  protected static int getParameterMinpts(Parameterization config) {
    final IntParameter param = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
    if (config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }
}