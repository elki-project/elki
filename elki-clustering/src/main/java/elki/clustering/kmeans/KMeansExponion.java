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
package elki.clustering.kmeans;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Newlings's exponion k-means algorithm, exploiting the triangle inequality.
 * <p>
 * This is <b>not</b> a complete implementation, the approximative sorting part
 * is missing. We also had to guess on the paper how to make best use of F.
 * <p>
 * Reference:
 * <p>
 * J. Newling<br>
 * Fast k-means with accurate bounds<br>
 * Proc. 33nd Int. Conf. on Machine Learning, ICML 2016
 *
 * @author Erich Schubert
 * @since 0.7.5
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
public class KMeansExponion<V extends NumberVector> extends KMeansHamerly<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansExponion.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansExponion(NumberVectorDistance<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distanceFunction, k, maxiter, initializer, varstat);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistance(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResult(varstat, relation);
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends KMeansHamerly.Instance {
    /**
     * Second nearest cluster.
     */
    WritableIntegerDataStore second;

    /**
     * Cluster center distances.
     */
    double[][] cdist;

    /**
     * Sorted neighbors
     */
    int[][] cnum;

    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      second = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
      cdist = new double[k][k];
      cnum = new int[k][k - 1];
    }

    /**
     * Reassign objects, but avoid unnecessary computations based on their
     * bounds.
     *
     * @return number of objects reassigned
     */
    protected int assignToNearestCluster() {
      assert (k == means.length);
      recomputeSeperation(sep, cdist);
      nearestMeans(cdist, cnum);
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
        double curd2 = distance(fv, means[cur]);
        u = isSquared ? FastMath.sqrt(curd2) : curd2;
        upper.putDouble(it, u);
        if(u <= z || u <= sa) {
          continue;
        }
        double r = u + 0.5 * sa; // Our cdist are scaled 0.5
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
        int minIndex = cur;
        for(int i = 0; i < k - 1; i++) {
          int c = cnum[cur][i];
          if(cdist[cur][c] > r) {
            break;
          }
          double dist = distance(fv, means[c]);
          if(dist < min1) {
            minIndex = c;
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
  public static class Parameterizer<V extends NumberVector> extends KMeansHamerly.Parameterizer<V> {
    @Override
    protected KMeansExponion<V> makeInstance() {
      return new KMeansExponion<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
