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

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;

/**
 * Newlings's Exponion k-means algorithm, exploiting the triangle inequality.
 * <p>
 * This is <b>not</b> a complete implementation, the approximative sorting part
 * is missing. We also had to guess on the paper how to make best use of F.
 * <p>
 * Reference:
 * <p>
 * J. Newling<br>
 * Fast k-means with accurate bounds<br>
 * Proc. 33nd Int. Conf. on Machine Learning, ICML 2016
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "J. Newling", //
    title = "Fast k-means with accurate bounds", //
    booktitle = "Proc. 33nd Int. Conf. on Machine Learning, ICML 2016", //
    url = "http://jmlr.org/proceedings/papers/v48/newling16.html", //
    bibkey = "DBLP:conf/icml/NewlingF16")
@Priority(Priority.RECOMMENDED - 1) // Shallot is usually better
public class ExponionKMeans<V extends NumberVector> extends HamerlyKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ExponionKMeans.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public ExponionKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distance, k, maxiter, initializer, varstat);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, distance, initialMeans(relation));
    instance.run(maxiter);
    return instance.buildResult(varstat, relation);
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends HamerlyKMeans.Instance {
    /**
     * Cluster center distances.
     */
    double[][] cdist;

    /**
     * Sorted neighbors
     */
    int[][] cnum;

    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      cdist = new double[k][k];
      cnum = new int[k][k - 1];
    }

    @Override
    protected int initialAssignToNearestCluster() {
      assert k == means.length;
      computeSquaredSeparation(cdist);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        // Find closest center, and distance to two closest centers:
        double min1 = distance(fv, means[0]);
        double min2 = k > 1 ? distance(fv, means[1]) : min1;
        int minIndex = 0;
        if(min2 < min1) {
          double tmp = min1;
          min1 = min2;
          min2 = tmp;
          minIndex = 1;
        }
        for(int i = 2; i < k; i++) {
          if(min2 > cdist[minIndex][i]) {
            double dist = distance(fv, means[i]);
            if(dist < min1) {
              minIndex = i;
              min2 = min1;
              min1 = dist;
            }
            else if(dist < min2) {
              min2 = dist;
            }
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        plusEquals(sums[minIndex], fv);
        upper.putDouble(it, isSquared ? Math.sqrt(min1) : min1);
        lower.putDouble(it, isSquared ? Math.sqrt(min2) : min2);
      }
      return relation.size();
    }

    @Override
    protected int assignToNearestCluster() {
      recomputeSeperation(sep, cdist);
      nearestMeans(cdist, cnum);
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        // Compute the current bound:
        final double z = lower.doubleValue(it);
        final double sa = sep[orig];
        double u = upper.doubleValue(it);
        if(u <= z || u <= sa) {
          continue;
        }
        // Update the upper bound
        NumberVector fv = relation.get(it);
        double curd2 = distance(fv, means[orig]);
        upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
        if(u <= z || u <= sa) {
          continue;
        }
        double rhalf = u + 0.5 * sa; // Our cdist are scaled 0.5
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
        int cur = orig;
        for(int i = 0; i < k - 1; i++) {
          final int c = cnum[orig][i]; // Optimized ordering
          if(cdist[orig][c] > rhalf) {
            break;
          }
          double dist = distance(fv, means[c]);
          if(dist < min1) {
            cur = c;
            min2 = min1;
            min1 = dist;
          }
          else if(dist < min2) {
            min2 = dist;
          }
        }
        // Object has to be reassigned.
        if(cur != orig) {
          clusters.get(cur).add(it);
          clusters.get(orig).remove(it);
          assignment.putInt(it, cur);
          plusMinusEquals(sums[cur], sums[orig], fv);
          ++changed;
          upper.putDouble(it, min1 == curd2 ? u : isSquared ? Math.sqrt(min1) : min1);
        }
        lower.putDouble(it, min2 == curd2 ? u : isSquared ? Math.sqrt(min2) : min2);
      }
      return changed;
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
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends HamerlyKMeans.Par<V> {
    @Override
    public ExponionKMeans<V> make() {
      return new ExponionKMeans<>(distance, k, maxiter, initializer, varstat);
    }
  }
}
