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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage.Linkage;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage.SingleLinkage;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage.WardLinkage;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @composed - - - PointerHierarchyRepresentationBuilder
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
@Alias({ "HAC", "NaiveAgglomerativeHierarchicalClustering", //
    "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.NaiveAgglomerativeHierarchicalClustering" })
public class AGNES<O> extends AbstractDistanceBasedAlgorithm<O, PointerHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AGNES.class);

  /**
   * Current linkage method in use.
   */
  Linkage linkage = WardLinkage.STATIC;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param linkage Linkage method
   */
  public AGNES(DistanceFunction<? super O> distanceFunction, Linkage linkage) {
    super(distanceFunction);
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   *
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult run(Database db, Relation<O> relation) {
    if(SingleLinkage.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }
    final DBIDs ids = relation.getDBIDs();
    final int size = ids.size();
    DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());

    // Compute the initial (lower triangular) distance matrix.
    MatrixParadigm mat = new MatrixParadigm(ids);
    initializeDistanceMatrix(mat, dq, linkage);

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistanceFunction().isSquared());

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    // Use end to shrink the matrix virtually as the tailing objects disappear
    DBIDArrayIter ix = mat.ix;
    for(int i = 1, end = size; i < size; i++) {
      end = shrinkActiveSet(ix, builder, end, //
          findMerge(end, mat, builder));
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    return builder.complete();
  }

  /**
   * Shrink the active set: if the last x objects are all merged, we can reduce
   * the working size accordingly.
   * 
   * @param ix Object iterator
   * @param builder Builder to detect merged status
   * @param end Current active set size
   * @param x Last merged object
   * @return New active set size
   */
  protected static int shrinkActiveSet(DBIDArrayIter ix, PointerHierarchyRepresentationBuilder builder, int end, int x) {
    if(x == end - 1) { // Can truncate active set.
      while(builder.isLinked(ix.seek(--end - 1))) {
        // Everything happens in while condition already.
      }
    }
    return end;
  }

  /**
   * Initialize a distance matrix.
   *
   * @param mat Matrix
   * @param dq Distance query
   * @param linkage Linkage method
   */
  protected static void initializeDistanceMatrix(MatrixParadigm mat, DistanceQuery<?> dq, Linkage linkage) {
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] matrix = mat.matrix;
    final boolean issquare = dq.getDistanceFunction().isSquared();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Distance matrix computation", matrix.length, LOG) : null;
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      final int x = ix.getOffset();
      assert (pos == MatrixParadigm.triangleSize(x));
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
  }

  /**
   * Perform the next merge step in AGNES.
   *
   * @param end Active set size
   * @param mat Matrix storage
   * @param builder Pointer representation builder
   * @return the index that has disappeared, for shrinking the working set
   */
  protected int findMerge(int end, MatrixParadigm mat, PointerHierarchyRepresentationBuilder builder) {
    assert (end > 0);
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] matrix = mat.matrix;
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;
    // Find minimum:
    for(int ox = 0, xbase = 0; ox < end; xbase += ox++) {
      // Skip if object has already joined a cluster:
      if(builder.isLinked(ix.seek(ox))) {
        continue;
      }
      assert (xbase == MatrixParadigm.triangleSize(ox));
      for(int oy = 0; oy < ox; oy++) {
        // Skip if object has already joined a cluster:
        if(builder.isLinked(iy.seek(oy))) {
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
    assert (x >= 0 && y >= 0);
    assert (y < x); // We could swap otherwise, but this shouldn't arise.
    merge(end, mat, builder, mindist, x, y);
    return x;
  }

  /**
   * Execute the cluster merge.
   *
   * @param end Active set size
   * @param mat Matrix paradigm
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   */
  protected void merge(int end, MatrixParadigm mat, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y) {
    // Avoid allocating memory, by reusing existing iterators:
    final DBIDArrayIter ix = mat.ix.seek(x), iy = mat.iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + mindist);
    }
    // Perform merge in data structure: x -> y
    assert (y < x);
    // Since y < x, prefer keeping y, dropping x.
    builder.add(ix, linkage.restore(mindist, getDistanceFunction().isSquared()), iy);
    // Update cluster size for y:
    final int sizex = builder.getSize(ix), sizey = builder.getSize(iy);
    builder.setSize(iy, sizex + sizey);
    updateMatrix(end, mat, builder, mindist, x, y, sizex, sizey);
  }

  /**
   * Update the scratch distance matrix.
   *
   * @param end Active set size
   * @param mat Matrix view
   * @param builder Hierarchy builder (to get cluster sizes)
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   * @param sizex Old size of first cluster
   * @param sizey Old size of second cluster
   */
  protected void updateMatrix(int end, MatrixParadigm mat, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y, final int sizex, final int sizey) {
    // Update distance matrix. Note: y < x
    final int xbase = MatrixParadigm.triangleSize(x);
    final int ybase = MatrixParadigm.triangleSize(y);
    double[] scratch = mat.matrix;
    DBIDArrayIter ij = mat.ix;

    // Write to (y, j), with j < y
    int j = 0;
    for(; j < y; j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      assert (j < y); // Otherwise, ybase + j is the wrong position!
      final int yb = ybase + j;
      scratch[yb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[yb], builder.getSize(ij), mindist);
    }
    j++; // Skip y
    // Write to (j, y), with y < j < x
    int jbase = MatrixParadigm.triangleSize(j);
    for(; j < x; jbase += j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int jb = jbase + y;
      scratch[jb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jb], builder.getSize(ij), mindist);
    }
    jbase += j++; // Skip x
    // Write to (j, y), with y < x < j
    for(; j < end; jbase += j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int jb = jbase + y;
      scratch[jb] = linkage.combine(sizex, scratch[jbase + x], sizey, scratch[jb], builder.getSize(ij), mindist);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // The input relation must match our distance function:
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option ID for linkage parameter.
     */
    public static final OptionID LINKAGE_ID = new OptionID("hierarchical.linkage", "Linkage method to use (e.g. Ward, Single-Link)");

    /**
     * Current linkage in use.
     */
    protected Linkage linkage;

    @Override
    protected void makeOptions(Parameterization config) {
      // We don't call super, because we want a different default distance.
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      ObjectParameter<Linkage> linkageP = new ObjectParameter<>(LINKAGE_ID, Linkage.class);
      linkageP.setDefaultValue(WardLinkage.class);
      if(config.grab(linkageP)) {
        linkage = linkageP.instantiateClass(config);
      }
    }

    @Override
    protected AGNES<O> makeInstance() {
      return new AGNES<>(distanceFunction, linkage);
    }
  }
}
