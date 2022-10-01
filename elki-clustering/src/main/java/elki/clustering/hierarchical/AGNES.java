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
import elki.clustering.hierarchical.linkage.CentroidLinkage;
import elki.clustering.hierarchical.linkage.Linkage;
import elki.clustering.hierarchical.linkage.SingleLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Hierarchical Agglomerative Clustering (HAC) or Agglomerative Nesting (AGNES)
 * is a classic hierarchical clustering algorithm. Initially, each element is
 * its own cluster; the closest clusters are merged at every step, until all the
 * data has become a single cluster.
 * <p>
 * This is the naive O(n³) algorithm. See {@link SLINK} for a much faster
 * algorithm (however, only for single-linkage).
 * <p>
 * This implementation uses the pointer-based representation used by SLINK, so
 * that the extraction algorithms we have can be used with either of them.
 * <p>
 * The algorithm is believed to be first published (for single-linkage) by:
 * <p>
 * P. H. Sneath<br>
 * The application of computers to taxonomy<br>
 * Journal of general microbiology, 17(1).
 * <p>
 * This algorithm is also known as AGNES (Agglomerative Nesting), where the use
 * of alternative linkage criterions is discussed:
 * <p>
 * L. Kaufman, P. J. Rousseeuw<br>
 * Agglomerative Nesting (Program AGNES),<br>
 * in Finding Groups in Data: An Introduction to Cluster Analysis
 * <p>
 * Reference for the unified concept:
 * <p>
 * G. N. Lance, W. T. Williams<br>
 * A general theory of classificatory sorting strategies 1. Hierarchical
 * systems<br>
 * The computer journal 9.4 (1967): 373-380.
 * <p>
 * See also:
 * <p>
 * R. M. Cormack<br>
 * A Review of Classification<br>
 * Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - - - LinkageMethod
 * @composed - - - ClusterMergeHistoryBuilder
 *
 * @param <O> Object type
 */
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Agglomerative Nesting (Program AGNES)", //
    booktitle = "Finding Groups in Data: An Introduction to Cluster Analysis", //
    url = "https://doi.org/10.1002/9780470316801.ch5", //
    bibkey = "doi:10.1002/9780470316801.ch5")
@Reference(authors = "P. H. Sneath", //
    title = "The application of computers to taxonomy", //
    booktitle = "Journal of general microbiology, 17(1)", //
    url = "https://doi.org/10.1099/00221287-17-1-201", //
    bibkey = "doi:10.1099/00221287-17-1-201")
@Reference(authors = "R. M. Cormack", //
    title = "A Review of Classification", //
    booktitle = "Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3", //
    url = "https://doi.org/10.2307/2344237", //
    bibkey = "doi:10.2307/2344237")
