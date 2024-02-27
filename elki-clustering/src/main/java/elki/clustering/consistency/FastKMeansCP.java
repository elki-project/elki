/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.consistency;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.dbscan.predicates.NeighborPredicate;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;

/**
 * Improves {@link KMeansCP} by using the center of each closed neighborhood set
 * instead of working with individual data points. This improved the runtime
 * over the original {@link KMeansCP} algorithm.
 * <p>
 * Reference:
 * <p>
 * Lars Lenssen, Niklas Strahmann, Erich Schubert<br>
 * Fast k-Nearest-Neighbor-Consistent Clustering<br>
 * Lernen, Wissen, Daten, Analysen (LWDA), 2023
 * 
 * @author Niklas Strahmann
 */
@Reference(authors = "Lars Lenssen, Niklas Strahmann, Erich Schubert", //
    title = "Fast k-Nearest-Neighbor-Consistent Clustering", //
    booktitle = "Lernen, Wissen, Daten, Analysen (LWDA)", //
    url = "https://ceur-ws.org/Vol-3630/LWDA2023-paper34.pdf", bibkey = "DBLP:conf/lwa/LenssenSS23")
public class FastKMeansCP<V extends NumberVector> extends KMeansCP<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FastKMeansCP.class);

  /**
   * Constructor.
   *
   * @param kCluster Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization method
   * @param predicate Neighbor predicate
   */
  public FastKMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, NeighborPredicate<? super V, ?> predicate) {
    super(kCluster, maxiter, initializer, predicate);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> rel) {
    Instance instance = new Instance(rel, distance, initialMeans(rel));
    instance.run(maxiter);
    return instance.buildResult();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Instance for a particular data set
   * 
   * @author Niklas Strahmann
   */
  protected class Instance extends KMeansCP<V>.Instance {
    /**
     * Summarizes of each clused neighborhood set.
     */
    protected List<SetSummary> summaries;

    /**
     * Cluster assignment of each neighborhood set.
     */
    protected List<List<SetSummary>> cnsClusters;

    /**
     * Constructor.
     *
     * @param relation Relation to process
     * @param df Distance function
     * @param means Initial mean
     */
    public Instance(Relation<? extends V> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(int maxiter) {
      closedNeighborhoods = ClosedNeighborhoods.getClosedNeighborhoods(predicate, (Relation<? extends V>) relation, LOG, this.getClass().getName());
      summaries = initializeSummaries(closedNeighborhoods);
      cnsClusters = new ArrayList<>(k);
      for(int i = 0; i < k; i++) {
        cnsClusters.add(new ArrayList<>());
      }
      super.run(maxiter);
    }

    @Override
    protected int iterate(int iteration) {
      means = iteration == 1 ? means : updateMeans(cnsClusters, means);
      return assignToNearestCluster(summaries, means);
    }

    /**
     * Creates a representative for each closed neighborhood set.
     * 
     * @param closedNeighborhoodSets closed neighborhood sets to operate on
     * @return representative consisting of mean and sizer of set
     */
    private List<SetSummary> initializeSummaries(List<DBIDs> closedNeighborhoodSets) {
      final int dim = RelationUtil.dimensionality(relation);
      List<SetSummary> summaries = new ArrayList<>(closedNeighborhoodSets.size());
      for(DBIDs cns : closedNeighborhoodSets) {
        double[] sum = new double[dim];
        for(DBIDIter element = cns.iter(); element.valid(); element.advance()) {
          plusEquals(sum, relation.get(element));
        }
        VMath.timesEquals(sum, 1.0 / cns.size()); // to mean
        summaries.add(new SetSummary(sum, cns));
      }
      return summaries;
    }

    /**
     * Recompute the cluster means, weighted by CNS size.
     * 
     * @param clusters Clusters
     * @param means Current means
     * @return New mean
     */
    protected double[][] updateMeans(List<List<SetSummary>> clusters, double[][] means) {
      final int k = means.length, dim = means[0].length;
      double[][] newMeans = new double[k][dim];
      for(int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
        List<SetSummary> cluster = clusters.get(clusterIndex);
        if(cluster.size() == 0) {
          newMeans[clusterIndex] = means[clusterIndex];
          continue;
        }

        double[] sum = newMeans[clusterIndex]; // local ref
        int size = 0;
        for(SetSummary summary : cluster) {
          VMath.plusTimesEquals(sum, summary.mean, summary.ids.size());
          size += summary.ids.size();
        }
        VMath.timesEquals(sum, 1.0 / size);
      }
      return newMeans;
    }

    /**
     * Assigns each closed neighborhood set to the nearest cluster.
     */
    protected int assignToNearestCluster(List<SetSummary> summaries, double[][] means) {
      int changed = 0;
      for(List<SetSummary> cluster : cnsClusters) {
        cluster.clear();
      }

      for(SetSummary representative : summaries) {
        int minIndex = 0;
        double minDist = distance(representative.mean, means[0]);
        for(int i = 1; i < k; i++) {
          double dist = distance(representative.mean, means[i]);
          if(dist < minDist) {
            minIndex = i;
            minDist = dist;
          }
        }
        minDist *= representative.ids.size();
        varsum[minIndex] += isSquared ? minDist : (minDist * minDist);
        cnsClusters.get(minIndex).add(representative);
        if(minIndex != representative.assignment) {
          representative.assignment = minIndex;
          changed += representative.ids.size();
        }
      }
      return changed;
    }

    @Override
    public Clustering<KMeansModel> buildResult() {
      for(int i = 0; i < cnsClusters.size(); i++) {
        for(SetSummary cns : cnsClusters.get(i)) {
          clusters.get(i).addDBIDs(cns.ids);
        }
      }
      return super.buildResult();
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Summary of a closed neighborhood set for FastKMeansCP
   * 
   * @author Niklas Strahmann
   */
  protected static class SetSummary {
    /**
     * Average of the closed neighborhood set
     */
    protected double[] mean;

    /**
     * Member elements of the CNS.
     */
    protected DBIDs ids;

    /**
     * Current cluster assignment.
     */
    protected int assignment;

    /**
     * Constructor.
     * 
     * @param mean Average of the closed neighborhood set
     * @param ids CNS elements
     */
    protected SetSummary(double[] mean, DBIDs ids) {
      this.mean = mean;
      this.ids = ids;
      this.assignment = -1;
    }
  }

  /**
   * Class parameterizer
   * 
   * @author Niklas Strahmann
   *
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> extends KMeansCP.Par<V> {
    @Override
    public FastKMeansCP<V> make() {
      return new FastKMeansCP<>(k, maxiter, initializer, predicate);
    }
  }
}
