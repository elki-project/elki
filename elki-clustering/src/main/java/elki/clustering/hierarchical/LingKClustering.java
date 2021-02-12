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

import java.util.*;

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
import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.datastructures.unionfind.UnionFind;
import elki.utilities.datastructures.unionfind.UnionFindUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.constraints.LessEqualConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Construction of k-cluster algorithm as introduced by R. F. Ling. A relatively
 * unknown clustering algorithm based on k-connected subsets. Introduced as a
 * clustering on "well defined mathematical properties".
 * <p>
 * The algorithm is fairly expensive, because it uses the ranks of pairs in the
 * distance matrix. Hence it needs quadratic memory. This implementation is
 * limited to data sets with about 2<sup>16</sup> instances.
 * <p>
 * Reference:
 * <p>
 * R. F. Ling<br>
 * On the theory and construction of k-clusters<br>
 * The Computer Journal 15(4)
 * 
 * @author Robert Gehde
 * @author Erich Schubert
 * 
 * @param <O> Database object on which the Distance is based on.
 */
@Reference(authors = "R. F. Ling", //
    title = "On the theory and construction of k-clusters", //
    booktitle = "The Computer Journal 15(4)", //
    url = "https://doi.org/10.1093/comjnl/15.4.326", //
    bibkey = "DBLP:journals/cj/Ling72")
