package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Implementation of the efficient Single-Link Algorithm SLINK of R. Sibson.
 * 
 * <p>
 * Reference:<br />
 * R. Sibson: SLINK: An optimally efficient algorithm for the single-link
 * cluster method. <br/>
 * In: The Computer Journal 16 (1973), No. 1, p. 30-34.
 * </p>
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.has SingleLinkageMethod
 * 
 * @param <O> the type of DatabaseObject the algorithm is applied on
 */
@Title("SLINK: Single Link Clustering")
@Description("Hierarchical clustering algorithm based on single-link connectivity.")
@Reference(authors = "R. Sibson", title = "SLINK: An optimally efficient algorithm for the single-link cluster method", booktitle = "The Computer Journal 16 (1973), No. 1, p. 30-34.", url = "http://dx.doi.org/10.1093/comjnl/16.1.30")
@Alias(value = { "de.lmu.ifi.dbs.elki.algorithm.clustering.SLINK", "clustering.SLINK", "SLINK", "single-link", "single-linkage" })
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
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    // Temporary storage for m.
    WritableDoubleDataStore m = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running SLINK", ids.size(), LOG) : null;
    // has to be an array for monotonicity reasons!
    ModifiableDBIDs processedIDs = DBIDUtil.newArray(ids.size());

    if(getDistanceFunction() instanceof PrimitiveDistanceFunction) {
      PrimitiveDistanceFunction<? super O> distf = (PrimitiveDistanceFunction<? super O>) getDistanceFunction();
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        step1(id, pi, lambda);
        step2primitive(id, processedIDs, relation, distf, m);
        step3(id, pi, lambda, processedIDs, m);
        step4(id, pi, lambda, processedIDs);

        processedIDs.add(id);

        LOG.incrementProcessed(progress);
      }
    }
    else {
      DistanceQuery<O> distQ = database.getDistanceQuery(relation, getDistanceFunction());
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        step1(id, pi, lambda);
        step2(id, processedIDs, distQ, m);
        step3(id, pi, lambda, processedIDs, m);
        step4(id, pi, lambda, processedIDs);

        processedIDs.add(id);

        LOG.incrementProcessed(progress);
      }
    }

    LOG.ensureCompleted(progress);
    // We don't need m anymore.
    m.destroy();
    m = null;

    return new PointerHierarchyRepresentationResult(ids, pi, lambda);
  }

  /**
   * First step: Initialize P(id) = id, L(id) = infinity.
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param pi Pi data store
   * @param lambda Lambda data store
   */
  private void step1(DBIDRef id, WritableDBIDDataStore pi, WritableDoubleDataStore lambda) {
    // P(n+1) = n+1:
    pi.put(id, id);
    // L(n+1) = infinity
    lambda.putDouble(id, Double.POSITIVE_INFINITY);
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   * @param distQuery Distnace query
   * @param m Data store
   */
  private void step2(DBIDRef id, DBIDs processedIDs, DistanceQuery<? super O> distQuery, WritableDoubleDataStore m) {
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
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
   * @param processedIDs the already processed ids
   * @param m Data store
   * @param relation Data relation
   * @param distFunc Distance function to use
   */
  private void step2primitive(DBIDRef id, DBIDs processedIDs, Relation<? extends O> relation, PrimitiveDistanceFunction<? super O> distFunc, WritableDoubleDataStore m) {
    O newObj = relation.get(id);
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      // M(i) = dist(i, n+1)
      m.putDouble(it, distFunc.distance(relation.get(it), newObj));
    }
  }

  /**
   * Third step: Determine the values for P and L
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param processedIDs the already processed ids
   * @param m Data store
   */
  private void step3(DBIDRef id, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, DBIDs processedIDs, WritableDoubleDataStore m) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      double l_i = lambda.doubleValue(it);
      double m_i = m.doubleValue(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
      double mp_i = m.doubleValue(p_i);

      // if L(i) >= M(i)
      if(l_i >= m_i) {
        // M(P(i)) = min { M(P(i)), L(i) }
        m.putDouble(p_i, Math.min(mp_i, l_i));

        // L(i) = M(i)
        lambda.putDouble(it, m_i);

        // P(i) = n+1;
        pi.put(it, id);
      }
      else {
        // M(P(i)) = min { M(P(i)), M(i) }
        m.putDouble(p_i, Math.min(mp_i, m_i));
      }
    }
  }

  /**
   * Fourth step: Actualize the clusters if necessary
   * 
   * @param id the id of the current object
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param processedIDs the already processed ids
   */
  private void step4(DBIDRef id, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, DBIDs processedIDs) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      double l_i = lambda.doubleValue(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    @Override
    protected SLINK<O> makeInstance() {
      return new SLINK<>(distanceFunction);
    }
  }
}
