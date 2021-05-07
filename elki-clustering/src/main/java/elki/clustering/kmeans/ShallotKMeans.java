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
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

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
        double best = distance(fv, means[0]), bes2 = k > 1 ? distance(fv, means[1]) : best;
        int minIdx = 0, minId2 = 1;
        if(bes2 < best) {
          double tmp = best;
          best = bes2;
          bes2 = tmp;
          minIdx = 1;
          minId2 = 0;
        }
        for(int j = 2; j < k; j++) {
          if(bes2 > cdist[minIdx][j]) {
            double dist = distance(fv, means[j]);
            if(dist < best) {
              minId2 = minIdx;
              minIdx = j;
              bes2 = best;
              best = dist;
            }
            else if(dist < bes2) {
              minId2 = j;
              bes2 = dist;
            }
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIdx).add(it);
        assignment.putInt(it, minIdx);
        // Overall like Exponion, but also store second closest
        second.putInt(it, minId2);
        plusEquals(sums[minIdx], fv);
        upper.putDouble(it, isSquared ? FastMath.sqrt(best) : best);
        lower.putDouble(it, isSquared ? FastMath.sqrt(bes2) : bes2);
      }
      return relation.size();
    }

    @Override
    protected int assignToNearestCluster() {
      assert (k == means.length);
      recomputeSeperation(sep, cdist);
      nearestMeans(cdist, cnum);
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int cur = assignment.intValue(it);
        // Compute the current bound:
        final double z = lower.doubleValue(it);
        final double sa = sep[cur];
        double u = upper.doubleValue(it);
        if(u <= z || u <= sa) {
          continue;
        }
        // Update the upper bound
        NumberVector fv = relation.get(it);
        double curd2 = distance(fv, means[cur]);
        upper.putDouble(it, u = isSquared ? FastMath.sqrt(curd2) : curd2);
        if(u <= z || u <= sa) {
          continue;
        }
        double r = u + 0.5 * sa; // Our cdist are scaled 0.5!
        if(cdist[cur][cnum[cur][0]] > r) {
          continue;
        }
        // Shallot modification #1: try old second-nearest first:
        int secn = second.intValue(it);
        // Exact distance to previous second nearest
        double secd2 = distance(fv, means[secn]);
        int ref = cur; // closest center "z" in Borgelts paper
        if(secd2 < curd2) {
          // Previous second closest is closer, swap:
          final double tmp = secd2;
          secd2 = curd2;
          curd2 = tmp;
          ref = secn;
          secn = cur;
          // Update u
          u = isSquared ? FastMath.sqrt(curd2) : curd2;
        }
        // Second Shallot improvement: r
        double l = Math.min(u + sa, 2 * u + cdist[cur][cnum[cur][0]]);
        r = 0.5 * (u + l); // Our cdist are scaled by 0.5!
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = secd2;
        int minIdx = ref, minId2 = secn;
        for(int i = 0; i < k - 1; i++) {
          int c = cnum[ref][i];
          if(c == secn) {
            continue; // Handled above
          }
          if(cdist[ref][c] > r) {
            break;
          }
          double dist = distance(fv, means[c]);
          if(dist < min1) {
            minId2 = minIdx;
            minIdx = c;
            min2 = min1;
            min1 = dist;
            // Second Shallot improvement: r shrinking
            if(min1 < l) {
              r = 0.5 * (u + (l = min1));
            }
          }
          else if(dist < min2) {
            minId2 = c;
            min2 = dist;
          }
        }
        if(minIdx != cur) {
          clusters.get(minIdx).add(it);
          clusters.get(cur).remove(it);
          assignment.putInt(it, minIdx);
          second.putInt(it, minId2);
          plusMinusEquals(sums[minIdx], sums[cur], fv);
          ++changed;
          upper.putDouble(it, min1 == curd2 ? u : isSquared ? FastMath.sqrt(min1) : min1);
        }
        lower.putDouble(it, min2 == curd2 ? u : isSquared ? FastMath.sqrt(min2) : min2);
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
