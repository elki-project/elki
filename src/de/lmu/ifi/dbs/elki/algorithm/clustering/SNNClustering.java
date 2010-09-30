package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
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
 * <p>
 * Shared nearest neighbor clustering.
 * </p>
 * <p>
 * Reference: L. Ertöz, M. Steinbach, V. Kumar: Finding Clusters of Different
 * Sizes, Shapes, and Densities in Noisy, High Dimensional Data. <br>
 * In: Proc. of SIAM Data Mining (SDM), 2003.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used for the preprocessing of the shared
 *        nearest neighbors neighborhood lists
 */
@Title("SNN: Shared Nearest Neighbor Clustering")
@Description("Algorithm to find shared-nearest-neighbors-density-connected sets in a database based on the " + "parameters 'minPts' and 'epsilon' (specifying a volume). " + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "L. Ertöz, M. Steinbach, V. Kumar", title = "Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High Dimensional Data", booktitle = "Proc. of SIAM Data Mining (SDM), 2003", url = "http://www.siam.org/meetings/sdm03/proceedings/sdm03_05.pdf")
public class SNNClustering<O extends DatabaseObject, D extends Distance<D>> extends AbstractAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SNNClustering.class);
  
  /**
   * Parameter to specify the minimum SNN density, must be an integer greater
   * than 0.
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("snn.epsilon", "The minimum SNN density.");

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private IntegerDistance epsilon;

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-SNN-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("snn.minpts", "Threshold for minimum number of points in " + "the epsilon-SNN-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

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
   * The similarity function for the shared nearest neighbor similarity.
   */
  private SharedNearestNeighborSimilarityFunction<O, D> similarityFunction;

  /**
   * Constructor.
   * 
   * @param similarityFunction Similarity function
   * @param epsilon Epsilon
   * @param minpts Minpts
   */
  public SNNClustering(SharedNearestNeighborSimilarityFunction<O, D> similarityFunction, IntegerDistance epsilon, int minpts) {
    super();
    this.similarityFunction = similarityFunction;
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the SNN clustering algorithm on the given database.
   */
  @Override
  protected Clustering<Model> runInTime(Database<O> database) {
    SimilarityQuery<O, IntegerDistance> snnInstance = similarityFunction.instantiate(database);

    FiniteProgress objprog = logger.isVerbose() ? new FiniteProgress("SNNClustering", database.size(), logger) : null;
    IndefiniteProgress clusprog = logger.isVerbose() ? new IndefiniteProgress("Number of clusters", logger) : null;
    resultList = new ArrayList<ModifiableDBIDs>();
    noise = DBIDUtil.newHashSet();
    processedIDs = DBIDUtil.newHashSet(database.size());
    if(database.size() >= minpts) {
      for(DBID id : database) {
        if(!processedIDs.contains(id)) {
          expandCluster(database, snnInstance, id, objprog, clusprog);
          if(processedIDs.size() == database.size() && noise.size() == 0) {
            break;
          }
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
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
    if(objprog != null && clusprog != null) {
      objprog.ensureCompleted(logger);
      clusprog.setCompleted(logger);
    }

    Clustering<Model> result = new Clustering<Model>("Shared-Nearest-Neighbor Clustering", "snn-clustering");
    for(Iterator<ModifiableDBIDs> resultListIter = resultList.iterator(); resultListIter.hasNext();) {
      result.addCluster(new Cluster<Model>(resultListIter.next(), ClusterModel.CLUSTER));
    }
    result.addCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));

    return result;
  }

  /**
   * Returns the shared nearest neighbors of the specified query object in the
   * given database.
   * 
   * @param database the database holding the objects
   * @param snnInstance shared nearest neighbors
   * @param queryObject the query object
   * @return the shared nearest neighbors of the specified query object in the
   *         given database
   */
  protected List<DBID> findSNNNeighbors(Database<O> database, SimilarityQuery<O, IntegerDistance> snnInstance, DBID queryObject) {
    List<DBID> neighbors = new LinkedList<DBID>();
    for(DBID id : database) {
      if(snnInstance.similarity(queryObject, id).compareTo(epsilon) >= 0) {
        neighbors.add(id);
      }
    }
    return neighbors;
  }

  /**
   * DBSCAN-function expandCluster adapted to SNN criterion.
   * <p/>
   * <p/>
   * Border-Objects become members of the first possible cluster.
   * 
   * @param database the database on which the algorithm is run
   * @param snnInstance shared nearest neighbors
   * @param startObjectID potential seed of a new potential cluster
   * @param objprog the progress object to report about the progress of
   *        clustering
   */
  protected void expandCluster(Database<O> database, SimilarityQuery<O, IntegerDistance> snnInstance, DBID startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    List<DBID> seeds = findSNNNeighbors(database, snnInstance, startObjectID);

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
    for(DBID seed : seeds) {
      if(!processedIDs.contains(seed)) {
        currentCluster.add(seed);
        processedIDs.add(seed);
      }
      else if(noise.contains(seed)) {
        currentCluster.add(seed);
        noise.remove(seed);
      }
    }
    seeds.remove(0);

    while(seeds.size() > 0) {
      DBID o = seeds.remove(0);
      List<DBID> neighborhood = findSNNNeighbors(database, snnInstance, o);

      if(neighborhood.size() >= minpts) {
        for(DBID p : neighborhood) {
          boolean inNoise = noise.contains(p);
          boolean unclassified = !processedIDs.contains(p);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(p);
            }
            currentCluster.add(p);
            processedIDs.add(p);
            if(inNoise) {
              noise.remove(p);
            }
          }
        }
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), logger);
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters, logger);
      }

      if(processedIDs.size() == database.size() && noise.size() == 0) {
        break;
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
  public static <O extends DatabaseObject, D extends Distance<D>> SNNClustering<O, D> parameterize(Parameterization config) {
    IntegerDistance epsilon = getParameterEpsilon(config);
    int minpts = getParameterMinpts(config);
    SharedNearestNeighborSimilarityFunction<O, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<O, D>(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SNNClustering<O, D>(similarityFunction, epsilon, minpts);
  }

  /**
   * Get parameter epsilon
   * 
   * @param config Parameterization
   * @return Epsilon
   */
  protected static IntegerDistance getParameterEpsilon(Parameterization config) {
    final DistanceParameter<IntegerDistance> param = new DistanceParameter<IntegerDistance>(EPSILON_ID, IntegerDistance.FACTORY);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  /**
   * Get parameter minpts
   * 
   * @param config Parameterization
   * @return minpts parameter
   */
  protected static int getParameterMinpts(Parameterization config) {
    final IntParameter param = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}