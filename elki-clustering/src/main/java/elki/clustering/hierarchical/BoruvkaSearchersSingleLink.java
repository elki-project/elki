/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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

import java.util.ArrayList;
import java.util.Arrays;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDEnum;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.LinearScanEuclideanPrioritySearcher;
import elki.database.query.distance.LinearScanPrioritySearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.datastructures.arrays.ArrayUtil;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;
import elki.utilities.datastructures.unionfind.WeightedQuickUnionInteger;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Index accelerated Heap-of-Searchers-Single-Link (HSSL) clustering algorithm.
 * This is more memory intensive than the restarting search approach, but will
 * need fewer distance computations.
 * <p>
 * Reference:
 * <p>
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public class BoruvkaSearchersSingleLink<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BoruvkaSearchersSingleLink.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  public BoruvkaSearchersSingleLink(Distance<? super O> distance) {
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
    PrioritySearcher<DBIDRef> pq = new QueryBuilder<>(relation, distance) //
        .lowSelectivity().priorityByDBID();
    if(pq instanceof LinearScanPrioritySearcher || pq instanceof LinearScanEuclideanPrioritySearcher) {
      throw new UnsupportedOperationException("No index acceleration available. This will be very slow.");
    }
    new Instance(ids, builder).run(relation);
    assert builder.mergecount == ids.size() - 1;
    builder.optimizeOrder();
    return builder.complete();
  }

  /**
   * Edges found in Boruvka step.
   * 
   * @author Erich Schubert
   */
  protected static class Edge implements Comparable<Edge> {
    /**
     * First node
     */
    int a;

    /**
     * Other node
     */
    int b;

    /**
     * Distance
     */
    double d;

    /**
     * Constructor.
     *
     * @param a first node
     * @param b second node
     * @param d distance
     */
    public Edge(int a, int b, double d) {
      this.a = a;
      this.b = b;
      this.d = d;
    }

    @Override
    public int compareTo(Edge o) {
      return Double.compare(d, o.d);
    }
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
     * Cluster merge helper
     */
    protected ClusterMergeHistoryBuilder builder;

    /**
     * Union-Find used during MST phase
     */
    protected WeightedQuickUnionInteger uf;

    /**
     * Priority searchers.
     */
    protected PrioritySearcher<DBIDRef>[] pqs;

    /**
     * Auxiliary heaps.
     */
    private DoubleIntegerMinHeap[] heaps;

    /**
     * Constructor for a single run.
     *
     * @param ids IDs
     * @param builder Merge helper
     */
    public Instance(DBIDEnum ids, ClusterMergeHistoryBuilder builder) {
      this.ids = ids;
      this.builder = builder;
      this.uf = new WeightedQuickUnionInteger().fullInit(ids.size());
    }

    /**
     * Run the main algorithm
     */
    public void run(Relation<? extends O> relation) {
      final int size = ids.size();
      initializeHeap(relation);
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Finding the minimum spanning tree", size, LOG) : null;
      if(cprog != null) {
        cprog.setProcessed(builder.mergecount + 1, LOG);
      }
      // Boruvka-like step to find the MST edges
      final int maxmerges = size - builder.mergecount - 1;
      ArrayList<Edge> edges = new ArrayList<>(maxmerges);
      int[] best = new int[size];
      double[] bestd = new double[size];
      outer: while(true) {
        final int l = findMerges(best, bestd);
        assert l > 0;
        // Perform best merge of each cluster:
        for(int i = 0; i < l; i++) {
          int a = best[i];
          double curd = bestd[i];
          DoubleIntegerMinHeap nn = heaps[a];
          assert curd == nn.peekKey();
          final int b = nn.peekValue();
          nn.poll();
          int ca = uf.find(a), cb = uf.find(b);
          if(ca != cb) {
            uf.union(a, b);
            edges.add(new Edge(a, b, curd));
            LOG.incrementProcessed(cprog);
            if(edges.size() == maxmerges) {
              break outer;
            }
          }
        }
        pollSearchers();
      }
      LOG.ensureCompleted(cprog);
      // Add remaining edges to the dendrogram
      edgesToBuilder(edges, Double.POSITIVE_INFINITY);
    }

    /**
     * Build the initial heap.
     * 
     * @param relation data relation
     */
    private void initializeHeap(Relation<? extends O> relation) {
      FiniteProgress iprog = LOG.isVerbose() ? new FiniteProgress("Heap initialization", ids.size(), LOG) : null;
      @SuppressWarnings("unchecked")
      PrioritySearcher<DBIDRef>[] pqs = (PrioritySearcher<DBIDRef>[]) new PrioritySearcher<?>[ids.size()];
      this.pqs = pqs;
      this.heaps = new DoubleIntegerMinHeap[ids.size()];
      for(DBIDArrayIter ita = ids.iter(); ita.valid(); ita.advance(), LOG.incrementProcessed(iprog)) {
        int a = ita.getOffset(), ca = builder.get(a);
        if(builder.getSize(ca) > 1) {
          continue; // duplicate
        }
        DoubleIntegerMinHeap h = heaps[a] = new DoubleIntegerMinHeap();
        PrioritySearcher<DBIDRef> pq = this.pqs[a] = new QueryBuilder<>(relation, distance).priorityByDBID();
        // Initial search to get an initial priority
        double thres = Double.POSITIVE_INFINITY;
        for(pq.search(DBIDUtil.deref(ita)); pq.valid() && pq.allLowerBound() < thres; pq.advance()) {
          final int b = ids.index(pq);
          if(a == b) {
            continue;
          }
          final double d = pq.computeExactDistance();
          if(d == 0.) { // duplicate, merge immediately
            uf.union(a, b); // update both union-find and builder
            int cb = builder.get(b);
            if(ca != cb) {
              ca = builder.add(ca, 0, cb);
            }
            continue;
          }
          h.add(d, b);
          thres = h.peekKey();
          // do not use pq.decreaseCutoff, as we continue later
        }
      }
      LOG.ensureCompleted(iprog);
      if(LOG.isDebugging()) {
        LOG.debug("Performed " + builder.mergecount + " merges of duplicates (may involve more objects) during initialization.");
      }
    }

    private int findMerges(int[] best, double[] bestd) {
      // Determine the best merge for each cluster:
      Arrays.fill(bestd, Double.NaN);
      double min = Double.POSITIVE_INFINITY;
      for(int a = 0; a < best.length; a++) {
        int ca = uf.find(a);
        DoubleIntegerMinHeap nn = heaps[a];
        if(nn != null && !nn.isEmpty()) {
          double d = nn.peekKey();
          // Note: bestd may be NaN
          if(!(d >= bestd[ca])) {
            bestd[ca] = d;
            best[ca] = a;
            min = d < min ? d : min;
          }
        }
      }
      return sortWithNaNs(bestd, best);
    }

    /**
     * Sort the list, but put all NaNs to the end.
     *
     * @param bestd Distances
     * @param best Keys
     * @return Number of valid entries
     */
    private int sortWithNaNs(double[] bestd, int[] best) {
      int i = 0, l = bestd.length - 1;
      // Pivot all NaNs to the end
      while(i < l) {
        while(i < l && Double.isNaN(bestd[l])) {
          l--;
        }
        while(i < l && !Double.isNaN(bestd[i])) {
          i++;
        }
        if(i < l) {
          ArrayUtil.swap(bestd, i, l);
          ArrayUtil.swap(best, i, l);
          i++;
          l--;
        }
      }
      DoubleIntegerArrayQuickSort.sort(bestd, best, i);
      return i;
    }

    /**
     * Transfer edges to the builder.
     *
     * @param edges Edges list
     * @param threshold Stopping threshold
     */
    private void edgesToBuilder(ArrayList<Edge> edges, double threshold) {
      edges.sort(null);
      int trim = 0;
      while(trim < edges.size()) {
        Edge e = edges.get(trim);
        if(e.d > threshold) {
          break;
        }
        builder.add(e.a, e.d, e.b);
        trim++;
      }
      edges.subList(0, trim).clear();
    }

    /**
     * Refill all searchers.
     */
    private void pollSearchers() {
      // Refill all searchers
      for(int a = 0; a < heaps.length; a++) {
        DoubleIntegerMinHeap nn = heaps[a];
        if(nn == null) {
          continue;
        }
        if(!nn.isEmpty()) {
          int ca = uf.find(a);
          // Poll any known neighbor that is already merged now.
          while(!nn.isEmpty() && ca == uf.find(nn.peekValue())) {
            nn.poll();
          }
        }
        if(nn.isEmpty() || nn.peekKey() > pqs[a].allLowerBound()) {
          refillNeighbors(a, uf.find(a));
        }
      }
    }

    /**
     * Refill the nearest neighbors.
     * 
     * @param a Query object number
     * @param ca Cluster id of the query object
     */
    private void refillNeighbors(int a, int ca) {
      PrioritySearcher<DBIDRef> pq = pqs[a];
      DoubleIntegerMinHeap h = heaps[a];
      double thres = h.isEmpty() ? Double.POSITIVE_INFINITY : h.peekKey();
      for(; pq.valid() && pq.allLowerBound() < thres; pq.advance()) {
        final int b = ids.index(pq);
        if(a == b || uf.find(b) == ca) {
          continue;
        }
        h.add(pq.computeExactDistance(), b);
        thres = h.peekKey();
        // do not use pq.decreaseCutoff, as we continue with the searcher
      }
      if(h.isEmpty()) {
        heaps[a] = null;
        pqs[a] = null;
      }
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
    public BoruvkaSearchersSingleLink<O> make() {
      return new BoruvkaSearchersSingleLink<>(distance);
    }
  }
}
