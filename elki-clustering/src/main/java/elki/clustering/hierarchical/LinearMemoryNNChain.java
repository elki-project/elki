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

import elki.clustering.hierarchical.linkage.GeometricLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * NNchain clustering algorithm with linear memory, for particular linkages
 * (that can be aggregated) and numerical vector data only.
 * <p>
 * Reference:
 * <p>
 * F. Murtagh<br>
 * Multidimensional Clustering Algorithms, 1985<br>
 * http://www.multiresolutions.com/strule/MClA/
 *
 * @author Erich Schubert, Robert Gehde
 * @since 0.8.0
 *
 * @param <O> Object type
 */
@Reference(authors = "F. Murtagh", //
    booktitle = "Multidimensional Clustering Algorithms", //
    title = "Multidimensional Clustering Algorithms", //
    url = "http://www.multiresolutions.com/strule/MClA/", //
    bibkey = "books/Murtagh85")
public class LinearMemoryNNChain<O extends NumberVector> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LinearMemoryNNChain.class);

  /**
   * Linkage method.
   */
  private GeometricLinkage linkage;

  /**
   * Constructor.
   *
   * @param linkage Linkage option
   */
  public LinearMemoryNNChain(GeometricLinkage linkage) {
    this.linkage = linkage;
  }

  /**
   * Run the NNchain algorithm.
   *
   * @param relation Data to process
   * @return cluster merges
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, true);
    return new Instance<O>(linkage).run(ids, relation, builder);
  }

  /**
   * Main worker instance of NNChain.
   * 
   * @author Erich Schubert
   * 
   * @param <O> vector type
   */
  public static class Instance<O extends NumberVector> {
    /**
     * Linkage method.
     */
    private GeometricLinkage linkage;

    /**
     * Constructor.
     *
     * @param linkage Linkage
     */
    public Instance(GeometricLinkage linkage) {
      this.linkage = linkage;
    }

    public ClusterMergeHistory run(ArrayDBIDs ids, Relation<O> relation, ClusterMergeHistoryBuilder builder) {
      DBIDArrayIter it = ids.iter(), it2 = ids.iter();
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
      boolean warnedIrreducible = false;
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
        if(chain.size() < 2) {
          a = NNChain.Instance.findUnlinked(0, end, clustermap);
          b = NNChain.Instance.findUnlinked(a + 1, end, clustermap);
          assert clustermap[a] >= 0 && clustermap[b] >= 0;
          chain.clear();
          chain.add(a);
        }
        else {
          // Chain is expected to look like (.... a, b, c, b) with b and c
          // merged.
          a = chain.get(chain.size - 2);
          b = chain.get(chain.size - 1);
          assert clustermap[b] >= 0;
          if(clustermap[a] < 0) {
            if(!warnedIrreducible) {
              LOG.warning("Detected an inversion in the clustering. NNChain on irreducible linkages may yield different results.");
              warnedIrreducible = true;
            }
            chain.size -= 2; // cut the chain
            k--; // retry
            continue;
          }
          chain.size--; // Remove b
        }
        // For ties, always prefer the second-last element b:
        final int bSize = builder.getSize(b);
        double minDist = linkage.distance(clusters[a], builder.getSize(a), clusters[b], bSize);
        do {
          final int aSize = builder.getSize(a);
          int c = b;
          for(int i = 0; i < end; i++) {
            if(i != a && i != b && clustermap[i] >= 0) {
              double dist = linkage.distance(clusters[a], aSize, clusters[i], builder.getSize(i));
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
        assert minDist == linkage.distance(clusters[a], builder.getSize(a), clusters[b], builder.getSize(b));
        merge(size, clusters, builder, clustermap, minDist, a, b);
        end = AGNES.Instance.shrinkActiveSet(clustermap, end, a);
        chain.size -= 3;
        chain.add(b);
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
      assert x >= 0 && y >= 0;
      final int xx = clustermap[x], yy = clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      int zz = builder.strictAdd(xx, linkage.restore(mindist, builder.isSquared), yy);
      assert builder.getSize(zz) == sizex + sizey;
      clustermap[y] = zz;
      clustermap[x] = -1; // deactivate
      // update the cluster center for y
      clusters[y] = linkage.merge(clusters[x], sizex, clusters[y], sizey);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Par<O extends NumberVector> implements Parameterizer {
    /**
     * Linkage to use.
     */
    public static final OptionID LINKAGE_ID = AGNES.Par.LINKAGE_ID;

    /**
     * geometric linkage parameter.
     */
    protected GeometricLinkage linkage = WardLinkage.STATIC;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<GeometricLinkage>(LINKAGE_ID, GeometricLinkage.class) //
          .setDefaultValue(WardLinkage.class) //
          .grab(config, x -> linkage = x);
    }

    @Override
    public LinearMemoryNNChain<O> make() {
      return new LinearMemoryNNChain<>(linkage);
    }
  }
}
