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
import elki.utilities.documentation.Reference;

/**
 * Elkan's fast k-means by exploiting the triangle inequality.
 * <p>
 * This variant needs O(n*k) additional memory to store bounds.
 * <p>
 * See {@link HamerlyKMeans} for a close variant that only uses O(n*2)
 * additional memory for bounds.
 * <p>
 * Reference:
 * <p>
 * C. Elkan<br>
 * Using the triangle inequality to accelerate k-means<br>
 * Proc. 20th International Conference on Machine Learning, ICML 2003
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "C. Elkan", //
    title = "Using the triangle inequality to accelerate k-means", //
    booktitle = "Proc. 20th International Conference on Machine Learning, ICML 2003", //
    url = "http://www.aaai.org/Library/ICML/2003/icml03-022.php", //
    bibkey = "DBLP:conf/icml/Elkan03")
public class ElkanKMeans<V extends NumberVector> extends SimplifiedElkanKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ElkanKMeans.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public ElkanKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
  protected static class Instance extends SimplifiedElkanKMeans.Instance {
    /**
     * Cluster center distances
     */
    double[][] cdist;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      cdist = new double[k][k];
    }

    @Override
    protected int initialAssignToNearestCluster() {
      assert k == means.length;
      initialSeperation(cdist);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        double[] l = lower.get(it);
        // Check all (other) means:
        double best = l[0] = sqrtdistance(fv, means[0]);
        int minIndex = 0;
        for(int j = 1; j < k; j++) {
          if(best > cdist[minIndex][j]) {
            double dist = l[j] = sqrtdistance(fv, means[j]);
            if(dist < best) {
              minIndex = j;
              best = dist;
            }
          }
        }
        for(int j = 1; j < k; j++) {
          if(l[j] == 0. && j != minIndex) {
            l[j] = 2 * cdist[minIndex][j] - best;
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        plusEquals(sums[minIndex], fv);
        upper.putDouble(it, best);
      }
      return relation.size();
    }

    @Override
    protected int assignToNearestCluster() {
      recomputeSeperation(sep, cdist); // #1
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        double u = upper.doubleValue(it);
        // Upper bound check (#2):
        if(u <= sep[orig]) {
          continue;
        }
        boolean recompute_u = true; // Elkan's r(x)
        NumberVector fv = relation.get(it);
        double[] l = lower.get(it);
        // Check all (other) means:
        int cur = orig;
        for(int j = 0; j < k; j++) {
          if(orig == j || u <= l[j] || u <= cdist[cur][j]) {
            continue; // Condition #3 i-iii not satisfied
          }
          if(recompute_u) { // Need to update bound? #3a
            upper.putDouble(it, u = sqrtdistance(fv, means[cur]));
            recompute_u = false; // Once only
            if(u <= l[j] || u <= cdist[cur][j]) { // #3b
              continue;
            }
          }
          double dist = l[j] = sqrtdistance(fv, means[j]);
          if(dist < u) {
            cur = j;
            u = dist;
          }
        }
        // Object has to be reassigned.
        if(cur != orig) {
          clusters.get(cur).add(it);
          clusters.get(orig).remove(it);
          assignment.putInt(it, cur);
          plusMinusEquals(sums[cur], sums[orig], fv);
          ++changed;
          upper.putDouble(it, u); // Remember bound.
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
  public static class Par<V extends NumberVector> extends SimplifiedElkanKMeans.Par<V> {
    @Override
    public ElkanKMeans<V> make() {
      return new ElkanKMeans<>(distance, k, maxiter, initializer, varstat);
    }
  }
}
