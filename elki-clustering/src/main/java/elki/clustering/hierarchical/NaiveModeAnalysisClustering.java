/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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

import java.util.HashSet;
import java.util.LinkedList;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.memory.MapIntegerDBIDIntegerStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.datastructures.heap.Heap;
import elki.utilities.datastructures.unionfind.WeightedQuickUnionInteger;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.jafama.FastMath;

/**
 * Mode Analysis as introduced by D. Wishart. A rather unknown Clustering
 * algorithm introducing Hierarchical aspects and KNN-Distance to Nearest
 * Neighbor Chaining. Originally introduced to cluster Stars into "dwarfs" and
 * "giants".
 * <p>
 * Its implemented in a Naive approach with runtime O(n^3)
 * <p>
 * Source:<br>
 * Mode Analysis: A generalization of nearest neighbor which reduces chaining
 * effects<br>
 * D. Wishart <br>
 * Computing Laboratory, University of St. Andrews <br>
 * 
 * @author Robert Gehde
 * 
 * @param <O>
 */
public class NaiveModeAnalysisClustering<O> implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NaiveModeAnalysisClustering.class);

  /**
   * DistanceFunction used.
   */
  private Distance<? super O> distanceFunction;

  /**
   * the k-th nearest neighbor to look at (excluding the point itself)
   */
  private int k;

  /**
   * 
   * Constructor.
   *
   * @param distanceFunction
   */
  public NaiveModeAnalysisClustering(Distance<? super O> distanceFunction, int k) {
    this.k = k + 1;
    this.distanceFunction = distanceFunction;
  }

  public Clustering<DendrogramModel> run(Relation<O> relation) {
    // mapping id <-> offset:
    ArrayDBIDs adbids = DBIDUtil.ensureArray(relation.getDBIDs());
    MapIntegerDBIDIntegerStore offsetStore = new MapIntegerDBIDIntegerStore(relation.size());

    // distance computations:
    MatrixParadigm mat = new MatrixParadigm(adbids);
    QueryBuilder<O> dq = new QueryBuilder<O>(relation, distanceFunction).precomputed();
    KNNSearcher<DBIDRef> knn = dq.kNNByDBID(k);
    mat.initializeWithDistances(dq.distanceQuery());
    Heap<HeapEl> distancePairHeap = new Heap<HeapEl>((relation.size() * (relation.size() - 1)) / 2);

    // cluster tracking
    WeightedQuickUnionInteger unions = new WeightedQuickUnionInteger();
    Int2ObjectOpenHashMap<TempCluster> unionToCluster = new Int2ObjectOpenHashMap<NaiveModeAnalysisClustering<O>.TempCluster>();
    HashSet<TempCluster> currentTLClusters = new HashSet<TempCluster>();

    // Result
    Clustering<DendrogramModel> clustering = new Clustering<DendrogramModel>();
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(adbids, distanceFunction.isSquared());

    // find distance k-th nearest neighbour for all points
    ModifiableDoubleDBIDList distances = initializeWorkOrder(mat, knn, offsetStore, unions);

    // now enter main loop
    for(DoubleDBIDListIter mainIt = distances.iter(); mainIt.valid(); mainIt.advance()) {
      double density = mainIt.doubleValue();

      // Search cluster pairs with distance < density
      searchNearerClusters(currentTLClusters, mat, density, distancePairHeap);

      while(!distancePairHeap.isEmpty()) {

        // get closest cluster pair
        HeapEl h = distancePairHeap.poll();

        // the clusters in the Heap Element could be already merged and
        // discarded, so we check the unions of those

        // check if still valid
        int off1 = unions.find(h.l.clusterMatrixOffset);
        int off2 = unions.find(h.r.clusterMatrixOffset);

        if(!unions.isConnected(off1, off2)) {
          // Merge clusters if they weren't merged already
          LinkedList<Cluster<DendrogramModel>> newClusters = new LinkedList<>();
          TempCluster t1 = unionToCluster.get(off1);
          TempCluster t2 = unionToCluster.get(off2);

          newClusters.add(t1.finalizeCluster(clustering));
          newClusters.add(t2.finalizeCluster(clustering));

          int i = unions.union(off1, off2);
          assert i == off1 || i == off2;
          TempCluster newTempCluster = new TempCluster(newClusters, i);

          /***/
          updateMatrix(mat, i, (i == off1 ? off2 : off1));
          updateBuilder(builder, mat, i, i == off1 ? off2 : off1, h.d);
          /***/

          unionToCluster.put(i, newTempCluster);

          currentTLClusters.remove(t1);
          currentTLClusters.remove(t2);
          currentTLClusters.add(newTempCluster);
        }
      }

      // Process all Points in density range
      HashSet<Integer> knnclus = processNearPoints(knn.getKNN(mainIt, k), unions, offsetStore, unionToCluster);

      if(knnclus.size() == 0) {
        // create new cluster if there is no nearby cluster
        int i = offsetStore.intValue(mainIt);
        TempCluster newCl = new TempCluster(mainIt, density, i);

        builder.setSize(mainIt, 1);

        unionToCluster.put(i, newCl);
        currentTLClusters.add(newCl);
      }
      else if(knnclus.size() == 1) {
        // join to nearby cluster if there is only one
        int neighOff = knnclus.iterator().next();
        int pointOff = offsetStore.intValue(mainIt);
        TempCluster tcluster = unionToCluster.get(neighOff);
        assert tcluster.clusterMatrixOffset == neighOff;

        int i = unions.union(pointOff, neighOff);
        assert i == pointOff || i == neighOff;
        tcluster.addPoint(mainIt, density, i);

        /***/
        updateMatrix(mat, i, i == pointOff ? neighOff : pointOff);
        updateBuilder(builder, mat, i, i == pointOff ? neighOff : pointOff, density);
        /***/

        unionToCluster.put(i, tcluster);
      }
      else {
        // if there are 2 or more nearby cluster: join all and add current point
        LinkedList<Cluster<DendrogramModel>> newClusters = new LinkedList<>();
        int currentOff = offsetStore.intValue(mainIt);

        for(int cluster : knnclus) {
          TempCluster tcluster = unionToCluster.get(cluster);
          newClusters.add(tcluster.finalizeCluster(clustering));

          assert tcluster.clusterMatrixOffset == cluster;
          int tcurrentOff = unions.union(currentOff, cluster);

          /***/
          updateMatrix(mat, tcurrentOff, tcurrentOff == currentOff ? cluster : currentOff);
          updateBuilder(builder, mat, tcurrentOff, tcurrentOff == currentOff ? cluster : currentOff, density);
          /***/

          currentTLClusters.remove(tcluster);
          currentOff = tcurrentOff;
        }
        TempCluster newTempCluster = new TempCluster(newClusters, currentOff);
        newTempCluster.addPoint(mainIt, density, currentOff);

        builder.setSize(mat.ix.seek(currentOff), builder.getSize(mat.ix.seek(currentOff)) + 1);

        unionToCluster.put(currentOff, newTempCluster);

        currentTLClusters.add(newTempCluster);
      }
    }
    if(LOG.isVerbose())
      LOG.verbose("Extracting top level clusters!");

    // finalize all leftover clusters with size > 1 and add them as clusters
    // accumulate clusters with size = 1 as noise
    ModifiableDBIDs noisePoints = DBIDUtil.newArray();

    // i should have found all points, so this should return nothing
    for(TempCluster tempCluster : currentTLClusters) {
      if(tempCluster.isNoise) {
        noisePoints.addDBIDs(tempCluster.members);
      }
      else {
        clustering.addToplevelCluster(tempCluster.finalizeCluster(clustering));
      }
    }
    if(LOG.isVerbose())
      LOG.verbose("Finished, " + noisePoints.size() + " noise points left!");

    clustering.addToplevelCluster(new Cluster<DendrogramModel>("noise", noisePoints, true, new DendrogramModel(0)));
    Metadata.hierarchyOf(clustering).addChild(builder.complete());

    return clustering;
  }

  /**
   * Processes the nearest neighbors and returns clusters in nearest neighbors
   * 
   * @param neigh Nearest neighbor list
   * @param unions UnionFind containing the clusters
   * @param offsetStore Mapping DBID -> offset
   * @param unionToCluster Mapping UnionFind component -> cluster
   * @return
   */
  private HashSet<Integer> processNearPoints(KNNList neigh, WeightedQuickUnionInteger unions, MapIntegerDBIDIntegerStore offsetStore, Int2ObjectOpenHashMap<TempCluster> unionToCluster) {
    HashSet<Integer> knnclus = new HashSet<Integer>();
    for(DoubleDBIDListIter knnit = neigh.iter(); knnit.valid(); knnit.advance()) {
      int tun = unions.find(offsetStore.intValue(knnit));
      if(unionToCluster.containsKey(tun) && !knnclus.contains(tun)) {
        knnclus.add(tun);
      }
    }
    return knnclus;
  }

  /**
   * Helper method for initializing the algorithms work order
   * 
   * @param mat Distance matrix
   * @param knns KNN store (for caching)
   * @param knn KNN Queue
   * @param offsetStore Mapping from DBID to integer in the matrix (filled
   *        during method)
   * @param unions Weighted integer union to track clusters (filled during
   *        method)
   * @return Distance list in work-order
   */
  private ModifiableDoubleDBIDList initializeWorkOrder(MatrixParadigm mat, KNNSearcher<DBIDRef> knn, MapIntegerDBIDIntegerStore offsetStore, WeightedQuickUnionInteger unions) {
    ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    for(DBIDArrayIter itx = mat.ix.seek(0); itx.valid(); itx.advance()) {
      offsetStore.put(itx, itx.getOffset());
      distances.add(knn.getKNN(itx, k).getKNNDistance(), itx);
      for(int i = -1; i < itx.getOffset(); i = unions.nextIndex(1)) {

      }
    }
    // sort for working order
    distances.sort();
    return distances;
  }

  /**
   * adds all cluster pairs with mat-distance < density to the distancePairHeap
   * 
   * @param currentTLClusters Clusters to check
   * @param mat Distance matrix
   * @param density Density Threshold
   * @param distancePairHeap Store heap
   */
  private void searchNearerClusters(HashSet<TempCluster> currentTLClusters, MatrixParadigm mat, double density, Heap<HeapEl> distancePairHeap) {
    for(TempCluster tC : currentTLClusters) {
      for(TempCluster oTC : currentTLClusters) {
        if(tC.clusterMatrixOffset < oTC.clusterMatrixOffset) {
          double d = mat.get(tC.clusterMatrixOffset, oTC.clusterMatrixOffset);
          if(d < density) {
            distancePairHeap.add(new HeapEl(d, tC, oTC));
          }
        }
      }
    }
  }

  /**
   * Add a Build node to the dendrogram builder and updates the sizes
   * 
   * @param builder Builder object
   * @param mat Distance Matrix
   * @param i First node index (keep)
   * @param j Second node index (discard)
   * @param distance distance value for the node
   */
  private void updateBuilder(PointerHierarchyRepresentationBuilder builder, MatrixParadigm mat, int i, int j, double distance) {
    final int sizex = builder.getSize(mat.ix.seek(i)),
        sizey = builder.getSize(mat.iy.seek(j));

    builder.add(mat.iy.seek(j), distance, mat.ix.seek(i));
    builder.setSize(mat.ix.seek(i), sizex + sizey);

  }

  /**
   * Matrix update function that works both with x > y and y > x
   * current merge is always towards x, but it is currently called so 
   * that it is merged towards the bigger x to maintain consistency
   * with the weighted integer union join
   * 
   * @param mat
   * @param x
   * @param y
   */
  private void updateMatrix(MatrixParadigm mat, int x, int y) {
    // assert x < y; current analysis does not follow this convention
    double[] matrix = mat.matrix;
    int j = 0;
    int xbase = MatrixParadigm.triangleSize(x);
    int ybase = MatrixParadigm.triangleSize(y);
    for(; j < FastMath.min(x, y); j++) {
      assert (j < FastMath.min(x, y));
      final int xb = xbase + j;
      matrix[xb] = FastMath.min(matrix[xb], matrix[ybase + j]);
    }
    j++; // Skip min(x,y)
    int jbase = MatrixParadigm.triangleSize(j);
    if(x < y) { // middle path is different for x > y
      for(; j < y; jbase += j++) {
        final int jb = jbase + x;
        matrix[jb] = FastMath.min(matrix[jb], matrix[ybase + j]);
      }
    }
    else { // other middle path
      for(; j < x; jbase += j++) {
        final int jb = xbase + j;
        matrix[jb] = FastMath.min(matrix[jb], matrix[jbase + y]);
      }
    }
    jbase += j++; // Skip y
    for(; j < mat.size; jbase += j++) {
      final int jb = jbase + x;
      matrix[jb] = FastMath.min(matrix[jb], matrix[jbase + y]);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distanceFunction.getInputTypeRestriction());
  }

  /**
   * Temporary cluster. Used to hold Cluster Information before the cluster is
   * added to the clustering structure.
   * 
   * @author Robert Gehde
   *
   */
  private class TempCluster {

    /**
     * Current Offset in the MatrixParadigm and Union Structure
     */
    int clusterMatrixOffset;

    /**
     * DBIDs in this cluster, not containing child cluster
     */
    ModifiableDBIDs members;

    /**
     * List of all child clusters
     */
    LinkedList<Cluster<DendrogramModel>> childs;

    /**
     * true if this is a noise cluster
     */
    boolean isNoise;

    /**
     * last density value where this cluster was modified
     */
    double lastHeight;

    /**
     * 
     * Creates a TempCluster with a single point.
     *
     * @param point
     * @param currentThreshold
     * @param offset
     */
    public TempCluster(DBIDRef point, double currentThreshold, int offset) {
      members = DBIDUtil.newArray();
      members.add(point);
      clusterMatrixOffset = offset;
      lastHeight = currentThreshold;
      isNoise = true;
      this.childs = new LinkedList<Cluster<DendrogramModel>>();
    }

    /**
     * Creates a none-leaf cluster with child clusters.
     *
     * @param childs
     * @param offset
     */
    public TempCluster(LinkedList<Cluster<DendrogramModel>> childs, int offset) {
      members = DBIDUtil.newArray();
      clusterMatrixOffset = offset;
      lastHeight = .0;
      isNoise = true;
      this.childs = new LinkedList<Cluster<DendrogramModel>>();
      for(Cluster<DendrogramModel> c : childs) {
        this.childs.add(c);
        isNoise = false;
        lastHeight = lastHeight < c.getModel().getDistance() ? c.getModel().getDistance() : lastHeight;
      }
    }

    /**
     * Adds a point to the cluster.
     * 
     * @param point
     * @param currentThreshold
     * @param offset
     */
    public void addPoint(DBIDRef point, double currentThreshold, int offset) {
      lastHeight = currentThreshold;
      clusterMatrixOffset = offset;
      isNoise = false;
      members.add(point);
    }

    /**
     * Adds the cluster hierarchy information to the clustering and creates a
     * cluster from this TempCluster.
     * 
     * @param clustering
     * @return
     */
    public Cluster<DendrogramModel> finalizeCluster(Clustering<DendrogramModel> clustering) {
      Cluster<DendrogramModel> t = new Cluster<DendrogramModel>(members, new DendrogramModel(lastHeight));
      for(Cluster<DendrogramModel> child : childs) {
        clustering.addChildCluster(t, child);
      }
      return t;
    }
  }

  /**
   * Elements for the distance Heap used to combine clusters.
   * 
   * @author Robert Gehde
   *
   */
  private class HeapEl implements Comparable<HeapEl> {
    double d;

    TempCluster l;

    TempCluster r;

    public HeapEl(double d, TempCluster leftCluster, TempCluster rightCluster) {
      this.d = d;
      this.l = leftCluster;
      this.r = rightCluster;
    }

    @Override
    public int compareTo(NaiveModeAnalysisClustering<O>.HeapEl arg0) {
      if(arg0.d > this.d) {
        return -1;
      }
      else if(arg0.d == this.d) {
        return 0;
      }
      else {
        return +1;
      }
    }
  }

  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("naiveModeAnalysis.distance", "Distance function to use for computing the the nearest neighbors in ModeAnalysis.");

    /**
     * Parameter for choosing the k Parameter
     */
    public static final OptionID Kp1_ID = new OptionID("naiveModeAnalysis.k", "The distance to the k-th nearest neighbor (excluding point itself) will be used for the density calculation.");

    /**
     * Distance to k-th nearest neighbor is used.
     */
    protected int k;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(Kp1_ID).addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT).grab(config, x -> k = x);
    }

    @Override
    public NaiveModeAnalysisClustering<O> make() {
      return new NaiveModeAnalysisClustering<O>(distance, k);
    }
  }
}
