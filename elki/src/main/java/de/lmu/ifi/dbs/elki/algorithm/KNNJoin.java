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
package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.MissingPrerequisitesException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Joins in a given spatial database to each object its k-nearest neighbors.
 * This algorithm only supports spatial databases based on a spatial index
 * structure.
 * <p>
 * Since this method compares the MBR of every single leaf with every other
 * leaf, it is essentially quadratic in the number of leaves, which may not be
 * appropriate for large trees. It does currently not yet use the tree structure
 * for pruning.
 * <p>
 * TODO: exploit the tree structure.
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.1
 *
 * @composed - - - Task
 *
 * @param <V> the type of FeatureVector handled by this Algorithm
 * @param <N> the type of node used in the spatial index structure
 * @param <E> the type of entry used in the spatial node
 */
@Title("K-Nearest Neighbor Join")
@Description("Algorithm to find the k-nearest neighbors of each object in a spatial database")
@Priority(Priority.DEFAULT - 10) // Mostly used inside others.
public class KNNJoin<V extends NumberVector, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDistanceBasedAlgorithm<V, Relation<KNNList>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNJoin.class);

  /**
   * The k parameter.
   */
  int k;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k k parameter
   */
  public KNNJoin(DistanceFunction<? super V> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Joins in the given spatial database to each object its k-nearest neighbors.
   *
   * @param relation Relation to process
   * @return result
   */
  public Relation<KNNList> run(Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDataStore<KNNList> knnLists = run(relation, ids);
    // Wrap as relation:
    return new MaterializedRelation<>("k nearest neighbors", "kNNs", TypeUtil.KNNLIST, knnLists, ids);
  }

  /**
   * Inner run method. This returns a double store, and is used by
   * {@link de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNJoinMaterializeKNNPreprocessor}
   *
   * @param relation Data relation
   * @param ids Object IDs
   * @return Data store
   */
  public WritableDataStore<KNNList> run(Relation<V> relation, DBIDs ids) {
    if(!(getDistanceFunction() instanceof SpatialPrimitiveDistanceFunction)) {
      throw new IllegalStateException("Distance Function must be an instance of " + SpatialPrimitiveDistanceFunction.class.getName());
    }
    Collection<SpatialIndexTree<N, E>> indexes = ResultUtil.filterResults(relation.getHierarchy(), relation, SpatialIndexTree.class);
    if(indexes.size() != 1) {
      throw new MissingPrerequisitesException("KNNJoin found " + indexes.size() + " spatial indexes, expected exactly one.");
    }
    SpatialIndexTree<N, E> index = indexes.iterator().next();
    return run(index, ids);
  }

  /**
   * Inner run method. This returns a double store, and is used by
   * {@link de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNJoinMaterializeKNNPreprocessor}
   *
   * @param index Index to process
   * @param ids Object IDs
   * @return Data store
   */
  public WritableDataStore<KNNList> run(SpatialIndexTree<N, E> index, DBIDs ids) {
    @SuppressWarnings("unchecked")
    SpatialPrimitiveDistanceFunction<V> distFunction = (SpatialPrimitiveDistanceFunction<V>) getDistanceFunction();

    // data pages
    List<E> ps_candidates = new ArrayList<>(index.getLeaves());
    // knn heaps
    List<List<KNNHeap>> heaps = new ArrayList<>(ps_candidates.size());

    // Initialize with the page self-pairing
    for(int i = 0; i < ps_candidates.size(); i++) {
      heaps.add(initHeaps(distFunction, index.getNode(ps_candidates.get(i))));
    }

    // Build priority queue
    final int sqsize = ps_candidates.size() * (ps_candidates.size() - 1) >>> 1;
    ComparableMinHeap<Task> pq = new ComparableMinHeap<>(sqsize);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Number of leaves: " + ps_candidates.size() + " so " + sqsize + " MBR computations.");
    }
    FiniteProgress mprogress = LOG.isVerbose() ? new FiniteProgress("Comparing leaf MBRs", sqsize, LOG) : null;
    for(int i = 0; i < ps_candidates.size(); i++) {
      E pr_entry = ps_candidates.get(i);
      N pr = index.getNode(pr_entry);
      List<KNNHeap> pr_heaps = heaps.get(i);
      double pr_knn_distance = computeStopDistance(pr_heaps);

      for(int j = i + 1; j < ps_candidates.size(); j++) {
        E ps_entry = ps_candidates.get(j);
        N ps = index.getNode(ps_entry);
        List<KNNHeap> ps_heaps = heaps.get(j);
        double ps_knn_distance = computeStopDistance(ps_heaps);
        double minDist = distFunction.minDist(pr_entry, ps_entry);
        // Resolve immediately:
        if(minDist <= 0.) {
          processDataPages(distFunction, pr_heaps, ps_heaps, pr, ps);
        }
        else if(minDist <= pr_knn_distance || minDist <= ps_knn_distance) {
          pq.add(new Task(minDist, i, j));
        }
        LOG.incrementProcessed(mprogress);
      }
    }
    LOG.ensureCompleted(mprogress);

    // Process the queue
    FiniteProgress qprogress = LOG.isVerbose() ? new FiniteProgress("Processing queue", pq.size(), LOG) : null;
    IndefiniteProgress fprogress = LOG.isVerbose() ? new IndefiniteProgress("Full comparisons", LOG) : null;
    while(!pq.isEmpty()) {
      Task task = pq.poll();
      List<KNNHeap> pr_heaps = heaps.get(task.i);
      List<KNNHeap> ps_heaps = heaps.get(task.j);
      double pr_knn_distance = computeStopDistance(pr_heaps);
      double ps_knn_distance = computeStopDistance(ps_heaps);
      boolean dor = task.mindist <= pr_knn_distance;
      boolean dos = task.mindist <= ps_knn_distance;
      if(dor || dos) {
        N pr = index.getNode(ps_candidates.get(task.i));
        N ps = index.getNode(ps_candidates.get(task.j));
        if(dor && dos) {
          processDataPages(distFunction, pr_heaps, ps_heaps, pr, ps);
        }
        else if(dor) {
          processDataPages(distFunction, pr_heaps, null, pr, ps);
        }
        else /* if(dos) */ {
          processDataPages(distFunction, ps_heaps, null, ps, pr);
        }
        LOG.incrementProcessed(fprogress);
      }
      LOG.incrementProcessed(qprogress);
    }
    LOG.ensureCompleted(qprogress);
    LOG.setCompleted(fprogress);

    WritableDataStore<KNNList> knnLists = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, KNNList.class);
    FiniteProgress pageprog = LOG.isVerbose() ? new FiniteProgress("Number of processed data pages", ps_candidates.size(), LOG) : null;
    for(int i = 0; i < ps_candidates.size(); i++) {
      N pr = index.getNode(ps_candidates.get(i));
      List<KNNHeap> pr_heaps = heaps.get(i);

      // Finalize lists
      for(int j = 0; j < pr.getNumEntries(); j++) {
        knnLists.put(((LeafEntry) pr.getEntry(j)).getDBID(), pr_heaps.get(j).toKNNList());
      }
      // Forget heaps and pq
      heaps.set(i, null);
      LOG.incrementProcessed(pageprog);
    }
    LOG.ensureCompleted(pageprog);
    return knnLists;
  }

  /**
   * Initialize the heaps.
   *
   * @param distFunction Distance function
   * @param pr Node to initialize for
   * @return List of heaps
   */
  private List<KNNHeap> initHeaps(SpatialPrimitiveDistanceFunction<V> distFunction, N pr) {
    List<KNNHeap> pr_heaps = new ArrayList<>(pr.getNumEntries());
    // Create for each data object a knn heap
    for(int j = 0; j < pr.getNumEntries(); j++) {
      pr_heaps.add(DBIDUtil.newHeap(k));
    }
    // Self-join first, as this is expected to improve most and cannot be
    // pruned.
    processDataPages(distFunction, pr_heaps, null, pr, pr);
    return pr_heaps;
  }

  /**
   * Processes the two data pages pr and ps and determines the k-nearest
   * neighbors of pr in ps.
   *
   * @param df the distance function to use
   * @param pr the first data page
   * @param ps the second data page
   * @param pr_heaps the knn lists for each data object
   * @param ps_heaps the knn lists for each data object in ps
   */
  private void processDataPages(SpatialPrimitiveDistanceFunction<? super V> df, List<KNNHeap> pr_heaps, List<KNNHeap> ps_heaps, N pr, N ps) {
    // Compare pairwise
    for(int j = 0; j < ps.getNumEntries(); j++) {
      final SpatialPointLeafEntry s_e = (SpatialPointLeafEntry) ps.getEntry(j);
      final KNNHeap hj = ps_heaps != null ? ps_heaps.get(j) : null;
      final DBID s_id = s_e.getDBID();
      for(int i = 0; i < pr.getNumEntries(); i++) {
        final SpatialPointLeafEntry r_e = (SpatialPointLeafEntry) pr.getEntry(i);
        double distance = df.minDist(s_e, r_e);
        pr_heaps.get(i).insert(distance, s_id);
        if(hj != null) {
          hj.insert(distance, r_e.getDBID());
        }
      }
    }
  }

  /**
   * Compute the maximum stop distance.
   *
   * @param heaps Heaps list
   * @return the k-nearest neighbor distance of pr in ps
   */
  private double computeStopDistance(List<KNNHeap> heaps) {
    // Update pruning distance
    double pr_knn_distance = Double.NaN;
    for(KNNHeap knnList : heaps) {
      // set kNN distance of r
      double kdist = knnList.getKNNDistance();
      pr_knn_distance = (kdist < pr_knn_distance) ? pr_knn_distance : kdist;
    }
    if(pr_knn_distance != pr_knn_distance) {
      return Double.POSITIVE_INFINITY;
    }
    return pr_knn_distance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Task in the processing queue.
   *
   * @author Erich Schubert
   */
  private class Task implements Comparable<Task> {
    /**
     * Minimum distance.
     */
    final double mindist;

    /**
     * First offset.
     */
    final int i;

    /**
     * Second offset.
     */
    final int j;

    /**
     * Constructor.
     *
     * @param mindist Minimum distance
     * @param i First offset
     * @param j Second offset
     */
    public Task(double mindist, int i, int j) {
      super();
      this.mindist = mindist;
      this.i = i;
      this.j = j;
    }

    @Override
    public int compareTo(Task o) {
      return Double.compare(mindist, o.mindist);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * Parameter that specifies the k-nearest neighbors to be assigned, must be
     * an integer greater than 0. Default value: 1.
     */
    public static final OptionID K_ID = new OptionID("knnjoin.k", "Specifies the k-nearest neighbors to be assigned.");

    /**
     * K parameter.
     */
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNJoin<V, N, E> makeInstance() {
      return new KNNJoin<>(distanceFunction, k);
    }
  }
}
