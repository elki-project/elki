/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.dbscan;

import java.util.ArrayList;
import java.util.List;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Density-Based Clustering of Applications with Noise (DBSCAN), an algorithm to
 * find density-connected sets in a database.
 * <p>
 * Reference:
 * <p>
 * Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu<br>
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases
 * with Noise<br>
 * Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)
 * <p>
 * Further discussion:
 * <p>
 * Erich Schubert, Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu<br>
 * DBSCAN Revisited, Revisited: Why and How You Should (Still) Use DBSCAN<br>
 * ACM Trans. Database Systems (TODS)
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 * @param <O> the type of Object the algorithm is applied to
 */
@Title("DBSCAN: Density-Based Clustering of Applications with Noise")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minpts' and 'epsilon' (specifying a volume). " + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu", //
    title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", //
    booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)", //
    url = "http://www.aaai.org/Library/KDD/1996/kdd96-037.php", //
    bibkey = "DBLP:conf/kdd/EsterKSX96")
@Reference(authors = "Erich Schubert, Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu", //
    title = "DBSCAN Revisited, Revisited: Why and How You Should (Still) Use DBSCAN", //
    booktitle = "ACM Trans. Database Systems (TODS)", //
    url = "https://doi.org/10.1145/3068335", //
    bibkey = "DBLP:journals/tods/SchubertSEKX17")
@Priority(Priority.RECOMMENDED)
public class DBSCAN<O> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DBSCAN.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Holds the epsilon radius threshold.
   */
  protected double epsilon;

  /**
   * Holds the minimum cluster size.
   */
  protected int minpts;

  /**
   * Constructor with parameters.
   *
   * @param distance Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts parameter
   */
  public DBSCAN(Distance<? super O> distance, double epsilon, int minpts) {
    super();
    this.distance = distance;
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Performs the DBSCAN algorithm on the given database.
   */
  public Clustering<Model> run(Relation<O> relation) {
    final int size = relation.size();
    if(size < minpts) {
      Clustering<Model> result = new Clustering<>();
      Metadata.of(result).setLongName("DBSCAN Clustering");
      result.addToplevelCluster(new Cluster<Model>(relation.getDBIDs(), true, ClusterModel.CLUSTER));
      return result;
    }

    Instance dbscan = new Instance();
    dbscan.run(relation, new QueryBuilder<>(relation, distance).rangeByDBID(epsilon));

    double averagen = dbscan.ncounter / (double) relation.size();
    LOG.statistics(new DoubleStatistic(DBSCAN.class.getName() + ".average-neighbors", averagen));
    if(averagen < 1 + 0.1 * (minpts - 1)) {
      LOG.warning("There are very few neighbors found. Epsilon may be too small.");
    }
    if(averagen > 100 * minpts) {
      LOG.warning("There are very many neighbors found. Epsilon may be too large.");
    }

    Clustering<Model> result = new Clustering<>();
    Metadata.of(result).setLongName("DBSCAN Clustering");
    for(ModifiableDBIDs res : dbscan.resultList) {
      result.addToplevelCluster(new Cluster<Model>(res, ClusterModel.CLUSTER));
    }
    result.addToplevelCluster(new Cluster<Model>(dbscan.noise, true, ClusterModel.CLUSTER));
    return result;
  }

  /**
   * Instance for a single data set.
   *
   * @author Erich Schubert
   */
  private class Instance {
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
     * Number of neighbors.
     */
    protected long ncounter;

    /**
     * Progress for objects (may be null).
     */
    protected FiniteProgress objprog;

    /**
     * Progress for clusters (may be null).
     */
    protected IndefiniteProgress clusprog;

    /**
     * Range query to use.
     */
    protected RangeSearcher<DBIDRef> rangeQuery;

    /**
     * Neighbor query output.
     */
    protected final ModifiableDoubleDBIDList neighbors = DBIDUtil.newDistanceDBIDList();

    /**
     * Run the DBSCAN algorithm
     *
     * @param relation Data relation
     * @param rangeSearcher Range query class
     */
    protected void run(Relation<O> relation, RangeSearcher<DBIDRef> rangeSearcher) {
      final int size = relation.size();
      this.objprog = LOG.isVerbose() ? new FiniteProgress("Processing objects", size, LOG) : null;
      this.clusprog = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters", LOG) : null;
      this.rangeQuery = rangeSearcher;

      resultList = new ArrayList<>();
      noise = DBIDUtil.newHashSet();
      processedIDs = DBIDUtil.newHashSet(size);
      ArrayModifiableDBIDs seeds = DBIDUtil.newArray();
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(!processedIDs.contains(iditer)) {
          expandCluster(iditer, seeds);
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), LOG);
          clusprog.setProcessed(resultList.size(), LOG);
        }
        if(processedIDs.size() == size) {
          break;
        }
      }
      // Finish progress logging
      LOG.ensureCompleted(objprog);
      LOG.setCompleted(clusprog);
    }

    /**
     * DBSCAN-function expandCluster.
     * <p>
     * Border-Objects become members of the first possible cluster.
     *
     * @param startObjectID potential seed of a new potential cluster
     * @param seeds Array to store the current seeds
     */
    protected void expandCluster(DBIDRef startObjectID, ArrayModifiableDBIDs seeds) {
      rangeQuery.getRange(startObjectID, epsilon, neighbors.clear());
      processedIDs.add(startObjectID);
      LOG.incrementProcessed(objprog);
      ncounter += neighbors.size();

      // startObject is no core-object
      if(neighbors.size() < minpts) {
        noise.add(startObjectID);
        return;
      }

      ModifiableDBIDs currentCluster = DBIDUtil.newArray(neighbors.size());
      currentCluster.add(startObjectID);
      processNeighbors(neighbors, currentCluster, seeds);

      DBIDVar o = DBIDUtil.newVar();
      while(!seeds.isEmpty()) {
        rangeQuery.getRange(seeds.pop(o), epsilon, neighbors.clear());
        ncounter += neighbors.size(); // statistics for assistance
        if(neighbors.size() >= minpts) { // neighbor is core
          processNeighbors(neighbors, currentCluster, seeds);
        }
        LOG.incrementProcessed(objprog);
      }
      resultList.add(currentCluster);
      LOG.incrementProcessed(clusprog);
    }

    /**
     * Process a single core point.
     *
     * @param neighbors Neighbors
     * @param currentCluster Current cluster
     * @param seeds Seed set
     */
    private void processNeighbors(DoubleDBIDList neighbors, ModifiableDBIDs currentCluster, ArrayModifiableDBIDs seeds) {
      final boolean ismetric = distance.isMetric();
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(processedIDs.add(neighbor)) {
          // No need to query again if metric and distance 0.
          if(!ismetric || neighbor.doubleValue() > 0.) {
            seeds.add(neighbor);
          }
        }
        else if(!noise.remove(neighbor)) {
          continue;
        }
        currentCluster.add(neighbor);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to the distance function specified.
     */
    public static final OptionID EPSILON_ID = new OptionID("dbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("dbscan.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point. The suggested value is '2 * dim - 1'.");

    /**
     * Holds the epsilon radius threshold.
     */
    protected double epsilon;

    /**
     * Holds the minimum cluster size.
     */
    protected int minpts;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new DoubleParameter(EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> epsilon = x);
      if(new IntParameter(MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minpts = x) && minpts <= 2) {
        LOG.warning("DBSCAN with minPts <= 2 is equivalent to single-link clustering at a single height. Consider using larger values of minPts.");
      }
    }

    @Override
    public DBSCAN<O> make() {
      return new DBSCAN<>(distance, epsilon, minpts);
    }
  }
}
