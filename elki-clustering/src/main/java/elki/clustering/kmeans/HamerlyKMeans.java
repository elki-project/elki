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
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Hamerly's fast k-means by exploiting the triangle inequality.
 * <p>
 * Reference:
 * <p>
 * G. Hamerly<br>
 * Making k-means even faster<br>
 * Proc. 2010 SIAM International Conference on Data Mining
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "G. Hamerly", //
    title = "Making k-means even faster", //
    booktitle = "Proc. 2010 SIAM International Conference on Data Mining", //
    url = "https://doi.org/10.1137/1.9781611972801.12", //
    bibkey = "DBLP:conf/sdm/Hamerly10")
public class HamerlyKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HamerlyKMeans.class);

  /**
   * Flag whether to compute the final variance statistic.
   */
  protected boolean varstat = false;

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public HamerlyKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distance, k, maxiter, initializer);
    this.varstat = varstat;
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
  protected static class Instance extends AbstractKMeans.Instance {
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
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      lower = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      final int dim = means[0].length;
      sums = new double[k][dim];
      newmeans = new double[k][dim];
      sep = new double[k];
    }

    @Override
    protected int iterate(int iteration) {
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
      double[][] cdist = new double[k][k];
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
      recomputeSeperation(sep);
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        // Compute the current bound:
        final double l = lower.doubleValue(it);
        final double sa = sep[orig];
        double u = upper.doubleValue(it);
        if(u <= l || u <= sa) {
          continue;
        }
        // Update the upper bound
        NumberVector fv = relation.get(it);
        double curd2 = distance(fv, means[orig]);
        upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
        if(u <= l || u <= sa) {
          continue;
        }
        // Find closest center, and distance to the second closest center
        double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
        int cur = orig;
        for(int i = 0; i < k; i++) {
          if(i == orig) {
            continue;
          }
          double dist = distance(fv, means[i]);
          if(dist < min1) {
            cur = i;
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

    /**
     * Recompute the separation of cluster means.
     * <p>
     * Used by Hamerly.
     *
     * @param sep Output array of separation (half-sqrt scaled)
     */
    protected void recomputeSeperation(double[] sep) {
      final int k = means.length;
      assert sep.length == k;
      Arrays.fill(sep, Double.POSITIVE_INFINITY);
      for(int i = 1; i < k; i++) {
        double[] m1 = means[i];
        for(int j = 0; j < i; j++) {
          double d = distance(m1, means[j]);
          sep[i] = (d < sep[i]) ? d : sep[i];
          sep[j] = (d < sep[j]) ? d : sep[j];
        }
      }
      // We need half the Euclidean distance
      for(int i = 0; i < k; i++) {
        sep[i] = .5 * (isSquared ? Math.sqrt(sep[i]) : sep[i]);
      }
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
          delta = move[most = i];
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
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    @Override
    protected boolean needsMetric() {
      return true;
    }

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      super.getParameterVarstat(config);
    }

    @Override
    public HamerlyKMeans<V> make() {
      return new HamerlyKMeans<>(distance, k, maxiter, initializer, varstat);
    }
  }
}
