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
package elki.clustering.hierarchical;

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
 * @since 0.8.0
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
   * Distance to use
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
  public ClusterPrototypeMergeHistory run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(ClusterDistanceMatrix.triangleSize(ids.size()));
    ClusterDistanceMatrix mat = MiniMax.initializeMatrices(ids, prots, dq);
    return new Instance(variant).run(ids, mat, new ClusterMergeHistoryBuilder(ids, dq.getDistance().isSquared()), dq, prots.iter());
  }

  /**
   * Main worker instance of AGNES.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends Anderberg.Instance {
    /**
     * Linkage variant to use
     */
    protected Variant variant;

    /**
     * Cluster to members map
     */
    protected Int2ObjectOpenHashMap<ModifiableDBIDs> clusters;

    /**
     * Total deviations (for minimum sum increase only)
     */
    protected double[] tds;

    /**
     * Distance query
     */
    protected DistanceQuery<?> dq;

    /**
     * Iterator into the prototypes
     */
    protected DBIDArrayMIter prots;

    /**
     * Iterators into the object ids.
     */
    protected DBIDArrayIter ix, iy;

    /**
     * Constructor.
     *
     * @param variant HACAM variant to use
     */
    public Instance(Variant variant) {
      super(null);
      this.variant = variant;
    }

    @Override
    public ClusterMergeHistory run(ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder) {
      throw new IllegalStateException("Need prototypes.");
    }

    /**
     * Run HACAM linkage
     * 
     * @param ids Object ids
     * @param mat Distance matrix
     * @param builder Result builder
     * @param dq Distance query
     * @param prots Iterator into prototypes
     * @return Cluster merge history
     */
    public ClusterPrototypeMergeHistory run(ArrayDBIDs ids, ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder, DistanceQuery<?> dq, DBIDArrayMIter prots) {
      final int size = mat.size;
      this.mat = mat;
      this.builder = builder;
      this.end = size;
      this.clusters = new Int2ObjectOpenHashMap<>(size);
      this.ix = ids.iter();
      this.iy = ids.iter();
      this.prots = prots;
      this.tds = variant == Variant.MINIMUM_SUM_INCREASE ? new double[size] : null;
      this.dq = dq;

      // Anderberg optimization
      this.bestd = new double[size];
      this.besti = new int[size];
      initializeNNCache(mat.matrix, bestd, besti);

      // Repeat until everything merged into 1 cluster
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("HACAM clustering", size - 1, LOG) : null;
      for(int i = 1; i < size; i++) {
        end = shrinkActiveSet(mat.clustermap, end, findMerge());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      return (ClusterPrototypeMergeHistory) builder.complete();
    }

    @Override
    protected int findMerge() {
      double mindist = Double.POSITIVE_INFINITY;
      int x = -1, y = -1;
      // Find minimum:
      for(int cx = 0; cx < end; cx++) {
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
      assert x >= 0 && y >= 0;
      if(y > x) {
        int tmp = x;
        x = y;
        y = tmp;
      }
      merge(x, y);
      return x;
    }

    /**
     * Execute the cluster merge.
     *
     * @param x first cluster to merge, with {@code x > y}
     * @param y second cluster to merge, with {@code y < x}
     */
    protected void merge(int x, int y) {
      assert x >= 0 && y >= 0;
      assert y < x; // We could swap otherwise, but this shouldn't arise.
      final double[] distances = mat.matrix;
      final int offset = ClusterDistanceMatrix.triangleSize(x) + y;
      ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);
      // Keep y
      if(cy == null) {
        cy = DBIDUtil.newArray();
        cy.add(iy.seek(y));
      }
      if(cx == null) {
        cy.add(ix.seek(x));
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
      final int xx = mat.clustermap[x], yy = mat.clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      // Since y < x, prefer keeping y, dropping x.
      int zz = builder.strictAdd(xx, distances[offset], yy, prots.seek(offset));
      assert builder.getSize(zz) == sizex + sizey;
      mat.clustermap[y] = zz;
      mat.clustermap[x] = -1; // Deactivate removed cluster
      besti[x] = -1; // Deactivate x in cache
      updateMatrices(x, y);
      if(besti[y] == x) {
        findBest(distances, bestd, besti, y);
      }
    }

    /**
     * Update the entries of the matrices that contain a distance to y, the
     * newly merged cluster.
     * 
     * @param x first cluster to merge, with {@code x > y}
     * @param y second cluster to merge, with {@code y < x}
     */
    private void updateMatrices(int x, int y) {
      final double[] distances = mat.matrix;
      // Update entries (at (a,b) with a > b) in the matrix where a = y or b = y
      // Update entries at (y,b) with b < y
      int a = y, b = 0;
      final int yoffset = ClusterDistanceMatrix.triangleSize(y);
      for(; b < a; b++) {
        // Skip entry if already merged
        if(mat.clustermap[b] < 0) {
          continue;
        }
        updateEntry(a, b);
        updateCache(distances, bestd, besti, x, y, b, distances[yoffset + b]);
      }
      // Update entries at (a,y) with a > y
      a = y + 1;
      b = y;
      for(; a < end; a++) {
        // Skip entry if already merged
        if(mat.clustermap[a] < 0) {
          continue;
        }
        updateEntry(a, b);
        updateCache(distances, bestd, besti, x, y, a, distances[ClusterDistanceMatrix.triangleSize(a) + y]);
      }
    }

    /**
     * Update entry at x,y for distance matrix distances
     * 
     * @param x index of cluster, {@code x > y}
     * @param y index of cluster, {@code y < x}
     */
    protected void updateEntry(int x, int y) {
      assert y < x;
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

      final int offset = ClusterDistanceMatrix.triangleSize(x) + y;
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
