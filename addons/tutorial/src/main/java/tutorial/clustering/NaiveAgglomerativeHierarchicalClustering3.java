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

import elki.AbstractDistanceBasedAlgorithm;
import elki.clustering.hierarchical.SLINK;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

/**
 * This tutorial will step you through implementing a well known clustering
 * algorithm, agglomerative hierarchical clustering, in multiple steps.
 * <p>
 * This is the third step, where we add support for different linkage
 * strategies.
 * <p>
 * This is the naive O(n³) algorithm. See {@link SLINK} for a much faster
 * algorithm (however, only for single-linkage).
 * <p>
 * Reference (for the update formulas):
 * <p>
 * R. M. Cormack<br>
 * A Review of Classification<br>
 * Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type
 */
@Reference(authors = "R. M. Cormack", //
    title = "A Review of Classification", //
    booktitle = "Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3", //
    url = "https://doi.org/10.2307/2344237", //
    bibkey = "doi:10.2307/2344237")
public class NaiveAgglomerativeHierarchicalClustering3<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, Clustering<Model>> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaiveAgglomerativeHierarchicalClustering3.class);

  /**
   * Different linkage strategies.
   * <p>
   * The update formulas here come from:<br>
   * R. M. Cormack, A Review of Classification
   *
   * @author Erich Schubert
   */
  public enum Linkage {//
    SINGLE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return Math.min(dx, dy);
      }
    }, // single-linkage hierarchical clustering
    COMPLETE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return Math.max(dx, dy);
      }
    }, // complete-linkage hierarchical clustering
    GROUP_AVERAGE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        final double wx = sizex / (double) (sizex + sizey);
        final double wy = sizey / (double) (sizex + sizey);
        return wx * dx + wy * dy;
      }
    }, // average-linkage hierarchical clustering
    WEIGHTED_AVERAGE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return .5 * (dx + dy);
      }
    }, // a more naive variant, McQuitty (1966)
    CENTROID {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        final double wx = sizex / (double) (sizex + sizey);
        final double wy = sizey / (double) (sizex + sizey);
        final double beta = (sizex * sizey) / (double) ((sizex + sizey) * (sizex + sizey));
        return wx * dx + wy * dy - beta * dxy;
      }
    }, // Sokal and Michener (1958), Gower (1967)
    MEDIAN {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return .5 * (dx + dy) - .25 * dxy;
      }
    }, // Gower (1967)
    WARD {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        final double wx = (sizex + sizej) / (double) (sizex + sizey + sizej);
        final double wy = (sizey + sizej) / (double) (sizex + sizey + sizej);
        final double beta = sizej / (double) (sizex + sizey + sizej);
        return wx * dx + wy * dy - beta * dxy;
      }
    }, // Minimum Variance, Wishart (1969), Anderson (1971)
    ;

    abstract public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy);
  }

  /**
   * Threshold, how many clusters to extract.
   */
  int numclusters;

  /**
   * Current linkage in use.
   */
  Linkage linkage = Linkage.WARD;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param numclusters Number of clusters
   * @param linkage Linkage strategy
   */
  public NaiveAgglomerativeHierarchicalClustering3(Distance<? super O> distance, int numclusters, Linkage linkage) {
    super(distance);
    this.numclusters = numclusters;
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public Clustering<Model> run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    if(size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + 0x10000 + " instances (~17 GB RAM), which results in an integer overflow.");
    }
    if(Linkage.SINGLE.equals(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[triangleSize(size)];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    // Position counter - must agree with computeOffset!
    int pos = 0;
    boolean square = Linkage.WARD.equals(linkage) && !getDistance().isSquared();
    for(int x = 0; ix.valid(); x++, ix.advance()) {
      iy.seek(0);
      for(int y = 0; y < x; y++, iy.advance()) {
        scratch[pos] = dq.distance(ix, iy);
        // Ward uses variances -- i.e. squared values
        if(square) {
          scratch[pos] *= scratch[pos];
        }
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
      int sizex = 1, sizey = 1; // cluster sizes, for averaging
      if(cy == null) {
        cy = DBIDUtil.newHashSet();
        cy.add(iy);
      }
      else {
        sizey = cy.size();
      }
      if(cx == null) {
        cy.add(ix);
      }
      else {
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
      for(int j = 0; j < miny; j++) {
        if(height[j] < Double.POSITIVE_INFINITY) {
          continue;
        }
        final DBIDs idsj = clusters.get(j);
        final int sizej = (idsj == null) ? 1 : idsj.size();
        scratch[ybase + j] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[ybase + j], sizej, min);
      }
      // Write to (j, y), with y < j < x
      for(int j = miny + 1; j < minx; j++) {
        if(height[j] < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int jbase = triangleSize(j);
        final DBIDs idsj = clusters.get(j);
        final int sizej = (idsj == null) ? 1 : idsj.size();
        scratch[jbase + miny] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jbase + miny], sizej, min);
      }
      // Write to (j, y), with y < x < j
      for(int j = minx + 1; j < size; j++) {
        if(height[j] < Double.POSITIVE_INFINITY) {
          continue;
        }
        final DBIDs idsj = clusters.get(j);
        final int sizej = (idsj == null) ? 1 : idsj.size();
        final int jbase = triangleSize(j);
        scratch[jbase + miny] = linkage.combine(sizex, scratch[jbase + minx], sizey, scratch[jbase + miny], sizej, min);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // Build the clustering result
    final Clustering<Model> dendrogram = new Clustering<>();
    Metadata.of(dendrogram).setLongName("Hierarchical-Clustering");
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
    return TypeUtil.array(getDistance().getInputTypeRestriction());
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
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Option ID for linkage parameter.
     */
    private static final OptionID LINKAGE_ID = new OptionID("hierarchical.linkage", "Parameter to choose the linkage strategy.");

    /**
     * Desired number of clusters.
     */
    int numclusters = 0;

    /**
     * Current linkage in use.
     */
    protected Linkage linkage = Linkage.SINGLE;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> numclusters = x);
      new EnumParameter<Linkage>(LINKAGE_ID, Linkage.class) //
          .setDefaultValue(Linkage.WARD) //
          .grab(config, x -> linkage = x);
    }

    @Override
    public NaiveAgglomerativeHierarchicalClustering3<O> make() {
      return new NaiveAgglomerativeHierarchicalClustering3<>(distance, numclusters, linkage);
    }
  }
}
