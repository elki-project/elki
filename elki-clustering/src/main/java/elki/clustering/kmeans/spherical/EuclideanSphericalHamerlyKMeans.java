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

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A spherical k-Means algorithm based on Hamerly's fast k-means by exploiting
 * the triangle inequality in the corresponding Euclidean space.
 * <p>
 * Please prefer {@link SphericalHamerlyKMeans}, which uses a tighter bound
 * based on Cosines instead.
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
 * @since 0.8.0
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
public class EuclideanSphericalHamerlyKMeans<V extends NumberVector> extends SphericalKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EuclideanSphericalHamerlyKMeans.class);

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
  public EuclideanSphericalHamerlyKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
    WritableDoubleDataStore lower;

    /**
     * Separation of means / distance moved.
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
      lower = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
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
        // Find closest center, and distance to the second closest center
        double max1 = similarity(fv, means[0]);
        double max2 = k > 1 ? similarity(fv, means[1]) : max1;
        int maxIndex = 0;
        if(max2 > max1) {
          double tmp = max1;
          max1 = max2;
          max2 = tmp;
          maxIndex = 1;
        }
        for(int j = 2; j < k; j++) {
          double sim = similarity(fv, means[j]);
          if(sim > max1) {
            maxIndex = j;
            max2 = max1;
            max1 = sim;
          }
          else if(sim > max2) {
            max2 = sim;
          }
        }
        // Assign to nearest cluster.
        clusters.get(maxIndex).add(it);
        assignment.putInt(it, maxIndex);
        plusEquals(sums[maxIndex], fv);
        upper.putDouble(it, Math.sqrt(2 - 2 * max1));
        lower.putDouble(it, Math.sqrt(2 - 2 * max2));
      }
      return relation.size();
    }

    @Override
    protected int assignToNearestCluster() {
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        // Compute the current bound:
        final double l = lower.doubleValue(it);
        double u = upper.doubleValue(it);
        if(u <= l) {
          continue;
        }
        // Update the upper bound
        NumberVector fv = relation.get(it);
        final double curSim = similarity(fv, means[orig]);
        upper.putDouble(it, u = Math.sqrt(2 - 2 * curSim));
        if(u <= l) {
          continue;
        }
        // Find closest center, and distance to the second closest center
        double max1 = curSim, max2 = Double.NEGATIVE_INFINITY;
        int cur = orig;
        for(int i = 0; i < k; i++) {
          if(i == orig) {
            continue;
          }
          double sim = similarity(fv, means[i]);
          if(sim > max1) {
            cur = i;
            max2 = max1;
            max1 = sim;
          }
          else if(sim > max2) {
            max2 = sim;
          }
        }
        // Object has to be reassigned.
        if(cur != orig) {
          clusters.get(cur).add(it);
          clusters.get(orig).remove(it);
          assignment.putInt(it, cur);
          plusMinusEquals(sums[cur], sums[orig], fv);
          ++changed;
          upper.putDouble(it, max1 == curSim ? u : Math.sqrt(2 - 2 * max1));
        }
        lower.putDouble(it, max2 == curSim ? u : Math.sqrt(2 - 2 * max2));
      }
      return changed;
    }

    /**
     * Update the bounds for k-means.
     *
     * @param move Movement of centers
     */
    protected void updateBounds(double[] move) {
      // Find the maximum and second largest movement.
      int most = 0;
      double delta = move[0], delta2 = 0;
      for(int i = 1; i < move.length; i++) {
        final double m = move[i];
        if(m > delta) {
          delta2 = delta;
          delta = m;
          most = i;
        }
        else if(m > delta2) {
          delta2 = m;
        }
      }
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int a = assignment.intValue(it);
        upper.increment(it, move[a]);
        lower.increment(it, a == most ? -delta2 : -delta);
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
    public EuclideanSphericalHamerlyKMeans<V> make() {
      return new EuclideanSphericalHamerlyKMeans<>(k, maxiter, initializer, varstat);
    }
  }
}
