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

import elki.Algorithm;
import elki.clustering.hierarchical.linkage.SingleLinkage;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Medoid linkage uses the distance of medoids as criterion. The implementation
 * is a simplified version of AGNES, as we do not have to update a distance
 * matrix. This approach was invented at least twice. An approach more
 * consistent with the ideas of optimum medoids is found in {@link HACAM}, which
 * uses the loss of the resulting medoid <i>after</i> merging instead.
 * <p>
 * References:
 * <p>
 * D. Herr, Q. Han, S. Lohmann, T. Ertl<br>
 * Visual clutter reduction through hierarchy-based
 * projection of high-dimensional labeled data<br>
 * Graphics Interface Conference
 * <p>
 * S. Miyamoto, Y. Kaizu, Y. Endo<br>
 * Hierarchical and non-hierarchical medoid clustering
 * using asymmetric similarity measures<br>
 * Soft Computing and Intelligent Systems (SCIS) and Int. Symp. Advanced
 * Intelligent Systems (ISIS)
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @has - - - PointerPrototypeHierarchyResult
 *
 * @param <O> Object type
 */
@Reference(authors = "D. Herr, Q. Han, S. Lohmann, T. Ertl", //
    title = "Visual clutter reduction through hierarchy-based projection of high-dimensional labeled data", //
    booktitle = "Graphics Interface Conference", //
    url = "https://doi.org/10.20380/GI2016.14", //
    bibkey = "DBLP:conf/graphicsinterface/HerrHLE16")
@Reference(authors = "S. Miyamoto, Y. Kaizu, Y. Endo", //
    title = "Hierarchical and non-hierarchical medoid clustering using asymmetric similarity measures", //
    booktitle = "Soft Computing and Intelligent Systems (SCIS) and Int. Symp. Advanced Intelligent Systems (ISIS)", //
    url = "https://doi.org/10.1109/SCIS-ISIS.2016.0091", //
    bibkey = "DBLP:conf/scisisis/MiyamotoKE16")
