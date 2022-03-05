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

import elki.clustering.hierarchical.linkage.GeometricLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.NumberVector;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * NNchain clustering algorithm with linear memory, for particular linkages
 * (that can be aggregated) and numerical vector data only.
 * <p>
 * Reference:
 * <p>
 * F. Murtagh<br>
 * Multidimensional Clustering Algorithms,1985<br>
 * http://www.multiresolutions.com/strule/MClA/
 *
 * @author Erich Schubert, Robert Gehde
 *
 * @param <O> Object type
 */
@Reference(authors = "F. Murtagh", //
    booktitle = "Multidimensional Clustering Algorithms", //
    title = "Multidimensional Clustering Algorithms", //
    url = "http://www.multiresolutions.com/strule/MClA/")
public class LinearMemoryNNChain<O extends NumberVector> extends NNChain<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LinearMemoryNNChain.class);

  /**
   * Linkage method.
   */
  private GeometricLinkage geometricLinkage;

  /**
   * Constructor.
   *
   * @param geomlinkage Linkage option
   */
  public LinearMemoryNNChain(GeometricLinkage geomlinkage) {
    super(SquaredEuclideanDistance.STATIC, geomlinkage);
    this.geometricLinkage = geomlinkage;
  }

  @Override
  public ClusterMergeHistory run(Relation<O> relation) {
    ArrayDBIDs aDBIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    DBIDArrayIter it = aDBIDs.iter(), it2 = aDBIDs.iter();

    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(aDBIDs, distance.isSquared());
    nnChainCore(it, it2, builder, relation);
    builder.optimizeOrder();
    return builder.complete();
  }

  /**
   * Core function of NNChain.
   * 
   * @param aIt Iterator to access relation objects
   * @param aIt2 Iterator to access relation objects
   * @param builder Result builder
   */
  private void nnChainCore(DBIDArrayIter aIt, DBIDArrayIter aIt2, ClusterMergeHistoryBuilder builder, Relation<O> rel) {
    final int size = rel.size();
    // The maximum chain size = number of ids + 1, but usually much less
    IntegerArray chain = new IntegerArray(size << 1);
    int[] clustermap = MathUtil.sequence(0, size);

    // Instead of a distance matrix we have an array of points
    double[][] clusters = new double[rel.size()][];
    int t = 0;
    for(aIt.seek(0); aIt.valid(); aIt.advance()) {
      // TODO: can we avoid these copies with reasonable effort?
      clusters[t++] = rel.get(aIt).toArray();
    }

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running LinearMemoryNNChain", size - 1, LOG) : null;
    for(int k = 1, end = size; k < size; k++) {
      int a = -1, b = -1;
      if(chain.size() <= 3) {
        a = findUnlinked(0, end, clustermap);
        b = findUnlinked(a + 1, end, clustermap);
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
        assert chain.get(lastIndex - 1) == c || chain.get(lastIndex - 1) == b;
        // if c < b, then we merged b -> c, otherwise c -> b
        b = c < b ? c : b;
        // Cut the tail:
        chain.size -= 3;
      }
      // For ties, always prefer the second-last element b:
      final int bSize = builder.getSize(b);
      double minDist = geometricLinkage.distance(clusters[a], builder.getSize(a), clusters[b], bSize);
      do {
        final int aSize = builder.getSize(a);
        int c = b;
        for(int i = 0; i < a; i++) {
          if(i != b && clustermap[i] >= 0) {
            double dist = geometricLinkage.distance(clusters[a], aSize, clusters[i], builder.getSize(i));
            if(dist < minDist) {
              minDist = dist;
              c = i;
            }
          }
        }
        for(int i = a + 1; i < size; i++) {
          if(i != b && clustermap[i] >= 0) {
            double dist = geometricLinkage.distance(clusters[a], aSize, clusters[i], builder.getSize(i));
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
      assert minDist == geometricLinkage.distance(clusters[a], builder.getSize(a), clusters[b], builder.getSize(b));
      assert b < a;
      merge(size, clusters, builder, clustermap, minDist, a, b);
      end = shrinkActiveSet(clustermap, end, a); // Shrink working set
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
   * @param clustermap Cluster assignment
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   */
  protected void merge(int end, double[][] clusters, ClusterMergeHistoryBuilder builder, int[] clustermap, double mindist, int x, int y) {
    assert y < x;
    final int xx = clustermap[x], yy = clustermap[y];
    final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
    int zz = builder.strictAdd(xx, linkage.restore(mindist, distance.isSquared()), yy);
    // Update cluster size for y:
    assert builder.getSize(zz) == sizex + sizey;
    // Since y < x, prefer keeping y, dropping x.
    clustermap[y] = zz;
    clustermap[x] = -1; // deactivate
    // update the cluster center for y
    clusters[y] = geometricLinkage.merge(clusters[x], sizex, clusters[y], sizey);
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
  public static class Par<O extends NumberVector> extends NNChain.Par<O> {
    /**
     * geometric linkage parameter.
     */
    public static GeometricLinkage geomLinkage = WardLinkage.STATIC;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<GeometricLinkage>(LINKAGE_ID, GeometricLinkage.class) //
          .setDefaultValue(WardLinkage.class) //
          .grab(config, x -> {
            geomLinkage = x;
          });
    }

    @Override
    public LinearMemoryNNChain<O> make() {
      return new LinearMemoryNNChain<>(geomLinkage);
    }
  }
}
