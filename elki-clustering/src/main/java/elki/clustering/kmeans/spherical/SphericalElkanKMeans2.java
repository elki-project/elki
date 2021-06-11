/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2021
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
package elki.clustering.kmeans.spherical;

import java.util.Arrays;

import elki.clustering.kmeans.HamerlyKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.logging.Logging;

/**
 * Elkan's fast k-means by exploiting the triangle inequality.
 * <p>
 * This variant needs O(n*k) additional memory to store bounds.
 * <p>
 * See {@link HamerlyKMeans} for a close variant that only uses O(n*2)
 * additional memory for bounds.
 * <p>
 *
 * @author Erich Schubert
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
public class SphericalElkanKMeans2<V extends NumberVector> extends SphericalSimplifiedElkanKMeans2<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SphericalElkanKMeans2.class);

  /**
   * Constructor.
   *
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public SphericalElkanKMeans2(int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(k, maxiter, initializer, varstat);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, initialMeans(relation));
    instance.run(maxiter);
    return instance.buildResult(varstat, relation);
  }

  /**
   * Inner instance, storing state for a single data set.
   * 
   * @author Erich Schubert
   */
  protected static class Instance extends SphericalSimplifiedElkanKMeans2.Instance {
    /**
     * Cluster center similarities
     */
    double[][] ccsim = new double[k][k];

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, double[][] means) {
      super(relation, means);
      ccsim = new double[k][k];
    }

    /**
     * Initial separation of means. Used by Elkan.
     *
     * @param ccsim Output square root of pairwise separation
     */
    protected void initialSeparation(double[][] ccsim) {
      final int k = means.length;
      for(int i = 1; i < k; i++) {
        double[] mi = means[i];
        for(int j = 0; j < i; j++) {
          double s = similarity(mi, means[j]);
          ccsim[i][j] = ccsim[j][i] = s > -1 ? Math.sqrt((s + 1) * 0.5) : 0;
        }
      }
    }

    @Override
    protected int initialAssignToNearestCluster() {
      assert k == means.length;
      initialSeparation(ccsim);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        double[] us = usim.get(it);
        // Check all (other) means:
        double best = us[0] = similarity(fv, means[0]);
        int minIndex = 0;
        for(int j = 1; j < k; j++) {
          if(best < ccsim[minIndex][j]) {
            double sim = us[j] = similarity(fv, means[j]);
            if(sim > best) {
              minIndex = j;
              best = sim;
            }
          }
          else {
            us[j] = 2.; // Use a bound below - we don't know minIndex yet.
          }
        }
        // We may have skipped some (not the first) distance computations above
        // In these cases, we initialize with an upper bound based on ccsim
        for(int j = 1; j < k; j++) {
          if(us[j] == 2.) {
            // note: cc=sqrt((sim+1)/2), hence sim=cc^2*2-1
            double cc = ccsim[minIndex][j], simcc = cc * cc * 2 - 1;
            us[j] = best * simcc + Math.sqrt((1 - best * best) * (1 - simcc * simcc));
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        plusEquals(sums[minIndex], fv);
        lsim.putDouble(it, best);
      }
      return relation.size();
    }

    /**
     * Recompute the separation of cluster means.
     * <p>
     * Used by Elkan's variant and Exponion.
     *
     * @param csim Output array of similarity
     * @param ccsim Output square root of Center-to-Center similarities
     */
    protected void recomputeSeperation(double[] csim, double[][] ccsim) {
      final int k = means.length;
      assert csim.length == k;
      Arrays.fill(csim, 0.);
      for(int i = 1; i < k; i++) {
        double[] mi = means[i];
        for(int j = 0; j < i; j++) {
          double s = similarity(mi, means[j]);
          double sqrtsim = s > -1 ? Math.sqrt((s + 1) * 0.5) : 0;
          ccsim[i][j] = ccsim[j][i] = sqrtsim;
          csim[i] = (sqrtsim > csim[i]) ? sqrtsim : csim[i];
          csim[j] = (sqrtsim > csim[j]) ? sqrtsim : csim[j];
        }
      }
    }

    @Override
    protected int assignToNearestCluster() {
      recomputeSeperation(csim, ccsim); // #1
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        double ls = lsim.doubleValue(it);
        // Upper bound check (#2):
        if(ls >= csim[orig]) {
          continue;
        }
        boolean recompute_ls = true; // Elkan's r(x)
        NumberVector fv = relation.get(it);
        double[] us = usim.get(it);
        // Check all (other) means:
        int cur = orig;
        for(int j = 0; j < k; j++) {
          if(orig == j || ls >= us[j] || ls >= ccsim[cur][j]) {
            continue; // Condition #3 i-iii not satisfied
          }
          if(recompute_ls) { // Need to update bound? #3a
            lsim.putDouble(it, ls = similarity(fv, means[cur]));
            recompute_ls = false; // Once only
            if(ls >= us[j] || ls >= ccsim[cur][j]) { // #3b
              continue;
            }
          }
          double sim = us[j] = similarity(fv, means[j]);
          if(sim > ls) {
            cur = j;
            ls = sim;
          }
        }
        // Object has to be reassigned.
        if(cur != orig) {
          clusters.get(cur).add(it);
          clusters.get(orig).remove(it);
          assignment.putInt(it, cur);
          plusMinusEquals(sums[cur], sums[orig], fv);
          ++changed;
          lsim.putDouble(it, ls); // Remember bound.
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
  public static class Par<V extends NumberVector> extends SphericalSimplifiedElkanKMeans2.Par<V> {
    @Override
    public SphericalElkanKMeans2<V> make() {
      return new SphericalElkanKMeans2<>(k, maxiter, initializer, varstat);
    }
  }
}
