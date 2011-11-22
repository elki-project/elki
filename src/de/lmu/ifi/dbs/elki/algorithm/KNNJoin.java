package de.lmu.ifi.dbs.elki.algorithm;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNList;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Joins in a given spatial database to each object its k-nearest neighbors.
 * This algorithm only supports spatial databases based on a spatial index
 * structure.
 * 
 * Since this method compares the MBR of every single leaf with every other
 * leaf, it is essentially quadratic in the number of leaves, which may not be
 * appropriate for large trees.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @param <V> the type of FeatureVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <N> the type of node used in the spatial index structure
 * @param <E> the type of entry used in the spatial node
 */
@Title("K-Nearest Neighbor Join")
@Description("Algorithm to find the k-nearest neighbors of each object in a spatial database")
public class KNNJoin<V extends NumberVector<V, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDistanceBasedAlgorithm<V, D, DataStore<KNNList<D>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNJoin.class);

  /**
   * Parameter that specifies the k-nearest neighbors to be assigned, must be an
   * integer greater than 0. Default value: 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnjoin.k", "Specifies the k-nearest neighbors to be assigned.");

  /**
   * The k parameter
   */
  int k;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k k parameter
   */
  public KNNJoin(DistanceFunction<? super V, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Joins in the given spatial database to each object its k-nearest neighbors.
   * 
   * @throws IllegalStateException if not suitable {@link SpatialIndexTree} was
   *         found or the specified distance function is not an instance of
   *         {@link SpatialPrimitiveDistanceFunction}.
   */
  @SuppressWarnings("unchecked")
  public WritableDataStore<KNNList<D>> run(Database database, Relation<V> relation) throws IllegalStateException {
    if(!(getDistanceFunction() instanceof SpatialPrimitiveDistanceFunction)) {
      throw new IllegalStateException("Distance Function must be an instance of " + SpatialPrimitiveDistanceFunction.class.getName());
    }
    Collection<SpatialIndexTree<N, E>> indexes = ResultUtil.filterResults(database, SpatialIndexTree.class);
    if(indexes.size() != 1) {
      throw new AbortException("KNNJoin found " + indexes.size() + " spatial indexes, expected exactly one.");
    }
    // FIXME: Ensure were looking at the right relation!
    SpatialIndexTree<N, E> index = indexes.iterator().next();
    SpatialPrimitiveDistanceFunction<V, D> distFunction = (SpatialPrimitiveDistanceFunction<V, D>) getDistanceFunction();
    DBIDs ids = relation.getDBIDs();

    // Optimize for double?
    final boolean doubleOptimize = (getDistanceFunction() instanceof SpatialPrimitiveDoubleDistanceFunction);

    // data pages
    List<E> ps_candidates = new ArrayList<E>(index.getLeaves());
    // knn heaps
    List<List<KNNHeap<D>>> heaps = new ArrayList<List<KNNHeap<D>>>(ps_candidates.size());
    Heap<Task> pq = new Heap<Task>(ps_candidates.size() * ps_candidates.size() / 10);

    // Initialize with the page self-pairing
    for(int i = 0; i < ps_candidates.size(); i++) {
      E pr_entry = ps_candidates.get(i);
      N pr = index.getNode(pr_entry);
      heaps.add(initHeaps(distFunction, doubleOptimize, pr));
    }

    // Build priority queue
    final int sqsize = ps_candidates.size() * (ps_candidates.size() - 1) / 2;
    if(logger.isDebuggingFine()) {
      logger.debugFine("Number of leaves: " + ps_candidates.size() + " so " + sqsize + " MBR computations.");
    }
    FiniteProgress mprogress = logger.isVerbose() ? new FiniteProgress("Comparing leaf MBRs", sqsize, logger) : null;
    for(int i = 0; i < ps_candidates.size(); i++) {
      E pr_entry = ps_candidates.get(i);
      List<KNNHeap<D>> pr_heaps = heaps.get(i);
      D pr_knn_distance = computeStopDistance(pr_heaps);

      for(int j = i + 1; j < ps_candidates.size(); j++) {
        E ps_entry = ps_candidates.get(j);
        List<KNNHeap<D>> ps_heaps = heaps.get(j);
        D ps_knn_distance = computeStopDistance(ps_heaps);
        D minDist = distFunction.minDist(pr_entry, ps_entry);
        // Resolve immediately:
        if(minDist.isNullDistance()) {
          N pr = index.getNode(ps_candidates.get(i));
          N ps = index.getNode(ps_candidates.get(j));
          processDataPagesOptimize(distFunction, doubleOptimize, pr_heaps, ps_heaps, pr, ps);
        }
        else if(minDist.compareTo(pr_knn_distance) <= 0 || minDist.compareTo(ps_knn_distance) <= 0) {
          pq.add(new Task(minDist, i, j));
        }
        if(mprogress != null) {
          mprogress.incrementProcessed(logger);
        }
      }
    }
    if(mprogress != null) {
      mprogress.ensureCompleted(logger);
    }

    // Process the queue
    FiniteProgress qprogress = logger.isVerbose() ? new FiniteProgress("Processing queue", pq.size(), logger) : null;
    IndefiniteProgress fprogress = logger.isVerbose() ? new IndefiniteProgress("Full comparisons", logger) : null;
    while(!pq.isEmpty()) {
      Task task = pq.poll();
      List<KNNHeap<D>> pr_heaps = heaps.get(task.i);
      List<KNNHeap<D>> ps_heaps = heaps.get(task.j);
      D pr_knn_distance = computeStopDistance(pr_heaps);
      D ps_knn_distance = computeStopDistance(ps_heaps);
      boolean dor = task.mindist.compareTo(pr_knn_distance) <= 0;
      boolean dos = task.mindist.compareTo(ps_knn_distance) <= 0;
      if(dor || dos) {
        N pr = index.getNode(ps_candidates.get(task.i));
        N ps = index.getNode(ps_candidates.get(task.j));
        if(dor && dos) {
          processDataPagesOptimize(distFunction, doubleOptimize, pr_heaps, ps_heaps, pr, ps);
        }
        if(dor) {
          processDataPagesOptimize(distFunction, doubleOptimize, pr_heaps, null, pr, ps);
        }
        else /* dos */{
          processDataPagesOptimize(distFunction, doubleOptimize, ps_heaps, null, ps, pr);
        }
        if(fprogress != null) {
          fprogress.incrementProcessed(logger);
        }
      }
      if(qprogress != null) {
        qprogress.incrementProcessed(logger);
      }
    }
    if(qprogress != null) {
      qprogress.ensureCompleted(logger);
    }
    if(fprogress != null) {
      fprogress.setCompleted(logger);
    }

    WritableDataStore<KNNList<D>> knnLists = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, KNNList.class);
    // FiniteProgress progress = logger.isVerbose() ? new
    // FiniteProgress(this.getClass().getName(), relation.size(), logger) :
    // null;
    FiniteProgress pageprog = logger.isVerbose() ? new FiniteProgress("Number of processed data pages", ps_candidates.size(), logger) : null;
    // int processed = 0;
    for(int i = 0; i < ps_candidates.size(); i++) {
      N pr = index.getNode(ps_candidates.get(i));
      List<KNNHeap<D>> pr_heaps = heaps.get(i);

      // Finalize lists
      for(int j = 0; j < pr.getNumEntries(); j++) {
        knnLists.put(((LeafEntry) pr.getEntry(j)).getDBID(), pr_heaps.get(j).toKNNList());
      }
      // Forget heaps and pq
      heaps.set(i, null);
      // processed += pr.getNumEntries();

      // if(progress != null) {
      // progress.setProcessed(processed, logger);
      // }
      if(pageprog != null) {
        pageprog.incrementProcessed(logger);
      }
    }
    // if(progress != null) {
    // progress.ensureCompleted(logger);
    // }
    if(pageprog != null) {
      pageprog.ensureCompleted(logger);
    }
    return knnLists;
  }

  private List<KNNHeap<D>> initHeaps(SpatialPrimitiveDistanceFunction<V, D> distFunction, final boolean doubleOptimize, N pr) {
    List<KNNHeap<D>> pr_heaps;
    // Create for each data object a knn heap
    pr_heaps = new ArrayList<KNNHeap<D>>(pr.getNumEntries());
    for(int j = 0; j < pr.getNumEntries(); j++) {
      pr_heaps.add(new KNNHeap<D>(k, distFunction.getDistanceFactory().infiniteDistance()));
    }
    // Self-join first, as this is expected to improve most and cannot be
    // pruned.
    processDataPagesOptimize(distFunction, doubleOptimize, pr_heaps, null, pr, pr);
    return pr_heaps;
  }

  /**
   * Processes the two data pages pr and ps and determines the k-nearest
   * neighbors of pr in ps.
   * 
   * @param distFunction the distance to use
   * @param doubleOptimize Flag whether to optimize for doubles.
   * @param pr the first data page
   * @param ps the second data page
   * @param pr_heaps the knn lists for each data object in pr
   * @param ps_heaps the knn lists for each data object in ps (if ps != pr)
   * @param pr_knn_distance the current knn distance of data page pr
   * @return the k-nearest neighbor distance of pr in ps
   */
  private void processDataPagesOptimize(SpatialPrimitiveDistanceFunction<V, D> distFunction, final boolean doubleOptimize, List<KNNHeap<D>> pr_heaps, List<KNNHeap<D>> ps_heaps, N pr, N ps) {
    if(doubleOptimize) {
      List<?> khp = (List<?>) pr_heaps;
      List<?> khs = (List<?>) ps_heaps;
      processDataPagesDouble((SpatialPrimitiveDoubleDistanceFunction<? super V>) distFunction, pr, ps, (List<KNNHeap<DoubleDistance>>) khp, (List<KNNHeap<DoubleDistance>>) khs);
    }
    else {
      for(int j = 0; j < ps.getNumEntries(); j++) {
        final SpatialPointLeafEntry s_e = (SpatialPointLeafEntry) ps.getEntry(j);
        DBID s_id = s_e.getDBID();
        for(int i = 0; i < pr.getNumEntries(); i++) {
          final SpatialPointLeafEntry r_e = (SpatialPointLeafEntry) pr.getEntry(i);
          D distance = distFunction.minDist(s_e, r_e);
          pr_heaps.get(i).add(distance, s_id);
          if(pr != ps && ps_heaps != null) {
            ps_heaps.get(j).add(distance, r_e.getDBID());
          }
        }
      }
    }
  }

  /**
   * Processes the two data pages pr and ps and determines the k-nearest
   * neighbors of pr in ps.
   * 
   * @param distQ the distance to use
   * @param pr the first data page
   * @param ps the second data page
   * @param pr_heaps the knn lists for each data object
   * @param ps_heaps the knn lists for each data object in ps
   * @param pr_knn_distance the current knn distance of data page pr
   * @return the k-nearest neighbor distance of pr in ps
   */
  private void processDataPagesDouble(SpatialPrimitiveDoubleDistanceFunction<? super V> df, N pr, N ps, List<KNNHeap<DoubleDistance>> pr_heaps, List<KNNHeap<DoubleDistance>> ps_heaps) {
    // Compare pairwise
    for(int j = 0; j < ps.getNumEntries(); j++) {
      final SpatialPointLeafEntry s_e = (SpatialPointLeafEntry) ps.getEntry(j);
      DBID s_id = s_e.getDBID();
      for(int i = 0; i < pr.getNumEntries(); i++) {
        final SpatialPointLeafEntry r_e = (SpatialPointLeafEntry) pr.getEntry(i);
        double distance = df.doubleMinDist(s_e, r_e);
        pr_heaps.get(i).add(new DoubleDistanceResultPair(distance, s_id));
        if(pr != ps && ps_heaps != null) {
          ps_heaps.get(j).add(new DoubleDistanceResultPair(distance, r_e.getDBID()));
        }
      }
    }
  }

  /**
   * Compute the maximum stop distance
   * 
   * @param heaps
   * @return the k-nearest neighbor distance of pr in ps
   */
  private D computeStopDistance(List<KNNHeap<D>> heaps) {
    // Update pruning distance
    D pr_knn_distance = null;
    for(KNNHeap<D> knnList : heaps) {
      // set kNN distance of r
      if(pr_knn_distance == null) {
        pr_knn_distance = knnList.getKNNDistance();
      }
      else {
        pr_knn_distance = DistanceUtil.max(knnList.getKNNDistance(), pr_knn_distance);
      }
    }
    return pr_knn_distance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Task in the processing queue
   * 
   * @author Erich Schubert
   * 
   * @apiviz.hide
   */
  private class Task implements Comparable<Task> {
    final D mindist;

    final int i;

    final int j;

    /**
     * Constructor.
     * 
     * @param mindist
     * @param i
     * @param j
     */
    public Task(D mindist, int i, int j) {
      super();
      this.mindist = mindist;
      this.i = i;
      this.j = j;
    }

    @Override
    public int compareTo(Task o) {
      return mindist.compareTo(o.mindist);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<V, D> {
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, 1);
      kP.addConstraint(new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNJoin<V, D, N, E> makeInstance() {
      return new KNNJoin<V, D, N, E>(distanceFunction, k);
    }
  }
}