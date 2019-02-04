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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

import net.jafama.FastMath;

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
public class KMeansHamerly<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansHamerly.class);

  /**
   * Flag whether to compute the final variance statistic.
   */
  protected boolean varstat = false;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansHamerly(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distanceFunction, k, maxiter, initializer);
    this.varstat = varstat;
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistanceFunction(), initialMeans(database, relation));
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
     * Temporary storage for the new means.
     */
    double[][] newmeans;

    /**
     * Separation of means / distance moved.
     */
    double[] sep;

    /**
     * Upper bounding distance
     */
    WritableDoubleDataStore upper;

    /**
     * Lower bounding distance
     */
    WritableDoubleDataStore lower;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
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
      meansFromSums(newmeans, sums);
      updateBounds(sep, movedDistance(means, newmeans, sep));
      copyMeans(newmeans, means);
      return assignToNearestCluster();
    }

    /**
     * Perform initial cluster assignment.
     *
     * @return Number of changes (i.e. relation size)
     */
    protected int initialAssignToNearestCluster() {
      assert (k == means.length);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        // Find closest center, and distance to two closest centers
        double min1 = Double.POSITIVE_INFINITY, min2 = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for(int i = 0; i < k; i++) {
          double dist = distance(fv, DoubleVector.wrap(means[i]));
          if(dist < min1) {
            minIndex = i;
            min2 = min1;
            min1 = dist;
          }
          else if(dist < min2) {
            min2 = dist;
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        plusEquals(sums[minIndex], fv);
        upper.putDouble(it, isSquared ? FastMath.sqrt(min1) : min1);
        lower.putDouble(it, isSquared ? FastMath.sqrt(min2) : min2);
      }
      return relation.size();
    }

    /**
     * Reassign objects, but avoid unnecessary computations based on their
     * bounds.
     *
     * @return number of objects reassigned
     */
    protected int assignToNearestCluster() {
      assert (k == means.length);
      recomputeSeperation(means, sep);
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
        double curd2 = distance(fv, DoubleVector.wrap(means[cur]));
        u = isSquared ? FastMath.sqrt(curd2) : curd2;
        upper.putDouble(it, u);
        if(u <= z || u <= sa) {
          continue;
        }
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
        int minIndex = cur;
        for(int i = 0; i < k; i++) {
          if(i == cur) {
            continue;
          }
          double dist = distance(fv, DoubleVector.wrap(means[i]));
          if(dist < min1) {
            minIndex = i;
            min2 = min1;
            min1 = dist;
          }
          else if(dist < min2) {
            min2 = dist;
          }
        }
        if(minIndex != cur) {
          clusters.get(minIndex).add(it);
          clusters.get(cur).remove(it);
          assignment.putInt(it, minIndex);
          plusMinusEquals(sums[minIndex], sums[cur], fv);
          ++changed;
          upper.putDouble(it, min1 == curd2 ? u : isSquared ? FastMath.sqrt(min1) : min1);
        }
        lower.putDouble(it, min2 == curd2 ? u : isSquared ? FastMath.sqrt(min2) : min2);
      }
      return changed;
    }

    /**
     * Recompute the separation of cluster means.
     * <p>
     * Used by Hamerly.
     *
     * @param means Means
     * @param sep Output array of separation (half-sqrt scaled)
     */
    protected void recomputeSeperation(double[][] means, double[] sep) {
      final int k = means.length;
      assert (sep.length == k);
      Arrays.fill(sep, Double.POSITIVE_INFINITY);
      for(int i = 1; i < k; i++) {
        DoubleVector m1 = DoubleVector.wrap(means[i]);
        for(int j = 0; j < i; j++) {
          double d = distance(m1, DoubleVector.wrap(means[j]));
          sep[i] = (d < sep[i]) ? d : sep[i];
          sep[j] = (d < sep[j]) ? d : sep[j];
        }
      }
      // We need half the Euclidean distance
      final boolean issquared = isSquared();
      for(int i = 0; i < k; i++) {
        sep[i] = .5 * (issquared ? FastMath.sqrt(sep[i]) : sep[i]);
      }
    }

    /**
     * Update the bounds for k-means.
     *
     * @param move Movement of centers
     * @param delta Maximum center movement.
     */
    protected void updateBounds(double[] move, double delta) {
      delta = -delta;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        upper.increment(it, move[assignment.intValue(it)]);
        lower.increment(it, delta);
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
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    @Override
    protected boolean needsMetric() {
      return true;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      super.getParameterVarstat(config);
    }

    @Override
    protected KMeansHamerly<V> makeInstance() {
      return new KMeansHamerly<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
