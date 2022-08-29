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
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;

/**
 * Borgelt's Shallot k-means algorithm, exploiting the triangle inequality.
 * <p>
 * Reference:
 * <p>
 * C. Borgelt<br>
 * Even Faster Exact k-Means Clustering<br>
 * Proc. 18th Int. Symp. Intelligent Data Analysis (IDA)
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "C. Borgelt", //
    title = "Even Faster Exact k-Means Clustering", //
    booktitle = "Proc. 18th Int. Symp. Intelligent Data Analysis (IDA)", //
    url = "https://doi.org/10.1007/978-3-030-44584-3_8", //
    bibkey = "DBLP:conf/ida/Borgelt20")
@Priority(Priority.RECOMMENDED)
public class ShallotKMeans<V extends NumberVector> extends ExponionKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ShallotKMeans.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public ShallotKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
  protected static class Instance extends ExponionKMeans.Instance {
    /**
     * Second nearest cluster.
     */
    WritableIntegerDataStore second;

    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      second = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
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
        int minIdx = 0, minId2 = 1;
        if(min2 < min1) {
          double tmp = min1;
          min1 = min2;
          min2 = tmp;
          minIdx = 1;
          minId2 = 0;
        }
        for(int i = 2; i < k; i++) {
          if(min2 > cdist[minIdx][i]) {
            double dist = distance(fv, means[i]);
            if(dist < min1) {
              minId2 = minIdx;
              minIdx = i;
              min2 = min1;
              min1 = dist;
            }
            else if(dist < min2) {
              minId2 = i;
              min2 = dist;
            }
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIdx).add(it);
        assignment.putInt(it, minIdx);
        plusEquals(sums[minIdx], fv);
        upper.putDouble(it, isSquared ? Math.sqrt(min1) : min1);
        lower.putDouble(it, isSquared ? Math.sqrt(min2) : min2);
        // Overall like Exponion, but also store second closest
        second.putInt(it, minId2);
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
        final double z = lower.doubleValue(it);
        final double so = sep[orig];
        double u = upper.doubleValue(it);
        if(u <= z || u <= so) {
          continue;
        }
        // Make the upper bound tight first:
        final NumberVector fv = relation.get(it);
        double curd2 = distance(fv, means[orig]);
        upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
        if(u <= z || u <= so) {
          continue;
        }
        // Our cdist are scaled 0.5, so we need half r:
        if(cdist[orig][cnum[orig][0]] > u + 0.5 * so) {
          continue;
        }
        // Shallot modification #1: try old second-nearest first:
        final int osecn = second.intValue(it);
        // Exact distance to previous second nearest
        double secd2 = distance(fv, means[osecn]);
        int ref = orig, secn = osecn; // closest center "z" in Borgelts paper
        if(secd2 < curd2) {
          // Previous second closest is closer, swap:
          final double tmp = secd2;
          secd2 = curd2;
          curd2 = tmp;
          ref = secn;
          secn = orig;
          // Update u
          u = isSquared ? Math.sqrt(curd2) : curd2;
        }
        // Shallot improvement 1.5:
        // note that secd2 is still squared, cdist is half the distance
        // 0.5*(u+l), with l=min(u+d(x,p), 2u+2*cdist[z])
        double lp = u + (isSquared ? Math.sqrt(secd2) : secd2); // l for p
        double lv = 2 * (u + cdist[ref][cnum[ref][0]]); // l for v2(z)y
        double l = lp < lv ? lp : lv;
        double rhalf = Math.min(u + 0.5 * sep[ref], 0.5 * (u + l));
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = l * l;
        int cur = ref, minId2 = lp < lv ? secn : cnum[ref][0];
        for(int i = 0; i < k - 1; i++) {
          int c = cnum[ref][i];
          if(cdist[ref][c] > rhalf) {
            break;
          }
          final double dist = c == secn ? secd2 : distance(fv, means[c]);
          if(dist < min1) {
            minId2 = cur;
            cur = c;
            min2 = min1;
            min1 = dist;
            if(min1 < l * l) {
              l = isSquared ? Math.sqrt(min2) : min2;
              // Second Shallot improvement: r shrinking
              rhalf = Math.min(rhalf, 0.5 * (u + l));
            }
          }
          else if(dist < min2) {
            minId2 = c;
            min2 = dist;
            l = isSquared ? Math.sqrt(min2) : min2;
            // Second Shallot improvement: r shrinking
            rhalf = Math.min(rhalf, 0.5 * (u + l));
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
        lower.putDouble(it, l);
        if(osecn != minId2) { // second might have changed
          second.putInt(it, minId2);
        }
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
    public ShallotKMeans<V> make() {
      return new ShallotKMeans<>(distance, k, maxiter, initializer, varstat);
    }
  }
}
