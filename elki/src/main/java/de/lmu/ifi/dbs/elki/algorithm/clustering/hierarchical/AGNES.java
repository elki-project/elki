package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Hierarchical Agglomerative Clustering (HAC) or Agglomerative Nesting (AGNES)
 * is a classic hierarchical clustering algorithm. Initially, each element is
 * its own cluster; the closest clusters are merged at every step, until all the
 * data has become a single cluster.
 *
 * This is the naive O(n^3) algorithm. See {@link SLINK} for a much faster
 * algorithm (however, only for single-linkage).
 *
 * This implementation uses the pointer-based representation used by SLINK, so
 * that the extraction algorithms we have can be used with either of them.
 *
 * The algorithm is believed to be first published (for single-linkage) by:
 * <p>
 * P. H. Sneath<br />
 * The application of computers to taxonomy<br />
 * Journal of general microbiology, 17(1).
 * </p>
 *
 * This algorithm is also known as AGNES (Agglomerative Nesting), where the use
 * of alternative linkage criterions is discussed:
 * <p>
 * L. Kaufman and P. J. Rousseeuw<br />
 * Agglomerative Nesting (Program AGNES),<br />
 * in Finding Groups in Data: An Introduction to Cluster Analysis
 * </p>
 *
 * Reference for the unified concept:
 * <p>
 * G. N. Lance and W. T. Williams<br />
 * A general theory of classificatory sorting strategies 1. Hierarchical systems
 * <br/>
 * The computer journal 9.4 (1967): 373-380.
 * </p>
 *
 * See also:
 * <p>
 * R. M. Cormack<br />
 * A Review of Classification<br />
 * Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3
 * </p>
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.composedOf LinkageMethod
 * @apiviz.composedOf PointerHierarchyRepresentationBuilder
 *
 * @param <O> Object type
 */
