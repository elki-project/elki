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

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.*;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Implementation of the efficient Single-Link Algorithm SLINK of R. Sibson.
 * <p>
 * For ELKI 0.8.0, this was rewritten to no longer use the original "pointer"
 * output format, but instead generate an easier to use merge history now.
 * <p>
 * This is probably the fastest exact single-link algorithm currently in use.
 * <p>
 * Reference:
 * <p>
 * R. Sibson<br>
 * SLINK: An optimally efficient algorithm for the single-link cluster
 * method<br>
 * In: The Computer Journal 16 (1)
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - implicitly - SingleLinkageMethod
 * @navassoc - generates - ClusterMergeHistory
 *
 * @param <O> the type of DatabaseObject the algorithm is applied on
 */
@Title("SLINK: Single Link Clustering")
@Description("Hierarchical clustering algorithm based on single-link connectivity.")
@Reference(authors = "R. Sibson", //
    title = "SLINK: An optimally efficient algorithm for the single-link cluster method", //
    booktitle = "The Computer Journal 16 (1)", //
    url = "https://doi.org/10.1093/comjnl/16.1.30", //
    bibkey = "DBLP:journals/cj/Sibson73")
@Alias({ "single-link", "single-linkage" })
@Priority(Priority.RECOMMENDED)
public class SLINK<O> implements HierarchicalClusteringAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SLINK.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  public SLINK(Distance<? super O> distance) {
    super();
    this.distance = distance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Performs the SLINK algorithm on the given database.
   *
   * @param relation Data relation to use
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    final Logging log = getLogger(); // To allow CLINK logger override
    DBIDs ids = relation.getDBIDs();
    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    // Temporary storage for m.
    WritableDoubleDataStore m = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

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
    if(distance instanceof PrimitiveDistance) {
      PrimitiveDistance<? super O> distf = (PrimitiveDistance<? super O>) distance;
      for(id.seek(1); id.valid(); id.advance()) {
        step2primitive(id, it, id.getOffset(), relation, distf, m);
        process(id, aids, it, id.getOffset(), pi, lambda, m); // SLINK or CLINK
        log.incrementProcessed(progress);
      }
    }
    else {
      // Fallback branch
      DistanceQuery<O> distQ = new QueryBuilder<>(relation, distance).distanceQuery();
      for(id.seek(1); id.valid(); id.advance()) {
        step2(id, it, id.getOffset(), distQ, m);
        process(id, aids, it, id.getOffset(), pi, lambda, m); // SLINK or CLINK
        log.incrementProcessed(progress);
      }
    }

    log.ensureCompleted(progress);
    m.destroy();
    return convertOutput(new ClusterMergeHistoryBuilder(aids, distance.isSquared()), aids, pi, lambda).complete();
  }

  /**
   * Convert a SLINK pointer representation to a cluster merge history.
   *
   * @param builder Builder
   * @param oids original DBIDs
   * @param pi Parent pointer
   * @param lambda Parent distance
   * @return Builder
   */
  protected static ClusterMergeHistoryBuilder convertOutput(ClusterMergeHistoryBuilder builder, ArrayDBIDs oids, DBIDDataStore pi, DoubleDataStore lambda) {
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(oids);
    ids.sort(new DataStoreUtil.AscendingByDoubleDataStoreAndId(lambda));
    DBIDVar p = DBIDUtil.newVar();
    if(oids instanceof DBIDRange) { // simple case, can use offsets
      DBIDRange range = (DBIDRange) oids;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        double d = lambda.doubleValue(it);
        if(!DBIDUtil.equal(it, pi.assignVar(it, p))) {
          builder.add(range.getOffset(it), d, range.getOffset(p));
        }
      }
    }
    else { // need an index to map back to integer offsets
      WritableIntegerDataStore idx = DataStoreFactory.FACTORY.makeIntegerStorage(oids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      for(DBIDArrayIter it = oids.iter(); it.valid(); it.advance()) {
        idx.putInt(it, it.getOffset());
      }
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        double d = lambda.doubleValue(it);
        if(!DBIDUtil.equal(it, pi.assignVar(it, p))) {
          builder.add(idx.intValue(it), d, idx.intValue(p));
        }
      }
      idx.destroy();
    }
    return builder;
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
   * @param distance Distance function to use
   */
  private void step2primitive(DBIDRef id, DBIDArrayIter it, int n, Relation<? extends O> relation, PrimitiveDistance<? super O> distance, WritableDoubleDataStore m) {
    O newObj = relation.get(id);
    for(it.seek(0); it.getOffset() < n; it.advance()) {
      // M(i) = dist(i, n+1)
      m.putDouble(it, distance.distance(relation.get(it), newObj));
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

  /**
   * Get the (static) class logger.
   */
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
    }

    @Override
    public SLINK<O> make() {
      return new SLINK<>(distance);
    }
  }
}
