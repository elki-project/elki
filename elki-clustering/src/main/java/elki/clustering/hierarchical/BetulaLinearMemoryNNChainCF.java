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

import java.util.ArrayList;
import java.util.ListIterator;

import elki.Algorithm;
import elki.clustering.hierarchical.linkage.GeometricLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CFDistance;
import elki.index.tree.betula.distance.VarianceIncreaseDistance;
import elki.index.tree.betula.features.ClusterFeature;
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
public class BetulaLinearMemoryNNChainCF implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BetulaLinearMemoryNNChainCF.class);

  /**
   * Distance function used.
   */
  protected CFDistance distance;

  /**
   * CFTree factory.
   */
  CFTree.Factory<?> cffactory;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param linkage Linkage method
   * @param
   * @param
   */
  public BetulaLinearMemoryNNChainCF(CFDistance distance, CFTree.Factory<?> cffactory) {
    super();
    this.distance = distance;
    this.cffactory = cffactory;
  }

  /**
   * Run the NNchain algorithm.
   *
   * @param relation Data to process
   * @return cluster merges
   */
  public ClusterMergeHistory run(Relation<NumberVector> relation) {
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    CFTree<?> tree = cffactory.newTree(ids, relation, true);
    ArrayList<? extends ClusterFeature> cfs = tree.getLeaves();

    ArrayList<DBIDs> idList = new ArrayList<>();
    double[] dists = new double[cfs.size()];
    ListIterator<? extends ClusterFeature> lit = cfs.listIterator();
    int i = 0;

    ClusterFeature[] clusters = new ClusterFeature[cfs.size()];

    while(lit.hasNext()) {
      ClusterFeature cf = lit.next();
      idList.add(tree.getDBIDs(cf));
      dists[i] = cf.variance() / cf.getWeight();
      clusters[i] = cf;
      i++;
    }

    int[] clustermap = new int[cfs.size()];
    ClusterMergeHistoryBuilder cmhb = BetulaAnderberg.initializeHistoryBuilder(idList, relation.size(), dists, clustermap, false);

    return new Instance(distance).run(clusters, clustermap, cmhb);
  }

  /**
   * Main worker instance of NNChain.
   * 
   * @author Erich Schubert
   * 
   * @param <O> vector type
   */
  public static class Instance {
    /**
     * Linkage method.
     */
    private CFDistance distance;

    /**
     * Constructor.
     *
     * @param linkage Linkage
     */
    public Instance(CFDistance distance) {
      this.distance = distance;
    }

    public ClusterMergeHistory run(ClusterFeature[] clusters, int[] clustermap, ClusterMergeHistoryBuilder builder) {

      nnChainCore(clusters, clustermap, builder);
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
    protected void nnChainCore(ClusterFeature[] clusters, int[] clustermap, ClusterMergeHistoryBuilder builder) {
      // The maximum chain size = number of ids + 1, but usually much less
      int size = clusters.length;
      IntegerArray chain = new IntegerArray(size << 1);

      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running LinearMemoryNNChain", size - 1, LOG) : null;
      for(int k = 1, end = size; k < size; k++) {
        int a = -1, b = -1;
        if(chain.size() <= 3) {
          a = NNChain.Instance.findUnlinked(0, end, clustermap);
          b = NNChain.Instance.findUnlinked(a + 1, end, clustermap);
          chain.clear();
          chain.add(a);
        }
        else {
          // Chain is expected to look like (.... a, b, c, b) with b and c
          // merged.
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
        double minDist = distance.squaredDistance(clusters[a], clusters[b]);
        do {
          int c = b;
          for(int i = 0; i < a; i++) {
            if(i != b && clustermap[i] >= 0) {
              double dist = distance.squaredDistance(clusters[a], clusters[i]);
              if(dist < minDist) {
                minDist = dist;
                c = i;
              }
            }
          }
          for(int i = a + 1; i < size; i++) {
            if(i != b && clustermap[i] >= 0) {
              double dist = distance.squaredDistance(clusters[a], clusters[i]);
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
        assert minDist == distance.squaredDistance(clusters[a], clusters[b]);
        merge(size, clusters, builder, clustermap, minDist, a, b);
        end = AGNES.Instance.shrinkActiveSet(clustermap, end, a);
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
    protected void merge(int end, ClusterFeature[] clusters, ClusterMergeHistoryBuilder builder, int[] clustermap, double mindist, int x, int y) {
      assert x >= 0 && y >= 0;
      final int xx = clustermap[x], yy = clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      int zz = builder.strictAdd(xx, mindist, yy);
      assert builder.getSize(zz) == sizex + sizey;
      clustermap[y] = zz;
      clustermap[x] = -1; // deactivate
      // update the cluster center for y
      clusters[y].addToStatistics(clusters[x]);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    /**
     * Ignore cluster weights (naive approach)
     */
    public static final OptionID IGNORE_WEIGHT_ID = new OptionID("betulaAnderberg.naive", "Treat leaves as single points, not weighted points.");

    /**
     * The distance function to use.
     */
    protected CFDistance distance;

    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    @Override
    public void configure(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
      new ObjectParameter<CFDistance>(Algorithm.Utils.DISTANCE_FUNCTION_ID, CFDistance.class, VarianceIncreaseDistance.class) //
          .grab(config, x -> distance = x);
    }

    @Override
    public BetulaLinearMemoryNNChainCF make() {
      return new BetulaLinearMemoryNNChainCF(distance, cffactory);
    }
  }
}
