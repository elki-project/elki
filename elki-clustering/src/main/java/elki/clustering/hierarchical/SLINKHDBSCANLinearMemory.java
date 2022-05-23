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
package elki.clustering.hierarchical;

import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.*;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.utilities.documentation.Reference;

/**
 * Linear memory implementation of HDBSCAN clustering based on SLINK.
 * <p>
 * By not building a distance matrix, we can reduce memory usage to linear
 * memory only; but at the cost of roughly double the runtime (unless using
 * indexes) as we first need to compute all kNN distances (for core sizes), then
 * recompute distances when building the spanning tree.
 * <p>
 * This version uses the SLINK algorithm to directly produce the pointer
 * representation expected by the extraction methods. The SLINK algorithm is
 * closely related to Prim's minimum spanning tree, but produces the more
 * compact pointer representation instead of an edges list.
 * <p>
 * This implementation does <em>not</em> include the cluster extraction
 * discussed as Step 4. This functionality should however already be provided by
 * {@link elki.clustering.hierarchical.extraction.HDBSCANHierarchyExtraction}
 * . For this reason, we also do <em>not include self-edges</em>.
 * <p>
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, J. Sander<br>
 * Density-Based Clustering Based on Hierarchical Density Estimates<br>
 * Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - ClusterMergeHistory
 */
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, J. Sander", //
    title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
    booktitle = "Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)", //
    url = "https://doi.org/10.1007/978-3-642-37456-2_14", //
    bibkey = "DBLP:conf/pakdd/CampelloMS13")
public class SLINKHDBSCANLinearMemory<O> extends AbstractHDBSCAN<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SLINKHDBSCANLinearMemory.class);

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param minPts Minimum number of points for coredists
   */
  public SLINKHDBSCANLinearMemory(Distance<? super O> distance, int minPts) {
    super(distance, minPts);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    final QueryBuilder<O> qb = new QueryBuilder<>(relation, distance);
    final DistanceQuery<O> distQ = qb.distanceQuery();
    final KNNSearcher<DBIDRef> knnQ = qb.kNNByDBID(minPts);
    // We need array addressing later.
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    // Compute the core distances
    // minPts + 1: ignore query point.
    final WritableDoubleDataStore coredists = computeCoreDists(ids, knnQ, minPts);

    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    // Temporary storage for m.
    WritableDoubleDataStore m = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running HDBSCAN*-SLINK", ids.size(), LOG) : null;
    // has to be an array for monotonicity reasons!
    ModifiableDBIDs processedIDs = DBIDUtil.newArray(ids.size());

    for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      // Steps 1,3,4 are exactly as in SLINK
      pi.put(id, id);
      // Step 2 is modified to use a different distance
      step2(id, processedIDs, distQ, coredists, m);
      step3(id, pi, lambda, processedIDs, m);
      step4(id, pi, lambda, processedIDs);

      processedIDs.add(id);

      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    return SLINK.convertOutput(new ClusterMergeHistoryBuilder(ids, distance.isSquared()), ids, pi, lambda) //
        .complete(coredists);
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   *
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   * @param distQuery Distance query
   * @param m Data store
   */
  private void step2(DBIDRef id, DBIDs processedIDs, DistanceQuery<? super O> distQuery, DoubleDataStore coredists, WritableDoubleDataStore m) {
    double coreP = coredists.doubleValue(id);
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      // M(i) = dist(i, n+1)
      m.putDouble(it, MathUtil.max(coreP, coredists.doubleValue(it), distQuery.distance(id, it)));
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
      double l_i = lambda.doubleValue(it), m_i = m.doubleValue(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
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
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> extends AbstractHDBSCAN.Par<O> {
    @Override
    public SLINKHDBSCANLinearMemory<O> make() {
      return new SLINKHDBSCANLinearMemory<>(distance, minPts);
    }
  }
}
