/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.algorithm.outlier.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansSort;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.ModelUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Cluster-based local outlier factor (CBLOF).
 * <p>
 * Reference:
 * <p>
 * Z. He, X. Xu, S. Deng<br>
 * Discovering cluster-based local outliers<br>
 * Pattern Recognition Letters 24(9-10)
 * <p>
 * Implementation note: this algorithm is hard to implement in a generic
 * fashion, as to support arbitrary clustering algorithms and distances, because
 * it is not trivial to ensure both the clustering algorithm and the outlier
 * method use compatible data types and distances.
 *
 * @author Patrick Kostjens
 * @since 0.7.5
 *
 * @param <O> the type of data objects handled by this algorithm
 */
@Title("Discovering cluster-based local outliers")
@Reference(authors = "Z. He, X. Xu, S. Deng", //
    title = "Discovering cluster-based local outliers", //
    booktitle = "Pattern Recognition Letters 24(9-10)", //
    url = "https://doi.org/10.1016/S0167-8655(03)00003-5", //
    bibkey = "DBLP:journals/prl/HeXD03")
public class CBLOF<O extends NumberVector> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CBLOF.class);

  /**
   * The clustering algorithm to use.
   */
  protected ClusteringAlgorithm<Clustering<MeanModel>> clusteringAlgorithm;

  /**
   * The ratio of the size that separates the large clusters from the small
   * clusters. The clusters are ordered descending by size and are taken until
   * the specified ratio of the data is included. For example: a ratio of 0.9
   * indicates that the large clusters should cover at least 90% of the data
   * points.
   */
  protected double alpha;

  /**
   * The minimal ratio between two consecutive clusters (when ordered descending
   * by size) at which the boundary between the large and small clusters is set.
   * For example: a ratio of 3 means that the clusters are separated between
   * cluster i and (i+1) (where (i+1) is the first cluster smaller than i) when
   * cluster i is at least 3 times bigger than (i+1).
   */
  protected double beta;

  /**
   * Distance function to use.
   */
  protected NumberVectorDistanceFunction<? super O> distance;

  /**
   * Constructor.
   *
   * @param distanceFunction the neighborhood distance function
   * @param clusteringAlgorithm the clustering algorithm
   * @param alpha the ratio of the data that should be included in the large
   *        clusters
   * @param beta the ratio of the sizes of the clusters at the boundary between
   *        the large and the small clusters
   */
  public CBLOF(NumberVectorDistanceFunction<? super O> distanceFunction, ClusteringAlgorithm<Clustering<MeanModel>> clusteringAlgorithm, double alpha, double beta) {
    super(distanceFunction);
    this.clusteringAlgorithm = clusteringAlgorithm;
    this.alpha = alpha;
    this.beta = beta;
    this.distance = distanceFunction;
  }

  /**
   * Runs the CBLOF algorithm on the given database.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return CBLOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("CBLOF", 3) : null;
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 1, "Computing clustering.");
    Clustering<MeanModel> clustering = clusteringAlgorithm.run(database);

    LOG.beginStep(stepprog, 2, "Computing boundary between large and small clusters.");
    List<? extends Cluster<MeanModel>> clusters = clustering.getAllClusters();
    Collections.sort(clusters, new Comparator<Cluster<MeanModel>>() {
      @Override
      public int compare(Cluster<MeanModel> o1, Cluster<MeanModel> o2) {
        // Sort in descending order by size
        return Integer.compare(o2.size(), o1.size());
      }
    });
    int clusterBoundary = getClusterBoundary(relation, clusters);

    List<? extends Cluster<MeanModel>> largeClusters = clusters.subList(0, clusterBoundary + 1);
    List<? extends Cluster<MeanModel>> smallClusters = clusters.subList(clusterBoundary + 1, clusters.size());

    LOG.beginStep(stepprog, 3, "Computing Cluster-Based Local Outlier Factors (CBLOF).");
    WritableDoubleDataStore cblofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    DoubleMinMax cblofMinMax = new DoubleMinMax();
    computeCBLOFs(relation, distance, cblofs, cblofMinMax, largeClusters, smallClusters);

    LOG.setCompleted(stepprog);

    DoubleRelation scoreResult = new MaterializedDoubleRelation("Cluster-Based Local Outlier Factor", "cblof-outlier", cblofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(cblofMinMax.getMin(), cblofMinMax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute the boundary index separating the large cluster from the small
   * cluster.
   *
   * @param relation Data to process
   * @param clusters All clusters that were found
   * @return Index of boundary between large and small cluster.
   */
  private int getClusterBoundary(Relation<O> relation, List<? extends Cluster<MeanModel>> clusters) {
    int totalSize = relation.size();
    int clusterBoundary = clusters.size() - 1;
    int cumulativeSize = 0;
    for(int i = 0; i < clusters.size() - 1; i++) {
      cumulativeSize += clusters.get(i).size();

      // Given majority covered by large cluster
      if(cumulativeSize >= totalSize * alpha) {
        clusterBoundary = i;
        break;
      }

      // Relative difference in cluster size between two consecutive clusters
      if(clusters.get(i).size() / (double) clusters.get(i + 1).size() >= beta) {
        clusterBoundary = i;
        break;
      }
    }
    return clusterBoundary;
  }

  /**
   * Compute the CBLOF scores for all the data.
   *
   * @param relation Data to process
   * @param distance The distance function
   * @param cblofs CBLOF scores
   * @param cblofMinMax Minimum/maximum score tracker
   * @param largeClusters Large clusters output
   * @param smallClusters Small clusters output
   */
  private void computeCBLOFs(Relation<O> relation, NumberVectorDistanceFunction<? super O> distance, WritableDoubleDataStore cblofs, DoubleMinMax cblofMinMax, List<? extends Cluster<MeanModel>> largeClusters, List<? extends Cluster<MeanModel>> smallClusters) {
    List<NumberVector> largeClusterMeans = new ArrayList<>(largeClusters.size());
    for(Cluster<MeanModel> largeCluster : largeClusters) {
      NumberVector mean = ModelUtil.getPrototypeOrCentroid(largeCluster.getModel(), relation, largeCluster.getIDs());
      largeClusterMeans.add(mean);

      // Compute CBLOF scores for members of large clusters
      for(DBIDIter iter = largeCluster.getIDs().iter(); iter.valid(); iter.advance()) {
        double cblof = computeLargeClusterCBLOF(relation.get(iter), distance, mean, largeCluster);
        storeCBLOFScore(cblofs, cblofMinMax, cblof, iter);
      }
    }

    for(Cluster<MeanModel> smallCluster : smallClusters) {
      for(DBIDIter iter = smallCluster.getIDs().iter(); iter.valid(); iter.advance()) {
        double cblof = computeSmallClusterCBLOF(relation.get(iter), distance, largeClusterMeans, smallCluster);
        storeCBLOFScore(cblofs, cblofMinMax, cblof, iter);
      }
    }
  }

  private void storeCBLOFScore(WritableDoubleDataStore cblofs, DoubleMinMax cblofMinMax, double cblof, DBIDIter iter) {
    cblofs.putDouble(iter, cblof);
    cblofMinMax.put(cblof);
  }

  private double computeSmallClusterCBLOF(O obj, NumberVectorDistanceFunction<? super O> distance, List<NumberVector> largeClusterMeans, Cluster<MeanModel> cluster) {
    // Get distance to nearest large cluster
    double nearestLargeClusterDistance = Double.MAX_VALUE;
    for(NumberVector clusterMean : largeClusterMeans) {
      double clusterDistance = distance.distance(obj, clusterMean);
      if(clusterDistance < nearestLargeClusterDistance) {
        nearestLargeClusterDistance = clusterDistance;
      }
    }
    return cluster.size() * nearestLargeClusterDistance;
  }

  private double computeLargeClusterCBLOF(O obj, NumberVectorDistanceFunction<? super O> distanceQuery, NumberVector clusterMean, Cluster<MeanModel> cluster) {
    // Get distance to center of containing cluster
    return cluster.size() * distanceQuery.distance(obj, clusterMean);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Patrick Kostjens
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the algorithm to be used for clustering.
     */
    public static final OptionID CLUSTERING_ID = new OptionID("cblof.algorithm", "Clustering algorithm to use for detecting outliers.");

    /**
     * Parameter to specify the alpha value to be used by the algorithm.
     */
    public static final OptionID ALPHPA_ID = new OptionID("cblof.alpha", "The ratio of the data that should be included in the large clusters");

    /**
     * Parameter to specify the beta value to be used by the algorithm.
     */
    public static final OptionID BETA_ID = new OptionID("cblof.beta", "The ratio of the data that should be included in the large clusters");

    /**
     * The clustering algorithm to use.
     */
    protected ClusteringAlgorithm<Clustering<MeanModel>> clusteringAlgorithm;

    /**
     * The ratio of the size that separates the large clusters from the small
     * clusters. The clusters are ordered descending by size and are taken until
     * the specified ratio of the data is included. For example: a ratio of 0.9
     * indicates that the large clusters should cover at least 90% of the data
     * points.
     */
    protected double alpha;

    /**
     * The minimal ratio between two consecutive clusters (when ordered
     * descending by size) at which the boundary between the large and small
     * clusters is set. For example: a ratio of 3 means that the clusters are
     * separated between cluster i and (i+1) (where (i+1) is the first cluster
     * smaller than i) when cluster i is at least 3 times bigger than (i+1).
     */
    protected double beta;

    /**
     * Distance function to use.
     */
    protected NumberVectorDistanceFunction<? super O> distance;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<NumberVectorDistanceFunction<? super O>> distanceP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, NumberVectorDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceP)) {
        distance = distanceP.instantiateClass(config);
      }

      final DoubleParameter pA = new DoubleParameter(ALPHPA_ID)//
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(pA)) {
        alpha = pA.doubleValue();
      }

      final DoubleParameter pB = new DoubleParameter(BETA_ID)//
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE);
      if(config.grab(pB)) {
        beta = pB.doubleValue();
      }

      ObjectParameter<ClusteringAlgorithm<Clustering<MeanModel>>> clusterP = new ObjectParameter<>(CLUSTERING_ID, ClusteringAlgorithm.class, KMeansSort.class);
      if(config.grab(clusterP)) {
        clusteringAlgorithm = clusterP.instantiateClass(config);
      }
    }

    @Override
    protected CBLOF<O> makeInstance() {
      return new CBLOF<>(distance, clusteringAlgorithm, alpha, beta);
    }
  }
}