public class LingKClustering<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LingKClustering.class);

  /**
   * Distance function used.
   */
  private Distance<? super O> distanceFunction;

  /**
   * k parameter.
   */
  private int k;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance to use
   * @param k K parameter
   */
  public LingKClustering(Distance<? super O> distanceFunction, int k) {
    this.distanceFunction = distanceFunction;
    this.k = k;
  }

  public PointerHierarchyRepresentationResult run(Relation<O> relation) {

    final ArrayDBIDs adbids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int trisize = MatrixParadigm.triangleSize(adbids.size());
    // pairwise distance rank matrix
    int[] rankmat = new int[trisize];
    // The k-th occurrence is the minimum rank where the id can be in a cluster.
    long[] kthOcc = new long[adbids.size()];
    HashSet<ModifiableDBIDs> clustermin = new HashSet<>();
    HashMap<DBID, Integer> idToIndex = new HashMap<DBID, Integer>();
    // The algorithm operates on distance ranks
    calculateRanks(adbids, rankmat, relation, kthOcc);
    for(DBIDArrayIter it = adbids.iter(); it.valid(); it.advance()) {
      idToIndex.put(DBIDUtil.deref(it), it.getOffset());
    }
    List<ModifiableDBIDs> clusterCandidates, topLevelClusters;
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(adbids, distanceFunction.isSquared());

    IntegerArray inclusters = new IntegerArray(adbids.size());
    IntegerArray toprocess = new IntegerArray(adbids.size()),
        notinclusters = new IntegerArray(adbids.size());

    // Starting value of r for finding k-bonded sets
    int r = (int) MathUtil.binomialCoefficient(k, 2) - 1;
    // note ranks in clusters to skip them
    boolean[] rsfound = new boolean[trisize];

    // keep track of clusters
    UnionFind uf = UnionFindUtil.make(DBIDUtil.makeUnmodifiable(adbids));
    int rankindex = raiseIndexToRank(r, toprocess, kthOcc, 0);
    while(r < rsfound.length && !(clustermin.size() == 1 && clustermin.iterator().next().size() == relation.size())) {

      // sort toprocess into inclusters and notinclusters
      klink(inclusters, toprocess, notinclusters, r, rankmat);
      // now inclusters contains the maximal (k,r)-bonded set
      if(!inclusters.isEmpty()) {
        // partition the max (k,r)-bonded set into r-connected sets
        clusterCandidates = link1(adbids, inclusters, r, rankmat, rsfound);
        // now clusterCandidates conatains all k-clusters of the given rank

        // find the new cluster (according to paper only 1 new cluster exists)
        topLevelClusters = new ArrayList<>(clustermin);
        filterClusterList(clusterCandidates, topLevelClusters, uf);

        // process the new cluster
        if(!clusterCandidates.isEmpty()) {
          if(LOG.isVerbose()) {
            LOG.verbose(clusterCandidates.size() + " Candidates and " + topLevelClusters.size() + " old clusters");
          }
          assert clusterCandidates.size() == 1 : "More than one candidate; actual number of candidates: " + clusterCandidates.size() + "at rank " + r;
          addClusterToRepresentation(clusterCandidates.get(0), topLevelClusters, uf, clustermin, r, builder, idToIndex);
        }
      }
      // chose first r* bigger than current r and not in a cluster
      if((clustermin.size() == 1 && clustermin.iterator().next().size() == relation.size())) {
        continue;
      }
      IntegerArray t = notinclusters;
      notinclusters = toprocess;
      toprocess = t;
      do {
        r++;
      }
      while(r < rsfound.length && rsfound[r]);
      rankindex = raiseIndexToRank(r, toprocess, kthOcc, rankindex);
    }
    return builder.complete();
  }

  /**
   * Calculates the maximal (k,r)-bonded sets of the database by removing
   * elements that don't match the definition until no changes are made. Before
   * the call, out should be empty, as elements that can't satisfy the
   * conditions are not looked at due to the main algorithm. After the call,
   * items in 'in' are (k,r)-bonded and items in 'out' are not. 'proc' should be
   * empty after this step.
   * 
   * @param in ids in clusters
   * @param proc ids that could possibly be in clusters after this step
   * @param out ids that are not in clusters before this step
   * @param r current rank threshold
   * @param rankmat pairwise distance-rank-matrix
   */
  private void klink(IntegerArray in, IntegerArray proc, IntegerArray out, int r, int[] rankmat) {
    boolean change;
    do {
      change = false;
      // check all proc points. Elements in 'in' satisfy the condition by
      // definition
      for(int i = 0; i < proc.size; i++) {
        final int x = proc.data[i];
        final int trix = MatrixParadigm.triangleSize(x);
        int c = 0;
        // find close elements in in and proc
        for(int j = 0; j < in.size; j++) {
          final int y = in.data[j];
          // FIXME: don't recompute triangleSize all the time. Makes it stable,
          // but not necassarily faster
          if(rankmat[offset(x, y, trix)] <= r) {
            c++;
          }
        }
        for(int j = 0; j < proc.size; j++) {
          final int y = proc.data[j];
          if(x != y && rankmat[offset(x, y, trix)] <= r) {
            c++;
          }
        }
        // if not enough close elements, move id to 'out' and note change
        if(c < k) {
          change = true;
          // ?
          proc.remove(i--, 1);
          out.add(x);
        }
      }
    }
    while(change);
    // if no change happened, all remaining elements in 'proc' are 'in'
    assert in.size + proc.size <= in.data.length; // capacity check
    System.arraycopy(proc.data, 0, in.data, in.size, proc.size);
    in.size += proc.size;
    proc.clear();
  }

  /**
   * Finds r-connected Set in the (k,r)-bonded set, aka the connected components
   * of the induced graph. These are the clusters of the algorithm. After call,
   * the array of visited ids is updated.
   * 
   * @param adbids original DBID Data
   * @param in (k,r)-bonded set
   * @param r current rank threshold
   * @param rankmat pairwise distance-rank-matrix
   * @param rindices visited ids
   * @return List of r-connected DBID-Sets
   */
  private List<ModifiableDBIDs> link1(ArrayDBIDs adbids, IntegerArray in, int r, int[] rankmat, boolean[] rindices) {
    List<ModifiableDBIDs> result = new ArrayList<>();
    IntegerArray s = new IntegerArray(in);
    IntegerArray itres = new IntegerArray();
    PriorityQueue<Integer> q = new PriorityQueue<>();
    // BFS algorithm
    while(!s.isEmpty()) {
      itres.clear();
      q.add(s.data[--s.size]);
      while(!q.isEmpty()) {
        int x = q.poll();
        for(int j = 0; j < s.size; j++) {
          int y = s.data[j];
          if(rankmat[offset(x, y)] <= r) {
            s.remove(j--, 1);
            q.offer(y);
          }
        }
        // update visited ids
        for(int j = 0; j < itres.size; j++) {
          int y = itres.data[j];
          rindices[rankmat[offset(x, y)]] = true;
        }
        itres.add(x);
      }
      // return ids as DBID entries
      ModifiableDBIDs tres = DBIDUtil.newArray(itres.size);
      DBIDArrayIter it = adbids.iter();
      for(int j = 0; j < itres.size; j++) {
        tres.add(it.seek(itres.data[j]));
      }
      result.add(tres);
    }
    return result;
  }

  /**
   * Filters the targetClusters list by removing elements from the filterList.
   * In this application, it is used to find the new cluster in
   * clusterCandidates, if there is one.
   * targetClusters and filterList should be based on the same DataSet
   * 
   * @param targetClusters Cluster list to be filtered
   * @param filterList Cluster list to be filtered by
   * @param uf Unionfind containing the Clusters in filterList as Connected Sets
   */
  private void filterClusterList(List<ModifiableDBIDs> targetClusters, List<ModifiableDBIDs> filterList, UnionFind uf) {
    // try to delete all clusters that didn't change (same size and sharing ids)
    for(Iterator<ModifiableDBIDs> it = targetClusters.iterator(); it.hasNext();) {
      ModifiableDBIDs cand = it.next();
      DBIDRef c = cand.iter(); // some cluster representative
      for(Iterator<ModifiableDBIDs> it2 = filterList.iterator(); it2.hasNext();) {
        ModifiableDBIDs cluster = it2.next();
        if(cand.size() == cluster.size() && uf.isConnected(c, cluster.iter())) {
          it.remove();
          it2.remove();
          continue;
        }
      }
    }
  }

  /**
   * removes the child cluster from the Set of current clusters, adds the new
   * cluster and updates unionfind and pointerhirarchybuilder
   * 
   * @param clusterCandidate the new cluster
   * @param candidateChilds the current clusters to be replaced
   * @param uf unionfind containing connected sets of the current clusters
   * @param currentClusters map of the current clusters to their child clusters
   * @param clustering the clustering result object
   * @param pointschildids map of toplevel clusters to their child. as the
   *        childcluster objects only contain "their" points, we need this to
   *        access all child points
   * @param r current rank
   */
  private void addClusterToRepresentation(ModifiableDBIDs clusterCandidate, List<ModifiableDBIDs> candidateChilds, UnionFind uf, HashSet<ModifiableDBIDs> currentClusters, int r, PointerHierarchyRepresentationBuilder builder, HashMap<DBID, Integer> map) {
    int minIDClusterCandidate = Integer.MAX_VALUE;
    DBID minDBID = null;
    ArrayModifiableDBIDs tmp = DBIDUtil.newArray(clusterCandidate);

    // search mininmum ID in the new Cluster
    for(DBIDIter it = clusterCandidate.iter(); it.valid(); it.advance()) {
      DBID temp = DBIDUtil.deref(it);
      int id = map.get(temp);
      if(id < minIDClusterCandidate) {
        minIDClusterCandidate = id;
        minDBID = temp;
      }
    }

    // remove the old cluster from the represantation
    currentClusters.add(clusterCandidate);
    for(ModifiableDBIDs cluster : candidateChilds) {
      currentClusters.remove(cluster);
      tmp.removeDBIDs(cluster);
    }
    //////////////

    // search mininmum ID in the new Points
    int minNewPoints = Integer.MAX_VALUE;
    DBID minDBIDNew = null;
    for(DBIDIter it = tmp.iter(); it.valid(); it.advance()) {
      DBID temp = DBIDUtil.deref(it);
      int id = map.get(temp);
      if(id < minNewPoints) {
        minNewPoints = id;
        minDBIDNew = temp;
      }
    }

    for(DBIDIter it = tmp.iter(); it.valid(); it.advance()) {
      if(!DBIDUtil.equal(minDBIDNew, it))
        builder.add(it, FastMath.log(r) - 6, minDBIDNew);
    }
    //////////////
    // update unionfind and builder
    for(DBIDIter it = clusterCandidate.iter(); it.valid(); it.advance()) {
      if(!builder.isLinked(it) && !DBIDUtil.equal(minDBID, it))
        builder.add(it, r /*FastMath.log(r) - 6*/, minDBID);
      if(!uf.isConnected(minDBID, it)) {
        uf.union(minDBID, it);
      }
    }
    builder.setSize(minDBID, clusterCandidate.size());
  }

  /**
   * Calculates the distance ranks and populates the rank matrix and the array
   * of k-th occurrence. The k-th occurrence is the minimum rank where the id
   * can be in a cluster.
   * 
   * @param adbids original DBID Data
   * @param rankmat pairwise distance-rank-matrix
   * @param relation original input relation
   * @param kthOcc Array for storing necessary condition
   */
  private void calculateRanks(ArrayDBIDs adbids, int[] rankmat, Relation<O> relation, long[] kthOcc) {
    DistanceQuery<O> dist = new QueryBuilder<>(relation, distanceFunction).distanceQuery();
    // Get a modifiable, serialized triangle distance matrix:
    double[] dists = new double[rankmat.length];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing distance matrix.", rankmat.length, LOG) : null;
    int pos = 0;
    DBIDArrayIter iter2 = adbids.iter();
    for(DBIDArrayIter itx = adbids.iter(); itx.valid(); itx.advance()) {
      final int x = itx.getOffset();
      assert pos == MatrixParadigm.triangleSize(x);
      for(DBIDArrayIter ity = iter2.seek(0); ity.getOffset() < x; ity.advance()) {
        assert (int) FastMath.floor(FastMath.sqrt(0.25 + 2 * pos) + 0.5) == x;
        assert pos - MatrixParadigm.triangleSize((int) FastMath.floor(FastMath.sqrt(0.25 + 2 * pos) + 0.5)) == ity.getOffset();
        dists[pos++] = dist.distance(itx, ity);
      }
      if(prog != null) {
        prog.setProcessed(pos, LOG);
      }
    }
    assert pos == dists.length;
    if(LOG.isVerbose()) {
      LOG.verbose("Converting to rank matrix");
    }
    int[] idx = MathUtil.sequence(0, rankmat.length);
    DoubleIntegerArrayQuickSort.sort(dists, idx, dists.length);
    int[] count = new int[adbids.size()];
    for(int i = 0; i < pos; i++) {
      int p = idx[i];
      rankmat[p] = i;
      final int x = (int) FastMath.floor(FastMath.sqrt(0.25 + 2 * p) + 0.5);
      final int y = p - MatrixParadigm.triangleSize(x);
      assert y < x;
      if(++count[x] == 1) {
        kthOcc[x] = pair(i, x);
      }
      if(++count[y] == 1) {
        kthOcc[y] = pair(i, y);
      }
    }
    Arrays.sort(kthOcc);
  }

  /**
   * Adds all points that can be in a clustering with the given rank and returns
   * the index of the first not added point.
   * 
   * @param r Rank
   * @param toprocess Array to add the points to
   * @param kthOcc the array containing the rank where the point occured the kth
   *        time in a distance pair
   * @param index index of the first not added point before invocation
   * @return index of the first not added point after this method
   */
  private static int raiseIndexToRank(int r, IntegerArray toprocess, long[] kthOcc, int index) {
    while(index < kthOcc.length && first(kthOcc[index]) <= r) {
      toprocess.add(second(kthOcc[index++]));
    }
    return index;
  }

  /**
   * Store an int,int pair as a long.
   *
   * @param p1 first part
   * @param p2 second part
   * @return Pair
   */
  private static long pair(int p1, int p2) {
    return (((long) p1) << 32) | p2;
  }

  /**
   * First part of a int,int pair stored as long.
   *
   * @param p pair
   * @return First part
   */
  private static int first(long p) {
    return (int) (p >>> 32);
  }

  /**
   * Second part of a int,int pair stored as long.
   *
   * @param p pair
   * @return Second part
   */
  private static int second(long p) {
    return (int) p; // truncate
  }

  /**
   * x,y to triangle index.
   *
   * @param x Coordinate 1
   * @param y Coordinate 2
   * @return Offset
   */
  private static int offset(int x, int y) {
    return x < y ? (MatrixParadigm.triangleSize(y) + x) : (MatrixParadigm.triangleSize(x) + y);
  }

  /**
   * x,y to triangle index.
   *
   * @param x Coordinate 1
   * @param y Coordinate 2
   * @param possible shortcut for triangle size x
   * @return Offset
   */
  private static int offset(int x, int y, int trix) {
    return x < y ? (MatrixParadigm.triangleSize(y) + x) : (trix + y);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distanceFunction.getInputTypeRestriction());
  }

  /**
   * Parameterizer
   *
   * @author Robert Gehde
   *
   * @param <O> Input data type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("LingKClustering.distance", "Distance function to use for computing the rank matrix.");

    /**
     * Parameter for choosing the k Parameter
     */
    public static final OptionID K_ID = new OptionID("LingKClustering.k", "Parameter used for finding (k,r)-bonded sets.");

    /**
     * k used for (k,r)-bonded sets.
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
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .addConstraint(new LessEqualConstraint(65535)) //
          .grab(config, x -> k = x);
    }

    @Override
    public LingKClustering<O> make() {
      return new LingKClustering<>(distance, k);
    }
  }
}