public class MedoidLinkage<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MedoidLinkage.class);

  /**
   * The distance function to use.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   */
  public MedoidLinkage(Distance<? super O> distance) {
    this.distance = distance;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public ClusterPrototypeMergeHistory run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ClusterDistanceMatrix mat = AGNES.initializeDistanceMatrix(ids, dq, SingleLinkage.STATIC);
    return new Instance().run(ids, mat, new ClusterMergeHistoryBuilder(ids, distance.isSquared()), dq);
  }

  /**
   * Main worker instance of AGNES.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AGNES.Instance {
    /**
     * Cluster to members map
     */
    protected Int2ObjectOpenHashMap<ModifiableDBIDs> clusters;

    /**
     * Distance query
     */
    protected DistanceQuery<?> dq;

    /**
     * Iterators into medoid array.
     */
    protected DBIDArrayMIter mi, mj;

    /**
     * Iterators into the object ids.
     */
    protected DBIDArrayIter ix, iy;

    /**
     * Constructor.
     */
    public Instance() {
      super(null /* medoid linkage */);
    }

    @Override
    public ClusterMergeHistory run(ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder) {
      throw new IllegalStateException("Need prototypes.");
    }

    /**
     * Run medoid linkage
     * 
     * @param ids Object ids
     * @param mat Distance matrix
     * @param builder Result builder
     * @param dq Distance query
     * @return Cluster merge history
     */
    public ClusterPrototypeMergeHistory run(ArrayDBIDs ids, ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder, DistanceQuery<?> dq) {
      final int size = mat.size;
      this.mat = mat;
      this.builder = builder;
      this.end = size;
      this.clusters = new Int2ObjectOpenHashMap<>(size);
      this.ix = ids.iter();
      this.iy = ids.iter();
      ArrayModifiableDBIDs medoids = DBIDUtil.newArray(ids);
      this.mi = medoids.iter();
      this.mj = medoids.iter();
      this.dq = dq;

      // Repeat until everything merged into 1 cluster
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Medoid linkage clustering", size - 1, LOG) : null;
      for(int i = 1; i < size; i++) {
        end = shrinkActiveSet(mat.clustermap, end, findMerge());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      return (ClusterPrototypeMergeHistory) builder.complete();
    }

    @Override
    protected int findMerge() {
      final double[] distances = mat.matrix;
      double mindist = Double.POSITIVE_INFINITY;
      int x = -1, y = -1;
      for(int dx = 0; dx < end; dx++) {
        // Skip if object is already linked
        if(mat.clustermap[dx] < 0) {
          continue;
        }
        final int xoffset = ClusterDistanceMatrix.triangleSize(dx);

        for(int dy = 0; dy < dx; dy++) {
          // Skip if object is already linked
          if(mat.clustermap[dy] < 0) {
            continue;
          }

          double dist = distances[xoffset + dy];
          if(dist < mindist) {
            mindist = dist;
            x = dx;
            y = dy;
          }
        }
      }
      merge(x, y);
      return x;
    }

    /**
     * Execute the cluster merge.
     *
     * @param x first cluster to merge, with {@code x > y}
     * @param y second cluster to merge, with {@code y < x}
     */
    protected void merge(int x, int y) {
      assert x >= 0 && y >= 0;
      assert y < x; // We could swap otherwise, but this shouldn't arise.
      final double[] distances = mat.matrix;
      final int offset = ClusterDistanceMatrix.triangleSize(x) + y;
      ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);
      // Keep y
      if(cy == null) {
        cy = DBIDUtil.newArray();
        cy.add(iy.seek(y));
      }
      if(cx == null) {
        cy.add(ix.seek(x));
      }
      else {
        cy.addDBIDs(cx);
        clusters.remove(x);
      }
      clusters.put(y, cy);

      // New cluster medoid
      findMedoid(dq, cy, mj.seek(y));
      // parent of x is set to y
      final int xx = mat.clustermap[x], yy = mat.clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      int zz = builder.strictAdd(xx, distances[offset], yy, mj);
      assert builder.getSize(zz) == sizex + sizey;
      mat.clustermap[y] = zz;
      mat.clustermap[x] = -1; // deactivate
      updateMatrix(x, y);
    }

    /**
     * Find the prototypes.
     * 
     * @param dq Distance query
     * @param c Cluster
     * @param prototype Prototype output
     * @return New best distance
     */
    private static double findMedoid(DistanceQuery<?> dq, DBIDs c, DBIDArrayMIter prototype) {
      if(c.size() == 2) {
        final DBIDIter other = c.iter();
        prototype.setDBID(other);
        return dq.distance(prototype, other.advance());
      }
      double minDistSum = Double.POSITIVE_INFINITY;
      for(DBIDIter i = c.iter(); i.valid(); i.advance()) {
        // Maximum distance of i to all elements in cx
        double distsum = 0;
        for(DBIDIter j = c.iter(); j.valid(); j.advance()) {
          if(!DBIDUtil.equal(i, j)) {
            distsum += dq.distance(i, j);
            if(distsum >= minDistSum) {
              break;
            }
          }
        }
        // New best solution?
        if(distsum < minDistSum) {
          minDistSum = distsum;
          prototype.setDBID(i);
        }
      }
      return minDistSum;
    }

    /**
     * Update the scratch distance matrix.
     *
     * @param x First matrix position
     * @param y Second matrix position
     */
    protected void updateMatrix(int x, int y) {
      // Update distance matrix. Note: y < x
      final int ybase = ClusterDistanceMatrix.triangleSize(y);
      double[] scratch = mat.matrix;

      // Write to (y, j), with j < y
      int j = 0;
      mi.seek(y);
      for(; j < y; j++) {
        if(mat.clustermap[j] < 0) {
          continue;
        }
        assert j < y; // Otherwise, ybase + j is the wrong position!
        scratch[ybase + j] = dq.distance(mi, mj.seek(j));
      }
      j++; // Skip y
      // Write to (j, y), with y < j < x
      int jbase = ClusterDistanceMatrix.triangleSize(j);
      for(; j < x; jbase += j++) {
        if(mat.clustermap[j] < 0) {
          continue;
        }
        scratch[jbase + y] = dq.distance(mi, mj.seek(j));
      }
      jbase += j++; // Skip x
      // Write to (j, y), with y < x < j
      for(; j < end; jbase += j++) {
        if(mat.clustermap[j] < 0) {
          continue;
        }
        scratch[jbase + y] = dq.distance(mi, mj.seek(j));
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // The input relation must match our distance function:
    return TypeUtil.array(distance.getInputTypeRestriction());
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
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
    }

    @Override
    public MedoidLinkage<O> make() {
      return new MedoidLinkage<>(distance);
    }
  }
}
