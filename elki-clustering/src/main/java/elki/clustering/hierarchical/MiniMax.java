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
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Minimax Linkage clustering.
 * <p>
 * Reference:
 * <p>
 * S. I. Ao, K. Yip, M. Ng, D. Cheung, P.-Y. Fong, I. Melhado, P. C. Sham<br>
 * CLUSTAG: hierarchical clustering and graph methods for selecting tag SNPs<br>
 * Bioinformatics, 21 (8)
 * <p>
 * J. Bien and R. Tibshirani<br>
 * Hierarchical Clustering with Prototypes via Minimax Linkage<br>
 * Journal of the American Statistical Association 106(495)
 *
 * @author Julian Erhard
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - PointerPrototypeHierarchyResult
 *
 * @param <O> Object type
 */
@Reference(authors = "S. I. Ao, K. Yip, M. Ng, D. Cheung, P.-Y. Fong, I. Melhado, P. C. Sham", //
    title = "CLUSTAG: hierarchical clustering and graph methods for selecting tag SNPs", //
    booktitle = "Bioinformatics, 21 (8)", //
    url = "https://doi.org/10.1093/bioinformatics/bti201", //
    bibkey = "DBLP:journals/bioinformatics/AoYNCFMS05")
@Reference(authors = "J. Bien, R. Tibshirani", //
    title = "Hierarchical Clustering with Prototypes via Minimax Linkage", //
    booktitle = "Journal of the American Statistical Association 106(495)", //
    url = "https://doi.org/10.1198/jasa.2011.tm10183", //
    bibkey = "doi:10.1198/jasa.2011.tm10183")
