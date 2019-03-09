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
package elki.algorithm.clustering.optics;

import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDBIDDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * The OPTICS algorithm for density-based hierarchical clustering.
 * <p>
 * Algorithm to find density-connected sets in a database based on the
 * parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters
 * determine a density threshold for clustering.
 * <p>
 * This version is implemented using a list, always scanning the list for the
 * maximum. While this could be cheaper than the complex heap updates,
 * benchmarks indicate the heap version is usually still preferable.
 * <p>
 * Reference:
 * <p>
 * Mihael Ankerst, Markus M. Breunig, Hans-Peter Kriegel, Jörg Sander<br>
 * OPTICS: Ordering Points to Identify the Clustering Structure<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - produces - ClusterOrder
 *
 * @param <O> the type of objects handled by the algorithm
 */
@Title("OPTICS: Density-Based Hierarchical Clustering (implementation using a list)")
@Reference(authors = "Mihael Ankerst, Markus M. Breunig, Hans-Peter Kriegel, Jörg Sander", //
    title = "OPTICS: Ordering Points to Identify the Clustering Structure", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
    url = "https://doi.org/10.1145/304181.304187", //
    bibkey = "DBLP:conf/sigmod/AnkerstBKS99")
public class OPTICSList<O> extends AbstractOPTICS<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSList.class);

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public OPTICSList(Distance<? super O> distanceFunction, double epsilon, int minpts) {
    super(distanceFunction, epsilon, minpts);
  }

  @Override
  public ClusterOrder run(Database db, Relation<O> relation) {
    return new Instance(db, relation).run();
  }

  /**
   * Instance for processing a single data set.
   *
   * @author Erich Schubert
   */
  private class Instance {
    /**
     * Holds a set of processed ids.
     */
    ModifiableDBIDs processedIDs;

    /**
     * Current list of candidates.
     */
    ArrayModifiableDBIDs candidates;

    /**
     * Predecessor storage.
     */
    WritableDBIDDataStore predecessor;

    /**
     * Reachability storage.
     */
    WritableDoubleDataStore reachability;

    /**
     * Output cluster order.
     */
    ClusterOrder clusterOrder;

    /**
     * IDs to process.
     */
    DBIDs ids;

    /**
     * Progress for logging.
     */
    FiniteProgress progress;

    /**
     * Range query.
     */
    RangeQuery<O> rangeQuery;

    /**
     * Constructor for a single data set.
     *
     * @param db Database
     * @param relation Data relation
     */
    public Instance(Database db, Relation<O> relation) {
      ids = relation.getDBIDs();
      processedIDs = DBIDUtil.newHashSet(ids.size());
      candidates = DBIDUtil.newArray();
      predecessor = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT);
      reachability = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      clusterOrder = new ClusterOrder(ids);
      Metadata.of(clusterOrder).setLongName("OPTICS Clusterorder");
      progress = LOG.isVerbose() ? new FiniteProgress("OPTICS", ids.size(), LOG) : null;
      DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistance());
      rangeQuery = db.getRangeQuery(dq, epsilon);
    }

    /**
     * Process the data set.
     *
     * @return Cluster order result.
     */
    public ClusterOrder run() {
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        if(processedIDs.contains(iditer)) {
          continue;
        }
        expandClusterOrder(iditer);
      }
      LOG.ensureCompleted(progress);
      return clusterOrder;
    }

    /**
     * OPTICS-function expandClusterOrder.
     *
     * @param objectID the currently processed object
     */
    protected void expandClusterOrder(DBIDRef objectID) {
      ModifiableDoubleDBIDList neighbors = DBIDUtil.newDistanceDBIDList();
      DoubleDBIDListIter neighbor = neighbors.iter();
      candidates.add(objectID);
      predecessor.putDBID(objectID, objectID);
      reachability.put(objectID, Double.POSITIVE_INFINITY);

      DBIDArrayMIter it = candidates.iter();
      DBIDVar cur = DBIDUtil.newVar(), prev = DBIDUtil.newVar();
      while(!candidates.isEmpty()) {
        findBest(candidates, it, cur);
        processedIDs.add(cur);
        // Build cluster order entry
        clusterOrder.add(cur, reachability.doubleValue(cur), predecessor.assignVar(cur, prev));
        LOG.incrementProcessed(progress);

        neighbors.clear();
        rangeQuery.getRangeForDBID(cur, epsilon, neighbors);
        if(neighbors.size() >= minpts) {
          neighbors.sort(); // A quick select would be enough, but its cheap.
          final double coreDistance = neighbor.seek(minpts - 1).doubleValue();

          for(neighbor.seek(0); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reach = MathUtil.max(neighbor.doubleValue(), coreDistance);
            double prevreach = reachability.doubleValue(neighbor);
            if(reach < prevreach) {
              reachability.put(neighbor, reach);
              predecessor.putDBID(neighbor, cur);
              if(prevreach == Double.POSITIVE_INFINITY) {
                candidates.add(neighbor);
              }
            }
          }
        }
      }
    }

    /**
     * Find the minimum in the candidates array.
     *
     * @param candidates Candidates set
     * @param it Array iterator
     * @param out Output variable
     */
    public void findBest(ArrayModifiableDBIDs candidates, DBIDArrayMIter it, DBIDVar out) {
      assert (candidates.size() > 0);
      int bestidx = 0;
      double min = reachability.doubleValue(out.set(it.seek(0)));
      for(it.advance(); it.valid(); it.advance()) {
        final double reach = reachability.doubleValue(it);
        if(reach < min || reach == min && DBIDUtil.compare(it, out) < 0) {
          min = reach;
          out.set(it);
          bestidx = it.getOffset();
        }
      }
      it.seek(bestidx).remove();
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
  public static class Parameterizer<O> extends AbstractOPTICS.Parameterizer<O> {
    @Override
    protected OPTICSList<O> makeInstance() {
      return new OPTICSList<>(distanceFunction, epsilon, minpts);
    }
  }
}
