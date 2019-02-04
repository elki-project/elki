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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Elkan's fast k-means by exploiting the triangle inequality.
 * <p>
 * This variant needs O(n*k) additional memory to store bounds.
 * <p>
 * See {@link KMeansHamerly} for a close variant that only uses O(n*2)
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
public class KMeansElkan<V extends NumberVector> extends KMeansSimplifiedElkan<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansElkan.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansElkan(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distanceFunction, k, maxiter, initializer, varstat);
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
  protected static class Instance extends KMeansSimplifiedElkan.Instance {
    /**
     * Cluster center distances
     */
    double[][] cdist = new double[k][k];

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
      super(relation, df, means);
      cdist = new double[k][k];
    }

    @Override
    protected int assignToNearestCluster() {
      assert (k == means.length);
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
            u = distance(fv, DoubleVector.wrap(means[cur]));
            u = isSquared ? FastMath.sqrt(u) : u;
            upper.putDouble(it, u);
            recompute_u = false; // Once only
            if(u <= l[j] || u <= cdist[cur][j]) { // #3b
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
  public static class Parameterizer<V extends NumberVector> extends KMeansSimplifiedElkan.Parameterizer<V> {
    @Override
    protected KMeansElkan<V> makeInstance() {
      return new KMeansElkan<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
