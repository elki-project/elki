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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * CLINK algorithm for complete linkage.
 * <p>
 * This algorithm runs in O(n²) time, and needs only O(n) memory. The results
 * can differ from the standard algorithm in unfavorable ways, and are
 * order-dependent (Defays: "Modifications of the labeling permit us to obtain
 * different minimal superior ultrametric dissimilarities"). Unfortunately, the
 * results are usually perceived to be substantially worse than the more
 * expensive algorithms for complete linkage clustering. This arises from the
 * fact that this algorithm has to add the new object to the existing tree in
 * every step, instead of being able to always do the globally best merge.
 * <p>
 * Reference:
 * <p>
 * D. Defays<br>
 * An Efficient Algorithm for the Complete Link Cluster Method<br>
 * In: The Computer Journal 20.4
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - CompleteLinkageMethod
 *
 * @param <O> the type of DatabaseObject the algorithm is applied on
 */
@Reference(authors = "D. Defays", //
    title = "An Efficient Algorithm for the Complete Link Cluster Method", //
    booktitle = "The Computer Journal 20.4", //
    url = "https://doi.org/10.1093/comjnl/20.4.364", //
    bibkey = "DBLP:journals/cj/Defays77")
@Alias("Defays")
public class CLINK<O> extends SLINK<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CLINK.class);

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   */
  public CLINK(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * CLINK main loop, based on the SLINK main loop.
   *
   * @param id Current object
   * @param ids All objects
   * @param it Array iterator
   * @param n Last object to process at this run
   * @param pi Parent
   * @param lambda Height
   * @param m Distance
   */
  @Override
  protected void process(DBIDRef id, ArrayDBIDs ids, DBIDArrayIter it, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDoubleDataStore m) {
    clinkstep3(id, it, n, pi, lambda, m);
    clinkstep4567(id, ids, it, n, pi, lambda, m);
    clinkstep8(id, it, n, pi, lambda, m);
  }

  /**
   * Third step: Determine the values for P and L
   *
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param i Iterator
   * @param n Stopping position
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param m Distance data store
   */
  private void clinkstep3(DBIDRef id, DBIDArrayIter i, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDoubleDataStore m) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for(i.seek(0); i.getOffset() < n; i.advance()) {
      double l_i = lambda.doubleValue(i);
      double m_i = m.doubleValue(i);

      // if L(i) < M(i)
      if(l_i < m_i) {
        p_i.from(pi, i); // p_i = pi(it)
        double mp_i = m.doubleValue(p_i);
        // M(P(i)) = max { M(P(i)), M(i) }
        if(mp_i < m_i) {
          m.putDouble(p_i, m_i);
        }

        // M(i) = inf
        m.putDouble(i, Double.POSITIVE_INFINITY);
      }
    }
  }

  /**
   * Fourth to seventh step of CLINK: find best insertion
   *
   * @param id Current objct
   * @param ids All objects
   * @param it Iterator
   * @param n Index threshold
   * @param pi Parent data store
   * @param lambda Height data store
   * @param m Distance data store
   */
  private void clinkstep4567(DBIDRef id, ArrayDBIDs ids, DBIDArrayIter it, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDoubleDataStore m) {
    // step 4: a = n
    DBIDArrayIter a = ids.iter().seek(n - 1);
    // step 5:
    {
      DBIDVar p_i = DBIDUtil.newVar();
      for(it.seek(n - 1); it.valid(); it.retract()) {
        double l_i = lambda.doubleValue(it);
        double mp_i = m.doubleValue(p_i.from(pi, it));
        if(l_i >= mp_i) {
          if(m.doubleValue(it) < m.doubleValue(a)) {
            a.seek(it.getOffset());
          }
        }
        else {
          m.putDouble(it, Double.POSITIVE_INFINITY);
        }
      }
    }

    // step 6
    DBIDVar b = DBIDUtil.newVar().from(pi, a); // b = pi[a]
    double c = lambda.doubleValue(a);
    pi.putDBID(a, id);
    lambda.putDouble(a, m.doubleValue(a));

    // step 7
    if(a.getOffset() < n - 1) {
      DBIDRef last = DBIDUtil.newVar(it.seek(n - 1)); // Used below
      DBIDVar d = DBIDUtil.newVar();
      // if b < n: (then goto 7)
      while(!DBIDUtil.equal(b, id)) {
        if(DBIDUtil.equal(b, last)) {
          pi.putDBID(b, id);
          lambda.putDouble(b, c);
          break;
        }
        d.from(pi, b); // d = pi[b]
        pi.putDBID(b, id); // pi[b] = n + 1
        c = lambda.putDouble(b, c); // c = old l[b], l[b] = c
        b.set(d); // b = d = old pi[b]
      }
    }
  }

  /**
   * Update hierarchy.
   *
   * @param id Current object
   * @param it Iterator
   * @param n Last object to process
   * @param pi Parent data store
   * @param lambda Height data store
   * @param m Distance data store
   */
  private void clinkstep8(DBIDRef id, DBIDArrayIter it, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDoubleDataStore m) {
    DBIDVar p_i = DBIDUtil.newVar(), pp_i = DBIDUtil.newVar();
    for(it.seek(0); it.getOffset() < n; it.advance()) {
      p_i.from(pi, it); // p_i = pi[i]
      pp_i.from(pi, p_i); // pp_i = pi[pi[i]]
      if(DBIDUtil.equal(pp_i, id) && lambda.doubleValue(it) >= lambda.doubleValue(p_i)) {
        pi.putDBID(it, id);
      }
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    @Override
    protected CLINK<O> makeInstance() {
      return new CLINK<>(distanceFunction);
    }
  }
}