public class MiniMax<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class Logger.
   */
  private static final Logging LOG = Logging.getLogger(MiniMax.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function to use.
   */
  public MiniMax(Distance<? super O> distance) {
    super();
    this.distance = distance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the algorithm on a database.
   *
   * @param relation Relation to process.
   * @return Hierarchical result
   */
  public ClusterPrototypeMergeHistory run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(ClusterDistanceMatrix.triangleSize(ids.size()));
    ClusterDistanceMatrix mat = initializeMatrices(ids, prots, dq);
    return new Instance().run(ids, mat, new ClusterMergeHistoryBuilder(ids, dq.getDistance().isSquared()), dq, prots.iter());
  }

  /**
   * Initializes the inter-cluster distance matrix of possible merges
   * 
   * @param ids Object ids
   * @param prots Prototype storage
   * @param dq The distance query
   * @return mat Cluster distance matrix
   */
  protected static <O> ClusterDistanceMatrix initializeMatrices(ArrayDBIDs ids, ArrayModifiableDBIDs prots, DistanceQuery<O> dq) {
    ClusterDistanceMatrix mat = new ClusterDistanceMatrix(ids.size());
    final DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    final double[] distances = mat.matrix;
    int pos = 0;
    for(ix.seek(1); ix.valid(); ix.advance()) {
      final int x = ix.getOffset();
      assert pos == ClusterDistanceMatrix.triangleSize(x);
      for(iy.seek(0); iy.getOffset() < x; iy.advance()) {
        distances[pos++] = dq.distance(ix, iy);
        prots.add(iy);
      }
    }
    assert prots.size() == pos;
    return mat;
  }

  /**
   * Main worker instance of MiniMax.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AGNES.Instance {
    /**
     * Map to cluster members
     */
    protected Int2ObjectOpenHashMap<ModifiableDBIDs> clusters;

    /**
     * Iterator into prototype cache
     */
    protected DBIDArrayMIter protiter;

    /**
     * Distance query function
     */
    protected DistanceQuery<?> dq;

    /**
     * Iterators into the object ids.
     */
    protected DBIDArrayIter ix, iy;

    /**
     * Constructor.
     */
    public Instance() {
      super(null);
    }

    @Override
    public ClusterMergeHistory run(ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder) {
      throw new IllegalStateException("Need prototypes.");
    }

    public ClusterPrototypeMergeHistory run(ArrayDBIDs ids, ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder, DistanceQuery<?> dq, DBIDArrayMIter prots) {
      final int size = mat.size;
      this.mat = mat;
      this.builder = builder;
      this.end = size;
      this.clusters = new Int2ObjectOpenHashMap<>(size);
      this.protiter = prots;
      this.dq = dq;
      this.ix = ids.iter();
      this.iy = ids.iter();
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("MiniMax clustering", size - 1, LOG) : null;
      for(int i = 1; i < size; i++) {
        end = shrinkActiveSet(mat.clustermap, end, findMerge());
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      return (ClusterPrototypeMergeHistory) builder.complete();
    }

    @Override
    protected int findMerge() {
      final double[] distances = mat.matrix;
      double mindist = Double.POSITIVE_INFINITY;
      int x = -1, y = -1;

      for(int dx = 0; dx < end; dx++) {
        // Skip if object is already linked
        if(mat.clustermap[dx] < 0) {
          continue;
        }
        final int xoffset = ClusterDistanceMatrix.triangleSize(dx);

        for(int dy = 0; dy < dx; dy++) {
          // Skip if object is already linked
          if(mat.clustermap[dy] < 0) {
            continue;
          }

          double dist = distances[xoffset + dy];
          if(dist < mindist) {
            mindist = dist;
            x = dx;
            y = dy;
          }
        }
      }
      merge(x, y);
      return x;
    }

    /**
     * Merges two clusters given by x, y, their points with smallest IDs, and y
     * to keep
     * 
     * @param x first cluster to merge
     * @param y second cluster to merge
     */
    protected void merge(int x, int y) {
      assert x >= 0 && y >= 0;
      assert y < x;
      final double[] distances = mat.matrix;
      final int offset = ClusterDistanceMatrix.triangleSize(x) + y;
      ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);
      // Keep y
      if(cy == null) {
        cy = DBIDUtil.newHashSet();
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

      // parent of x is set to y
      final int xx = mat.clustermap[x], yy = mat.clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      int zz = builder.strictAdd(xx, distances[offset], yy, protiter.seek(offset));
      assert builder.getSize(zz) == sizex + sizey;
      mat.clustermap[y] = zz;
      mat.clustermap[x] = -1; // Deactivate removed cluster.
      updateMatrices(y);
    }

    /**
     * Update the entries of the matrices that contain a distance to c, the
     * newly merged cluster.
     * 
     * @param c the cluster to update distances to
     */
    protected void updateMatrices(int c) {
      // Update entries (at (x,y) with x > y) in the matrix where x = c or y = c
      for(int y = 0; y < c; y++) {
        if(mat.clustermap[y] < 0) { // skip disabled
          continue;
        }
        updateEntry(c, y);
      }
      // Update entries at (x,c) with x > c
      for(int x = c + 1; x < end; x++) {
        if(mat.clustermap[x] < 0) { // skip disabled
          continue;
        }
        updateEntry(x, c);
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
      if(cx != null && cy != null) { // two "real" clusters:
        minMaxDist = findPrototype(dq, cx, cy, prototype, Double.POSITIVE_INFINITY);
        minMaxDist = findPrototype(dq, cy, cx, prototype, minMaxDist);
      }
      else if(cx != null) { // cy is singleton.
        minMaxDist = findPrototypeSingleton(dq, cx, iy.seek(y), prototype);
      }
      else if(cy != null) { // cx is singleton.
        minMaxDist = findPrototypeSingleton(dq, cy, ix.seek(x), prototype);
      }
      else {
        minMaxDist = dq.distance(ix.seek(x), iy.seek(y));
        prototype.set(ix);
      }

      final int offset = ClusterDistanceMatrix.triangleSize(x) + y;
      distances[offset] = minMaxDist;
      protiter.seek(offset).setDBID(prototype);
    }

    /**
     * Find the prototypes.
     * 
     * @param dq Distance query
     * @param cx First set
     * @param cy Second set
     * @param prototype Prototype output variable
     * @param minMaxDist Previously best distance.
     * @return New best distance
     */
    private static double findPrototype(DistanceQuery<?> dq, DBIDs cx, DBIDs cy, DBIDVar prototype, double minMaxDist) {
      for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
        // Maximum distance of i to all elements in cy
        double maxDist = findMax(dq, i, cy, 0., minMaxDist);
        if(maxDist >= minMaxDist) {
          // We already have an at least equally good candidate.
          continue;
        }
        // Maximum distance of i to all elements in cx
        maxDist = findMax(dq, i, cx, maxDist, minMaxDist);

        // New best solution?
        if(maxDist < minMaxDist) {
          minMaxDist = maxDist;
          prototype.set(i);
        }
      }
      return minMaxDist;
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
      double maxDisty = 0., minMaxDist = Double.POSITIVE_INFINITY;
      for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
        // Maximum distance of i to the element in cy
        double maxDist = dq.distance(i, cy);
        // Maximum of distances from cy.
        maxDisty = (maxDist > maxDisty) ? maxDist : maxDisty;
        if(maxDist >= minMaxDist) {
          // We know a better solution already.
          continue;
        }
        // Maximum distance of i to all other elements in cx
        maxDist = findMax(dq, i, cx, maxDist, minMaxDist);

        if(maxDist < minMaxDist) {
          minMaxDist = maxDist;
          prototype.set(i);
        }
      }
      // Singleton point.
      if(maxDisty < minMaxDist) {
        minMaxDist = maxDisty;
        prototype.set(cy);
      }
      return minMaxDist;
    }

    /**
     * Find the maximum distance of one object to a set.
     *
     * @param dq Distance query
     * @param i Current object
     * @param cy Set of candidates
     * @param maxDist Known maximum to others
     * @param minMaxDist Early stopping threshold
     * @return Maximum distance
     */
    private static double findMax(DistanceQuery<?> dq, DBIDIter i, DBIDs cy, double maxDist, double minMaxDist) {
      for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {
        double dist = dq.distance(i, j);
        if(dist > maxDist) {
          // Stop early, if we already know a better candidate.
          if(dist >= minMaxDist) {
            return dist;
          }
          maxDist = dist;
        }
      }
      return maxDist;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Julian Erhard
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
    public MiniMax<O> make() {
      return new MiniMax<>(distance);
    }
  }
}
