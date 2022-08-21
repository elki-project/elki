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
package elki.clustering.optics;

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.ModifiableDBIDs;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.result.Metadata;
import elki.utilities.datastructures.heap.UpdatableHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * The OPTICS algorithm for density-based hierarchical clustering.
 * <p>
 * Algorithm to find density-connected sets in a database based on the
 * parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters
 * determine a density threshold for clustering.
 * <p>
 * This implementation uses a heap.
 * <p>
 * Reference:
 * <p>
 * Mihael Ankerst, Markus M. Breunig, Hans-Peter Kriegel, Jörg Sander<br>
 * OPTICS: Ordering Points to Identify the Clustering Structure<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.1
 *
 * @navassoc - produces - ClusterOrder
 * @has - - - OPTICSHeapEntry
 *
 * @param <O> the type of objects handled by the algorithm
 */
@Title("OPTICS: Density-Based Hierarchical Clustering (implementation using a heap)")
@Reference(authors = "Mihael Ankerst, Markus M. Breunig, Hans-Peter Kriegel, Jörg Sander", //
    title = "OPTICS: Ordering Points to Identify the Clustering Structure", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
    url = "https://doi.org/10.1145/304181.304187", //
    bibkey = "DBLP:conf/sigmod/AnkerstBKS99")
public class OPTICSHeap<O> extends AbstractOPTICS<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSHeap.class);

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public OPTICSHeap(Distance<? super O> distance, double epsilon, int minpts) {
    super(distance, epsilon, minpts);
  }

  @Override
  public ClusterOrder run(Relation<O> relation) {
    return new Instance(relation).run();
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
    private ModifiableDBIDs processedIDs;

    /**
     * Heap of candidates.
     */
    UpdatableHeap<OPTICSHeapEntry> heap;

    /**
     * Output cluster order.
     */
    ClusterOrder clusterOrder;

    /**
     * IDs to process.
     */
    private DBIDs ids;

    /**
     * Progress for logging.
     */
    FiniteProgress progress;

    /**
     * Range query.
     */
    RangeSearcher<DBIDRef> rangeQuery;

    /**
     * Constructor for a single data set.
     *
     * @param relation Data relation
     */
    public Instance(Relation<O> relation) {
      ids = relation.getDBIDs();
      processedIDs = DBIDUtil.newHashSet(ids.size());
      clusterOrder = new ClusterOrder(ids);
      Metadata.of(clusterOrder).setLongName("OPTICS Clusterorder");
      progress = LOG.isVerbose() ? new FiniteProgress("OPTICS", ids.size(), LOG) : null;
      rangeQuery = new QueryBuilder<>(relation, distance).rangeByDBID(epsilon);
      heap = new UpdatableHeap<>();
    }

    /**
     * Process the data set.
     *
     * @return Cluster order result.
     */
    public ClusterOrder run() {
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        if(!processedIDs.contains(iditer)) {
          assert (heap.isEmpty());
          expandClusterOrder(iditer);
        }
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
      heap.add(new OPTICSHeapEntry(DBIDUtil.deref(objectID), null, Double.POSITIVE_INFINITY));

      while(!heap.isEmpty()) {
        final OPTICSHeapEntry current = heap.poll();
        clusterOrder.add(current.objectID, current.reachability, current.predecessorID);
        processedIDs.add(current.objectID);

        rangeQuery.getRange(current.objectID, epsilon, neighbors.clear());
        if(neighbors.size() >= minpts) {
          neighbors.sort();
          final double coreDistance = neighbor.seek(minpts - 1).doubleValue();

          for(neighbor.seek(0); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reachability = MathUtil.max(neighbor.doubleValue(), coreDistance);
            heap.add(new OPTICSHeapEntry(DBIDUtil.deref(neighbor), current.objectID, reachability));
          }
        }
        LOG.incrementProcessed(progress);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractOPTICS.Par<O> {
    @Override
    public OPTICSHeap<O> make() {
      return new OPTICSHeap<>(distance, epsilon, minpts);
    }
  }
}
