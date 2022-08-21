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
package elki.clustering.kmeans.initialization;

import java.util.ArrayList;
import java.util.List;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.math.MeanVariance;
import elki.utilities.documentation.Reference;
import elki.utilities.random.RandomFactory;

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
 */
@Reference(authors = "R. Ostrovsky, Y. Rabani, L. J. Schulman, C. Swamy", //
    title = "The effectiveness of Lloyd-type methods for the k-means problem", //
    booktitle = "Symposium on Foundations of Computer Science (FOCS)", //
    url = "https://doi.org/10.1109/FOCS.2006.75", //
    bibkey = "DBLP:conf/focs/OstrovskyRSS06")
@Reference(authors = "R. Ostrovsky, Y. Rabani, L. J. Schulman, C. Swamy", //
    title = "The effectiveness of Lloyd-type methods for the k-means problem", //
    booktitle = "Journal of the ACM 59(6)", //
    url = "https://doi.org/10.1145/2395116.2395117", //
    bibkey = "DBLP:journals/jacm/OstrovskyRSS12")
public class Ostrovsky extends AbstractKMeansInitialization {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Ostrovsky.class);

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public Ostrovsky(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    if(!(distance instanceof SquaredEuclideanDistance)) {
      // Really. This uses variance and König-Huygens below, and WILL fail
      throw new IllegalArgumentException("This initialization works ONLY with squared Euclidean distances for correctness.");
    }
    return new NumberVectorInstance(relation, distance, rnd).run(relation, k);
  }

  /**
   * Instance for number vectors.
   *
   * @author Erich Schubert
   */
  protected class NumberVectorInstance extends KMeansPlusPlus.NumberVectorInstance {
    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param distance Distance function
     * @param rnd Random generator
     */
    public NumberVectorInstance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> distance, RandomFactory rnd) {
      super(relation, distance, rnd);
    }

    public double[][] run(Relation<? extends NumberVector> relation, int k) {
      // Center and total variance
      MeanVariance[] mv = MeanVariance.of(relation);
      double[] center = new double[mv.length];
      double total = 0;
      for(int d = 0; d < mv.length; d++) {
        center[d] = mv[d].getMean();
        total += mv[d].getSumOfSquares();
      }
      final double bias = total / ids.size();
      NumberVector cnv = DoubleVector.wrap(center);

      // Pick first vector:
      List<NumberVector> means = new ArrayList<>(k);
      NumberVector firstvec = null;
      double firstdist, r = random.nextDouble() * total * 2;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        ++diststat;
        // distance is squared Euclidean as per above
        firstdist = distance.distance(cnv, firstvec = relation.get(it));
        if((r -= bias + firstdist) <= 0) {
          break;
        }
      }
      means.add(firstvec);
      // The rule for picking the second vector is effectively the same
      // as for picking all the remaining vectors, so we can use the inherited
      // code from our k-means++ implementation.
      chooseRemaining(k, means, initialWeights(firstvec));
      weights.destroy();
      LOG.statistics(new LongStatistic(KMeansPlusPlus.class.getName() + ".distance-computations", diststat));
      return unboxVectors(means);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractKMeansInitialization.Par {
    @Override
    public Ostrovsky make() {
      return new Ostrovsky(rnd);
    }
  }
}
