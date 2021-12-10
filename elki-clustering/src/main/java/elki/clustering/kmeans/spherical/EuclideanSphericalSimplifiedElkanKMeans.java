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
import elki.math.linearalgebra.VMath;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A spherical k-Means algorithm based on Hamerly's fast k-means by exploiting
 * the triangle inequality in the corresponding Euclidean space.
 * <p>
 * Please prefer {@link SphericalSimplifiedElkanKMeans}, which uses a tighter
 * bound based on Cosines instead.
 * <p>
 * FIXME: currently requires the vectors to be L2 normalized beforehand
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Andreas Lang, Gloria Feher<br>
 * Accelerating Spherical k-Means<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2021
 *
 * @author Alexander Voß
 * @author Erich Schubert
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "Erich Schubert, Andreas Lang, Gloria Feher", //
    title = "Accelerating Spherical k-Means", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2021", //
    url = "https://doi.org/10.1007/978-3-030-89657-7_17", //
    bibkey = "DBLP:conf/sisap/SchubertLF21")
public class EuclideanSphericalSimplifiedElkanKMeans<V extends NumberVector> extends SphericalKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EuclideanSphericalSimplifiedElkanKMeans.class);

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
  public EuclideanSphericalSimplifiedElkanKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
   * @author Alexander Voß
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
     * Upper bounds
     */
    WritableDoubleDataStore upper;

    /**
     * Lower bounds
     */
    WritableDataStore<double[]> lower;

    /**
     * Cluster separation
     */
    double[] sep;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, double[][] means) {
      super(relation, means);
      upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      lower = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, double[].class);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        lower.put(it, new double[k]); // Filled with 0.
      }
      final int dim = RelationUtil.maxDimensionality(relation);
      sums = new double[k][dim];
      newmeans = new double[k][dim];
      sep = new double[k];
    }

    @Override
    public int iterate(int iteration) {
      if(iteration == 1) {
        return initialAssignToNearestCluster();
      }
      meansFromSums(newmeans, sums, means);
      movedDistance(means, newmeans, sep);
      updateBounds(sep);
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
        double[] l = lower.get(it);
        // Check all (other) means:
        double best = l[0] = sqrtdistance(fv, means[0]);
        int minIndex = 0;
        for(int j = 1; j < k; j++) {
          double dist = l[j] = sqrtdistance(fv, means[j]);
          if(dist < best) {
            minIndex = j;
            best = dist;
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
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        double u = upper.doubleValue(it);
        boolean recompute_u = true; // Elkan's r(x)
        NumberVector fv = relation.get(it);
        double[] l = lower.get(it);
        // Check all (other) means:
        int cur = orig;
        for(int j = 0; j < k; j++) {
          if(orig == j || u <= l[j]) {
            continue; // Condition #3 i-iii not satisfied
          }
          if(recompute_u) { // Need to update bound? #3a
            upper.putDouble(it, u = sqrtdistance(fv, means[cur]));
            recompute_u = false; // Once only
            if(u <= l[j]) { // #3b
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

    /**
     * Update the bounds for k-means.
     *
     * @param move Movement of centers
     */
    protected void updateBounds(double[] move) {
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        upper.increment(it, move[assignment.intValue(it)]);
        VMath.minusEquals(lower.get(it), move);
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
   * @author Alexander Voß
   */
  public static class Par<V extends NumberVector> extends SphericalKMeans.Par<V> {
    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      super.getParameterVarstat(config);
    }

    @Override
    public EuclideanSphericalSimplifiedElkanKMeans<V> make() {
      return new EuclideanSphericalSimplifiedElkanKMeans<>(k, maxiter, initializer, varstat);
    }
  }
}