@Reference(authors = "L. Kaufman and P. J. Rousseeuw", //
title = "Agglomerative Nesting (Program AGNES)", //
booktitle = "Finding Groups in Data: An Introduction to Cluster Analysis", //
url = "http://dx.doi.org/10.1002/9780470316801.ch5")
@Alias({ "HAC", "NaiveAgglomerativeHierarchicalClustering", //
"de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.NaiveAgglomerativeHierarchicalClustering" })
public class AGNES<O> extends AbstractDistanceBasedAlgorithm<O, PointerHierarchyRepresentationResult>implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AGNES.class);

  /**
   * Current linkage method in use.
   */
  LinkageMethod linkage = WardLinkageMethod.STATIC;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param linkage Linkage method
   */
  public AGNES(DistanceFunction<? super O> distanceFunction, LinkageMethod linkage) {
    super(distanceFunction);
    this.linkage = linkage;
  }

  /**
   * Additional historical reference for single-linkage.
   */
  @Reference(authors = "P. H. Sneath", //
  title = "The application of computers to taxonomy", //
  booktitle = "Journal of general microbiology, 17(1)", //
  url = "http://dx.doi.org/10.1099/00221287-17-1-201")
  public static final Void ADDITIONAL_REFERENCE = null;

  /**
   * Run the algorithm
   *
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult run(Database db, Relation<O> relation) {
    DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    if(size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + //
      0x10000 // = 65535
      + " instances (~16 GB RAM), at which point the Java maximum array size is reached.");
    }
    if(SingleLinkageMethod.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[triangleSize(size)];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    boolean square = WardLinkageMethod.class.isInstance(linkage) && !(SquaredEuclideanDistanceFunction.class.isInstance(getDistanceFunction()));
    initializeDistanceMatrix(scratch, dq, ix, iy, square);

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids);

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    int wsize = size;
    for(int i = 1; i < size; i++) {
      int x = findMerge(wsize, scratch, ix, iy, builder);
      if(x == wsize - 1) {
        --wsize;
        for(ix.seek(wsize - 1); builder.isLinked(ix); ix.retract()) {
          --wsize;
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    return builder.complete();
  }

  /**
   * Compute the size of a complete x by x triangle (minus diagonal)
   *
   * @param x Offset
   * @return Size of complete triangle
   */
  protected static int triangleSize(int x) {
    return (x * (x - 1)) >>> 1;
  }

  /**
   * Initialize a distance matrix.
   *
   * @param scratch Scratch space to be used.
   * @param dq Distance query
   * @param ix Data iterator
   * @param iy Data iterator
   * @param square Flag to use squared distances.
   */
  protected static <O> void initializeDistanceMatrix(double[] scratch, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy, boolean square) {
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        double dist = dq.distance(ix, iy);
        // Ward uses variances -- i.e. squared values
        dist = square ? (dist * dist) : dist;
        scratch[pos] = dist;
        pos++;
      }
    }
  }

  /**
   * Perform the next merge step in AGNES.
   *
   * @param size Data set size
   * @param scratch Scratch space.
   * @param ix First iterator
   * @param iy Second iterator
   * @param builder Pointer representation builder
   * @return x, for shrinking the working set.
   */
  protected int findMerge(int size, double[] scratch, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder) {
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;
    // Find minimum:
    for(int ox = 0, xbase = 0; ox < size; xbase += ox++) {
      // Skip if object has already joined a cluster:
      if(builder.isLinked(ix.seek(ox))) {
        continue;
      }
      assert(xbase == triangleSize(ox));
      for(int oy = 0; oy < ox; oy++) {
        // Skip if object has already joined a cluster:
        if(builder.isLinked(iy.seek(oy))) {
          continue;
        }
        final int idx = xbase + oy;
        if(scratch[idx] <= mindist) {
          mindist = scratch[idx];
          x = ox;
          y = oy;
        }
      }
    }
    assert(x >= 0 && y >= 0);
    merge(size, scratch, ix, iy, builder, mindist, x, y);
    return x;
  }

  /**
   * Execute the cluster merge.
   *
   * @param size Data set size
   * @param scratch Scratch space.
   * @param ix First iterator
   * @param iy Second iterator
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   */
  protected void merge(int size, double[] scratch, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y) {
    // Avoid allocating memory, by reusing existing iterators:
    ix.seek(x);
    iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + mindist);
    }
    // Perform merge in data structure: x -> y
    assert(y < x);
    // Since y < x, prefer keeping y, dropping x.
    builder.add(ix, mindist, iy);
    // Update cluster size for y:
    final int sizex = builder.getSize(ix), sizey = builder.getSize(iy);
    builder.setSize(iy, sizex + sizey);

    // Note: this changes iy.
    updateMatrix(size, scratch, iy, builder, mindist, x, y, sizex, sizey);
  }

  /**
   * Update the scratch distance matrix.
   *
   * @param size Data set size
   * @param scratch Scratch matrix.
   * @param ij Iterator to reuse
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   * @param sizex Old size of first cluster
   * @param sizey Old size of second cluster
   */
  protected void updateMatrix(int size, double[] scratch, DBIDArrayIter ij, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y, final int sizex, final int sizey) {
    // Update distance matrix. Note: miny < minx
    final int xbase = triangleSize(x), ybase = triangleSize(y);

    // Write to (y, j), with j < y
    int j = 0;
    for(; j < y; j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int sizej = builder.getSize(ij);
      final int yb = ybase + j;
      scratch[yb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[yb], sizej, mindist);
    }
    j++; // Skip y
    // Write to (j, y), with y < j < x
    int jbase = triangleSize(j);
    for(; j < x; jbase += j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int sizej = builder.getSize(ij);
      final int jb = jbase + y;
      scratch[jb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jb], sizej, mindist);
    }
    jbase += j++; // Skip x
    // Write to (j, y), with y < x < j
    for(; j < size; jbase += j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int sizej = builder.getSize(ij);
      scratch[jbase + y] = linkage.combine(sizex, scratch[jbase + x], sizey, scratch[jbase + y], sizej, mindist);
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
   * @apiviz.exclude
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
    protected LinkageMethod linkage;

    @Override
    protected void makeOptions(Parameterization config) {
      // We don't call super, because we want a different default distance.
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = makeParameterDistanceFunction(SquaredEuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      ObjectParameter<LinkageMethod> linkageP = new ObjectParameter<>(LINKAGE_ID, LinkageMethod.class);
      linkageP.setDefaultValue(WardLinkageMethod.class);
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
