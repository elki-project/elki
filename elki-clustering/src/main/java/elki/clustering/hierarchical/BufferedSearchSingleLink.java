/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.LinearScanEuclideanPrioritySearcher;
import elki.database.query.distance.LinearScanPrioritySearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Index accelerated single-link clustering algorithm with a heap-of-candidates
 * strategy, where for each point we store a buffer of candidates in a heap.
 * This did not work much better than the restarting search approach,
 * unfortunately, so it was not included in the publication.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert:<br>
 * Hierarchical Clustering Without Pairwise Distances by Incremental Similarity
 * Search<br>
 * Int. Conf. on Similarity Search and Applications (SISAP 2024)
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Reference(authors = "Erich Schubert", //
    title = "Hierarchical Clustering Without Pairwise Distances by Incremental Similarity Search", //
    booktitle = "Int. Conf. on Similarity Search and Applications (SISAP 2024)", //
    bibkey = "DBLP:conf/sisap/Schubert24", url = "https://doi.org/10.1007/978-3-031-75823-2_20")
public class BufferedSearchSingleLink<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BufferedSearchSingleLink.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  public BufferedSearchSingleLink(Distance<? super O> distance) {
    super();
    this.distance = distance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the clustering algorithm.
   * 
   * @param relation Input data
   * @return Cluster merge history
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    DBIDEnum ids = DBIDUtil.ensureEnum(relation.getDBIDs());
    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, distance.isSquared());
    // Create one for testing we have a suitable index.
    // TODO: enforce a well-tuned VP-tree?
    PrioritySearcher<DBIDRef> pq = new QueryBuilder<>(relation, distance).priorityByDBID();
    if(pq instanceof LinearScanPrioritySearcher || pq instanceof LinearScanEuclideanPrioritySearcher) {
      throw new UnsupportedOperationException("No index acceleration available. This will be very slow.");
    }
    new Instance(ids, builder, pq).run();
    assert builder.mergecount == ids.size() - 1;
    builder.optimizeOrder();
    return builder.complete();
  }

  /**
   * Instance for a single run.
   * 
   * @author Erich Schubert
   */
  protected class Instance {
    /**
     * Object IDs
     */
    protected DBIDEnum ids;

    /**
     * Temporary iterator for mapping
     */
    protected DBIDArrayIter ita;

    /**
     * Cluster merge helper
     */
    protected ClusterMergeHistoryBuilder builder;

    /**
     * Priority search
     */
    protected PrioritySearcher<DBIDRef> pq;

    /**
     * Primary heap.
     */
    private DoubleIntegerMinHeap heap;

    /**
     * Nearest (known) neighbors
     */
    private DoubleIntegerMinHeap[] heaps;

    /**
     * Threshold for complete results
     */
    private double[] threshold;

    /**
     * Constructor for a single run.
     *
     * @param ids IDs
     * @param builder Merge helper
     * @param pq Priority searcher
     */
    public Instance(DBIDEnum ids, ClusterMergeHistoryBuilder builder, PrioritySearcher<DBIDRef> pq) {
      this.ids = ids;
      this.builder = builder;
      this.pq = pq;
      this.ita = ids.iter();
    }

    /**
     * Run the main algorithm
     */
    public void run() {
      initializeHeap();
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Clustering", ids.size() - 1, LOG) : null;
      if(cprog != null) {
        cprog.setProcessed(builder.mergecount, LOG);
      }
      while(true) {
        final double curd = heap.peekKey();
        final int a = heap.peekValue();
        DoubleIntegerMinHeap nn = heaps[a];
        final int b = nn.peekValue();
        int ca = builder.get(a), cb = builder.get(b);
        if(ca != cb) {
          ca = builder.add(ca, curd, cb);
          LOG.incrementProcessed(cprog);
          if(builder.mergecount == ids.size() - 1) {
            break;
          }
        }
        nn.poll();
        // need refill?
        if(nn.isEmpty() || nn.peekKey() > threshold[a]) {
          refillNeighbors(a, ca);
        }
        if(nn.isEmpty()) {
          heap.poll();
        }
        else {
          heap.replaceTopElement(nn.peekKey(), a);
        }
      }
      LOG.ensureCompleted(cprog);
      assert builder.mergecount == ids.size() - 1;
    }

    /**
     * Build the initial heap.
     */
    private void initializeHeap() {
      FiniteProgress iprog = LOG.isVerbose() ? new FiniteProgress("Heap initialization", ids.size(), LOG) : null;
      this.heap = new DoubleIntegerMinHeap(ids.size());
      this.heaps = new DoubleIntegerMinHeap[ids.size()];
      this.threshold = new double[ids.size()];
      outer: for(ita.seek(0); ita.valid(); ita.advance(), LOG.incrementProcessed(iprog)) {
        int a = ita.getOffset(), ca = builder.get(a);
        if(builder.getSize(ca) > 1) {
          continue; // duplicate
        }
        DoubleIntegerMinHeap h = heaps[a] = new DoubleIntegerMinHeap((int) Math.sqrt(ids.size()));
        double t = Double.POSITIVE_INFINITY;
        for(pq.search(ita); pq.valid() && pq.allLowerBound() < t; pq.advance()) {
          final int b = ids.index(pq);
          if(a == b) {
            continue;
          }
          final double d = pq.computeExactDistance();
          if(d == 0.) {
            // duplicate, merge immediately
            int cb = builder.get(b);
            assert ca != cb;
            ca = builder.add(ca, 0, cb);
            continue outer;
          }
          h.add(d, b);
          t = h.peekKey();
        }
        if(!h.isEmpty()) {
          heap.add(t, a);
          threshold[a] = pq.allLowerBound();
        }
      }
      LOG.ensureCompleted(iprog);
      if(LOG.isDebugging()) {
        LOG.debug("Performed " + builder.mergecount + " merges of duplicates (may involve more object) during initialization.");
      }
    }

    /**
     * Refill the nearest neighbors.
     * 
     * @param a Query object number
     * @param ca Cluster id of the query object
     */
    private void refillNeighbors(int a, int ca) {
      DoubleIntegerMinHeap h = heaps[a];
      double t = h.isEmpty() ? Double.POSITIVE_INFINITY : h.peekKey();
      for(pq.search(ita.seek(a)).increaseSkip(threshold[a]); pq.valid() && pq.allLowerBound() < t; pq.advance()) {
        final int b = ids.index(pq);
        if(a == b || builder.get(b) == ca) {
          continue;
        }
        h.add(pq.computeExactDistance(), b);
        t = h.peekKey();
      }
      threshold[a] = pq.allLowerBound();
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
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
    public BufferedSearchSingleLink<O> make() {
      return new BufferedSearchSingleLink<>(distance);
    }
  }
}
