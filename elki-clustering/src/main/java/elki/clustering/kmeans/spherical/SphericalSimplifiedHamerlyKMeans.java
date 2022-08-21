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
public class SphericalSimplifiedHamerlyKMeans<V extends NumberVector> extends SphericalKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SphericalSimplifiedHamerlyKMeans.class);

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
  public SphericalSimplifiedHamerlyKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
      lsim = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      usim = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 2.);
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
        // Find closest center, and distance to the second closest center
        double max1 = similarity(fv, means[0]), max2 = -1;
        int maxIndex = 0;
        for(int j = 1; j < k; j++) {
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
        double ls = lsim.doubleValue(it);
        final double us = usim.doubleValue(it);
        if(ls >= us) {
          continue;
        }
        // Update the lower similarity bound
        NumberVector fv = relation.get(it);
        lsim.putDouble(it, ls = similarity(fv, means[orig]));
        if(ls >= us) {
          continue;
        }
        // Find closest center, and distance to the second closest center
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
     * Update the bounds for k-means.
     *
     * @param msim Similarity movement of centers
     */
    protected void updateBounds(double[] msim) {
      // Find the minimum and second smallest similarity.
      int least = 0; // , most = 0;
      double delta = msim[0], delta2 = 1;
      // double tau = delta, tau2 = -1;
      for(int i = 1; i < msim.length; i++) {
        final double m = msim[i];
        if(m < delta) {
          delta2 = delta;
          delta = m;
          least = i;
        }
        else if(m < delta2) {
          delta2 = m;
        }
        /*if(m > tau) {
          tau2 = tau;
          tau = m;
          most = i;
        }
        else if(m > tau2) {
          tau2 = m;
        }*/
      }
      delta = 1 - delta * delta;
      delta2 = 1 - delta2 * delta2;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int ai = assignment.intValue(it);
        final double v2 = msim[ai];
        if(v2 < 1) {
          final double v1 = Math.min(1, lsim.doubleValue(it));
          // tightest: FastMath.cos(FastMath.acos(v1) + FastMath.acos(v2))
          // should be equivalent: v1*v2 - Math.sqrt((1 - v1*v1) * (1 - v2*v2))
          // less tight but cheaper: v1 * v2 + vmin * vmin - 1
          lsim.putDouble(it, v1 * v2 - Math.sqrt((1 - v1 * v1) * (1 - v2 * v2)));
        }
        final double w2 = least == ai ? delta2 : delta;
        if(w2 > 0) {
          double w1 = Math.min(1, usim.doubleValue(it));
          // tightest: FastMath.cos(FastMath.acos(w1) - FastMath.acos(w2))
          // should be equivalent: w1*w2 + Math.sqrt((1 - w1*w1) * (1 - w2*w2)))
          // less tight but cheaper: (w1 * w2 - wmin * wmin + 1)
          // double w2p = most == ai ? tau2 : tau;
          usim.putDouble(it, w1 + Math.sqrt((1 - w1 * w1) * w2));
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
    public SphericalSimplifiedHamerlyKMeans<V> make() {
      return new SphericalSimplifiedHamerlyKMeans<>(k, maxiter, initializer, varstat);
    }
  }
}
