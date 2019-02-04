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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Ostrovsky initial means, a variant of k-means++ that is expected to give
 * slightly better results on average, but only works for k-means and not for,
 * e.g., PAM (k-medoids).
 * <p>
 * Reference:
 * <p>
 * R. Ostrovsky, Y. Rabani, L. J. Schulman, C. Swamy<br>
 * The effectiveness of Lloyd-type methods for the k-means problem.<br>
 * Symposium on Foundations of Computer Science (FOCS)
 * <p>
 * R. Ostrovsky, Y. Rabani, L. J. Schulman, C. Swamy<br>
 * The effectiveness of Lloyd-type methods for the k-means problem.<br>
 * Journal of the ACM 59(6)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Vector type
 */
@Reference(authors = "R. Ostrovsky, Y. Rabani, L. J. Schulman, C. Swamy", //
    title = "The effectiveness of Lloyd-type methods for the k-means problem", //
    booktitle = "Symposium on Foundations of Computer Science (FOCS)", //
    url = "https://doi.org/10.1109/FOCS.2006.75", //
    bibkey = "DBLP:conf/focs/OstrovskyRSS062")
@Reference(authors = "R. Ostrovsky, Y. Rabani, L. J. Schulman, C. Swamy", //
    title = "The effectiveness of lloyd-type methods for the k-means problem", //
    booktitle = "Journal of the ACM 59(6)", //
    url = "https://doi.org/10.1145/2395116.2395117", //
    bibkey = "DBLP:journals/jacm/OstrovskyRSS12")
public class OstrovskyInitialMeans<O> extends AbstractKMeansInitialization {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public OstrovskyInitialMeans(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    if(!(distanceFunction instanceof SquaredEuclideanDistanceFunction)) {
      // Really. This uses variance and König-Huygens below, and WILL fail
      throw new IllegalArgumentException("This initialization works ONLY with squared Euclidean distances for correctness.");
    }
    DBIDs ids = relation.getDBIDs();
    @SuppressWarnings("unchecked")
    DistanceQuery<NumberVector> distQ = database.getDistanceQuery((Relation<NumberVector>) relation, (NumberVectorDistanceFunction<NumberVector>) distanceFunction);
    Random random = rnd.getSingleThreadedRandom();

    // Center and total variance
    final int dim = RelationUtil.dimensionality(relation);
    MeanVariance[] mv = MeanVariance.newArray(dim);
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      NumberVector vec = relation.get(it);
      for(int d = 0; d < dim; d++) {
        mv[d].put(vec.doubleValue(d));
      }
    }
    double[] center = new double[dim];
    double total = 0;
    for(int d = 0; d < dim; d++) {
      center[d] = mv[d].getMean();
      total += mv[d].getSumOfSquares();
    }
    final double bias = total / ids.size();

    NumberVector cnv = DoubleVector.wrap(center);

    // Pick first vector:
    NumberVector firstvec = null, secondvec = null;
    double firstdist = 0.;
    double r = random.nextDouble() * total * 2;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      firstdist = distQ.distance(cnv, firstvec = relation.get(it));
      if((r -= bias + firstdist) <= 0) {
        break;
      }
    }

    // Pick second vector:
    double r2 = random.nextDouble() * (total + relation.size() * firstdist);
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double seconddist = distQ.distance(firstvec, secondvec = relation.get(it));
      if((r2 -= seconddist) <= 0) {
        break;
      }
    }

    List<NumberVector> means = new ArrayList<>(k);
    means.add(firstvec);
    means.add(secondvec);

    // Initialize weights
    WritableDoubleDataStore weights = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
    double weightsum = initialWeights(weights, relation, ids, firstvec, secondvec, distQ);
    KMeansPlusPlusInitialMeans.chooseRemaining(relation, ids, distQ, k, means, weights, weightsum, random);
    weights.destroy();
    return unboxVectors(means);
  }

  /**
   * Initialize the weight list.
   *
   * @param weights Weight list
   * @param ids IDs
   * @param relation Data relation
   * @param first First ID
   * @param second Second ID
   * @param distQ Distance query
   * @return Weight sum
   * @param <T> Object type
   */
  protected static <T> double initialWeights(WritableDoubleDataStore weights, Relation<? extends T> relation, DBIDs ids, T first, T second, DistanceQuery<? super T> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      // distance will usually already be squared
      T v = relation.get(it);
      double weight = Math.min(distQ.distance(first, v), distQ.distance(second, v));
      weights.putDouble(it, weight);
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V> extends AbstractKMeansInitialization.Parameterizer {
    @Override
    protected OstrovskyInitialMeans<V> makeInstance() {
      return new OstrovskyInitialMeans<>(rnd);
    }
  }
}
