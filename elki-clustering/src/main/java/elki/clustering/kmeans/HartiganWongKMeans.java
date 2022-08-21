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
package elki.clustering.kmeans;

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Hartigan and Wong k-means clustering.
 * This implementation is derived from the Fortran code included in the
 * referenced publication, but not a literal port.
 * <p>
 * Reference:
 * <p>
 * J. A. Hartigan, M. A. Wong<br>
 * Algorithm AS 136: A K-Means Clustering Algorithm<br>
 * J. Royal Statistical Society. Series C (Applied Statistics) 28(1)
 *
 * @author Minh Nhat Nguyen
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <V> Number vector type
 */
@Reference(authors = "J. A. Hartigan, M. A. Wong", //
    title = "Algorithm AS 136: A K-Means Clustering Algorithm", //
    booktitle = "J. Royal Statistical Society. Series C (Applied Statistics) 28(1)", //
    url = "https://doi.org/10.2307/2346830", //
    bibkey = "doi:10.2307/2346830")
public class HartiganWongKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(HartiganWongKMeans.class);

  /**
   * Constructor.
   *
   * @param k Number of clusters
   * @param initializer Initialization method
   */
  public HartiganWongKMeans(int k, KMeansInitialization initializer) {
    super(SquaredEuclideanDistance.STATIC, k, 0, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> rel) {
    Instance instance = new Instance(rel, getDistance(), initialMeans(rel));
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Minh Nhat Nguyen
   */
  protected static class Instance extends AbstractKMeans.Instance {
    /**
     * Second nearest cluster. IC2(I).
     */
    WritableIntegerDataStore secondary;

    /**
     * The value [NC(L1) * D(I,L1)^2] / [NC(L1) -1] will be remembered and will
     * remain the same for Point I until cluster L1 is updated.
     */
    WritableDoubleDataStore r1s;

    /**
     * In Optimal-transfer-stage, NCP(L) indicates the step at which cluster L
     * is last updated.
     * In the Quick-transfer-stage, NCP(L) is equal to the step at which cluster
     * L is last updated plus M (number of points).
     */
    int[] ncp;

    /**
     * Live set indicators
     */
    int[] live;

    /**
     * Updated in quick-transfer
     */
    boolean[] itran;

    /**
     * Weights for adding/removing points from a cluster.
     */
    double[] an1, an2;

    /**
     * Number of attempts to make an optimal transfer.
     */
    private int optries;

    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      secondary = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0);
      r1s = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.POSITIVE_INFINITY);
      ncp = new int[k];
      live = new int[k];
      itran = new boolean[k];
      an1 = new double[k];
      an2 = new double[k];
      optries = 0;
    }

    @Override
    protected int iterate(int iteration) {
      if(iteration == 1) {
        // Step 1: find closest centers
        int changed = initialAssignToNearestCluster();
        // Step 2: update cluster centers.
        means = means(clusters, means, relation);
        initialize();
        return changed;
      }
      if(iteration > 2 && k == 2) {
        return 0;
      }
      // Step 4. Optimal Transfer Stage (OPTRA)
      int changed = optimalTransfer();
      // Step 5. stop if no transfer took place in the last M OPTRA steps.
      if(optries == relation.size()) {
        return 0;
      }
      // Step 6. Quick Transfer Stage (QTRAN)
      changed += quickTransfer();
      return changed;
    }

    /**
     * Step 1: For each point I, find its two closest centers, IC1(I) and
     * IC2(I). Assign it to IC1(I).
     * 
     * @return Number of reassigned points
     */
    private int initialAssignToNearestCluster() {
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        // Find the two closest centers
        int best = 0, sec = 0;
        double bdist = distance(fv, means[0]), sdist = bdist;
        for(int i = 1; i < means.length; i++) {
          final double d = distance(fv, means[i]);
          if(d < bdist) {
            sec = best;
            sdist = bdist;
            best = i;
            bdist = d;
          }
          else if(d < sdist) {
            sec = i;
            sdist = d;
          }
        }
        // Assign to nearest cluster.
        clusters.get(best).add(it);
        assignment.putInt(it, best);
        secondary.putInt(it, sec);
      }
      return relation.size();
    }

    /**
     * Initialize AN1, AN2, ITRAN, NCP
     */
    private void initialize() {
      for(int l = 0; l < means.length; l++) {
        final int aa = clusters.get(l).size();
        an2[l] = aa / (double) (aa + 1);
        an1[l] = aa >= 1 ? aa / (double) (aa - 1) : Double.POSITIVE_INFINITY;
      }
      Arrays.fill(itran, true);
      Arrays.fill(ncp, -1);
    }

    /**
     * In this stage, there is only one pass through the data. Each point is
     * reallocated, if necessary, to the cluster that will induce the maximum
     * reduction in wcss.
     * 
     * @return Number of reassigned objects
     */
    private int optimalTransfer() {
      final int m = relation.size();
      Arrays.fill(ncp, -1);
      // If cluster j is updated in the last quick transfer step, it belongs to
      // the live set in this stage. Otherwise at each step it is not in the
      // live set if it has not been updated in the last m optimal transfer
      // steps.
      for(int j = 0; j < means.length; j++) {
        if(itran[j]) {
          live[j] = m; // make live
        }
      }

      int changed = 0;
      int i = 0; // point number
      for(DBIDIter it = relation.iterDBIDs(); it.valid() && optries < m; it.advance(), ++i) {
        ++optries;
        // nearest and second nearest
        final int l1 = assignment.intValue(it);
        // if point I is the only member of cluster L1, no transfer
        if(clusters.get(l1).size() == 1) {
          continue;
        }

        NumberVector vec = relation.get(it);
        // if L1 has been updated -> recompute R1
        double r1 = ncp[l1] >= 0 ? cacheR1(it, vec, l1) : r1s.doubleValue(it);

        int l2 = assignment.intValue(it), newl2 = l2;
        // find the cluster with minimum R2
        double r2 = Double.POSITIVE_INFINITY;
        final boolean ilivel1 = i < live[l1];
        for(int l = 0; l < means.length; l++) {
          // Omit last nearest and second nearest; consider live set only
          if(l != l1 && l != l2 && (ilivel1 || i < live[l])) {
            double rr2 = distance(vec, means[l]) * an2[l];
            if(rr2 < r2) {
              r2 = rr2;
              newl2 = l;
            }
          }
        }

        if(r2 < r1) {
          // Update cluster centers, LIVE, NCP, AN1, AN2, IC1(I), IC2(I)
          optries = 0;
          live[l1] = live[newl2] = relation.size() + i;
          ncp[l1] = ncp[newl2] = i;

          transfer(it, vec, l1, newl2);
          ++changed;
        }
        else if(newl2 != l2) {
          // if no transfer is necessary, L2 is the new IC2(I)
          secondary.putInt(it, newl2);
        }
      }

      // also LIVE(L) has to be decreased by M before re-entering OPTRA
      for(int k = 0; k < live.length; k++) {
        live[k] -= m;
      }
      return changed;
    }

    /**
     * Step 6: the quick transfer (QTRAN) phase. Each point is tested in turn to
     * see if it should be reallocated to the cluster which it is most likely to
     * be transferred to IC2(I) from its present cluster IC1(I).
     * Loop through the data until no further change is to take place.
     */
    private int quickTransfer() {
      // ITRAN(L) is set to zero before entering QTRAN
      Arrays.fill(itran, false);
      int changed = 0;
      int icoun = 0, istep = 0;
      while(icoun < relation.size()) {
        for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
          ++icoun;
          ++istep;
          int l1 = assignment.intValue(it), l2 = secondary.intValue(it);
          // if point I is the only member of cluster L1, no transfer
          if(clusters.get(l1).size() == 1) {
            continue;
          }

          NumberVector vec = relation.get(it);
          // if istep is greater than ncp(l1) no need to recompute distance
          // from point i to cluster l1
          final double r1 = istep < ncp[l1] ? cacheR1(it, vec, l1) : r1s.doubleValue(it);

          // if istep is greater than or equal to both ncp(l1) and ncp(l2)
          // there will be no transfer of point I at this step
          if(istep > ncp[l1] && istep > ncp[l2]) {
            continue;
          }
          if(r1 > distance(vec, means[l2]) * an2[l2]) {
            icoun = optries = 0;
            itran[l1] = itran[l2] = true;
            ncp[l1] = ncp[l2] = istep + relation.size() - 1;

            transfer(it, vec, l1, l2);
            changed++;
          }
        }
      }
      return changed;
    }

    /**
     * Compute and cache the R1 value.
     *
     * @param it Point id
     * @param vec Data vector
     * @param l1 Center id
     * @return R1
     */
    private double cacheR1(DBIDIter it, NumberVector vec, int l1) {
      double r1 = distance(vec, means[l1]) * an1[l1];
      r1s.putDouble(it, r1);
      return r1;
    }

    /**
     * Transfer a point from one cluster to another.
     *
     * @param it Point id
     * @param vec Vector
     * @param l1 First cluster
     * @param l2 Second cluster
     */
    private void transfer(DBIDRef it, NumberVector vec, int l1, int l2) {
      final int al1 = clusters.get(l1).size(), alw = al1 - 1;
      final int al2 = clusters.get(l2).size(), alt = al2 + 1;

      // update cluster centers
      final double[] mean1 = means[l1], mean2 = means[l2];
      for(int j = 0; j < mean1.length; j++) {
        mean1[j] = (mean1[j] * al1 - vec.doubleValue(j)) / alw;
        mean2[j] = (mean2[j] * al2 + vec.doubleValue(j)) / alt;
      }
      // Recompute an2, an1 for l1, l2 with new cluster sizes:
      an2[l1] = alw / (double) al1;
      an1[l1] = alw > 1 ? alw / (double) (alw - 1) : Double.POSITIVE_INFINITY;
      an1[l2] = alt / (double) al2;
      an2[l2] = alt / (double) (alt + 1);
      clusters.get(l1).remove(it);
      clusters.get(l2).add(it);
      assignment.put(it, l2);
      secondary.put(it, l1);
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Minh Nhat Nguyen
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Par<V> {
    @Override
    public void configure(Parameterization config) {
      // Override - no choice in distance function
      getParameterK(config);
      getParameterInitialization(config);
    }

    @Override
    public HartiganWongKMeans<V> make() {
      return new HartiganWongKMeans<>(k, initializer);
    }
  }
}
