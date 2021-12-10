/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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

import java.util.Arrays;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Hierarchical Agglomerative Clustering Around Medoids (HACAM) is a
 * hierarchical clustering method that merges the clusters with the smallest
 * distance to the medoid of the union. This is different from the earlier
 * {@link MedoidLinkage}, which used the distance of the two previous medoids.
 * <p>
 * The implementation incorporates the approach of Anderson for acceleration.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert<br>
 * HACAM: Hierarchical Agglomerative Clustering Around Medoids
 * - and its Limitations<br>
 * Proceedings of the Conference "Lernen, Wissen, Daten, Analysen", LWDA
 *
 * @author Erich Schubert
 *
 * @has - - - PointerPrototypeHierarchyResult
 *
 * @param <O> Object type
 */
@Reference(authors = "Erich Schubert", //
    title = "HACAM: Hierarchical Agglomerative Clustering Around Medoids - and its Limitations", //
    booktitle = "Proc. Conf. \"Lernen, Wissen, Daten, Analysen\", LWDA", //
    url = "http://ceur-ws.org/Vol-2993/paper-19.pdf", //
    bibkey = "DBLP:conf/lwa/Schubert21")
public class HACAM<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(HACAM.class);

  /**
   * The distance function to use.
   */
  protected Distance<? super O> distance;

  /**
   * Linkage variant to use
   */
  protected Variant variant;

  /**
   * Variants of the HACAM method.
   *
   * @author Erich Schubert
   */
  public static enum Variant {
    /**
     * Minimum sum variant
     */
    MINIMUM_SUM,
    /**
     * Minimum sum increase variant
     */
    MINIMUM_SUM_INCREASE
  }

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param variant Variant to use
   */
  public HACAM(Distance<? super O> distance, Variant variant) {
    this.distance = distance;
    this.variant = variant;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerPrototypeHierarchyResult run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final DBIDs ids = relation.getDBIDs();
    final int size = ids.size();

    // Initialize space for result:
    PointerHierarchyBuilder builder = new PointerHierarchyBuilder(ids, dq.getDistance().isSquared());
    Int2ObjectOpenHashMap<ModifiableDBIDs> clusters = new Int2ObjectOpenHashMap<>();
    double[] tds = variant == Variant.MINIMUM_SUM_INCREASE ? new double[ids.size()] : null;

    // Compute the initial (lower triangular) distance matrix.
    MatrixParadigm mat = new MatrixParadigm(ids);
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(MatrixParadigm.triangleSize(size));
    DBIDArrayMIter protiter = prots.iter();

    MiniMax.initializeMatrices(mat, prots, dq);

    // Arrays used for caching:
    double[] bestd = new double[size];
    int[] besti = new int[size];
    initializeNNCache(mat.matrix, bestd, besti);

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    DBIDArrayIter ix = mat.ix;
    for(int i = 1, end = size; i < size; i++) {
      end = AGNES.shrinkActiveSet(ix, builder, end, //
          findMerge(end, mat, protiter, builder, clusters, bestd, besti, dq, tds));
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return (PointerPrototypeHierarchyResult) builder.complete();
  }

  /**
   * Initialize the NN cache.
   *
   * @param scratch Scratch space
   * @param bestd Best distance
   * @param besti Best index
   */
  private static void initializeNNCache(double[] scratch, double[] bestd, int[] besti) {
    final int size = bestd.length;
    Arrays.fill(bestd, Double.POSITIVE_INFINITY);
    Arrays.fill(besti, -1);
    for(int x = 0, p = 0; x < size; x++) {
      assert (p == MatrixParadigm.triangleSize(x));
      double bestdx = Double.POSITIVE_INFINITY;
      int bestix = -1;
      for(int y = 0; y < x; y++, p++) {
        final double v = scratch[p];
        if(v < bestd[y]) {
          bestd[y] = v;
          besti[y] = x;
        }
        if(v < bestdx) {
          bestdx = v;
          bestix = y;
        }
      }
      bestd[x] = bestdx;
      besti[x] = bestix;
    }
  }

  /**
   * Perform the next merge step.
   * 
   * @param size size of the data set
   * @param mat matrix view
   * @param prots the prototypes of merges between clusters
   * @param builder Result builder
   * @param clusters the current clustering
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param dq the range query
   * @param tds per cluster TD
   * @return x, for shrinking the active set.
   */
  protected int findMerge(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, double[] bestd, int[] besti, DistanceQuery<O> dq, double[] tds) {
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;
    // Find minimum:
    for(int cx = 0; cx < size; cx++) {
      // Skip if object has already joined a cluster:
      final int cy = besti[cx];
      if(cy < 0) {
        continue;
      }
      final double dist = bestd[cx];
      if(dist <= mindist) { // Prefer later on ==, to truncate more often.
        mindist = dist;
        x = cx;
        y = cy;
      }
    }
    // assert(lambda.doubleValue(ix.seek(x<y?y:x))==Double.POSITIVE_INFINITY);
    assert (x >= 0 && y >= 0);
    if(y > x) {
      int tmp = x;
      x = y;
      y = tmp;
    }
    assert (y < x);
    merge(size, mat, prots, builder, clusters, dq, bestd, besti, x, y, tds);
    return x;
  }

  /**
   * Execute the cluster merge
   * 
   * @param size size of data set
   * @param mat Matrix paradigm
   * @param prots the prototypes of merges between clusters
   * @param builder Result builder
   * @param clusters the current clustering
   * @param dq the range query
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param x first cluster to merge, with {@code x > y}
   * @param y second cluster to merge, with {@code y < x}
   * @param tds per cluster TD
   */
  protected void merge(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, double[] bestd, int[] besti, int x, int y, double[] tds) {
    final DBIDArrayIter ix = mat.ix.seek(x), iy = mat.iy.seek(y);
    final double[] distances = mat.matrix;
    int offset = MatrixParadigm.triangleSize(x) + y;

    assert (y < x);

    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + distances[offset]);
    }

    ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);
    // Keep y
    if(cy == null) {
      cy = DBIDUtil.newHashSet();
      cy.add(iy);
    }
    if(cx == null) {
      cy.add(ix);
    }
    else {
      cy.addDBIDs(cx);
      clusters.remove(x);
    }
    clusters.put(y, cy);
    if(tds != null) { // min-sum-increase variant
      tds[y] = distances[offset] + tds[x] + tds[y];
    }

    // parent of x is set to y
    builder.strictAdd(ix, distances[offset], iy, prots.seek(offset));

    // Deactivate x in cache:
    besti[x] = -1;
    updateMatrices(size, mat, prots, builder, clusters, dq, bestd, besti, x, y, tds);
    if(besti[y] == x) {
      findBest(size, distances, bestd, besti, y);
    }
  }

  /**
   * Update the entries of the matrices that contain a distance to y, the newly
   * merged cluster.
   * 
   * @param size size of data set
   * @param mat matrix view
   * @param prots the prototypes of merges between clusters
   * @param builder Result builder
   * @param clusters the current clustering
   * @param dq the range query
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param x first cluster to merge, with {@code x > y}
   * @param y second cluster to merge, with {@code y < x}
   * @param tds per cluster TD
   */
  private void updateMatrices(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, double[] bestd, int[] besti, int x, int y, double[] tds) {
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] distances = mat.matrix;
    // c is the new cluster.
    // Update entries (at (a,b) with a > b) in the matrix where a = y or b = y

    // Update entries at (y,b) with b < y
    int a = y, b = 0;
    ix.seek(a);
    final int yoffset = MatrixParadigm.triangleSize(y);
    for(; b < a; b++) {
      // Skip entry if already merged
      if(builder.isLinked(iy.seek(b))) {
        continue;
      }
      updateEntry(mat, prots, clusters, dq, a, b, tds);
      updateCache(size, distances, bestd, besti, x, y, b, distances[yoffset + b]);
    }

    // Update entries at (a,y) with a > y
    a = y + 1;
    b = y;
    iy.seek(b);
    for(; a < size; a++) {
      // Skip entry if already merged
      if(builder.isLinked(ix.seek(a))) {
        continue;
      }
      updateEntry(mat, prots, clusters, dq, a, b, tds);
      updateCache(size, distances, bestd, besti, x, y, a, distances[MatrixParadigm.triangleSize(a) + y]);
    }
  }

  /**
   * Update entry at x,y for distance matrix distances
   * 
   * @param mat distance matrix
   * @param prots calculated prototypes
   * @param clusters the clusters
   * @param dq distance query on the data set
   * @param x index of cluster, {@code x > y}
   * @param y index of cluster, {@code y < x}
   * @param tds per cluster TD
   */
  protected static void updateEntry(MatrixParadigm mat, DBIDArrayMIter prots, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq, int x, int y, double[] tds) {
    assert (y < x);
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] distances = mat.matrix;
    ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);

    DBIDVar prototype = DBIDUtil.newVar(ix.seek(x)); // Default prototype
    double minMaxDist;
    // Two "real" clusters:
    if(cx != null && cy != null) {
      minMaxDist = findPrototype(dq, cx, cy, prototype, Double.POSITIVE_INFINITY);
      minMaxDist = findPrototype(dq, cy, cx, prototype, minMaxDist);
    }
    else if(cx != null) {
      // cy is singleton.
      minMaxDist = findPrototypeSingleton(dq, cx, iy.seek(y), prototype);
    }
    else if(cy != null) {
      // cx is singleton.
      minMaxDist = findPrototypeSingleton(dq, cy, ix.seek(x), prototype);
    }
    else {
      minMaxDist = dq.distance(ix.seek(x), iy.seek(y));
      prototype.set(ix);
    }
    if(tds != null) { // min-sum-increase variant
      minMaxDist -= tds[x] + tds[y];
    }

    final int offset = MatrixParadigm.triangleSize(x) + y;
    distances[offset] = minMaxDist;
    prots.seek(offset).setDBID(prototype);
  }

  /**
   * Find the prototypes.
   * 
   * @param dq Distance query
   * @param cx First set
   * @param cy Second set
   * @param prototype Prototype output variable
   * @param minDistSum Previously best distance.
   * @return New best distance
   */
  private static double findPrototype(DistanceQuery<?> dq, DBIDs cx, DBIDs cy, DBIDVar prototype, double minDistSum) {
    for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
      // Maximum distance of i to all elements in cy
      double distsum = distanceSum(dq, i, cy, 0., minDistSum);
      if(distsum >= minDistSum) {
        // We already have an at least equally good candidate.
        continue;
      }
      // Maximum distance of i to all elements in cx
      distsum = distanceSum(dq, i, cx, distsum, minDistSum);

      // New best solution?
      if(distsum < minDistSum) {
        minDistSum = distsum;
        prototype.set(i);
      }
    }
    return minDistSum;
  }

  /**
   * Find the prototypes.
   * 
   * @param dq Distance query
   * @param cx First set
   * @param cy Singleton object
   * @param prototype Prototype output variable
   * @return New best distance
   */
  private static double findPrototypeSingleton(DistanceQuery<?> dq, DBIDs cx, DBIDRef cy, DBIDVar prototype) {
    double sumDisty = 0., minDistSum = Double.POSITIVE_INFINITY;
    for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
      // Maximum distance of i to the element in cy
      double distsum = dq.distance(i, cy);
      // Maximum of distances from cy.
      sumDisty += distsum;
      if(distsum >= minDistSum) {
        // We know a better solution already.
        continue;
      }
      // Distance sum of i to all other elements in cx
      distsum = distanceSum(dq, i, cx, distsum, minDistSum);

      if(distsum < minDistSum) {
        minDistSum = distsum;
        prototype.set(i);
      }
    }
    // Singleton point.
    if(sumDisty < minDistSum) {
      minDistSum = sumDisty;
      prototype.set(cy);
    }
    return minDistSum;
  }

  /**
   * Find the maximum distance of one object to a set.
   *
   * @param dq Distance query
   * @param i Current object
   * @param cy Set of candidates
   * @param distsum Current sum
   * @param minDistSum Early stopping threshold
   * @return Distance sum
   */
  private static double distanceSum(DistanceQuery<?> dq, DBIDIter i, DBIDs cy, double distsum, double minDistSum) {
    for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {
      distsum += dq.distance(i, j);
      // Stop early, if we already know a better candidate.
      if(distsum >= minDistSum) {
        return distsum;
      }
    }
    return distsum;
  }

  /**
   * Update the cache.
   *
   * @param size Working set size
   * @param scratch Scratch matrix
   * @param bestd Best distance
   * @param besti Best index
   * @param x First cluster
   * @param y Second cluster, {@code y < x}
   * @param j Updated value d(y, j)
   * @param d New distance
   */
  private void updateCache(int size, double[] scratch, double[] bestd, int[] besti, int x, int y, int j, double d) {
    // New best
    if(d <= bestd[j]) {
      bestd[j] = d;
      besti[j] = y;
      return;
    }
    // Needs slow update.
    if(besti[j] == x || besti[j] == y) {
      findBest(size, scratch, bestd, besti, j);
    }
  }

  protected void findBest(int size, double[] scratch, double[] bestd, int[] besti, int j) {
    final int jbase = MatrixParadigm.triangleSize(j);
    // The distance has increased, we may no longer be the best merge.
    double bestdj = Double.POSITIVE_INFINITY;
    int bestij = -1;
    for(int i = 0, o = jbase; i < j; i++, o++) {
      if(besti[i] < 0) {
        continue;
      }
      final double dist = scratch[o];
      if(dist < bestdj) {
        bestdj = dist;
        bestij = i;
      }
    }
    for(int i = j + 1, o = jbase + j + j; i < size; o += i, i++) {
      // assert(o == MatrixParadigm.triangleSize(i) + j);
      if(besti[i] < 0) {
        continue;
      }
      final double dist = scratch[o];
      if(dist < bestdj) {
        bestdj = dist;
        bestij = i;
      }
    }
    bestd[j] = bestdj;
    besti[j] = bestij;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // The input relation must match our distance function:
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Variant of the algorithm to use
     */
    public static final OptionID VARIANT_ID = new OptionID("hacam.variant", "Variant of the algorithm to use.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Variant to use
     */
    protected Variant variant;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<Variant>(VARIANT_ID, Variant.class, Variant.MINIMUM_SUM_INCREASE) //
          .grab(config, x -> variant = x);
    }

    @Override
    public HACAM<O> make() {
      return new HACAM<>(distance, variant);
    }
  }
}
