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
package elki.clustering.kmeans.spherical;

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A spherical k-Means algorithm based on Hamerly's fast k-means by exploiting
 * the triangle inequality.
 * <p>
 * FIXME: currently requires the vectors to be L2 normalized beforehand
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Andreas Lang, Gloria Feher<br>
 * Accelerating Spherical k-Means<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2021
 * <p>
 * The underlying triangle inequality used for pruning is introduced in:
 * <p>
 * Erich Schubert<br>
 * A Triangle Inequality for Cosine Similarity<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2021
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "Erich Schubert, Andreas Lang, Gloria Feher", //
    title = "Accelerating Spherical k-Means", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2021", //
    url = "https://doi.org/10.1007/978-3-030-89657-7_17", //
    bibkey = "DBLP:conf/sisap/SchubertLF21")
@Reference(authors = "Erich Schubert", //
    title = "A Triangle Inequality for Cosine Similarity", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2021", //
    url = "https://doi.org/10.1007/978-3-030-89657-7_3", //
    bibkey = "DBLP:conf/sisap/Schubert21")
public class SphericalSimplifiedElkanKMeans<V extends NumberVector> extends SphericalKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SphericalSimplifiedElkanKMeans.class);

  /**
   * Flag whether to compute the final variance statistic.
   */
  protected boolean varstat;

  /**
   * Constructor.
   *
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public SphericalSimplifiedElkanKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(k, maxiter, initializer);
    this.varstat = varstat;
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
  protected static class Instance extends SphericalKMeans.Instance {
    /**
     * Sum aggregate for the new mean.
     */
    double[][] sums;

    /**
     * Scratch space for new means.
     */
    double[][] newmeans;

    /**
     * Similarity lower bound.
     */
    WritableDoubleDataStore lsim;

    /**
     * Similarity upper bound.
     */
    WritableDataStore<double[]> usim;

    /**
     * Cluster self-similarity.
     */
    double[] csim;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, double[][] means) {
      super(relation, means);
      lsim = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      usim = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, double[].class);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final double[] a = new double[k];
        Arrays.fill(a, 2);
        usim.put(it, a);
      }
      final int dim = RelationUtil.maxDimensionality(relation);
      sums = new double[k][dim];
      newmeans = new double[k][dim];
      csim = new double[k];
    }

    @Override
    public int iterate(int iteration) {
      if(iteration == 1) {
        return initialAssignToNearestCluster();
      }
      meansFromSums(newmeans, sums, means);
      movedSimilarity(means, newmeans, csim);
      updateBounds(csim);
      copyMeans(newmeans, means);
      return assignToNearestCluster();
    }

    /**
     * Perform initial cluster assignment.
     *
     * @return Number of changes (i.e., relation size)
     */
    protected int initialAssignToNearestCluster() {
      assert k == means.length;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        double[] us = usim.get(it);
        // Check all (other) means:
        double best = us[0] = similarity(fv, means[0]);
        int maxIndex = 0;
        for(int j = 1; j < k; j++) {
          double sim = us[j] = similarity(fv, means[j]);
          if(sim > best) {
            maxIndex = j;
            best = sim;
          }
        }
        // Assign to nearest cluster.
        clusters.get(maxIndex).add(it);
        assignment.putInt(it, maxIndex);
        plusEquals(sums[maxIndex], fv);
        lsim.putDouble(it, best);
      }
      return relation.size();
    }

    @Override
    protected int assignToNearestCluster() {
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        double ls = lsim.doubleValue(it);
        boolean recompute_ls = true; // Elkan's r(x)
        NumberVector fv = relation.get(it);
        double[] us = usim.get(it);
        // Check all (other) means:
        int cur = orig;
        for(int j = 0; j < k; j++) {
          if(orig == j || ls >= us[j]) {
            continue; // Condition #3 i-iii not satisfied
          }
          if(recompute_ls) { // Need to update bound? #3a
            lsim.putDouble(it, ls = similarity(fv, means[cur]));
            recompute_ls = false; // Once only
            if(ls >= us[j]) { // #3b
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

    /**
     * Update the bounds for k-means.
     *
     * @param msim Similarity of moved centers
     */
    protected void updateBounds(double[] msim) {
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int ai = assignment.intValue(it);
        final double v1 = Math.min(1, lsim.doubleValue(it)), v2 = msim[ai];
        // tightest: FastMath.cos(FastMath.acos(v1) + FastMath.acos(v2))
        // should be equivalent: v1*v2 - Math.sqrt((1 - v1*v1) * (1 - v2*v2))
        // less tight but cheaper: v1 * v2 + vmin * vmin - 1
        lsim.putDouble(it, v1 * v2 - Math.sqrt((1 - v1 * v1) * (1 - v2 * v2)));
        double[] us = usim.get(it);
        for(int i = 0; i < us.length; i++) {
          final double w1 = Math.min(1, us[i]), w2 = msim[i];
          // tightest: FastMath.cos(FastMath.acos(w1) - FastMath.acos(w2))
          // should be equivalent: w1*w2 + Math.sqrt((1 - w1*w1) * (1 - w2*w2)))
          // less tight but cheaper: (w1 * w2 - wmin * wmin + 1)
          us[i] = w1 * w2 + Math.sqrt((1 - w1 * w1) * (1 - w2 * w2));
        }
      }
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
  public static class Par<V extends NumberVector> extends SphericalKMeans.Par<V> {
    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      super.getParameterVarstat(config);
    }

    @Override
    public SphericalSimplifiedElkanKMeans<V> make() {
      return new SphericalSimplifiedElkanKMeans<>(k, maxiter, initializer, varstat);
    }
  }
}
