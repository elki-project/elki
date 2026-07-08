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

import java.util.Random;

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
import elki.utilities.datastructures.arraylike.DoubleArray;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

/**
 * Uniform Manifold Approximation and Projection (UMAP).
 * <p>
 * Reference:
 * <p>
 * L. McInnes, J. Healy, J. Melville<br>
 * UMAP: Uniform Manifold Approximation and Projection for Dimension
 * Reduction<br>
 * arXiv preprint arXiv:1802.03426
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Title("UMAP")
@Reference(authors = "L. McInnes, J. Healy, J. Melville", //
    title = "UMAP: Uniform Manifold Approximation and Projection for Dimension Reduction", //
    booktitle = "arXiv preprint arXiv:1802.03426", //
    url = "https://arxiv.org/abs/1802.03426", //
    bibkey = "DBLP:journals/corr/abs-1802-03426")
public class UMAP<O> extends AbstractProjectionAlgorithm<Relation<DoubleVector>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(UMAP.class);

  /**
   * Initial embedding scale.
   */
  private static final double INITIAL_SCALE = 1e-4;

  /**
   * Distance function.
   */
  protected Distance<? super O> distance;

  /**
   * Embedding dimensionality.
   */
  protected int dim;

  /**
   * Number of neighbors.
   */
  protected int neighbors;

  /**
   * Minimum distance in low-dimensional space.
   */
  protected double minDist;

  /**
   * Spread parameter.
   */
  protected double spread;

  /**
   * Number of optimization epochs.
   */
  protected int epochs;

  /**
   * Learning rate.
   */
  protected double learningRate;

  /**
   * Negative sample rate.
   */
  protected int negativeSampleRate;

  /**
   * Random generator.
   */
  protected RandomFactory random;

  /**
   * Create a new UMAP instance.
   *
   * @param distance Distance function for the input space
   * @param dim Target dimensionality of the embedding
   * @param neighbors Number of nearest neighbors for graph construction
   * @param minDist Minimum distance between points in the embedding
   * @param spread Spread parameter controlling the scale of the embedding
   * @param epochs Number of optimization epochs
   * @param learningRate Learning rate for stochastic gradient descent
   * @param negativeSampleRate Number of negative samples per positive edge
   * @param random Random factory for reproducible sampling
   * @param keep Whether to keep the original relation alongside the result
   */
  public UMAP(Distance<? super O> distance, int dim, int neighbors, double minDist, double spread, int epochs, double learningRate, int negativeSampleRate, RandomFactory random, boolean keep) {
    super(keep);
    this.distance = distance;
    this.dim = dim;
    this.neighbors = neighbors;
    this.minDist = minDist;
    this.spread = spread;
    this.epochs = epochs;
    this.learningRate = learningRate;
    this.negativeSampleRate = negativeSampleRate;
    this.random = random;
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
   * Perform UMAP projection.
   *
   * @param relation Input relation
   * @return Output relation
   */
  public Relation<DoubleVector> run(Relation<O> relation) {
    DBIDEnum ids = DBIDUtil.ensureEnum(relation.getDBIDs());
    final int size = ids.size();
    if(dim > size) {
      throw new AbortException("UMAP dimensionality " + dim + " exceeds relation size " + size + ".");
    }

    int[][] knn = new int[size][];
    double[][] knnDist = new double[size][];
    buildKNN(relation, ids, knn, knnDist);

    double[] rho = new double[size];
    double[] sigma = new double[size];
    double target = Math.log(neighbors) / Math.log(2.0);
    for(int i = 0; i < size; i++) {
      rho[i] = localConnectivity(knnDist[i]);
      sigma[i] = smoothKnnDistance(knnDist[i], rho[i], target);
    }

    EdgeList edges = buildFuzzyGraph(knn, knnDist, rho, sigma);

    double[][] embed = randomInitialSolution(size, dim, random.getSingleThreadedRandom());
    optimizeEmbedding(embed, edges, epochs, learningRate, negativeSampleRate, random.getSingleThreadedRandom());

    removePreviousRelation(relation);

    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    for(DBIDArrayIter it = ids.iter(); it.valid(); it.advance()) {
      proj.put(it, DoubleVector.wrap(embed[it.getOffset()]));
    }
    return new MaterializedRelation<>("UMAP", otype, ids, proj);
  }

  /**
   * Build the k‑nearest neighbor lists and corresponding distances for all
   * objects.
   *
   * @param relation input relation
   * @param ids enumeration of DBIDs
   * @param knn output array of neighbor indices (size = number of objects)
   * @param knnDist output array of neighbor distances (same shape as
   *        {@code knn})
   */
  protected void buildKNN(Relation<O> relation, DBIDEnum ids, int[][] knn, double[][] knnDist) {
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).kNNByDBID(neighbors + 1);
    if(knnq instanceof LinearScanQuery && (long) neighbors * neighbors < ids.size()) {
      LOG.warning("To accelerate UMAP, please use an index.");
    }
    if(!distance.isSymmetric()) {
      LOG.warning("UMAP expects a symmetric distance function.");
    }
    final boolean square = distance.isSquared();
    for(DBIDArrayIter it = ids.iter(); it.valid(); it.advance()) {
      KNNList list = knnq.getKNN(it, neighbors + 1);
      IntegerArray inds = new IntegerArray(neighbors + 2);
      DoubleArray dists = new DoubleArray(neighbors + 2);
      for(DoubleDBIDListIter iter = list.iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iter, it)) {
          continue;
        }
        final int j = ids.index(iter);
        double d = iter.doubleValue();
        if(square) {
          d = Math.sqrt(d);
        }
        inds.add(j);
        dists.add(d);
      }
      knn[it.getOffset()] = inds.toArray();
      knnDist[it.getOffset()] = dists.toArray();
    }
  }

  /**
   * Compute the local connectivity, i.e., the distance to the closest non‑zero
   * neighbor.
   *
   * @param distances sorted distances to neighbors
   * @return the smallest positive distance, or {@code 0.0} if none
   */
  protected static double localConnectivity(double[] distances) {
    for(int i = 0; i < distances.length; i++) {
      if(distances[i] > 0.) {
        return distances[i];
      }
    }
    return 0.;
  }

  /**
   * Find the smooth k‑NN distance (sigma) by binary search such that the sum of
   * the membership strengths matches the target value.
   *
   * @param distances distances to the k‑nearest neighbors
   * @param rho the local connectivity value
   * @param target the desired sum of membership strengths (log₂(k))
   * @return the estimated sigma value
   */
  protected static double smoothKnnDistance(double[] distances, double rho, double target) {
    double lo = 0., hi = Double.POSITIVE_INFINITY, sigma = 1.;
    for(int iter = 0; iter < 64; iter++) {
      double sum = 0.;
      for(int i = 0; i < distances.length; i++) {
        double d = distances[i] - rho;
        sum += (d > 0.) ? Math.exp(-d / sigma) : 1.;
      }
      if(Math.abs(sum - target) < 1e-5) {
        break;
      }
      if(sum > target) {
        hi = sigma;
        sigma = 0.5 * (lo + hi);
      }
      else {
        lo = sigma;
        sigma = (hi == Double.POSITIVE_INFINITY) ? sigma * 2. : 0.5 * (lo + hi);
      }
    }
    return Math.max(sigma, 1e-6);
  }

  /**
   * Construct the fuzzy simplicial set (edge list) from the k‑NN graph.
   *
   * @param knn neighbor indices for each point
   * @param knnDist neighbor distances for each point
   * @param rho local connectivity values per point
   * @param sigma smooth distance scaling per point
   * @return an {@link EdgeList} containing source, destination and weight
   *         arrays
   */
  protected EdgeList buildFuzzyGraph(int[][] knn, double[][] knnDist, double[] rho, double[] sigma) {
    Long2DoubleOpenHashMap map = new Long2DoubleOpenHashMap();
    map.defaultReturnValue(0.);
    final int size = knn.length;
    for(int i = 0; i < size; i++) {
      final int[] nbr = knn[i];
      final double[] dist = knnDist[i];
      for(int p = 0; p < nbr.length; p++) {
        final int j = nbr[p];
        final double d = dist[p];
        final double w = Math.exp(-(Math.max(d - rho[i], 0.)) / sigma[i]);
        if(w <= 0.) {
          continue;
        }
        final int a = Math.min(i, j), b = Math.max(i, j);
        final long key = (((long) a) << 32) | (b & 0xffffffffL);
        final double old = map.get(key);
        map.put(key, old + w - old * w);
      }
    }
    IntegerArray src = new IntegerArray(map.size());
    IntegerArray dst = new IntegerArray(map.size());
    DoubleArray wts = new DoubleArray(map.size());
    for(Long2DoubleMap.Entry e : map.long2DoubleEntrySet()) {
      final long key = e.getLongKey();
      final int i = (int) (key >>> 32);
      final int j = (int) key;
      final double w = e.getDoubleValue();
      if(w <= 0.) {
        continue;
      }
      src.add(i);
      dst.add(j);
      wts.add(w);
    }
    return new EdgeList(src.toArray(), dst.toArray(), wts.toArray());
  }

  /**
   * Fit the parameters {@code a} and {@code b} of the low‑dimensional distance
   * curve
   * used by UMAP.
   *
   * @param spread the {@code spread} parameter
   * @param minDist the {@code minDist} parameter
   * @return an array {@code [a, b]}
   */
  protected static double[] fitAB(double spread, double minDist) {
    final int n = 200;
    double[] xs = new double[n];
    double[] ys = new double[n];
    double max = spread * 3.;
    for(int i = 0; i < n; i++) {
      double x = max * i / (n - 1.);
      xs[i] = x;
      ys[i] = (x < minDist) ? 1. : Math.exp(-(x - minDist) / spread);
    }
    double bestA = 1., bestB = 1., bestErr = Double.POSITIVE_INFINITY;
    for(double b = 0.1; b <= 2.0; b += 0.1) {
      for(int ai = 0; ai < 60; ai++) {
        double a = Math.pow(10., -3. + ai * (6. / 59.));
        double err = abError(a, b, xs, ys);
        if(err < bestErr) {
          bestErr = err;
          bestA = a;
          bestB = b;
        }
      }
    }
    for(double b = Math.max(0.05, bestB - 0.1); b <= bestB + 0.1; b += 0.01) {
      for(int ai = 0; ai < 40; ai++) {
        double a = bestA * Math.pow(10., -0.5 + ai * (1.0 / 39.));
        double err = abError(a, b, xs, ys);
        if(err < bestErr) {
          bestErr = err;
          bestA = a;
          bestB = b;
        }
      }
    }
    return new double[] { bestA, bestB };
  }

  /**
   * Compute the sum of squared errors between the parametric curve
   * {@code 1 / (1 + a * x^{2b})} and the target values {@code ys}.
   *
   * @param a parameter {@code a} of the curve
   * @param b parameter {@code b} of the curve
   * @param xs input values
   * @param ys target values
   * @return sum of squared errors
   */
  private static double abError(double a, double b, double[] xs, double[] ys) {
    double err = 0.0;
    for(int i = 0; i < xs.length; i++) {
      double x = xs[i];
      double v = 1.0 / (1.0 + a * Math.pow(x, 2.0 * b));
      double d = v - ys[i];
      err += d * d;
    }
    return err;
  }

  /**
   * Optimize the embedding using stochastic gradient descent.
   *
   * @param embed current embedding matrix (size × dim)
   * @param edges edge list of the fuzzy graph
   * @param epochs number of optimization epochs
   * @param initialLR initial learning rate
   * @param negativeRate number of negative samples per positive edge
   * @param rnd random number generator
   */
  protected void optimizeEmbedding(double[][] embed, EdgeList edges, int epochs, double initialLR, int negativeRate, Random rnd) {
    final int size = embed.length;
    final double[] ab = fitAB(spread, minDist);
    final double a = ab[0], b = ab[1];

    final int m = edges.src.length;
    if(m == 0) {
      throw new AbortException("UMAP graph is empty; increase k or use a different distance.");
    }
    double maxW = 0.;
    for(int i = 0; i < m; i++) {
      maxW = Math.max(maxW, edges.weight[i]);
    }
    if(maxW <= 0.) {
      throw new AbortException("UMAP graph has zero weights; check distance and k.");
    }
    double[] epochsPerSample = new double[m];
    double[] nextSample = new double[m];
    for(int i = 0; i < m; i++) {
      double w = edges.weight[i] / maxW;
      if(w <= 0.) {
        epochsPerSample[i] = Double.POSITIVE_INFINITY;
        nextSample[i] = Double.POSITIVE_INFINITY;
        continue;
      }
      double nSamples = w * epochs;
      epochsPerSample[i] = epochs / nSamples;
      nextSample[i] = epochsPerSample[i];
    }

    for(int epoch = 0; epoch < epochs; epoch++) {
      final double lr = initialLR * (1. - (epoch / (double) epochs));
      for(int e = 0; e < m; e++) {
        if(nextSample[e] > epoch) {
          continue;
        }
        nextSample[e] += epochsPerSample[e];
        int i = edges.src[e];
        int j = edges.dst[e];
        updateAttractive(embed, i, j, a, b, lr);
        for(int ns = 0; ns < negativeRate; ns++) {
          int k = rnd.nextInt(size);
          if(k == i) {
            continue;
          }
          updateRepulsive(embed, i, k, a, b, lr);
        }
      }
    }
  }

  /**
   * Perform an attractive update for a positive edge between points {@code i}
   * and {@code j}.
   *
   * @param embed current embedding matrix
   * @param i index of the first point
   * @param j index of the second point
   * @param a parameter {@code a} of the low‑dimensional distance function
   * @param b parameter {@code b} of the low‑dimensional distance function
   * @param lr learning rate for this iteration
   */
  private static void updateAttractive(double[][] embed, int i, int j, double a, double b, double lr) {
    final double[] xi = embed[i];
    final double[] xj = embed[j];
    double dist2 = 0.;
    for(int d = 0; d < xi.length; d++) {
      double diff = xi[d] - xj[d];
      dist2 += diff * diff;
    }
    if(dist2 <= 0.) {
      return;
    }
    double dist2b = Math.pow(dist2, b);
    double gradCoeff = -2. * a * b * Math.pow(dist2, b - 1.) / (1. + a * dist2b);
    for(int d = 0; d < xi.length; d++) {
      double grad = gradCoeff * (xi[d] - xj[d]);
      xi[d] += grad * lr;
      xj[d] -= grad * lr;
    }
  }

  /**
   * Perform a repulsive update for a negative sample between points {@code i}
   * and {@code k}.
   *
   * @param embed current embedding matrix
   * @param i index of the source point
   * @param k index of the negative sample point
   * @param a parameter {@code a} of the low‑dimensional distance function
   * @param b parameter {@code b} of the low‑dimensional distance function
   * @param lr learning rate for this iteration
   */
  private static void updateRepulsive(double[][] embed, int i, int k, double a, double b, double lr) {
    final double[] xi = embed[i];
    final double[] xk = embed[k];
    double dist2 = 0.;
    for(int d = 0; d < xi.length; d++) {
      double diff = xi[d] - xk[d];
      dist2 += diff * diff;
    }
    double dist2b = Math.pow(dist2, b);
    double gradCoeff = 2. * b / ((0.001 + dist2) * (1. + a * dist2b));
    for(int d = 0; d < xi.length; d++) {
      double grad = gradCoeff * (xi[d] - xk[d]);
      xi[d] += grad * lr;
      xk[d] -= grad * lr;
    }
  }

  /**
   * Generate a random initial embedding.
   *
   * @param size number of points
   * @param dim target dimensionality
   * @param rnd random number generator
   * @return a {@code size × dim} matrix with small Gaussian values
   */
  protected static double[][] randomInitialSolution(int size, int dim, Random rnd) {
    double[][] sol = new double[size][dim];
    for(int i = 0; i < size; i++) {
      for(int j = 0; j < dim; j++) {
        sol[i][j] = rnd.nextGaussian() * INITIAL_SCALE;
      }
    }
    return sol;
  }

  /**
   * Edge list container.
   */
  protected static class EdgeList {
    /** Source vertex indices. */
    final int[] src;

    /** Destination vertex indices. */
    final int[] dst;

    /** Edge weights. */
    final double[] weight;

    /**
     * Construct an edge list.
     *
     * @param src source vertex indices
     * @param dst destination vertex indices
     * @param weight edge weights
     */
    EdgeList(int[] src, int[] dst, double[] weight) {
      this.src = src;
      this.dst = dst;
      this.weight = weight;
    }
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
    public static final OptionID DIM_ID = new OptionID("umap.dim", "Target dimensionality for UMAP.");

    /**
     * Number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("umap.k", "Number of neighbors for the kNN graph.");

    /**
     * Minimum distance parameter.
     */
    public static final OptionID MIN_DIST_ID = new OptionID("umap.min_dist", "Minimum distance between embedded points.");

    /**
     * Spread parameter.
     */
    public static final OptionID SPREAD_ID = new OptionID("umap.spread", "Effective scale of embedded points.");

    /**
     * Number of epochs.
     */
    public static final OptionID EPOCHS_ID = new OptionID("umap.epochs", "Number of optimization epochs.");

    /**
     * Learning rate.
     */
    public static final OptionID LEARNING_RATE_ID = new OptionID("umap.learning_rate", "Learning rate for optimization.");

    /**
     * Negative sample rate.
     */
    public static final OptionID NEGATIVE_SAMPLE_RATE_ID = new OptionID("umap.negative_sample_rate", "Number of negative samples per positive edge.");

    /**
     * Random generator seed.
     */
    public static final OptionID RANDOM_ID = new OptionID("umap.seed", "Random generator seed.");

    /**
     * Dimensionality.
     */
    protected int dim;

    /**
     * Number of neighbors.
     */
    protected int k;

    /**
     * Minimum distance.
     */
    protected double minDist;

    /**
     * Spread.
     */
    protected double spread;

    /**
     * Epochs.
     */
    protected int epochs;

    /**
     * Learning rate.
     */
    protected double learningRate;

    /**
     * Negative sample rate.
     */
    protected int negativeSampleRate;

    /**
     * Distance function.
     */
    protected Distance<? super O> distance;

    /**
     * Random generator.
     */
    protected RandomFactory random;

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
      new IntParameter(K_ID, 15) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(MIN_DIST_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> minDist = x);
      new DoubleParameter(SPREAD_ID, 1.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> spread = x);
      new IntParameter(EPOCHS_ID, 200) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> epochs = x);
      new DoubleParameter(LEARNING_RATE_ID, 1.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> learningRate = x);
      new IntParameter(NEGATIVE_SAMPLE_RATE_ID, 5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> negativeSampleRate = x);
      new RandomParameter(RANDOM_ID) //
          .grab(config, x -> random = x);
      new Flag(KEEP_ID) //
          .grab(config, x -> keep = x);
    }

    @Override
    public UMAP<O> make() {
      return new UMAP<>(distance, dim, k, minDist, spread, epochs, learningRate, negativeSampleRate, random, keep);
    }
  }
}
