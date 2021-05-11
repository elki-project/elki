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
package elki.clustering.hierarchical;

import elki.clustering.hierarchical.linkage.Linkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.clustering.hierarchical.linkage.MedianLinkage;
import elki.clustering.hierarchical.linkage.CentroidLinkage;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.linearalgebra.VMath;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;

/**
 * NNchain clustering algorithm with linear Memory.
 * <p>
 * Reference:
 * <p>
 * F. Murtagh<br>
 * Multidimensional Clustering Algorithms,1985<br>
 * http://www.multiresolutions.com/strule/MClA/
 * <p>
 *
 * @author Erich Schubert, Robert Gehde
 *
 * @param <O> Object type
 */
@Reference(authors = "F. Murtagh", //
    booktitle = "Multidimensional Clustering Algorithms", //
    title = "Multidimensional Clustering Algorithms", //
    url = "http://www.multiresolutions.com/strule/MClA/")
public class LinearMemoryNNChain<O extends NumberVector> extends AGNES<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LinearMemoryNNChain.class);

  private GeometricLinkage geometricLinkage;

  /**
   * Constructor.
   *
   * @param linkage Linkage option; currently unused, only ward
   */
  public LinearMemoryNNChain(GeometricLinkage geomlinkage, Linkage linkage) {
    super(SquaredEuclideanDistance.STATIC, linkage);
    this.geometricLinkage = geomlinkage;
  }

  @Override
  public PointerHierarchyResult run(Relation<O> relation) {
    final DBIDs ids = relation.getDBIDs();

    // create Array Iter for PointerHirarchyBuilder
    ArrayDBIDs aDBIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    DBIDArrayIter it = aDBIDs.iter();
    DBIDArrayIter it2 = aDBIDs.iter();

    // Initialize space for result:
    PointerHierarchyBuilder builder = new PointerHierarchyBuilder(ids, true);

    nnChainCore(it, it2, builder, relation);
    return builder.complete();
  }

  /**
   * 
   * @param aIt Iterator to access relation objects
   * @param aIt2 Iterator to access relation objects
   * @param builder Result builder
   */
  private void nnChainCore(DBIDArrayIter aIt, DBIDArrayIter aIt2, PointerHierarchyBuilder builder, Relation<O> rel) {
    final int size = rel.size();
    // The maximum chain size = number of ids + 1
    IntegerArray chain = new IntegerArray(size + 1);

    // Instead of a DistanceMatrix we have a PointArray
    NumberVector[] clusters = new NumberVector[rel.size()];
    int t = 0;
    for(aIt.seek(0); aIt.valid(); aIt.advance()) {
      clusters[t++] = rel.get(aIt);
    }

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running LinearMemoryNNChain", size - 1, LOG) : null;
    for(int k = 1, end = size; k < size; k++) {
      int a = -1, b = -1;
      if(chain.size() <= 3) {
        a = findUnlinked(0, end, aIt, builder);
        b = findUnlinked(a + 1, end, aIt, builder);
        chain.clear();
        chain.add(a);
      }
      else {
        // Chain is expected to look like (.... a, b, c, b) with b and c merged.
        int lastIndex = chain.size;
        int c = chain.get(lastIndex - 2);
        b = chain.get(lastIndex - 3);
        a = chain.get(lastIndex - 4);
        // Ensure we had a loop at the end:
        assert (chain.get(lastIndex - 1) == c || chain.get(lastIndex - 1) == b);
        // if c < b, then we merged b -> c, otherwise c -> b
        b = c < b ? c : b;
        // Cut the tail:
        chain.size -= 3;
      }
      // For ties, always prefer the second-last element b:
      final int bSize = builder.getSize(aIt.seek(b));
      double minDist = geometricLinkage.distance(builder.getSize(aIt.seek(a)), bSize, clusters[a], clusters[b]);
      do {
        final int aSize = builder.getSize(aIt.seek(a));
        int c = b;
        for(int i = 0; i < a; i++) {
          if(i != b && !builder.isLinked(aIt.seek(i))) {
            double dist = geometricLinkage.distance(aSize, builder.getSize(aIt2.seek(i)), clusters[a], clusters[i]);
            if(dist < minDist) {
              minDist = dist;
              c = i;
            }
          }
        }
        for(int i = a + 1; i < size; i++) {
          if(i != b && !builder.isLinked(aIt.seek(i))) {
            double dist = geometricLinkage.distance(aSize, builder.getSize(aIt2.seek(i)), clusters[a], clusters[i]);
            if(dist < minDist) {
              minDist = dist;
              c = i;
            }
          }
        }

        b = a;
        a = c;
        chain.add(a);
      }
      while(chain.size() < 3 || a != chain.get(chain.size - 1 - 2));

      // We always merge the larger into the smaller index:
      if(a < b) {
        int tmp = a;
        a = b;
        b = tmp;
      }
      assert (minDist == geometricLinkage.distance(builder.getSize(aIt.seek(a)), builder.getSize(aIt2.seek(b)), clusters[a], clusters[b]));
      assert (b < a);
      merge(size, clusters, aIt.seek(a), aIt2.seek(b), builder, minDist, a, b);
      end = AGNES.shrinkActiveSet(aIt, builder, end, a); // Shrink working set
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
  }

  /**
   * Execute the cluster merge.
   *
   * @param end Active set size
   * @param clusters Array of cluster centers
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   */
  protected void merge(int end, NumberVector[] clusters, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyBuilder builder, double mindist, int x, int y) {
    // Avoid allocating memory, by reusing existing iterators:
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + mindist);
    }
    // Perform merge in data structure: x -> y
    assert (y < x);
    // Since y < x, prefer keeping y, dropping x.
    builder.strictAdd(ix, 2. * mindist, iy); // 2* because of linkage restore,
                                             // idk if needed
    // Update cluster size for y:
    final int sizex = builder.getSize(ix), sizey = builder.getSize(iy);
    builder.setSize(iy, sizex + sizey);
    // update the cluster center for y
    clusters[y] = geometricLinkage.merge(sizex, sizey, clusters[x], clusters[y]);
  }

  /**
   * Find an unlinked object.
   *
   * @param pos Starting position
   * @param end End position
   * @param ix Iterator to translate into DBIDs
   * @param builder Linkage information
   * @return Position
   */
  public static int findUnlinked(int pos, int end, DBIDArrayIter ix, PointerHierarchyBuilder builder) {
    while(pos < end) {
      if(!builder.isLinked(ix.seek(pos))) {
        return pos;
      }
      ++pos;
    }
    return -1;
  }

  public static enum GeometricLinkage {
    WARD {
      @Override
      public NumberVector merge(int sizex, int sizey, NumberVector x, NumberVector y) {
        double[] c = VMath.timesPlusTimes(y.toArray(), sizey / (double) (sizex + sizey), x.toArray(), sizex / (double) (sizex + sizey));
        return new DoubleVector(c);
      }

      @Override
      public double distance(int sizex, int sizey, NumberVector x, NumberVector y) {
        return ((sizex * sizey) / (double) (sizex + sizey)) * SquaredEuclideanDistance.STATIC.distance(x, y);
      }

      @Override
      public Linkage getAssociatedLinkage() {
        return WardLinkage.STATIC;
      }

    },
    MEDIAN_WPGMC {
      @Override
      public NumberVector merge(int sizex, int sizey, NumberVector x, NumberVector y) {
        double[] c = VMath.timesPlusTimes(y.toArray(), .5, x.toArray(), .5);
        return new DoubleVector(c);
      }

      @Override
      public double distance(int sizex, int sizey, NumberVector x, NumberVector y) {
        return SquaredEuclideanDistance.STATIC.distance(x, y);
      }

      @Override
      public Linkage getAssociatedLinkage() {
        return MedianLinkage.STATIC;
      }
    },
    CENTROID_UPGMC {
      @Override
      public NumberVector merge(int sizex, int sizey, NumberVector x, NumberVector y) {
        double[] c = VMath.timesPlusTimes(y.toArray(), sizey / (double) (sizex + sizey), x.toArray(), sizex / (double) (sizex + sizey));
        return new DoubleVector(c);
      }

      @Override
      public double distance(int sizex, int sizey, NumberVector x, NumberVector y) {
        return SquaredEuclideanDistance.STATIC.distance(x, y);
      }

      @Override
      public Linkage getAssociatedLinkage() {
        return CentroidLinkage.STATIC;
      }
    };

    public abstract NumberVector merge(int sizex, int sizey, NumberVector x, NumberVector y);

    public abstract double distance(int sizex, int sizey, NumberVector x, NumberVector y);

    public abstract Linkage getAssociatedLinkage();
  }

  /**
   * Parameterization class.
   * 
   * @author Robert Gehde
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O extends NumberVector> extends AGNES.Par<O> {
    /**
     * Option ID for geometric linkage parameter.
     */
    public static final OptionID GEOM_LINKAGE_ID = new OptionID("linearMemoryNNChain.geom_linkage", "Linkage method to use (can be WARD, UPGMC,WPGMC)");

    /**
     * geometric linkage parameter.
     */
    public static GeometricLinkage geomLinkage = GeometricLinkage.WARD;

    @Override
    public void configure(Parameterization config) {
      new EnumParameter<GeometricLinkage>(GEOM_LINKAGE_ID, GeometricLinkage.class) //
          .setDefaultValue(GeometricLinkage.WARD) //
          .grab(config, x -> {
            geomLinkage = x;
            linkage = x.getAssociatedLinkage();
          });
    }

    @Override
    public LinearMemoryNNChain<O> make() {
      return new LinearMemoryNNChain<>(geomLinkage, linkage);
    }
  }
}
