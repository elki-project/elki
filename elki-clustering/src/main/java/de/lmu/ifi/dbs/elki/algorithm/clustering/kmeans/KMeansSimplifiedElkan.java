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

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

import net.jafama.FastMath;

/**
 * Simplified version of Elkan's k-means by exploiting the triangle inequality.
 * <p>
 * Compared to {@link KMeansElkan}, this uses less pruning, but also does not
 * need to maintain a matrix of pairwise centroid separation.
 * <p>
 * Reference:
 * <p>
 * J. Newling<br>
 * Fast k-means with accurate bounds<br>
 * Proc. 33nd Int. Conf. on Machine Learning, ICML 2016
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "J. Newling", //
    title = "Fast k-means with accurate bounds", //
    booktitle = "Proc. 33nd Int. Conf. on Machine Learning, ICML 2016", //
    url = "http://jmlr.org/proceedings/papers/v48/newling16.html", //
    bibkey = "DBLP:conf/icml/NewlingF16")
public class KMeansSimplifiedElkan<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansSimplifiedElkan.class);

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
  public KMeansSimplifiedElkan(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
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
     * Upper bounds
     */
    WritableDoubleDataStore upper;

    /**
     * Lower bounds
     */
    WritableDataStore<double[]> lower;

    /**
     * Sums of clusters.
     */
    double[][] sums;

    /**
     * Scratch space for new means.
     */
    double[][] newmeans;

    /**
     * Cluster separation
     */
    double[] sep = new double[k];

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
      super(relation, df, means);
      upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      lower = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, double[].class);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        lower.put(it, new double[k]); // Filled with 0.
      }
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
      movedDistance(means, newmeans, sep);
      updateBounds(sep);
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
        double[] l = lower.get(it);
        // Check all (other) means:
        double best = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for(int j = 0; j < k; j++) {
          double dist = distance(fv, DoubleVector.wrap(means[j]));
          dist = isSquared ? FastMath.sqrt(dist) : dist;
          l[j] = dist;
          if(dist < best) {
            minIndex = j;
            best = dist;
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        upper.putDouble(it, best);
        plusEquals(sums[minIndex], fv);
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
            u = distance(fv, DoubleVector.wrap(means[cur]));
            u = isSquared ? FastMath.sqrt(u) : u;
            upper.putDouble(it, u);
            recompute_u = false; // Once only
            if(u <= l[j]) { // #3b
              continue;
            }
          }
          double dist = distance(fv, DoubleVector.wrap(means[j]));
          dist = isSquared ? FastMath.sqrt(dist) : dist;
          l[j] = dist;
          if(dist < u) {
            cur = j;
            u = dist;
          }
        }
        // Object is to be reassigned.
        if(cur != orig) {
          upper.putDouble(it, u); // Remember bound.
          clusters.get(cur).add(it);
          clusters.get(orig).remove(it);
          assignment.putInt(it, cur);
          plusMinusEquals(sums[cur], sums[orig], fv);
          ++changed;
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
    protected KMeansSimplifiedElkan<V> makeInstance() {
      return new KMeansSimplifiedElkan<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
