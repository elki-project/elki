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

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.random.RandomFactory;

/**
 * AFK-MC² initialization
 * <p>
 * Reference:
 * <p>
 * <br>
 * O. Bachem, M. Lucic, S. H. Hassani, A. Krause<br>
 * Fast and Provably Good Seedings for k-Means<br>
 * Neural Information Processing Systems 2016
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Title("AFK-MC²")
@Reference(authors = "O. Bachem, M. Lucic, S. H. Hassani, A. Krause", //
    title = "Fast and Provably Good Seedings for k-Means", //
    booktitle = "Neural Information Processing Systems 2016", //
    url = "https://proceedings.neurips.cc/paper/2016/hash/d67d8ab4f4c10bf22aa353e27879133c-Abstract.html", //
    bibkey = "DBLP:conf/nips/BachemLH016")
public class AFKMC2 extends KMC2 {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AFKMC2.class);

  /**
   * Constructor.
   *
   * @param m M parameter
   * @param rnd Random generator.
   */
  public AFKMC2(int m, RandomFactory rnd) {
    super(m, rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    return new Instance(relation, distance, m, rnd).run(k);
  }

  /**
   * Abstract instance implementing the weight handling.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends KMC2.Instance {
    /**
     * Constructor.
     *
     * @param relation Data relation to process
     * @param distance Distance function
     * @param m M parameter
     * @param rnd Random generator
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> distance, int m, RandomFactory rnd) {
      super(relation, distance, m, rnd);
    }

    @Override
    protected DBIDRef sample(double weightsum) {
      final int n = relation.size();
      while(true) {
        if(weightsum > Double.MAX_VALUE) {
          throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
        }
        if(weightsum < Double.MIN_NORMAL) {
          LOG.warning("Could not choose a reasonable mean - to few unique data points?");
        }
        // Compared to the original publication, we sample from q*weightsum*2:
        double r = random.nextDouble() * weightsum * 2, bias = weightsum / n;
        DBIDIter it = relation.iterDBIDs();
        while(it.valid()) {
          if((r -= weights.doubleValue(it) + bias) <= 0) {
            break;
          }
          it.advance();
        }
        if(!it.valid()) { // Rare case, but happens due to floating math
          weightsum -= r; // Decrease
          continue; // Retry
        }
        return it;
      }
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends KMC2.Par {
    @Override
    public AFKMC2 make() {
      return new AFKMC2(m, rnd);
    }
  }
}
