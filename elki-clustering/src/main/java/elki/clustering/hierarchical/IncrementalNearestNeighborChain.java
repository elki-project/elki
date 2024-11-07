/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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

import java.util.Arrays;

import elki.clustering.hierarchical.linkage.GeometricLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDEnum;
import elki.database.ids.DBIDUtil;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.LinearScanEuclideanPrioritySearcher;
import elki.database.query.distance.LinearScanPrioritySearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.LongStatistic;
import elki.utilities.Alias;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Incremental Nearest-Neighbor Chain (INNC) clustering for geometric linkages
 * and vector data only, which uses incremental similarity search with bounds
 * and filtering to find the nearest clusters efficiently using an index.
 * <p>
 * FIXME: ensure the automatic indexing chooses an appropriate (large!) page
 * size
 * <p>
 * Reference:
 * <p>
 * Erich Schubert:<br>
 * Hierarchical Clustering Without Pairwise Distances by Incremental Similarity
 * Search<br>
 * Int. Conf. on Similarity Search and Applications (SISAP 2024)
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Reference(authors = "Erich Schubert", //
    title = "Hierarchical Clustering Without Pairwise Distances by Incremental Similarity Search", //
    booktitle = "Int. Conf. on Similarity Search and Applications (SISAP 2024)", //
    bibkey = "DBLP:conf/sisap/Schubert24", url = "https://doi.org/10.1007/978-3-031-75823-2_20")
