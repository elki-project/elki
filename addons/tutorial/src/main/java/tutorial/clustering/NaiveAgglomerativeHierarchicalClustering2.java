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
package tutorial.clustering;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

/**
 * This tutorial will step you through implementing a well known clustering
 * algorithm, agglomerative hierarchical clustering, in multiple steps.
 * <p>
 * This is the second step, where we increase the performance of the algorithm
 * by using an improved linear memory layout instead of ragged arrays.
 * <p>
 * This is the naive O(n³) algorithm. See {@link SLINK} for a much faster
 * algorithm (however, only for single-linkage).
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type
 */
public class NaiveAgglomerativeHierarchicalClustering2<O> extends AbstractDistanceBasedAlgorithm<O, Result> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaiveAgglomerativeHierarchicalClustering2.class);

  /**
   * Threshold, how many clusters to extract.
   */
  int numclusters;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param numclusters Number of clusters
   */
  public NaiveAgglomerativeHierarchicalClustering2(DistanceFunction<? super O> distanceFunction, int numclusters) {
    super(distanceFunction);
    this.numclusters = numclusters;
  }

  /**
   * Run the algorithm
   *
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public Result run(Database db, Relation<O> relation) {
    DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    if(size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + 0x10000 + " instances (~17 GB RAM), which results in an integer overflow.");
    }
    LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[triangleSize(size)];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    // Position counter - must agree with computeOffset!
    int pos = 0;
    for(int x = 0; ix.valid(); x++, ix.advance()) {
      iy.seek(0);
      for(int y = 0; y < x; y++, iy.advance()) {
        scratch[pos] = dq.distance(ix, iy);
        pos++;
      }
    }

    // Initialize space for result:
    double[] height = new double[size];
    Arrays.fill(height, Double.POSITIVE_INFINITY);
    // Parent node, to track merges
    // have every object point to itself initially
    ArrayModifiableDBIDs parent = DBIDUtil.newArray(ids);
    // Active clusters, when not trivial.
    Int2ReferenceMap<ModifiableDBIDs> clusters = new Int2ReferenceOpenHashMap<>();

    // Repeat until everything merged, except the desired number of clusters:
    final int stop = size - numclusters;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", stop, LOG) : null;
    for(int i = 0; i < stop; i++) {
      double min = Double.POSITIVE_INFINITY;
      int minx = -1, miny = -1;
      for(int x = 0; x < size; x++) {
        if(height[x] < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int xbase = triangleSize(x);
        for(int y = 0; y < x; y++) {
          if(height[y] < Double.POSITIVE_INFINITY) {
            continue;
          }
          final int idx = xbase + y;
          if(scratch[idx] < min) {
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
      if(cy == null) {
        cy = DBIDUtil.newHashSet();
        cy.add(iy);
      }
      if(cx == null) {
        cy.add(ix);
      }
      else {
        cy.addDBIDs(cx);
        clusters.remove(minx);
      }
      clusters.put(miny, cy);
      // Update distance matrix. Note: miny < minx
      final int xbase = triangleSize(minx), ybase = triangleSize(miny);
      // Write to (y, j), with j < y
      for(int j = 0; j < miny; j++) {
        if(height[j] < Double.POSITIVE_INFINITY) {
          continue;
        }
        scratch[ybase + j] = Math.min(scratch[xbase + j], scratch[ybase + j]);
      }
      // Write to (j, y), with y < j < x
      for(int j = miny + 1; j < minx; j++) {
        if(height[j] < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int jbase = triangleSize(j);
        scratch[jbase + miny] = Math.min(scratch[xbase + j], scratch[jbase + miny]);
      }
      // Write to (j, y), with y < x < j
      for(int j = minx + 1; j < size; j++) {
        if(height[j] < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int jbase = triangleSize(j);
        scratch[jbase + miny] = Math.min(scratch[jbase + minx], scratch[jbase + miny]);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // Build the clustering result
    final Clustering<Model> dendrogram = new Clustering<>("Hierarchical-Clustering", "hierarchical-clustering");
    for(int x = 0; x < size; x++) {
      if(height[x] < Double.POSITIVE_INFINITY) {
        DBIDs cids = clusters.get(x);
        if(cids == null) {
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
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Desired number of clusters.
     */
    int numclusters = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter numclustersP = new IntParameter(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(numclustersP)) {
        numclusters = numclustersP.intValue();
      }
    }

    @Override
    protected NaiveAgglomerativeHierarchicalClustering2<O> makeInstance() {
      return new NaiveAgglomerativeHierarchicalClustering2<>(distanceFunction, numclusters);
    }
  }
}
