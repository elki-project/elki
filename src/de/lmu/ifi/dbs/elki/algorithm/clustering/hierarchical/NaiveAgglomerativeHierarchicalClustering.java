package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * This tutorial will step you through implementing a well known clustering
 * algorithm, agglomerative hierarchical clustering, in multiple steps.
 * 
 * This is the third step, where we add support for different linkage
 * strategies.
 * 
 * This is the naive O(n^3) algorithm. See {@link SLINK} for a much faster
 * algorithm (however, only for single-linkage).
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
 * A Review of Classification<br />
 * R. M. Cormack<br />
 * Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Reference(authors = "G. N. Lance and W. T. Williams", title = "A general theory of classificatory sorting strategies 1. Hierarchical systems", booktitle = "The computer journal 9.4", url = "http://dx.doi.org/ 10.1093/comjnl/9.4.373")
public class NaiveAgglomerativeHierarchicalClustering<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, Result> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaiveAgglomerativeHierarchicalClustering.class);

  /**
   * Threshold, how many clusters to extract.
   */
  int numclusters;

  /**
   * Current linkage method in use.
   */
  LinkageMethod linkage = WardLinkageMethod.STATIC;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to use
   * @param numclusters Number of clusters
   * @param linkage Linkage method
   */
  public NaiveAgglomerativeHierarchicalClustering(DistanceFunction<? super O, D> distanceFunction, int numclusters, LinkageMethod linkage) {
    super(distanceFunction);
    this.numclusters = numclusters;
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   * 
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public Result run(Database db, Relation<O> relation) {
    DistanceQuery<O, D> dq = db.getDistanceQuery(relation, getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    if (size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + 0x10000 + " instances (~17 GB RAM), which results in an integer overflow.");
    }
    if (SingleLinkageMethod.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[triangleSize(size)];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    // Position counter - must agree with computeOffset!
    int pos = 0;
    boolean square = WardLinkageMethod.class.isInstance(linkage) && !(SquaredEuclideanDistanceFunction.class.isInstance(getDistanceFunction()));
    for (int x = 0; ix.valid(); x++, ix.advance()) {
      iy.seek(0);
      for (int y = 0; y < x; y++, iy.advance()) {
        scratch[pos] = dq.distance(ix, iy).doubleValue();
        // Ward uses variances -- i.e. squared values
        if (square) {
          scratch[pos] *= scratch[pos];
        }
        pos++;
      }
    }

    // Initialize space for result:
    double[] height = new double[size];
    Arrays.fill(height, -1.);
    // Parent node, to track merges
    // have every object point to itself initially
    ArrayModifiableDBIDs parent = DBIDUtil.newArray(ids);
    // Active clusters, when not trivial.
    TIntObjectMap<ModifiableDBIDs> clusters = new TIntObjectHashMap<>();

    // Repeat until everything merged, except the desired number of clusters:
    final int stop = size - numclusters;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", stop, LOG) : null;
    for (int i = 0; i < stop; i++) {
      double min = Double.POSITIVE_INFINITY;
      int minx = -1, miny = -1;
      for (int x = 0; x < size; x++) {
        if (height[x] >= 0) {
          continue;
        }
        final int xbase = triangleSize(x);
        for (int y = 0; y < x; y++) {
          if (height[y] >= 0) {
            continue;
          }
          final int idx = xbase + y;
          if (scratch[idx] < min) {
            min = scratch[idx];
            minx = x;
            miny = y;
          }
        }
      }
      assert (minx >= 0 && miny >= 0);
      // Avoid allocating memory, by reusing existing iterators:
      ix.seek(minx);
      iy.seek(miny);
      // Perform merge in data structure: x -> y
      // Since y < x, prefer keeping y, dropping x.
      height[minx] = min;
      parent.set(minx, iy);
      // Merge into cluster
      ModifiableDBIDs cx = clusters.get(minx);
      ModifiableDBIDs cy = clusters.get(miny);
      int sizex = 1, sizey = 1; // cluster sizes, for averaging
      if (cy == null) {
        cy = DBIDUtil.newHashSet();
        cy.add(iy);
      } else {
        sizey = cy.size();
      }
      if (cx == null) {
        cy.add(ix);
      } else {
        sizex = cx.size();
        cy.addDBIDs(cx);
        clusters.remove(minx);
      }
      clusters.put(miny, cy);

      // Update distance matrix. Note: miny < minx

      // Implementation note: most will not need sizej, and could save the
      // hashmap lookup.
      final int xbase = triangleSize(minx), ybase = triangleSize(miny);
      // Write to (y, j), with j < y
      for (int j = 0; j < miny; j++) {
        if (height[j] < 0) {
          final DBIDs idsj = clusters.get(j);
          final int sizej = (idsj == null) ? 1 : idsj.size();
          scratch[ybase + j] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[ybase + j], sizej, min);
        }
      }
      // Write to (j, y), with y < j < x
      for (int j = miny + 1; j < minx; j++) {
        if (height[j] < 0) {
          final int jbase = triangleSize(j);
          final DBIDs idsj = clusters.get(j);
          final int sizej = (idsj == null) ? 1 : idsj.size();
          scratch[jbase + miny] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jbase + miny], sizej, min);
        }
      }
      // Write to (j, y), with y < x < j
      for (int j = minx + 1; j < size; j++) {
        if (height[j] < 0) {
          final DBIDs idsj = clusters.get(j);
          final int sizej = (idsj == null) ? 1 : idsj.size();
          final int jbase = triangleSize(j);
          scratch[jbase + miny] = linkage.combine(sizex, scratch[jbase + minx], sizey, scratch[jbase + miny], sizej, min);
        }
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
    }

    // Build the clustering result
    final Clustering<Model> dendrogram = new Clustering<>("Hierarchical-Clustering", "hierarchical-clustering");
    for (int x = 0; x < size; x++) {
      if (height[x] < 0) {
        DBIDs cids = clusters.get(x);
        if (cids == null) {
          ix.seek(x);
          cids = DBIDUtil.deref(ix);
        }
        Cluster<Model> cluster = new Cluster<>("Cluster", cids);
        dendrogram.addToplevelCluster(cluster);
      }
    }

    return dendrogram;
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
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * Option ID for linkage parameter.
     */
    private static final OptionID LINKAGE_ID = new OptionID("hierarchical.linkage", "Linkage method to use (e.g. Ward, Single-Link)");

    /**
     * Desired number of clusters.
     */
    int numclusters = 0;

    /**
     * Current linkage in use.
     */
    protected LinkageMethod linkage;

    @Override
    protected void makeOptions(Parameterization config) {
      // We don't call super, because we want a different default distance.
      ObjectParameter<DistanceFunction<O, D>> distanceFunctionP = makeParameterDistanceFunction(SquaredEuclideanDistanceFunction.class, DistanceFunction.class);
      if (config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      IntParameter numclustersP = new IntParameter(SLINK.Parameterizer.SLINK_MINCLUSTERS_ID);
      numclustersP.addConstraint(new GreaterEqualConstraint(1));
      if (config.grab(numclustersP)) {
        numclusters = numclustersP.intValue();
      }

      ObjectParameter<LinkageMethod> linkageP = new ObjectParameter<>(LINKAGE_ID, LinkageMethod.class);
      linkageP.setDefaultValue(WardLinkageMethod.class);
      if (config.grab(linkageP)) {
        linkage = linkageP.instantiateClass(config);
      }
    }

    @Override
    protected NaiveAgglomerativeHierarchicalClustering<O, D> makeInstance() {
      return new NaiveAgglomerativeHierarchicalClustering<>(distanceFunction, numclusters, linkage);
    }
  }
}
