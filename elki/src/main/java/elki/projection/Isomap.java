/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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
package elki.projection;

import java.util.Arrays;

import elki.Algorithm;
import elki.data.DoubleVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDEnum;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.LinearScanQuery;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.EigenvalueDecomposition;
import elki.utilities.datastructures.arraylike.DoubleArray;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Isomap projection (kNN graph + shortest paths + classical MDS).
 * <p>
 * Reference:
 * J. B. Tenenbaum, V. de Silva, J. C. Langford<br>
 * A Global Geometric Framework for Nonlinear Dimensionality Reduction<br>
 * Science 290(5500)
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Title("Isomap")
@Reference(authors = "J. B. Tenenbaum, V. de Silva, J. C. Langford", //
    title = "A Global Geometric Framework for Nonlinear Dimensionality Reduction", //
    booktitle = "Science 290(5500)", //
    url = "https://doi.org/10.1126/science.290.5500.2319", //
    bibkey = "doi:10.1126/science.290.5500.2319")
public class Isomap<O> extends AbstractProjectionAlgorithm<Relation<DoubleVector>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Isomap.class);

  /**
   * Distance function.
   */
  protected Distance<? super O> distance;

  /**
   * Embedding dimensionality.
   */
  protected int dim;

  /**
   * Number of neighbors for graph construction.
   */
  protected int neighbors;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param dim Embedding dimensionality
   * @param neighbors k for kNN graph
   * @param keep Keep original relation
   */
  public Isomap(Distance<? super O> distance, int dim, int neighbors, boolean keep) {
    super(keep);
    this.distance = distance;
    this.dim = dim;
    this.neighbors = neighbors;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Relation<DoubleVector> autorun(Database database) {
    return (Relation<DoubleVector>) Utils.autorun(this, database);
  }

  /**
   * Perform Isomap projection.
   *
   * @param relation Input relation
   * @return Projection result
   */
  public Relation<DoubleVector> run(Relation<O> relation) {
    DBIDEnum ids = DBIDUtil.ensureEnum(relation.getDBIDs());
    final int size = ids.size();
    if(dim > size) {
      throw new AbortException("Isomap dimensionality " + dim + " exceeds relation size " + size + ".");
    }
    if(size * (long) size > 0x7FFF_FFFAL) {
      throw new AbortException("Memory exceeds Java array size limit.");
    }

    int[][] adj = new int[size][];
    double[][] wts = new double[size][];
    buildKNNGraph(relation, ids, adj, wts);

    double[][] geod = allPairsShortestPaths(adj, wts);
    double[][] embed = classicalMDS(geod, dim);

    removePreviousRelation(relation);

    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    for(DBIDArrayIter it = ids.iter(); it.valid(); it.advance()) {
      proj.put(it, DoubleVector.wrap(embed[it.getOffset()]));
    }
    return new MaterializedRelation<>("Isomap", otype, ids, proj);
  }

  /**
   * Build a symmetric kNN graph.
   * 
   * @param wts Output weight list, where {@code wts[i]} will contain the
   *        corresponding distances to the neighbors in {@code adj[i]}.
   */
  protected void buildKNNGraph(Relation<O> relation, DBIDEnum ids, int[][] adj, double[][] wts) {
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).kNNByDBID(neighbors + 1);
    if(knnq instanceof LinearScanQuery && (long) neighbors * neighbors < ids.size()) {
      LOG.warning("To accelerate Isomap, please use an index.");
    }
    if(!distance.isSymmetric()) {
      LOG.warning("Isomap expects a symmetric distance function.");
    }
    final boolean square = distance.isSquared();
    IntegerArray[] nbrs = new IntegerArray[ids.size()];
    DoubleArray[] dists = new DoubleArray[ids.size()];
    for(DBIDArrayIter it = ids.iter(); it.valid(); it.advance()) {
      final int i = it.getOffset();
      KNNList knn = knnq.getKNN(it, neighbors + 1);
      for(DoubleDBIDListIter iter = knn.iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iter, it)) {
          continue;
        }
        final int j = ids.index(iter);
        double d = iter.doubleValue();
        if(square) {
          d = Math.sqrt(d);
        }
        if(nbrs[i] == null) {
          nbrs[i] = new IntegerArray(neighbors * 2);
          dists[i] = new DoubleArray(neighbors * 2);
        }
        nbrs[i].add(j);
        dists[i].add(d);
        if(nbrs[j] == null) {
          nbrs[j] = new IntegerArray(neighbors * 2);
          dists[j] = new DoubleArray(neighbors * 2);
        }
        nbrs[j].add(i);
        dists[j].add(d);
      }
    }
    for(int i = 0; i < ids.size(); i++) {
      if(nbrs[i] == null) {
        adj[i] = new int[0];
        wts[i] = new double[0];
      }
      else {
        adj[i] = nbrs[i].toArray();
        wts[i] = dists[i].toArray();
      }
    }
  }

  /**
   * Compute all-pairs shortest paths using repeated Dijkstra runs.
   *
   * @param adj adjacency list of the graph.
   * @param wts weight matrix corresponding to {@code adj}.
   * @return a distance matrix where {@code dist[i][j]} is the shortest path
   *         distance between vertices {@code i} and {@code j}.
   */
  protected static double[][] allPairsShortestPaths(int[][] adj, double[][] wts) {
    final int size = adj.length;
    double[][] dist = new double[size][size];
    for(int i = 0; i < size; i++) {
      dist[i] = dijkstra(i, adj, wts);
    }
    for(int i = 0; i < size; i++) {
      for(int j = 0; j < size; j++) {
        if(!Double.isFinite(dist[i][j])) {
          throw new AbortException("Isomap graph is disconnected; increase k or use a different distance.");
        }
      }
    }
    return dist;
  }

  /**
   * Compute shortest paths from a single source vertex using Dijkstra's
   * algorithm.
   *
   * @param source index of the source vertex.
   * @param adj adjacency list of the graph.
   * @param wts weight matrix corresponding to {@code adj}.
   * @return an array of distances from the source to every other vertex.
   */
  protected static double[] dijkstra(int source, int[][] adj, double[][] wts) {
    final int size = adj.length;
    double[] dist = new double[size];
    Arrays.fill(dist, Double.POSITIVE_INFINITY);
    dist[source] = 0.;
    DoubleIntegerMinHeap heap = new DoubleIntegerMinHeap();
    heap.add(0.0, source);
    while(!heap.isEmpty()) {
      double curDist = heap.peekKey();
      int curIdx = heap.peekValue();
      heap.poll();
      // Skip outdated entries.
      if(curDist != dist[curIdx]) {
        continue;
      }
      final int[] nbr = adj[curIdx];
      final double[] wt = wts[curIdx];
      for(int i = 0; i < nbr.length; i++) {
        final int v = nbr[i];
        final double nd = curDist + wt[i];
        if(nd < dist[v]) {
          dist[v] = nd;
          heap.add(nd, v);
        }
      }
    }
    return dist;
  }

  /**
   * Perform classical multidimensional scaling (MDS) on a distance matrix.
   * The input matrix {@code dist} is overwritten with squared distances and
   * subsequently centered.
   *
   * @param dist distance matrix (will be modified in-place).
   * @param dim target embedding dimensionality.
   * @return embedding coordinates as a {@code dim}-dimensional array.
   */
  protected static double[][] classicalMDS(double[][] dist, int dim) {
    final int n = dist.length;
    double[] rowMean = new double[n];
    double total = 0.;
    for(int i = 0; i < n; i++) {
      for(int j = 0; j < n; j++) {
        final double d = dist[i][j];
        if(!Double.isFinite(d)) {
          throw new AbortException("Isomap encountered invalid distance values.");
        }
        final double d2 = d * d;
        dist[i][j] = d2;
        rowMean[i] += d2;
        total += d2;
      }
      rowMean[i] /= n;
    }
    total /= (n * (double) n);
    for(int i = 0; i < n; i++) {
      for(int j = 0; j < n; j++) {
        dist[i][j] = -0.5 * (dist[i][j] - rowMean[i] - rowMean[j] + total);
      }
    }

    EigenvalueDecomposition eig = new EigenvalueDecomposition(dist);
    double[] eval = eig.getRealEigenvalues();
    double[][] evec = eig.getV();
    Integer[] order = new Integer[n];
    for(int i = 0; i < n; i++) {
      order[i] = i;
    }
    Arrays.sort(order, (a, b) -> Double.compare(eval[b], eval[a]));

    double[][] embed = new double[n][dim];
    int used = 0;
    for(int oi = 0; oi < n && used < dim; oi++) {
      int k = order[oi];
      double ev = eval[k];
      if(ev <= 0.) {
        continue;
      }
      double scale = Math.sqrt(ev);
      for(int i = 0; i < n; i++) {
        embed[i][used] = evec[i][k] * scale;
      }
      used++;
    }
    if(used < dim) {
      throw new AbortException("Isomap found only " + used + " positive eigenvalues; reduce dimensionality.");
    }
    return embed;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Dimensionality parameter.
     */
    public static final OptionID DIM_ID = new OptionID("isomap.dim", "Target dimensionality for Isomap.");

    /**
     * Number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("isomap.k", "Number of neighbors for the kNN graph.");

    /**
     * Dimensionality.
     */
    protected int dim;

    /**
     * Number of neighbors.
     */
    protected int k;

    /**
     * Distance function.
     */
    protected Distance<? super O> distance;

    /**
     * Keep original data.
     */
    protected boolean keep = false;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, SquaredEuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(DIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> dim = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new Flag(KEEP_ID) //
          .grab(config, x -> keep = x);
    }

    @Override
    public Isomap<O> make() {
      return new Isomap<>(distance, dim, k, keep);
    }
  }
}