@Alias({ "HAC", "SAHN" })
public class AGNES<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AGNES.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Current linkage method in use.
   */
  protected Linkage linkage = WardLinkage.STATIC;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param linkage Linkage method
   */
  public AGNES(Distance<? super O> distance, Linkage linkage) {
    super();
    this.distance = distance;
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    if(SingleLinkage.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Compute the initial (lower triangular) distance matrix.
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    ClusterDistanceMatrix mat = initializeDistanceMatrix(ids, dq, linkage);
    return new Instance(linkage).run(mat, new ClusterMergeHistoryBuilder(ids, distance.isSquared()));
  }

  /**
   * Initialize a distance matrix.
   *
   * @param ids Object ids
   * @param dq Distance query
   * @param linkage Linkage method
   * @return cluster distance matrix
   */
  protected static ClusterDistanceMatrix initializeDistanceMatrix(ArrayDBIDs ids, DistanceQuery<?> dq, Linkage linkage) {
    ClusterDistanceMatrix mat = new ClusterDistanceMatrix(ids.size());
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    final double[] matrix = mat.matrix;
    final boolean issquare = dq.getDistance().isSquared();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Distance matrix computation", matrix.length, LOG) : null;
    int pos = 0;
    for(ix.seek(1); ix.valid(); ix.advance()) {
      final int x = ix.getOffset();
      assert pos == ClusterDistanceMatrix.triangleSize(x);
      for(iy.seek(0); iy.getOffset() < x; iy.advance()) {
        matrix[pos++] = linkage.initial(dq.distance(ix, iy), issquare);
      }
      if(prog != null) {
        prog.setProcessed(pos, LOG);
      }
    }
    // Avoid logging errors in case scratch space was too large:
    if(prog != null) {
      prog.setProcessed(matrix.length, LOG);
    }
    LOG.ensureCompleted(prog);
    return mat;
  }

  /**
   * Main worker instance of AGNES.
   * 
   * @author Erich Schubert
   */
  public static class Instance {
    /**
     * Current linkage method in use.
     */
    protected Linkage linkage;

    /**
     * Cluster distance matrix
     */
    protected ClusterDistanceMatrix mat;

    /**
     * Cluster result builder
     */
    protected ClusterMergeHistoryBuilder builder;

    /**
     * Active set size
     */
    protected int end;

    /**
     * Constructor.
     *
     * @param linkage Linkage
     */
    public Instance(Linkage linkage) {
      this.linkage = linkage;
    }

    /**
     * Run the main algorithm.
     *
     * @param mat Distance matrix
     * @param builder Result builder
     * @return Cluster history
     */
    public ClusterMergeHistory run(ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder) {
      final int size = mat.size;
      this.mat = mat;
      this.builder = builder;
      this.end = size;
      // Repeat until everything merged into 1 cluster
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
      // Use end to shrink the matrix virtually as the tailing objects disappear
      for(int i = 1; i < size; i++) {
        end = shrinkActiveSet(mat.clustermap, end, findMerge());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      return builder.complete();
    }

    /**
     * Perform the next merge step in AGNES.
     *
     * @return the index that has disappeared, for shrinking the working set
     */
    protected int findMerge() {
      assert end > 0;
      final double[] matrix = mat.matrix;
      double mindist = Double.POSITIVE_INFINITY;
      int x = -1, y = -1;
      // Find minimum:
      for(int ox = 0, xbase = 0; ox < end; xbase += ox++) {
        // Skip if object has already joined a cluster:
        if(mat.clustermap[ox] < 0) {
          continue;
        }
        assert (xbase == ClusterDistanceMatrix.triangleSize(ox));
        for(int oy = 0; oy < ox; oy++) {
          // Skip if object has already joined a cluster:
          if(mat.clustermap[oy] < 0) {
            continue;
          }
          final double dist = matrix[xbase + oy];
          if(dist <= mindist) { // Prefer later on ==, to truncate more often.
            mindist = dist;
            x = ox;
            y = oy;
          }
        }
      }
      merge(mindist, x, y);
      return x;
    }

    /**
     * Execute the cluster merge.
     *
     * @param mindist Distance that was used for merging
     * @param x First matrix position
     * @param y Second matrix position
     */
    protected void merge(double mindist, int x, int y) {
      assert x >= 0 && y >= 0;
      assert y < x; // more efficient
      final int xx = mat.clustermap[x], yy = mat.clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      int zz = builder.strictAdd(xx, linkage.restore(mindist, builder.isSquared), yy);
      assert builder.getSize(zz) == sizex + sizey;
      // Since y < x, prefer keeping y, dropping x.
      mat.clustermap[y] = zz;
      mat.clustermap[x] = -1; // deactivate
      updateMatrix(mindist, x, y, sizex, sizey);
    }

    /**
     * Update the scratch distance matrix.
     *
     * @param mindist Minimum distance
     * @param x First matrix position
     * @param y Second matrix position
     * @param sizex Old size of first cluster
     * @param sizey Old size of second cluster
     */
    protected void updateMatrix(double mindist, int x, int y, final int sizex, final int sizey) {
      final int xbase = ClusterDistanceMatrix.triangleSize(x);
      final int ybase = ClusterDistanceMatrix.triangleSize(y);
      double[] scratch = mat.matrix;

      // Write to (y, j), with j < y
      int j = 0;
      for(; j < y; j++) {
        if(mat.clustermap[j] >= 0) {
          assert j < y; // Otherwise, ybase + j is the wrong position!
          final int yb = ybase + j;
          scratch[yb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[yb], builder.getSize(mat.clustermap[j]), mindist);
        }
      }
      j++; // Skip y
      // Write to (j, y), with y < j < x
      int jbase = ClusterDistanceMatrix.triangleSize(j);
      for(; j < x; jbase += j++) {
        if(mat.clustermap[j] >= 0) {
          final int jb = jbase + y;
          scratch[jb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jb], builder.getSize(mat.clustermap[j]), mindist);
        }
      }
      jbase += j++; // Skip x
      // Write to (j, y), with y < x < j
      for(; j < end; jbase += j++) {
        if(mat.clustermap[j] >= 0) {
          final int jb = jbase + y;
          scratch[jb] = linkage.combine(sizex, scratch[jbase + x], sizey, scratch[jb], builder.getSize(mat.clustermap[j]), mindist);
        }
      }
    }

    /**
     * Shrink the active set: if the last x objects are all merged, we can
     * reduce
     * the working size accordingly.
     * 
     * @param clustermap Map to current clusters
     * @param end Current active set size
     * @param x Last merged object
     * @return New active set size
     */
    protected static int shrinkActiveSet(int[] clustermap, int end, int x) {
      if(x == end - 1) { // Can truncate active set.
        while(clustermap[--end - 1] < 0) {
          // decrement happens in while condition already.
        }
      }
      return end;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
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
     * Option ID for linkage parameter.
     */
    public static final OptionID LINKAGE_ID = new OptionID("hierarchical.linkage", "Linkage method to use (e.g., Ward, Single-Link)");

    /**
     * Current linkage in use.
     */
    protected Linkage linkage;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Linkage>(LINKAGE_ID, Linkage.class) //
          .setDefaultValue(WardLinkage.class) //
          .grab(config, x -> linkage = x);
      Class<? extends Distance<?>> defaultD = (linkage instanceof WardLinkage || linkage instanceof CentroidLinkage) //
          ? SquaredEuclideanDistance.class : EuclideanDistance.class;
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, defaultD) //
          .grab(config, x -> distance = x);
    }

    @Override
    public AGNES<O> make() {
      return new AGNES<>(distance, linkage);
    }
  }
}
