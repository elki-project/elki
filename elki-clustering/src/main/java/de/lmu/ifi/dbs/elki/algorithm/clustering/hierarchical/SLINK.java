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
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Implementation of the efficient Single-Link Algorithm SLINK of R. Sibson.
 * <p>
 * This is probably the fastest exact single-link algorithm currently in use.
 * <p>
 * Reference:
 * <p>
 * R. Sibson:<br>
 * SLINK: An optimally efficient algorithm for the single-link cluster
 * method<br>
 * In: The Computer Journal 16 (1973), No. 1, p. 30-34.
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - implicitly - SingleLinkageMethod
 * @navassoc - generates - PointerHierarchyRepresentationResult
 *
 * @param <O> the type of DatabaseObject the algorithm is applied on
 */
@Title("SLINK: Single Link Clustering")
@Description("Hierarchical clustering algorithm based on single-link connectivity.")
@Reference(authors = "R. Sibson", //
    title = "SLINK: An optimally efficient algorithm for the single-link cluster method", //
    booktitle = "The Computer Journal 16 (1973), No. 1, p. 30-34.", //
    url = "https://doi.org/10.1093/comjnl/16.1.30", //
    bibkey = "DBLP:journals/cj/Sibson73")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.clustering.SLINK", "clustering.SLINK", //
    "single-link", "single-linkage" })
@Priority(Priority.RECOMMENDED)
public class SLINK<O> extends AbstractDistanceBasedAlgorithm<O, PointerHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SLINK.class);

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   */
  public SLINK(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Performs the SLINK algorithm on the given database.
   *
   * @param database Database to process
   * @param relation Data relation to use
   */
  public PointerHierarchyRepresentationResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    // Temporary storage for m.
    WritableDoubleDataStore m = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    final Logging log = getLogger(); // To allow CLINK logger override

    FiniteProgress progress = log.isVerbose() ? new FiniteProgress("Running SLINK", ids.size(), log) : null;
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);

    // First element is trivial/special:
    DBIDArrayIter id = aids.iter(), it = aids.iter();
    // Step 1: initialize
    for(; id.valid(); id.advance()) {
      // P(n+1) = n+1:
      pi.put(id, id);
      // L(n+1) = infinity already.
    }
    // First element is finished already (start at seek(1) below!)
    log.incrementProcessed(progress);

    // Optimized branch
    if(getDistanceFunction() instanceof PrimitiveDistanceFunction) {
      PrimitiveDistanceFunction<? super O> distf = (PrimitiveDistanceFunction<? super O>) getDistanceFunction();
      for(id.seek(1); id.valid(); id.advance()) {
        step2primitive(id, it, id.getOffset(), relation, distf, m);
        process(id, aids, it, id.getOffset(), pi, lambda, m); // SLINK or CLINK
        log.incrementProcessed(progress);
      }
    }
    else {
      // Fallback branch
      DistanceQuery<O> distQ = database.getDistanceQuery(relation, getDistanceFunction());
      for(id.seek(1); id.valid(); id.advance()) {
        step2(id, it, id.getOffset(), distQ, m);
        process(id, aids, it, id.getOffset(), pi, lambda, m); // SLINK or CLINK
        log.incrementProcessed(progress);
      }
    }

    log.ensureCompleted(progress);
    // We don't need m anymore.
    m.destroy();
    m = null;

    return new PointerHierarchyRepresentationResult(ids, pi, lambda, getDistanceFunction().isSquared());
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   *
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param it Array iterator
   * @param n Last object
   * @param distQuery Distance query
   * @param m Data store
   */
  private void step2(DBIDRef id, DBIDArrayIter it, int n, DistanceQuery<? super O> distQuery, WritableDoubleDataStore m) {
    for(it.seek(0); it.getOffset() < n; it.advance()) {
      // M(i) = dist(i, n+1)
      m.putDouble(it, distQuery.distance(it, id));
    }
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   *
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param it Array iterator
   * @param n Last object
   * @param m Data store
   * @param relation Data relation
   * @param distFunc Distance function to use
   */
  private void step2primitive(DBIDRef id, DBIDArrayIter it, int n, Relation<? extends O> relation, PrimitiveDistanceFunction<? super O> distFunc, WritableDoubleDataStore m) {
    O newObj = relation.get(id);
    for(it.seek(0); it.getOffset() < n; it.advance()) {
      // M(i) = dist(i, n+1)
      m.putDouble(it, distFunc.distance(relation.get(it), newObj));
    }
  }

  /**
   * SLINK main loop.
   *
   * @param id Current object
   * @param ids All objects
   * @param it Array iterator
   * @param n Last object to process at this run
   * @param pi Parent
   * @param lambda Height
   * @param m Distance
   */
  protected void process(DBIDRef id, ArrayDBIDs ids, DBIDArrayIter it, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDoubleDataStore m) {
    slinkstep3(id, it, n, pi, lambda, m);
    slinkstep4(id, it, n, pi, lambda);
  }

  /**
   * Third step: Determine the values for P and L
   *
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param it array iterator
   * @param n Last object to process at this run
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param m Data store
   */
  private void slinkstep3(DBIDRef id, DBIDArrayIter it, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDoubleDataStore m) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for(it.seek(0); it.getOffset() < n; it.advance()) {
      double l_i = lambda.doubleValue(it);
      double m_i = m.doubleValue(it);
      p_i.from(pi, it); // p_i = pi(it)
      double mp_i = m.doubleValue(p_i);

      // if L(i) >= M(i)
      if(l_i >= m_i) {
        // M(P(i)) = min { M(P(i)), L(i) }
        if(l_i < mp_i) {
          m.putDouble(p_i, l_i);
        }

        // L(i) = M(i)
        lambda.putDouble(it, m_i);

        // P(i) = n+1;
        pi.put(it, id);
      }
      else {
        // M(P(i)) = min { M(P(i)), M(i) }
        if(m_i < mp_i) {
          m.putDouble(p_i, m_i);
        }
      }
    }
  }

  /**
   * Fourth step: Actualize the clusters if necessary
   *
   * @param id the id of the current object
   * @param it array iterator
   * @param n Last object to process at this run
   * @param pi Pi data store
   * @param lambda Lambda data store
   */
  private void slinkstep4(DBIDRef id, DBIDArrayIter it, int n, WritableDBIDDataStore pi, WritableDoubleDataStore lambda) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for(it.seek(0); it.getOffset() < n; it.advance()) {
      double l_i = lambda.doubleValue(it);
      p_i.from(pi, it); // p_i = pi(it)
      double lp_i = lambda.doubleValue(p_i);

      // if L(i) >= L(P(i))
      if(l_i >= lp_i) {
        // P(i) = n+1
        pi.put(it, id);
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
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
    protected SLINK<O> makeInstance() {
      return new SLINK<>(distanceFunction);
    }
  }
}
