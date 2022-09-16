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
package elki.index.tree.metrical.vptree;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.DistancePriorityIndex;
import elki.index.IndexFactory;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.utilities.Alias;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.datastructures.heap.ComparableMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Geometric Near-neighbor Access Tree (GNAT), also known as Multi Vantage Point
 * Tree or MVP-Tree.
 * <p>
 * In a Multi Vantage Point Tree the data is split into Voronoi Cells (Dirichlet
 * Domain) defined by the vantage Points
 * <p>
 * Reference:
 * <p>
 * S. Brin<br>
 * Near Neighbor Search in Large Metric Spaces<br>
 * Proc. 21th Int. Conf. on Very Large Data Bases (VLDB)
 * 
 * @author Robert Gehde
 * @since 0.8.0
 *
 * @param <O> Object type indexed
 */
@Reference(authors = "S. Brin", //
    title = "Near Neighbor Search in Large Metric Spaces", //
    booktitle = "Proc. 21th Int. Conf. on Very Large Data Bases (VLDB)", //
    url = "http://www.vldb.org/conf/1995/P574.PDF", //
    bibkey = "DBLP:conf/vldb/Brin95")
public class GNAT<O> implements DistancePriorityIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GNAT.class);

  /**
   * Counter for distance computations.
   */
  protected long distComputations;

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * Distance Function to use
   */
  Distance<? super O> distFunc;

  /**
   * Actual distance query on the Data
   */
  DistanceQuery<O> distQuery;

  /**
   * Distance storage for building
   */
  ModifiableDoubleDBIDList sorted;

  /**
   * Random factory for selecting vantage points
   */
  RandomFactory random;

  /**
   * Sample size for selecting vantage points
   */
  int numberVPs;

  /**
   * Root node from the tree
   */
  Node root;

  /**
   * Constructor.
   *
   * @param relation data for tree construction
   * @param distance distance function for tree construction
   */
  public GNAT(Relation<O> relation, Distance<? super O> distance, RandomFactory random, int numberVPs) {
    this.relation = relation;
    this.distFunc = distance;
    this.random = random;
    this.distQuery = distance.instantiate(relation);
    this.numberVPs = numberVPs;
  }

  @Override
  public void initialize() {
    sorted = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      sorted.add(Double.NaN, it);
    }
    root = new Node(numberVPs);
    buildTree(root, relation.getDBIDs(), numberVPs);
  }

  /**
   * Compute a distance, and count.
   *
   * @param a First object
   * @param b Second object
   * @return Distance
   */
  private double distance(DBIDRef a, DBIDRef b) {
    ++distComputations;
    return distQuery.distance(a, b);
  }

  /**
   * Compute a distance, and count.
   *
   * @param a First object
   * @param b Second object
   * @return Distance
   */
  private double distance(O a, DBIDRef b) {
    ++distComputations;
    return distQuery.distance(a, b);
  }

  /**
   * builds the tree recursively
   * 
   * @param current current node to build
   * @param content data to index
   * @param vps number of vantage points to use
   */
  private void buildTree(Node current, DBIDs content, int vps) {
    // find vantage points
    current.vps = findVantagePoints(content, vps);
    // array to cache the distances to the vps for bound tracking
    double[] distances = new double[vps];
    // array to build up the childtree contents
    ModifiableDBIDs[] children = new ModifiableDBIDs[vps];
    // sort content
    for(DBIDIter iter = content.iter(); iter.valid(); iter.advance()) {
      // check if iter is a vp
      int isvp = -1;
      for(DBIDArrayIter vpiter = current.vps.iter().seek(0); vpiter.valid(); vpiter.advance()) {
        if(DBIDUtil.equal(iter, vpiter)) {
          isvp = vpiter.getOffset();
          break;
        }
      }
      // sort to according child and calculate distance bounds
      int childoffset = -1;
      double mindist = Double.MAX_VALUE;
      for(DBIDArrayIter vpiter = current.vps.iter(); vpiter.valid(); vpiter.advance()) {
        final double distance = distance(vpiter, iter);
        distances[vpiter.getOffset()] = distance;
        if(distance < mindist) {
          mindist = distance;
          childoffset = vpiter.getOffset();
        }
      }
      // if not a vantage point, add to children
      if(isvp == -1) {
        // childoffset is now the index of the voronoi cell containing iter
        if(children[childoffset] == null) {
          children[childoffset] = DBIDUtil.newArray();
        }
        children[childoffset].add(iter);
      }
      // track bounds
      // (paper applies this only to children, but if i understood that right,
      // there can be a degenerate case where the vantage point is actually
      // nearest or farthest from the other vp. That would mean that the vp is
      // not checked in pruning cases, but could be in range. So we track that
      // point as well)
      for(int i = 0; i < children.length; i++) {
        if(current.lowerBound[i][childoffset] > distances[i]) {
          current.lowerBound[i][childoffset] = distances[i];
        }
        if(current.upperBound[i][childoffset] < distances[i]) {
          current.upperBound[i][childoffset] = distances[i];
        }
      }
    }
    // recursively build children
    for(int i = 0; i < vps; i++) {
      // only build child if there are nodes in the child-tree
      if(children[i] != null) {
        int cvps = (numberVPs * children[i].size()) / relation.size();
        // bound cvps to [2,200]
        cvps = cvps > 200 ? 200 : cvps < 2 ? 2 : cvps;
        buildTree(current.children[i] = new Node(cvps), children[i], cvps);
      }
    }
  }

  /**
   * Finds a vantage points in the DBIDs between left and right
   *
   * @param content content to process
   * @param vps Number of vantage points to choose
   * @return vantage point
   */
  private ArrayDBIDs findVantagePoints(DBIDs content, int vps) {
    vps = Math.min(content.size(), vps);
    int sampleSize = Math.min(vps * 3, content.size());
    // target workset
    ArrayModifiableDBIDs target = DBIDUtil.newArray(vps);
    // sample candidates
    ModifiableDBIDs candidates = DBIDUtil.randomSample(content, sampleSize, random);
    // saves the minimum distances to chosen candidates
    ModifiableDoubleDBIDList distlist = DBIDUtil.newDistanceDBIDList(sampleSize);
    // first loop
    DBIDVar f = DBIDUtil.randomSample(candidates, random);
    target.add(f);
    candidates.remove(f);
    for(DBIDIter iter = candidates.iter(); iter.valid(); iter.advance()) {
      distlist.add(distQuery.distance(f, iter), iter);
    }
    for(int i = 1; i < vps; i++) {
      DBIDVar best = DBIDUtil.newVar();
      double dbest = -1.0;
      // find the point with maximum minimum distance to a chosen candidate
      for(DoubleDBIDListMIter iter = distlist.iter(); iter.valid(); iter.advance()) {
        if(dbest < iter.doubleValue()) {
          dbest = iter.doubleValue();
          best.set(iter);
        }
      }
      target.add(best);
      candidates.remove(best);
      // update minimum distances to chosen candidates
      for(DoubleDBIDListMIter iter = distlist.iter(); iter.valid(); iter.advance()) {
        double newdist = distQuery.distance(best, iter);
        if(newdist < iter.doubleValue()) {
          iter.setDouble(newdist);
        }
      }
    }
    return target;
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new GNATKNNObjectSearcher() : null;
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new GNATKNNDBIDSearcher() : null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new GNATRangeObjectSearcher() : null;
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new GNATRangeDBIDSearcher() : null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new GNATPriorityObjectSearcher() : null;
  }

  @Override
  public PrioritySearcher<DBIDRef> priorityByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new GNATPriorityDBIDSearcher() : null;
  }

  /**
   * check intersection of 2 intervals
   * 
   * @param l1 first lower bound
   * @param u1 first upper bound
   * @param l2 second lower bound
   * @param u2 second upper bound
   * @return if intervals intersect
   */
  private static boolean intersect(double l1, double u1, double l2, double u2) {
    return l1 <= u2 && l2 <= u1;
  }

  @Override
  public void logStatistics() {
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".distancecalcs", distComputations));
    }
  }

  /**
   * The Node class saves the important information for the each node
   * 
   * @author Robert Gehde
   */
  static class Node {
    /**
     * vantage points
     */
    ArrayDBIDs vps;

    /**
     * child trees; children[i] corresponds to vps.iter.seek[i]
     */
    Node[] children;

    /**
     * upper and lower distance bound for the other subtrees;
     * lowerBound[vp][child] is distance Bound from vp to child
     */
    double[][] lowerBound, upperBound;

    public Node(int vps) {
      this.vps = DBIDUtil.newArray(vps);
      this.children = new Node[vps];
      this.lowerBound = new double[vps][vps];
      this.upperBound = new double[vps][vps];
      for(int i = 0; i < vps; i++) {
        Arrays.fill(lowerBound[i], Double.MAX_VALUE);
        Arrays.fill(upperBound[i], -1);
      }
    }
  }

  /**
   * kNN query for the mvp-tree.
   *
   * @author Robert Gehde
   */
  public static abstract class GNATKNNSearcher {
    /**
     * Search the k nearest neighbors
     * 
     * @param knns Output heap
     * @param node Current node
     * @param tau Threshold tau
     * @return new threshold
     */
    protected double mvpKNNSearch(KNNHeap knns, Node node, double tau) {
      if(node == null) {
        return tau;
      }
      DBIDArrayIter iter = node.vps.iter();
      double[] scrapDists = new double[node.vps.size()];
      int[] ind = new int[node.vps.size()];
      // sort distance and check vps
      for(iter.seek(0); iter.valid(); iter.advance()) {
        double x = queryDistance(iter);
        tau = knns.insert(x, iter);
        final int off = iter.getOffset();
        // prioritize by distance to vantage point
        scrapDists[off] = x;
        ind[off] = off;
        // alternative: minimum distance to a child
        // But beware that the distances are accessed below again!
        // scrapDists[candidates++] = x - node.upperBound[off][off];
      }
      DoubleIntegerArrayQuickSort.sort(scrapDists, ind, ind.length);

      outer: for(int i = 0; i < ind.length; i++) {
        final int offi = ind[i];
        if(node.children[offi] == null) {
          continue;
        }
        // check bounds for this child
        for(int j = 0; j < ind.length; j++) {
          final int offj = ind[j];
          final double x = scrapDists[j];
          if(x > node.upperBound[offj][offi] + tau || x < node.lowerBound[offj][offi] - tau) {
            continue outer;
          }
        }
        // recurse into child:
        tau = mvpKNNSearch(knns, node.children[offi], tau);
      }
      return tau;
    }

    /**
     * Query the distance to a query object.
     *
     * @param iter Target object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef iter);
  }

  /**
   * kNN search for the VP-Tree.
   *
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class GNATKNNObjectSearcher extends GNATKNNSearcher implements KNNSearcher<O> {
    /**
     * Current query object
     */
    private O query;

    @Override
    public KNNList getKNN(O query, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      this.query = query;
      mvpKNNSearch(knns, root, Double.POSITIVE_INFINITY);
      return knns.toKNNList();
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return GNAT.this.distance(query, p);
    }
  }

  /**
   * kNN search for the VP-Tree.
   *
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class GNATKNNDBIDSearcher extends GNATKNNSearcher implements KNNSearcher<DBIDRef> {
    /**
     * Current query object
     */
    private DBIDRef query;

    @Override
    public KNNList getKNN(DBIDRef query, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      this.query = query;
      mvpKNNSearch(knns, root, Double.POSITIVE_INFINITY);
      return knns.toKNNList();
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return GNAT.this.distance(query, p);
    }
  }

  /**
   * range query for the mvp-tree
   * 
   * @author Robert Gehde
   */
  public static abstract class GNATRangeSearcher {
    /**
     * Perform a range search on the MVP Tree
     *
     * @param result Output storage
     * @param node Current node
     * @param range Range threshold
     */
    protected void mvpRangeSearch(ModifiableDoubleDBIDList result, Node node, double range) {
      if(node == null) {
        return;
      }
      int numChild = node.vps.size();
      boolean[] ignoreChildren = new boolean[numChild];
      for(DBIDArrayIter iter = node.vps.iter().seek(0); iter.valid(); iter.advance()) {
        if(ignoreChildren[iter.getOffset()]) {
          continue;
        }
        double x = queryDistance(iter);
        if(x <= range) {
          result.add(x, iter);
        }
        // check intersection of [x-range,x+range] and child[i] range as seen
        // from iter, if empty (not intersecting), ignore child
        for(int i = 0; i < numChild; i++) {
          if(!ignoreChildren[i] && node.children[i] != null // not ignored yet
              && !intersect(x - range, x + range, node.lowerBound[iter.getOffset()][i], node.upperBound[iter.getOffset()][i])) {
            ignoreChildren[i] = true;
          }
        }
      }
      // search children
      for(int i = 0; i < numChild; i++) {
        if(!ignoreChildren[i] && node.children[i] != null) {
          mvpRangeSearch(result, node.children[i], range);
        }
      }
    }

    /**
     * Query the distance to a query object.
     *
     * @param iter Target object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef iter);
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class GNATRangeObjectSearcher extends GNATRangeSearcher implements RangeSearcher<O> {
    /**
     * Current query object
     */
    private O query;

    @Override
    public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      mvpRangeSearch(result, root, range);
      return result;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return GNAT.this.distance(query, p);
    }
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class GNATRangeDBIDSearcher extends GNATRangeSearcher implements RangeSearcher<DBIDRef> {
    /**
     * Current query object
     */
    private DBIDRef query;

    @Override
    public ModifiableDoubleDBIDList getRange(DBIDRef query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      mvpRangeSearch(result, root, range);
      return result;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return GNAT.this.distance(query, p);
    }
  }

  /**
   * Search position for priority search.
   *
   * @author Robert Gehde
   */
  private static class PrioritySearchBranch implements Comparable<PrioritySearchBranch> {
    /**
     * Minimum distance
     */
    double mindist;

    /**
     * associated vantage point
     */
    DBID vp;

    /**
     * Node
     */
    Node node;

    /**
     * Constructor.
     *
     * @param mindist Minimum distance
     * @param node Node
     * @param vp Vantage point
     */
    public PrioritySearchBranch(double mindist, Node node, DBID vp) {
      this.mindist = mindist;
      this.node = node;
      this.vp = vp;
    }

    @Override
    public int compareTo(PrioritySearchBranch o) {
      return Double.compare(this.mindist, o.mindist);
    }
  }

  /**
   * priority search query for mvp-tree
   * 
   * @author Robert Gehde
   *
   * @param <T> Query object
   */
  public abstract class GNATPrioritySearcher<T> implements PrioritySearcher<T> {
    /**
     * Min heap for searching.
     */
    private ComparableMinHeap<PrioritySearchBranch> heap = new ComparableMinHeap<>();

    /**
     * Stopping threshold.
     */
    private double threshold;

    /**
     * Current search position.
     */
    private PrioritySearchBranch cur;

    /**
     * Start the next search.
     *
     * @return Searcher
     */
    protected PrioritySearcher<T> doSearch() {
      this.threshold = Double.POSITIVE_INFINITY;
      this.heap.clear();
      this.heap.add(new PrioritySearchBranch(0, root, null));
      // process root node and put first childs on stack -> not valid yet
      advance();
      // process first child -> valid current
      return advance();
    }

    @Override
    public PrioritySearcher<T> advance() {
      if(heap.isEmpty()) {
        cur = null;
        return this;
      }
      // Get next
      cur = heap.poll();
      if(cur.node != null) {
        final int numChild = cur.node.vps.size();
        boolean[] ignoreChildren = new boolean[numChild];
        double[] distances = new double[numChild];
        DBIDArrayIter iter = cur.node.vps.iter();
        for(; iter.valid(); iter.advance()) {
          final int vi = iter.getOffset();
          if(ignoreChildren[vi]) {
            continue;
          }
          double x = distances[vi] = queryDistance(iter);
          // check intersection of [x-range,x+range] and child[i] range as seen
          // from iter, if empty (not intersecting), ignore child
          for(int i = 0; i < numChild; i++) {
            if(!ignoreChildren[i] && cur.node.children[i] != null //
                && !intersect(x - threshold, x + threshold, cur.node.lowerBound[vi][i], cur.node.upperBound[vi][i])) {
              ignoreChildren[i] = true;
            }
          }
        }
        // search children
        for(int i = 0; i < numChild; i++) {
          if(!ignoreChildren[i]) {
            // distance to this vp - maxdist from cell point to vp
            double cdist = Math.max(distances[i] - cur.node.upperBound[i][i], cur.mindist);
            heap.add(new PrioritySearchBranch(cdist, cur.node.children[i], DBIDUtil.deref(iter.seek(i))));
          }
        }
      }
      return this;
    }

    @Override
    public int internalGetIndex() {
      return cur.vp.internalGetIndex();
    }

    @Override
    public boolean valid() {
      return cur != null;
    }

    @Override
    public PrioritySearcher<T> decreaseCutoff(double threshold) {
      assert threshold <= this.threshold : "Thresholds must only decreasee.";
      this.threshold = threshold;
      return this;
    }

    @Override
    public double computeExactDistance() {
      return queryDistance(cur.vp);
    }

    @Override
    public double allLowerBound() {
      return cur.mindist;
    }

    @Override
    public double getLowerBound() {
      return cur.mindist;
    }

    /**
     * Query the distance to a query object.
     *
     * @param iter Target object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef iter);
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class GNATPriorityObjectSearcher extends GNATPrioritySearcher<O> {
    /**
     * Current query object
     */
    private O query;

    @Override
    public PrioritySearcher<O> search(O query) {
      this.query = query;
      doSearch();
      return this;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return GNAT.this.distance(query, p);
    }
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class GNATPriorityDBIDSearcher extends GNATPrioritySearcher<DBIDRef> {
    /**
     * Current query object
     */
    private DBIDRef query;

    @Override
    public PrioritySearcher<DBIDRef> search(DBIDRef query) {
      this.query = query;
      doSearch();
      return this;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return GNAT.this.distance(query, p);
    }
  }

  /**
   * Index Factory
   *
   * @author Robert Gehde
   *
   * @param <O> Object type
   */
  @Alias({ "MVPTree", "mvp" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O> {
    /**
     * Distance Function
     */
    Distance<? super O> distance;

    /**
     * Random factory
     */
    RandomFactory random;

    /**
     * Sample size
     */
    int numbervps;

    /**
     * Constructor.
     *
     * @param distFunc Distance function
     * @param random Random generator
     * @param numberVantagePoints Number of vantage points to use
     */
    public Factory(Distance<? super O> distFunc, RandomFactory random, int numberVantagePoints) {
      super();
      this.distance = distFunc;
      this.random = random;
      this.numbervps = numberVantagePoints;
    }

    @Override
    public GNAT<O> instantiate(Relation<O> relation) {
      return new GNAT<>(relation, distance, random, numbervps);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     *
     * @author Robert Gehde
     */
    public static class Par<O extends NumberVector> implements Parameterizer {
      /**
       * Parameter to specify the distance function to determine the distance
       * between database objects, must extend
       * {@link elki.distance.Distance}.
       */
      public final static OptionID DISTANCE_FUNCTION_ID = new OptionID("vptree.distanceFunction", "Distance function to determine the distance between objects.");

      /**
       * Parameter to specify the sample size for choosing vantage point
       */
      public final static OptionID NUMBER_VANTAGE_POINTS_ID = new OptionID("vptree.numberVantagePoints", "Number of Vantage points on root layer and thus number of children on root layer.");

      /**
       * Parameter to specify the random generator seed
       */
      public final static OptionID SEED_ID = new OptionID("vptree.seed", "The random number generator seed.");

      /**
       * Distance function
       */
      protected Distance<? super O> distance;

      /**
       * Random generator
       */
      protected RandomFactory random;

      /**
       * Sample size
       */
      protected int amountVantagePoints;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<? super O>>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class)//
            .grab(config, x -> {
              this.distance = x;
              if(!distance.isMetric()) {
                LOG.warning("GNAT requires a metric to be exact.");
              }
            });
        new IntParameter(NUMBER_VANTAGE_POINTS_ID, 10) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> this.amountVantagePoints = x);
        new RandomParameter(SEED_ID).grab(config, x -> random = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(distance, random, amountVantagePoints);
      }
    }
  }
}
