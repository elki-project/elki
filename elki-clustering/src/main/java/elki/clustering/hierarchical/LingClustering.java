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

import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.utilities.datastructures.unionfind.UnionFind;
import elki.utilities.datastructures.unionfind.UnionFindUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.pairs.IntIntPair;

/**
 * 
 * Construction of k-cluster algorithm as introduced by R. F. Ling. A rather
 * unknown clustering
 * algorithm based on k-connected subsets. Introduced as a clustering on "well
 * defined mathematical properties".
 * <p>
 * It is implemented according to the paper. The original code was not
 * available.
 * <p>
 * Source:<br>
 * On the theory and construction of k-clusters<br>
 * R. F. Ling <br>
 * Graduate School of Buisness, University of Chicago<br>
 * 
 * @author Robert Gehde
 * 
 * @param <O> Database object on which the Distance is based on.
 */
@Reference(authors = "R. F. Ling", //
    title = "On the theory and construction of \\emph{k}-clusters", //
    booktitle = "The Computer Journal, Volume 15, Issue 4", //
    url = "https://doi.org/10.1093/comjnl/15.4.326", //
    bibkey = "DBLP:journals/cj/Ling72")
public class LingClustering<O> implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LingClustering.class);

  /**
   * DistanceFunction used.
   */
  private Distance<? super O> distanceFunction;

  /**
   * k Attribute
   */
  private int k;

  /**
   * 
   * Constructor.
   *
   * @param distanceFunction
   */
  public LingClustering(Distance<? super O> distanceFunction, int k) {
    this.distanceFunction = distanceFunction;
    this.k = k;
  }

  public Clustering<DendrogramModel> run(Relation<O> relation) {

    ArrayDBIDs adbids = DBIDUtil.ensureArray(relation.getDBIDs());
    // pairwise distance rank matrix
    int[][] rankmat = new int[adbids.size()][adbids.size()];
    // The k-th occurrence is the minimum rank where the id can be in a cluster.
    IntIntPair[] kthOcc = new IntIntPair[adbids.size()];
    // note ranks in clusters to skip them
    boolean[] rsfound = new boolean[(adbids.size() * (adbids.size() - 1)) / 2];
    // keep track of clusters
    UnionFind uf = UnionFindUtil.make(DBIDUtil.makeUnmodifiable(adbids));

    LinkedList<ModifiableDBIDs> clusterCandidates, topLevelClusters;

    Clustering<DendrogramModel> clustering = new Clustering<DendrogramModel>();
    HashMap<ModifiableDBIDs, List<Cluster<DendrogramModel>>> oldclusters = new HashMap<ModifiableDBIDs, List<Cluster<DendrogramModel>>>();
    LinkedList<Integer> inclusters = new LinkedList<Integer>();
    LinkedList<Integer> toprocess = new LinkedList<Integer>(),
        notinclusters = new LinkedList<Integer>();

    calculateRanks(adbids, rankmat, relation, kthOcc);

    if(LOG.isVerbose())
      LOG.verbose("Initial Setup finished! ");
    int r = (int) MathUtil.binomialCoefficient(k + 1, 2) - 1;

    int rankindex = 0;
    rankindex = raiseRank(r, toprocess, kthOcc, rankindex);

    for(DBIDArrayIter it = adbids.iter().seek(0); it.valid(); it.advance()) {
      uf.find(it);
    }

    while(r < rsfound.length && !(oldclusters.size() == 1 && oldclusters.keySet().iterator().next().size() == relation.size())) {
      // sort toprocess into inclusters and notinclusters
      klink(inclusters, toprocess, notinclusters, r, rankmat);

      if(!inclusters.isEmpty()) {
        clusterCandidates = link1(adbids, inclusters, r, rankmat, rsfound);
        topLevelClusters = new LinkedList<>(oldclusters.keySet());

        // find the new cluster (according to paper only 1 new cluster
        // try to delete all clusters that didnt change (same size and sharing
        // ids)
        for(Iterator<ModifiableDBIDs> it = clusterCandidates.iterator(); it.hasNext();) {
          ModifiableDBIDs cand = it.next();
          DBIDIter c = cand.iter();
          for(Iterator<ModifiableDBIDs> it2 = topLevelClusters.iterator(); it2.hasNext();) {
            ModifiableDBIDs cluster = it2.next();
            if(cand.size() == cluster.size() && uf.isConnected(c, cluster.iter())) {
              it.remove();
              it2.remove();
              continue;
            }
          }
        }

        // process the new cluster
        if(!clusterCandidates.isEmpty()) {
          if(LOG.isVerbose()) {
            LOG.verbose(clusterCandidates.size() + " Candidates and " + topLevelClusters.size() + " old clusters");
          }
          assert clusterCandidates.size() == 1;
          if(topLevelClusters.size() == 1) {
            oldclusters.put(clusterCandidates.getFirst(), oldclusters.get(topLevelClusters.getFirst()));
            oldclusters.remove(topLevelClusters.getFirst());
          }
          else {
            LinkedList<Cluster<DendrogramModel>> clist = new LinkedList<Cluster<DendrogramModel>>();
            for(ModifiableDBIDs cluster : topLevelClusters) {
              Cluster<DendrogramModel> cn = new Cluster<DendrogramModel>(cluster, new DendrogramModel(r));
              clist.add(cn);
              for(Cluster<DendrogramModel> co : oldclusters.get(cluster)) {
                clustering.addChildCluster(cn, co);
              }
              oldclusters.remove(cluster);
            }
            oldclusters.put(clusterCandidates.getFirst(), clist);
          }
          DBIDIter it = clusterCandidates.getFirst().iter();
          DBID pivot = DBIDUtil.deref(it);
          if(it.valid())
            it.advance();
          for(; it.valid(); it.advance()) {
            if(!uf.isConnected(pivot, it)) {
              uf.union(pivot, it);
            }
          }
        }
      }
      // chose first r* bigger than current r and not in a cluster
      if((oldclusters.size() == 1 && oldclusters.keySet().iterator().next().size() == relation.size()))
        continue;
      LinkedList<Integer> t = notinclusters;
      notinclusters = toprocess;
      toprocess = t;
      do {
        r++;
      }
      while(r < rsfound.length && rsfound[r]);
      rankindex = raiseRank(r, toprocess, kthOcc, rankindex);
    }
    for(ModifiableDBIDs tlc : oldclusters.keySet()) {
      Cluster<DendrogramModel> cn = new Cluster<DendrogramModel>(tlc, new DendrogramModel(r));
      for(Cluster<DendrogramModel> co : oldclusters.get(tlc)) {
        clustering.addChildCluster(cn, co);
      }
      clustering.addToplevelCluster(cn);
    }
    return clustering;
  }

  private int raiseRank(int r, LinkedList<Integer> toprocess, IntIntPair[] kthOcc, int index) {
    while(index < kthOcc.length && kthOcc[index].first <= r) {
      toprocess.add(kthOcc[index].second);
      index++;
    }
    return index;
  }

  /**
   * calculates the distance ranks and populates the rank matrix and the array
   * of k-th occurrence. The k-th occurrence is the minimum rank where the id
   * can be in a cluster.
   * 
   * @param adbids original DBID Data
   * @param rankmat pairwise distance-rank-matrix
   * @param relation original input relation
   * @param kthOcc Array for storing necessary condition
   */
  private void calculateRanks(ArrayDBIDs adbids, int[][] rankmat, Relation<O> relation, IntIntPair[] kthOcc) {
    DistanceQuery<O> dist = new QueryBuilder<O>(relation, distanceFunction).distanceQuery();
    int[] count = new int[adbids.size()];
    sortEl[] distancePairs = new sortEl[relation.size() * (relation.size() - 1) / 2];

    int ir = 0;
    for(DBIDArrayIter itx = adbids.iter().seek(0); itx.valid(); itx.advance()) {
      for(DBIDArrayIter ity = adbids.iter().seek(itx.getOffset()).advance(); ity.valid(); ity.advance()) {
        distancePairs[ir] = new sortEl(dist.distance(itx, ity), new IntIntPair(itx.getOffset(), ity.getOffset()));
        ir++;
      }
    }
    Arrays.parallelSort(distancePairs);
    for(int i = 0; i < distancePairs.length; i++) {
      IntIntPair p = distancePairs[i].p;
      rankmat[p.first][p.second] = i;
      rankmat[p.second][p.first] = i;
      count[p.first]++;
      count[p.second]++;
      if(count[p.first] == k) {
        kthOcc[p.first] = new IntIntPair(i, p.first);
      }
      if(count[p.second] == k) {
        kthOcc[p.second] = new IntIntPair(i, p.second);
      }
    }
    Arrays.sort(kthOcc);
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
  private LinkedList<ModifiableDBIDs> link1(ArrayDBIDs adbids, LinkedList<Integer> in, int r, int[][] rankmat, boolean[] rindices) {
    LinkedList<ModifiableDBIDs> result = new LinkedList<>();
    LinkedList<Integer> s = new LinkedList<Integer>(in);
    PriorityQueue<Integer> q = new PriorityQueue<Integer>();
    // BFS algorithm
    while(!s.isEmpty()) {
      LinkedList<Integer> itres = new LinkedList<Integer>();

      q.add(s.pop());
      while(!q.isEmpty()) {
        int i = q.poll();
        for(Iterator<Integer> it = s.iterator(); it.hasNext();) {
          int j = it.next();
          if(rankmat[i][j] < r) {
            it.remove();
            q.offer(j);
          }
        }
        // update visited ids
        for(int j : itres) {
          rindices[rankmat[i][j]] = true;
        }
        itres.add(i);
      }
      // return ids as BDID entries
      ModifiableDBIDs tres = DBIDUtil.newArray();
      DBIDArrayIter it = adbids.iter();
      for(int off : itres) {
        tres.add(it.seek(off));
      }
      result.add(tres);
    }
    return result;
  }

  /**
   * calculates the maximal (k,r)-bonded sets of the database by removing
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
  private void klink(LinkedList<Integer> in, LinkedList<Integer> proc, LinkedList<Integer> out, int r, int[][] rankmat) {
    boolean change;
    do {
      change = false;
      // check all proc points. Elements in 'in' satisfy the condition by
      // definition
      for(Iterator<Integer> it = proc.iterator(); it.hasNext();) {
        int i = it.next();
        int c = 0;
        // find close elements in in and proc
        for(Integer j : in) {
          if(rankmat[i][j] <= r) {
            c++;
          }
        }
        for(Integer j : proc) {
          if(i != j && rankmat[i][j] <= r) {
            c++;
          }
        }
        // if not enough close elements, move id to 'out' and note change
        if(c < k) {
          change = true;
          it.remove();
          out.add(i);
        }
      }
    }
    while(change);
    // if no change happend, all remaining elements in 'proc' are 'in'
    in.addAll(proc);
    proc.clear();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distanceFunction.getInputTypeRestriction());
  }

  private static class sortEl implements Comparable<sortEl> {
    protected IntIntPair p;

    protected double d;

    public sortEl(double d, IntIntPair p) {
      this.p = p;
      this.d = d;
    }

    @Override
    public int compareTo(LingClustering.sortEl arg0) {
      return d < arg0.d ? -1 : d == arg0.d ? 0 : 1;
    }

  }

  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("LingClustering.distance", "Distance function to use for computing the rank matrix.");

    /**
     * Parameter for choosing the k Parameter
     */
    public static final OptionID K_ID = new OptionID("LingClustering.k", "Parameter used for finding (k,r)-bonded sets.");

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
      new IntParameter(K_ID).addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT).grab(config, x -> k = x);
    }

    @Override
    public LingClustering<O> make() {
      return new LingClustering<O>(distance, k);
    }
  }
}