@Alias({ "INNC" })
public class IncrementalNearestNeighborChain<O extends NumberVector> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(IncrementalNearestNeighborChain.class);

  /**
   * Key for statistics logging.
   */
  private final static String KEY = LinearMemoryNNChain.class.getName();

  /**
   * Linkage method.
   */
  private GeometricLinkage linkage;

  /**
   * Constructor.
   *
   * @param linkage Linkage option
   */
  public IncrementalNearestNeighborChain(GeometricLinkage linkage) {
    this.linkage = linkage;
  }

  /**
   * Run the NNchain algorithm.
   *
   * @param relation Data to process
   * @return cluster merges
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    DBIDEnum ids = DBIDUtil.ensureEnum(relation.getDBIDs());
    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, true);
    // TODO: ensure we have good page size!
    PrioritySearcher<O> pq = new QueryBuilder<>(relation, SquaredEuclideanDistance.STATIC).priorityByObject();
    if(pq instanceof LinearScanPrioritySearcher || pq instanceof LinearScanEuclideanPrioritySearcher) {
      throw new UnsupportedOperationException("No index acceleration available. This will be very slow.");
    }
    return new Instance<O>(linkage).run(ids, relation, builder, pq);
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
     * Number of non-index distance computations
     */
    private long distanceComputations = 0L;

    /**
     * Data set
     */
    private Relation<O> rel;

    /**
     * IDs
     */
    private DBIDEnum ids;

    /**
     * Iterator for retrieving object.
     */
    private DBIDArrayIter iter;

    /**
     * Seacher
     */
    private PrioritySearcher<O> pq;

    /**
     * Vector type
     */
    private NumberVector.Factory<O> factory;

    /**
     * Visited flag cache
     */
    private byte[] visited;

    /**
     * Cluster centers
     */
    private double[][] clusters;

    /**
     * Merge data structure
     */
    private ClusterMergeHistoryBuilder builder;

    /**
     * Constructor.
     *
     * @param linkage Linkage
     */
    public Instance(GeometricLinkage linkage) {
      this.linkage = linkage;
    }

    /**
     * Run the clustering algorithm.
     * 
     * @param ids Object IDs to process
     * @param relation Data relation
     * @param builder Cluster hierarchy builder
     * @param pq Priority query
     * @return Cluster hierarchy
     */
    public ClusterMergeHistory run(DBIDEnum ids, Relation<O> relation, ClusterMergeHistoryBuilder builder, PrioritySearcher<O> pq) {
      this.ids = ids;
      this.iter = ids.iter();
      this.rel = relation;
      this.pq = pq;
      this.builder = builder;

      final int size = rel.size();
      // The maximum chain size = number of ids + 1, but usually much less
      IntegerArray chain = new IntegerArray(size + 1);
      this.visited = new byte[size << 1];
      this.factory = RelationUtil.getNumberVectorFactory(rel);

      // Instead of a distance matrix we have merged clusters
      this.clusters = new double[rel.size() - 1][];
      int maxcluster = 1;

      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running LinearMemoryNNChain", size - 1, LOG) : null;
      for(int k = 1; k < size; k++) {
        if(!growChain(chain, maxcluster)) {
          k--; // retry
          continue;
        }
        int a = builder.get(chain.get(chain.size - 2));
        int b = builder.get(chain.get(chain.size - 1));
        int na = builder.getSize(a), nb = builder.getSize(b);
        double minLink = linkage.distance(getCenter(a), na, getCenter(b), nb);
        assert a != b;
        int ab = builder.add(a, linkage.restore(minLink, builder.isSquared), b);
        assert builder.getSize(ab) == na + nb;
        if(na + nb > maxcluster) {
          maxcluster = na + nb;
        }
        // update the cluster center
        clusters[ab - ids.size()] = linkage.merge(getCenter(a), na, getCenter(b), nb);
        chain.size -= 3;
        chain.add(ab);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      LOG.statistics(new LongStatistic(KEY + ".distance-computations", distanceComputations));
      builder.optimizeOrder();
      return builder.complete();
    }

    private boolean growChain(IntegerArray chain, int maxcluster) {
      int a = -1, b = -1;
      if(chain.size() < 2) {
        a = findUnlinked(-1, builder);
        b = findUnlinked(a, builder);
        assert a != b && b >= 0;
        chain.clear();
        chain.add(a);
      }
      else {
        // Chain is expected to look like (.... a, b, c, b) with b and c merged.
        a = builder.get(chain.get(chain.size - 2));
        b = builder.get(chain.get(chain.size - 1));
        if(a == b) {
          chain.size -= 2; // cut the chain
          return false;
        }
        /* if(clustermap[a] < 0) {
        if(!warnedIrreducible) {
          LOG.warning("Detected an inversion in the clustering. NNChain on irreducible linkages may yield different results.");
          warnedIrreducible = true;
        }
        chain.size -= 2; // cut the chain
        k--; // retry
        continue;
        } */
        chain.size--; // Remove b
      }
      // System.err.println("Start: " + a + " -> " + b);
      // For ties, always prefer the second-last element b:
      double[] va = getCenter(a), vb = getCenter(b);
      int na = builder.getSize(a), nb = builder.getSize(b);
      double minLink = linkage.distance(va, na, vb, nb);
      distanceComputations++;
      do {
        Arrays.fill(visited, (byte) 0);
        int c = b, nc = nb;
        @SuppressWarnings("unchecked")
        O qv = a < rel.size() ? rel.get(iter.seek(a)) : //
            factory == DoubleVector.FACTORY ? (O) DoubleVector.wrap(va) : factory.newNumberVector(va);
        double[] vc = vb;
        double f = 1;
        if(linkage.getClass() == WardLinkage.class) {
          // Note: compared to the reference paper, the Ward distances in this
          // implementation are scaled, so that they capture variances not
          // distances.
          f = (na + maxcluster) * 0.25 / na;
        }
        for(pq.search(qv, f * minLink); pq.valid(); pq.advance()) {
          final int i = builder.get(ids.index(pq));
          if(i != a && i != b && (i < rel.size() || visited[i - rel.size()] == 0)) {
            final double[] vi = getCenter(i);
            final int ni = builder.getSize(i);
            if(linkage.getClass() != WardLinkage.class || pq.getLowerBound() / (na + ni) * na * ni <= minLink) {
              double link = linkage.distance(va, na, vi, ni);
              distanceComputations++;
              if(link < minLink) {
                minLink = link;
                c = i;
                vc = vi;
                nc = ni;
                pq.decreaseCutoff(f * link);
              }
            }
            if(i >= rel.size()) {
              visited[i - rel.size()] = 1;
            }
          }
        }
        // System.err.println(a + " -> " + c);
        b = a;
        vb = va;
        nb = na;
        a = c;
        va = vc;
        na = nc;
        chain.add(c);
      }
      while(chain.size() < 3 || a != chain.get(chain.size - 1 - 2));
      return true;
    }

    /**
     * Get the center of a (virtual?) cluster
     * 
     * @param ca Cluster id
     * @return Center vector
     */
    private double[] getCenter(int ca) {
      return ca < rel.size() ? rel.get(iter.seek(ca)).toArray() : clusters[ca - rel.size()];
    }

    /**
     * Find unlinked id.
     *
     * @param c excluded cluster
     * @param builder Cluster merge history
     * @return first unlinked object (of size 1)
     */
    private int findUnlinked(int c, ClusterMergeHistoryBuilder builder) {
      if(builder.parent == null) {
        return c + 1;
      }
      for(int i = 0; i < builder.ids.size(); i++) {
        final int ci = builder.get(i);
        if(ci != c) {
          return ci;
        }
      }
      return -1;
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
    public IncrementalNearestNeighborChain<O> make() {
      return new IncrementalNearestNeighborChain<>(linkage);
    }
  }
}
