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
package tutorial.clustering;

import elki.Algorithm;
import elki.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.clustering.hierarchical.ClusterMergeHistoryBuilder;
import elki.clustering.hierarchical.SLINK;
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
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class NaiveAgglomerativeHierarchicalClustering4<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaiveAgglomerativeHierarchicalClustering4.class);

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

    public abstract double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy);
  }

  /**
   * Distance function used.
   */
  Distance<? super O> distance;

  /**
   * Current linkage in use.
   */
  Linkage linkage = Linkage.WARD;

  /**
   * Constructor.
   * 
   * @param distance Distance function to use
   * @param linkage Linkage strategy
   */
  public NaiveAgglomerativeHierarchicalClustering4(Distance<? super O> distance, Linkage linkage) {
    super();
    this.distance = distance;
    this.linkage = linkage;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // The input relation must match our distance function:
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public ClusterMergeHistory run(Relation<O> relation) {
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
    boolean square = Linkage.WARD.equals(linkage) && !distance.isSquared();
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

    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, distance.isSquared());
    int[] cidx = MathUtil.sequence(0, size);
    // Repeat until everything merged, except the desired number of clusters:
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    for(int i = 1; i < size; i++) {
      double min = Double.POSITIVE_INFINITY;
      int minx = -1, miny = -1;
      for(int x = 1; x < size; x++) {
        if(cidx[x] >= 0) {
          final int xbase = triangleSize(x);
          for(int y = 0; y < x; y++) {
            if(cidx[y] >= 0) {
              final int idx = xbase + y;
              if(scratch[idx] <= min) {
                min = scratch[idx];
                minx = x;
                miny = y;
              }
            }
          }
        }
      }
      assert minx >= 0 && miny >= 0;
      // Perform merge in data structure: x -> y
      // Since y < x, prefer keeping y, dropping x.
      final int sizex = builder.getSize(minx), sizey = builder.getSize(miny);

      // Update distance matrix. Note: miny < minx
      final int xbase = triangleSize(minx), ybase = triangleSize(miny);
      // Write to (y, j), with j < y
      for(int j = 0; j < miny; j++) {
        if(cidx[j] >= 0) {
          scratch[ybase + j] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[ybase + j], builder.getSize(j), min);
        }
      }
      // Write to (j, y), with y < j < x
      for(int j = miny + 1; j < minx; j++) {
        if(cidx[j] >= 0) {
          final int jbase = triangleSize(j);
          scratch[jbase + miny] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jbase + miny], builder.getSize(j), min);
        }
      }
      // Write to (j, y), with y < x < j
      for(int j = minx + 1; j < size; j++) {
        if(cidx[j] >= 0) {
          final int jbase = triangleSize(j);
          scratch[jbase + miny] = linkage.combine(sizex, scratch[jbase + minx], sizey, scratch[jbase + miny], builder.getSize(j), min);
        }
      }

      int zz = builder.add(minx, square ? Math.sqrt(min) : min, miny);
      cidx[minx] = -1;
      cidx[miny] = zz;

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
    public static final OptionID LINKAGE_ID = new OptionID("hierarchical.linkage", "Parameter to choose the linkage strategy.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Current linkage in use.
     */
    protected Linkage linkage = Linkage.SINGLE;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<Linkage>(LINKAGE_ID, Linkage.class) //
          .setDefaultValue(Linkage.WARD) //
          .grab(config, x -> linkage = x);
    }

    @Override
    public NaiveAgglomerativeHierarchicalClustering4<O> make() {
      return new NaiveAgglomerativeHierarchicalClustering4<>(distance, linkage);
    }
  }
}
