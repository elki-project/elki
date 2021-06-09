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
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A spherical k-Means algorithm based on Hamerly's fast k-means by exploiting
 * the triangle inequality.
 * <p>
 * FIXME: currently requires the vectors to be L2 normalized beforehand
 *
 * @author Erich Schubert
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
public class SphericalHamerlyKMeans2<V extends NumberVector> extends SphericalKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SphericalHamerlyKMeans2.class);

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
  public SphericalHamerlyKMeans2(int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
    WritableDoubleDataStore usim;

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
      lsim = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      usim = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
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
      meansFromSums(newmeans, sums);
      movedSimilarity(means, newmeans, csim);
      updateBounds(csim);
      copyMeans(newmeans, means);
      return assignToNearestCluster();
    }

    /**
     * Similarity to previous locations.
     * <p>
     * Used by Hamerly, Elkan (not using the maximum).
     *
     * @param means Old means
     * @param newmeans New means
     * @param sims Similarities moved (output)
     */
    protected void movedSimilarity(double[][] means, double[][] newmeans, double[] sims) {
      assert newmeans.length == means.length && sims.length == means.length;
      sims[0] = Math.min(1, similarity(means[0], newmeans[0]));
      for(int i = 1; i < means.length; i++) {
        sims[i] = Math.min(1, similarity(means[i], newmeans[i]));
      }
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
        for(int i = 2; i < k; i++) {
          double sim = similarity(fv, means[i]);
          if(sim > max1) {
            maxIndex = i;
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
        lsim.putDouble(it, max1);
        usim.putDouble(it, max2);
      }
      return relation.size();
    }

    @Override
    protected int assignToNearestCluster() {
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        // Compute the current bound:
        final double us = usim.doubleValue(it);
        double ls = lsim.doubleValue(it);
        if(ls >= us) {
          continue;
        }
        // Update the lower similarity bound
        NumberVector fv = relation.get(it);
        lsim.putDouble(it, ls = similarity(fv, means[orig]));
        if(ls >= us) {
          continue;
        }
        // Find closest center, and distance to second closest center
        double max2 = Double.NEGATIVE_INFINITY;
        int cur = orig;
        for(int i = 0; i < k; i++) {
          if(i == orig) {
            continue;
          }
          double sim = similarity(fv, means[i]);
          if(sim > ls) {
            cur = i;
            max2 = ls;
            ls = sim;
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
          lsim.putDouble(it, ls); // Remember bound.
        }
        usim.putDouble(it, max2); // Remember bound.
      }
      return changed;
    }

    /**
     * Compute means from cluster sums by adding and normalizing.
     * 
     * @param dst Output means
     * @param sums Input sums
     */
    protected void meansFromSums(double[][] dst, double[][] sums) {
      for(int i = 0; i < k; i++) {
        VMath.overwriteTimes(dst[i], sums[i], 1. / VMath.euclideanLength(sums[i]));
      }
    }

    /**
     * Update the bounds for k-means.
     *
     * @param msin Similarity movement of centers
     */
    protected void updateBounds(double[] msim) {
      // Find the maximum and second smallest similarity.
      int least = 0;
      double delta = msim[0], delta2 = 2;
      for(int i = 1; i < msim.length; i++) {
        final double m = msim[i];
        if(m < delta) {
          delta2 = delta;
          delta = msim[least = i];
        }
        else if(m < delta2) {
          delta2 = m;
        }
      }
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int ai = assignment.intValue(it);
        final double v1 = lsim.doubleValue(it), v2 = msim[ai];
        // tightest: FastMath.cos(FastMath.acos(v1) + FastMath.acos(v2))
        // should be equivalent: v1*v2 - Math.sqrt((1 - v1*v1) * (1 - v2*v2))
        // less tight but cheaper: v1 * v2 + vmin * vmin - 1
        lsim.putDouble(it, v1 * v2 - Math.sqrt((1 - v1 * v1) * (1 - v2 * v2)));
        double w1 = usim.doubleValue(it), w2 = least == ai ? delta2 : delta;
        // tightest: FastMath.cos(FastMath.acos(w1) - FastMath.acos(w2))
        // should be equivalent: w1*w2 + Math.sqrt((1 - w1*w1) * (1 - w2*w2)))
        // less tight but cheaper: (w1 * w - wmin * wmin + 1)
        usim.putDouble(it, w1 * w2 + Math.sqrt((1 - w1 * w1) * (1 - w2 * w2)));
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
    public SphericalHamerlyKMeans2<V> make() {
      return new SphericalHamerlyKMeans2<>(k, maxiter, initializer, varstat);
    }
  }
}
