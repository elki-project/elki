package de.lmu.ifi.dbs.elki.algorithm.projection;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * tSNE using Barnes-Hut-Approximation.
 * 
 * For larger data sets, use an index to make finding the nearest neighbors
 * faster.
 * 
 * Reference:
 * <p>
 * L. van der Maaten<br />
 * Accelerating t-SNE using Tree-Based Algorithms<br />
 * Journal of Machine Learning Research 15
 * </p>
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Reference(authors = "L. van der Maaten", //
    title = "Accelerating t-SNE using Tree-Based Algorithms", //
    booktitle = "Journal of Machine Learning Research 15", //
    url = "http://dl.acm.org/citation.cfm?id=2697068")
public class BarnesHutTSNE<O> extends TSNE<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BarnesHutTSNE.class);

  /**
   * Threshold for optimizing perplexity.
   */
  final static protected double PERPLEXITY_ERROR = 1e-4;

  /**
   * Maximum number of iterations when optimizing perplexity.
   */
  final static protected int PERPLEXITY_MAXITER = 25;

  /**
   * Capacity of the generated quadtree.
   */
  protected static final int QUADTREE_CAPACITY = 10;

  /**
   * (Squared) approximation quality threshold.
   */
  protected double sqtheta;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param dim Output dimensionality
   * @param perplexity Desired perplexity
   * @param finalMomentum Final momentum
   * @param learningRate Learning rate
   * @param maxIterations Maximum number of iterations
   * @param random Random generator
   * @param keep Keep the original data (or remove it)
   */
  public BarnesHutTSNE(DistanceFunction<? super O> distanceFunction, int dim, double perplexity, double finalMomentum, double learningRate, int maxIterations, RandomFactory random, boolean keep, double theta) {
    super(distanceFunction, dim, perplexity, finalMomentum, learningRate, maxIterations, random, keep);
    this.sqtheta = theta * theta;
  }

  public Relation<DoubleVector> run(Database database, Relation<O> relation) {
    final int numberOfNeighbours = (int) Math.ceil(3 * perplexity);
    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnq = database.getKNNQuery(dq, numberOfNeighbours + 1);
    if(knnq instanceof LinearScanQuery) {
      LOG.warning("To accelerate Barnes-Hut tSNE, please use an index.");
    }
    DBIDs rids = relation.getDBIDs();
    if(!(rids instanceof DBIDRange)) {
      throw new AbortException("Distance matrixes are currently only supported for DBID ranges (as used by static databases) for performance reasons (Patches welcome).");
    }
    DBIDRange ids = (DBIDRange) rids;
    final int size = ids.size();
    DBIDArrayIter ix = ids.iter();

    // Sparse affinity graph
    double[][] pij = new double[size][];
    int[][] indices = new int[size][];
    final boolean square = !SquaredEuclideanDistanceFunction.class.isInstance(dq.getDistanceFunction());
    computePij(ids, ix, knnq, square, numberOfNeighbours, perplexity, pij, indices);

    double[][] solution = randomInitialSolution(size, dim, random.getSingleThreadedRandom());
    optimizetSNE(pij, indices, solution);

    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    for(ix.seek(0); ix.valid(); ix.advance()) {
      proj.put(ix, new DoubleVector(solution[ix.getOffset()]));
    }
    // Remove the original (unprojected) data unless told otherwise.
    if(!keep) {
      ResultUtil.removeRecursive(relation.getHierarchy(), relation);
    }
    return new MaterializedRelation<>("tSNE", "t-SNE", otype, proj, ids);
  }

  /**
   * Compute the sparse pij using the nearest neighbors only.
   * 
   * @param ids ID range
   * @param ix Iterator
   * @param knnq kNN query
   * @param square Use squared distances
   * @param numberOfNeighbours Number of neighbors to get
   * @param perplexity Desired perplexity
   * @param pij Output of distances
   * @param indices Output of indexes
   */
  protected void computePij(DBIDRange ids, DBIDArrayIter ix, KNNQuery<O> knnq, boolean square, int numberOfNeighbours, double perplexity, double[][] pij, int[][] indices) {
    final double logPerp = Math.log(perplexity);
    // Scratch arrays, resizable
    DoubleArray dists = new DoubleArray(numberOfNeighbours + 10);
    IntegerArray inds = new IntegerArray(numberOfNeighbours + 10);
    // Compute nearest-neighbor sparse affinity matrix
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Finding neighbors and optimizing perplexity", ids.size(), LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(TSNE.class.getName() + ".runtime.pijmatrix").begin() : null;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      dists.clear();
      inds.clear();
      KNNList neighbours = knnq.getKNNForDBID(ix, numberOfNeighbours + 1);
      convertNeighbors(ids, ix, square, neighbours, dists, inds);
      computeSigma(ix.getOffset(), dists, perplexity, logPerp, //
          pij[ix.getOffset()] = new double[dists.size()]);
      indices[ix.getOffset()] = inds.toArray();
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(timer != null) {
      LOG.statistics(timer.end());
    }
    // Sum of the sparse affinity matrix:
    double sum = 0.;
    for(int i = 0; i < pij.length; i++) {
      final double[] pij_i = pij[i];
      for(int j = 0; j < pij_i.length; j++) {
        sum += pij_i[j];
      }
    }
    final double scale = EARLY_EXAGGERATION / (2. * sum);
    for(int i = 0; i < pij.length; i++) {
      final double[] pij_i = pij[i];
      for(int offi = 0; offi < pij_i.length; offi++) {
        int j = indices[i][offi];
        assert (i != j);
        int offj = containsIndex(indices[j], i);
        if(offj >= 0) { // Found
          assert (indices[j][offj] == i);
          // Exploit symmetry:
          if(i < j) {
            final double val = pij_i[offi] + pij[j][offj]; // Symmetrize
            pij_i[offi] = pij[j][offj] = MathUtil.max(val * scale, MIN_PIJ);
          }
        }
        else { // Not found
          pij_i[offi] = MathUtil.max(pij_i[offi] * scale, MIN_PIJ);
        }
      }
    }
  }

  /**
   * Load a neighbor query result into a double and and integer array, also
   * removing the query point. This is necessary, because we have to modify the
   * distances.
   * 
   * TODO: sort by index, not distance
   *
   * @param ids Indexes
   * @param ix Current Object
   * @param square Use squared distances
   * @param neighbours Neighbor list
   * @param dist Output distance array
   * @param ind Output index array
   */
  protected void convertNeighbors(DBIDRange ids, DBIDRef ix, boolean square, KNNList neighbours, DoubleArray dist, IntegerArray ind) {
    for(DoubleDBIDListIter iter = neighbours.iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, ix)) {
        continue; // Skip query point
      }
      double d = iter.doubleValue();
      dist.add(square ? (d * d) : d);
      ind.add(ids.getOffset(iter));
    }
  }

  /**
   * Compute row pij[i], using binary search on the kernel bandwidth sigma to
   * obtain the desired perplexity.
   *
   * @param i Current point
   * @param dist_i Distance matrix row pij[i]
   * @param perplexity Desired perplexity
   * @param logPerp Log of desired perplexity
   * @param pij_i Output row
   */
  protected static void computeSigma(int i, DoubleArray pij_row, double perplexity, double log_perp, double[] pij_i) {
    double max = pij_row.get((int) Math.ceil(perplexity)) / Math.E;
    double beta = 1 / max; // beta = 1. / (2*sigma*sigma)
    double diff = computeH(pij_row, pij_i, -beta) - log_perp;
    double betaMin = Double.NEGATIVE_INFINITY;
    double betaMax = Double.POSITIVE_INFINITY;
    for(int tries = 0; tries < PERPLEXITY_MAXITER && Math.abs(diff) > PERPLEXITY_ERROR; ++tries) {
      if(diff > 0) {
        betaMin = beta;
        beta += (betaMax == Double.POSITIVE_INFINITY) ? beta : ((betaMax - beta) * .5);
      }
      else {
        betaMax = beta;
        beta -= (betaMin == Double.NEGATIVE_INFINITY) ? beta : ((beta - betaMin) * .5);
      }
      diff = computeH(pij_row, pij_i, -beta) - log_perp;
    }
  }

  /**
   * Compute H (observed perplexity) for row i, and the row pij_i.
   * 
   * @param dist_i Distances to neighbors
   * @param pij_i Row pij[i] (output)
   * @param mbeta {@code -1. / (2 * sigma * sigma)}
   * @return Observed perplexity
   */
  protected static double computeH(DoubleArray dist_i, double[] pij_row, double mbeta) {
    final int len = dist_i.size();
    assert (pij_row.length == len);
    double sumP = 0.;
    for(int j = 0; j < len; j++) {
      sumP += (pij_row[j] = Math.exp(dist_i.get(j) * mbeta));
    }
    if(!(sumP > 0)) {
      // All pij are zero. Bad news.
      return Double.NEGATIVE_INFINITY;
    }
    final double s = 1. / sumP; // Scaling factor
    double sum = 0.;
    // While we could skip pi[i], it should be 0 anyway.
    for(int j = 0; j < len; j++) {
      sum += dist_i.get(j) * (pij_row[j] *= s);
    }
    return Math.log(sumP) - mbeta * sum;
  }

  /**
   * Check if the index array contains {@code i}.
   * 
   * TODO: sort arrays, use binary search!
   * 
   * @param i Index to search
   * @return Position of index i, or {@code -1} if not found.
   */
  protected static int containsIndex(int[] is, int i) {
    for(int j = 0; j < is.length; j++) {
      if(i == is[j]) {
        return j;
      }
    }
    return -1;
  }

  /**
   * Perform the actual tSNE optimization.
   * 
   * @param pij Sparse initial affinity matrix
   * @param indices Index array of affinity matrix
   * @param sol Solution output array (preinitialized)
   */
  protected void optimizetSNE(double[][] pij, int[][] indices, double[][] sol) {
    final int size = pij.length;
    // Meta information on each point; joined for memory locality.
    // Gradient, Momentum, and learning rate
    double[][] meta = new double[size][3 * dim];
    for(int i = 0; i < size; i++) {
      Arrays.fill(meta[i], 2 * dim, 3 * dim, 1.); // Initial learning rate
    }
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Iterative Optimization", maxIterations, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(TSNE.class.getName() + ".runtime.optimization").begin() : null;
    // Optimize
    for(int i = 0; i < maxIterations; i++) {
      computeGradient(pij, indices, sol, meta);
      updateSolution(sol, meta, i);
      if(i == EARLY_EXAGGERATION_ITERATIONS) {
        removeEarlyExaggeration(pij);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(timer != null) {
      LOG.statistics(timer.end());
    }
  }

  private void computeGradient(double[][] pij, int[][] indices, double[][] solution, double[][] grad) {
    double[][] attr = new double[solution.length][dim];
    double[][] rep = new double[solution.length][dim];

    computeAttractiveForces(attr, pij, indices, solution);

    QuadTree tree = QuadTree.build(dim, solution, QUADTREE_CAPACITY);
    double z = 0.;
    for(int i = 0; i < solution.length; i++) {
      z -= computeRepulsiveForces(rep[i], solution[i], tree);
    }
    z -= solution.length;
    for(int i = 0; i < grad.length; i++) {
      for(int j = 0; j < dim; j++) {
        rep[i][j] /= z;
        grad[i][j] = (attr[i][j] + rep[i][j]) * 4.;
      }
    }
  }

  private void computeAttractiveForces(double[][] attr, double[][] pij, int[][] indices, double[][] sol) {
    for(int i = 0; i < attr.length; i++) {
      final double[] pij_i = pij[i], sol_i = sol[i];
      final int[] ind_i = indices[i];
      for(int offj = 0; offj < ind_i.length; offj++) {
        final double[] sol_j = sol[ind_i[offj]];
        final double pij_ij = pij_i[offj];
        final double a = pij_ij / (1. + sqDist(sol_i, sol_j));
        for(int k = 0; k < dim; k++) {
          attr[i][k] += a * (sol_i[k] - sol_j[k]);
        }
      }
    }
  }

  private double computeRepulsiveForces(double[] rep_i, double[] sol_i, QuadTree node) {
    final double[] center = node.center;
    double dist = sqDist(sol_i, center);
    // Barnes-Hut approximation:
    // TODO: van der Maaten actually uses the maximum edge length, not the
    // diagonal. What is correct / better?
    if(node.weight == 1 || node.squareSize / dist < sqtheta) {
      double u = 1. / (1. + dist);
      double z = node.weight * u;
      double a = z * u;
      for(int k = 0; k < dim; k++) {
        // TODO: van der Maaten avoids computing this difference twice (also in
        // sqDist) - we should also use this optimization.
        rep_i[k] += a * (sol_i[k] - center[k]);
      }
      return z;
    }
    double z = 0.;
    // Aggregate points in this node:
    for(double[] point : node.points) {
      double pdist = sqDist(sol_i, point);
      double pz = 1. / (1. + pdist);
      double a = pz * pz;
      for(int k = 0; k < dim; k++) {
        rep_i[k] += a * (sol_i[k] - point[k]);
      }
      z += pz;
    }
    // Recurse into subtrees:
    for(QuadTree child : node.children) {
      z += computeRepulsiveForces(rep_i, sol_i, child);
    }
    return z;
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
   * Quad Tree for use in a Barnes-Hut approximation.
   * 
   * This tree stores in every node the number of points contained, the center
   * of mass, and the diagonal of the cell.
   * 
   * @author Erich Schubert
   */
  protected static class QuadTree {
    /**
     * Empty constant for non-leaf nodes.
     */
    private static final double[][] NO_POINTS = new double[0][]; // Empty

    /**
     * Empty constant for leaf nodes.
     */
    private static final QuadTree[] NO_CHILDREN = new QuadTree[0]; // Empty

    /**
     * Minimum resolution of quadtree.
     */
    private static final double MIN_RESOLUION = 1e-10;

    /**
     * Center of mass (NOT center of bounding box)
     */
    public double[] center;

    /**
     * Points stored in this node.
     */
    public double[][] points;

    /**
     * Square size of this node, for Barnes-Hut approximation.
     */
    public double squareSize;

    /**
     * Total weight of this node.
     */
    public int weight;

    /**
     * Child nodes.
     */
    public QuadTree[] children;

    /**
     * Constructor.
     *
     * @param data Data points
     * @param children Child nodes
     * @param mid Center of mass
     * @param weight Node weight
     * @param squareSize Square size of the node
     */
    private QuadTree(double[][] data, QuadTree[] children, double[] mid, int weight, double squareSize) {
      this.center = mid;
      this.points = data;
      this.weight = weight;
      this.squareSize = squareSize;
      this.children = children;
    }

    /**
     * Construct the quad tree approximation.
     * 
     * @param dim Dimensionality
     * @param data Data set (will be modified!)
     * @param capacity Node capacity
     * @return Quad tree
     */
    public static QuadTree build(int dim, double[][] data, int capacity) {
      return build(dim, data.clone(), 0, data.length, capacity);
    }

    /**
     * Recursive build function.
     * 
     * @param dim Dimensionality
     * @param data Input data (WILL BE MODIFIED)
     * @param begin Subset begin
     * @param end Subset end
     * @param capacity Node capacity
     * @return Subtree
     */
    private static QuadTree build(int dim, double[][] data, int begin, int end, int capacity) {
      double[] minmax = computeExtend(dim, data, begin, end);
      double squareSize = computeSquareSize(minmax);
      double[] mid = computeCenterofMass(dim, data, begin, end);
      // Leaf:
      final int size = end - begin;
      if(size < capacity || squareSize <= MIN_RESOLUION) {
        if(begin > 0 || end < data.length) {
          data = Arrays.copyOfRange(data, begin, end);
        }
        return new QuadTree(data, NO_CHILDREN, mid, size, squareSize);
      }

      ArrayList<double[]> singletons = new ArrayList<>();
      ArrayList<QuadTree> children = new ArrayList<>();
      splitRecursively(data, begin, end, 0, dim, minmax, singletons, children, capacity);

      double[][] sing = singletons.size() > 0 ? singletons.toArray(new double[singletons.size()][]) : NO_POINTS;
      QuadTree[] chil = children.size() > 0 ? children.toArray(new QuadTree[children.size()]) : NO_CHILDREN;
      return new QuadTree(sing, chil, mid, size, squareSize);
    }

    /**
     * Build the quadtree by recursive splitting.
     * 
     * @param data Input data
     * @param begin Subset begin
     * @param end Subset end
     * @param cur Current dimension
     * @param dims Data dimensionality
     * @param minmax Bounding box
     * @param singletons Output for singletons
     * @param children Output for child nodes
     * @param capacity Node capacity
     */
    private static void splitRecursively(double[][] data, int begin, int end, int initdim, int dims, double[] minmax, ArrayList<double[]> singletons, ArrayList<QuadTree> children, int capacity) {
      final int len = end - begin;
      if(len == 1) {
        singletons.add(data[begin]);
      }
      if(len < 1) {
        return;
      }
      double mid = Double.NaN;
      // Find next non-zero dimension:
      int cur = initdim;
      while(true) {
        int dim2 = cur << 1;
        double min = minmax[dim2], max = minmax[dim2 + 1];
        mid = .5 * (min + max);
        if(min < mid) {
          break; // Non-constant dimension found.
        }
        ++cur; // Try next dimension
        // All remaining dimensions were constant?
        if(cur == dims) {
          assert (initdim != 0) : "All dimensions constant?";
          LOG.warning("Unexpected all-constant split.");
          if(begin > 0 || end < data.length) {
            data = Arrays.copyOfRange(data, begin, end);
          }
          children.add(new QuadTree(data, null, data[0].clone(), data.length, 0.));
          return;
        }
      }
      // Pivotize
      int l = begin, r = end - 1;
      while(l <= r) {
        while(l <= r && data[l][cur] <= mid) {
          ++l;
        }
        while(l <= r && data[r][cur] >= mid) {
          --r;
        }
        if(l < r) {
          assert (data[l][cur] > mid);
          assert (data[r][cur] < mid);
          double[] tmp = data[r];
          data[r] = data[l];
          data[l] = tmp;
          ++l;
          --r;
        }
      }
      assert (l == end || data[l][cur] >= mid);
      assert (l == begin || data[l - 1][cur] <= mid);
      ++cur;
      // Recursion into next dimension:
      if(cur < dims) {
        if(begin < l) {
          splitRecursively(data, begin, l, cur, dims, minmax, singletons, children, capacity);
        }
        if(l < end) {
          splitRecursively(data, l, end, cur, dims, minmax, singletons, children, capacity);
        }
        return;
      }
      // Recurse into next depth:
      if(begin < l) {
        children.add(build(dims, data, begin, l, capacity));
      }
      if(l < end) {
        children.add(build(dims, data, l, end, capacity));
      }
    }

    /**
     * Computer the center of mass.
     * 
     * @param dim Dimensionality
     * @param data Data set
     * @param begin Begin of subset
     * @param end End of subset
     * @return Center of mass
     */
    private static double[] computeCenterofMass(int dim, double[][] data, int begin, int end) {
      final int size = end - begin;
      if(size == 1) {
        return data[begin];
      }
      double[] center = new double[dim];
      for(int i = begin; i < end; i++) {
        double[] row = data[i];
        for(int d = 0; d < dim; d++) {
          center[d] += row[d];
        }
      }
      double norm = 1. / size;
      for(int d = 0; d < dim; d++) {
        center[d] *= norm;
      }
      return center;
    }

    /**
     * Compute the bounding box of a data set.
     * 
     * @param dim Dimensionality
     * @param data Data set
     * @param begin Begin of subset
     * @param end End of subset
     * @return Bounding box
     */
    private static double[] computeExtend(int dim, double[][] data, int begin, int end) {
      double[] minmax = new double[dim << 1];
      for(int d = 0; d < minmax.length;) {
        minmax[d++] = Double.POSITIVE_INFINITY;
        minmax[d++] = Double.NEGATIVE_INFINITY;
      }
      for(int i = begin; i < end; i++) {
        double[] row = data[i];
        for(int d = 0, d2 = 0; d < dim; d++) {
          final double v = row[d];
          minmax[d2] = MathUtil.min(minmax[d2], v);
          ++d2;
          minmax[d2] = MathUtil.max(minmax[d2], v);
          ++d2;
        }
      }
      return minmax;
    }

    /**
     * Compute the diagonal of a bounding box.
     * 
     * @param minmax Bounding box
     * @return Diagonal length
     */
    private static double computeSquareSize(double[] minmax) {
      double sum = 0.;
      for(int d = 0; d < minmax.length; d += 2) {
        double width = minmax[d + 1] - minmax[d];
        sum += width * width;
      }
      return sum; // Math.sqrt(sum);
      /*
       * // Van der Maaten uses the maximum edge length!
       * 
       * double max = 0; for(int d = 0; d < minmax.length; d += 2) { double
       * width = minmax[d + 1] - minmax[d]; max = width > max ? width : max; }
       * return max * max;
       */
    }

    @Override
    public String toString() {
      return "QuadTree[center=" + FormatUtil.format(center) + ", weight=" + weight + ", points=" + points.length + ", children=" + children.length + ", sqSize=" + squareSize + "]";
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends TSNE.Parameterizer<O> {
    /**
     * Parameter for the approximation quality.
     */
    public static final OptionID THETA_ID = new OptionID("tsne.theta", "Approximation quality parameter");

    /**
     * Theta approximation quality parameter.
     */
    public double theta;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter tethaP = new DoubleParameter(THETA_ID) //
          .setDefaultValue(0.5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(tethaP)) {
        theta = tethaP.getValue();
      }
    }

    @Override
    protected BarnesHutTSNE<O> makeInstance() {
      return new BarnesHutTSNE<>(distanceFunction, dim, perplexity, finalMomentum, learningRate, maxIterations, random, keep, theta);
    }
  }
}
