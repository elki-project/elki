/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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

import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.type.TypeInformation;
import elki.database.datastore.DBIDDataStore;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.DistancePriorityIndex;
import elki.index.IndexFactory;
import elki.index.tree.metrical.covertree.CoverTree;
import elki.logging.Logging;
import elki.logging.statistics.Counter;
import elki.math.MathUtil;
import elki.utilities.Alias;
import elki.utilities.datastructures.arrays.ArrayUtil;
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
 * Multi Vantage Point Tree with no further information
 * 
 * In a Multi Vantage Point Tree the data is split into Voronoi Cells (Dirichlet
 * Domain) defined by the vantage Points
 * 
 * 
 * @author Robert Gehde
 *
 * @param <O>
 *
 */

public class MVPTree<O> implements DistancePriorityIndex<O> {

  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CoverTree.class);

  /**
   * Counter for comparisons.
   */
  protected final Counter objaccess;

  /**
   * Counter for distance computations.
   */
  protected final Counter distcalc;

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * Distance Function to use
   */
  Distance<O> distFunc;

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
   * The Node Class saves the important information for the each Node
   * 
   * @author Robert Gehde
   *
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
        for(int j = 0; j < vps; j++) {
          lowerBound[i][j] = -1.0; // dist cant be negative
          upperBound[i][j] = Double.MAX_VALUE;
        }
      }
    }
  }

  /**
   * 
   * Constructor.
   *
   * @param relation data for tree construction
   * @param distance distance function for tree construction
   */
  public MVPTree(Relation<O> relation, Distance<O> distance, RandomFactory random, int numberVPs) {
    this.relation = relation;
    this.distFunc = distance;
    this.random = random;
    this.distQuery = distance.instantiate(relation);
    this.numberVPs = numberVPs;
    if(LOG.isStatistics()) {
      String prefix = this.getClass().getName();
      this.objaccess = LOG.newCounter(prefix + ".objaccess");
      this.distcalc = LOG.newCounter(prefix + ".distancecalcs");
    }
    else {
      this.objaccess = null;
      this.distcalc = null;
    }
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
   * builds the tree recursively
   * 
   * @param current current node to build
   * @param left left bound of the node
   * @param right right bound of the node
   * @param iter reference to the sorted DBIDs
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
      boolean isvp = false;
      for(DBIDArrayIter vpiter = current.vps.iter().seek(0); vpiter.valid(); vpiter.advance()) {
        if(DBIDUtil.equal(iter, vpiter)) {
          isvp = true;
          break;
        }
      }
      if(isvp) {
        continue;
      }
      // iter is not a vp so we sort it into the according child
      int childoffset = -1;
      double mindist = Double.MAX_VALUE;
      for(DBIDArrayIter vpiter = current.vps.iter().seek(0); vpiter.valid(); vpiter.advance()) {
        double distance = distQuery.distance(vpiter, iter);
        countDistanceComputation();
        distances[vpiter.getOffset()] = distance;
        if(distance < mindist) {
          mindist = distance;
          childoffset = vpiter.getOffset();
        }
      }
      // childoffset is now the index of the voronoi cell containing iter
      if(children[childoffset] == null) {
        children[childoffset] = DBIDUtil.newArray();
      }
      children[childoffset].add(iter);

      // track bounds for children
      for(int i = 0; i < children.length; i++) {
        if(current.lowerBound[i][childoffset] > distances[i]) {
          current.lowerBound[i][childoffset] = distances[i];
        }
        if(current.upperBound[i][childoffset] < distances[i]) {
          current.upperBound[i][childoffset] = distances[i];
        }
      }
    }
    // recursivly build childs
    for(int i = 0; i < vps; i++) {
      // only build child if there are nodes in the child-tree
      if(children[i] != null) {
        int cvps = vps * (children[i].size() / relation.size());
        // bound cvps to [2,200]
        cvps = cvps > 200 ? 200 : cvps < 2 ? 2 : cvps;
        current.children[i] = new Node(cvps);
        buildTree(current.children[i], children[i], cvps);
      }
    }
  }

  /**
   * TODO size of random sample
   * TODO random seed
   * 
   * finds a vantage points in the DBIDs between left and right
   * 
   * @param left left bound
   * @param right right bound
   * @param iter DBIDs to find a vantage point in
   * @return vantage point
   */
  private ArrayDBIDs findVantagePoints(DBIDs content, int vps) {
    // target workset
    ArrayModifiableDBIDs target = DBIDUtil.newArray(vps);
    // sample candidates
    ModifiableDBIDs candidates = DBIDUtil.randomSample(content, vps * 3, random);
    // saves the minimum distances to chosen candidates
    ModifiableDoubleDBIDList distlist = DBIDUtil.newDistanceDBIDList(vps * 3);
    // first loop
    DBIDVar f = DBIDUtil.randomSample(candidates, random);
    target.add(f);
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
    Distance<? super O> df = distanceQuery.getDistance();
    // only should work for same distance as construction distance
    if(df.getClass().equals(distFunc.getClass())) {
      return new MVPTreeKNNSearcher();
    }
    return null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    Distance<? super O> df = distanceQuery.getDistance();
    // only should work for same distance as construction distance
    if(df.getClass().equals(distFunc.getClass())) {
      return new MVPTreeRangeSearcher();
    }
    return null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
//    Distance<? super O> df = distanceQuery.getDistance();
//    // only should work for same distance as construction distance
//    if(df.getClass().equals(distFunc.getClass())) {
//      return new MVPTreePrioritySearcher();
//    }
    return null;
  }

  /**
   * kNN query for the vp-tree.
   *
   * @author Robert Gehde
   */
  public class MVPTreeKNNSearcher implements KNNSearcher<O> {

    @Override
    public KNNList getKNN(O obj, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      mvpKNNSearch(obj, knns, root, Double.MAX_VALUE);
      return knns.toKNNList();
    }

    private double mvpKNNSearch(O obj, KNNHeap knns, Node node, double tau) {
      if(node == null) {
        return tau;
      }
      int numChild = node.vps.size();
      boolean[] ignoreChildren = new boolean[numChild];
      for(DBIDArrayIter iter = node.vps.iter().seek(0); iter.valid(); iter.advance()) {
        double x = distQuery.distance(iter, obj);
        countDistanceComputation();
        if(x < tau) {
          knns.insert(x, iter);
          tau = knns.getKNNDistance();
        }
        // we only check the ingore flag after the distance calculation, because
        // the vp is not tracked in the child bounds and thus could be closer
        // and still valid
        if(ignoreChildren[iter.getOffset()]) {
          continue;
        }
        // check intersection of [x-range,x+range] and child[i] range as seen
        // from iter, if empty (not intersecting), ignore child
        for(int i = 0; i < numChild; i++) {
          if(!ignoreChildren[i] && node.children[i] != null // not ignored yet
              && (x - tau < node.upperBound[iter.getOffset()][i] || x + tau > node.lowerBound[iter.getOffset()][i])) {
            ignoreChildren[i] = true;
          }
          else if(node.children[i] == null) {
            ignoreChildren[i] = true;
          }
        }
      }
      // search children
      for(int i = 0; i < numChild; i++) {
        if(!ignoreChildren[i])
          mvpKNNSearch(obj, knns, node.children[i], tau);
      }
      return tau;
    }
  }

  public class MVPTreeRangeSearcher implements RangeSearcher<O> {

    @Override
    public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
      mvpRangeSearch(query, result, root, range);
      return result;
    }

    private void mvpRangeSearch(O query, ModifiableDoubleDBIDList result, Node node, double range) {
      if(node == null) {
        return;
      }
      int numChild = node.vps.size();
      boolean[] ignoreChildren = new boolean[numChild];
      for(DBIDArrayIter iter = node.vps.iter().seek(0); iter.valid(); iter.advance()) {
        double x = distQuery.distance(iter, query);
        countDistanceComputation();
        if(x <= range) {
          result.add(x, iter);
        }
        // we only check the ingore flag after the distance calculation, because
        // the vp is not tracked in the child bounds and thus could be closer
        // and still valid
        if(ignoreChildren[iter.getOffset()]) {
          continue;
        }
        // check intersection of [x-range,x+range] and child[i] range as seen
        // from iter, if empty (not intersecting), ignore child
        for(int i = 0; i < numChild; i++) {
          if(!ignoreChildren[i] && node.children[i] != null // not ignored yet
              && (x - range < node.upperBound[iter.getOffset()][i] || x + range > node.lowerBound[iter.getOffset()][i])) {
            ignoreChildren[i] = true;
          }
          else if(node.children[i] == null) {
            ignoreChildren[i] = true;
          }
        }
      }
      // search children
      for(int i = 0; i < numChild; i++) {
        if(!ignoreChildren[i])
          mvpRangeSearch(query, result, node.children[i], range);
      }
    }
  }

  /**
   * Search position for priority search.
   *
   * @author Erich Schubert
   */
  private class PrioritySearchBranch implements Comparable<PrioritySearchBranch> {
    /**
     * Minimum distance
     */
    double mindist;

    /**
     * Node
     */
    Node node;

    /**
     * Constructor.
     *
     * @param mindist Minimum distance
     * @param left Interval begin
     * @param right Interval end (exclusive)
     * @param axis Next axis
     */
    public PrioritySearchBranch(double mindist, Node node) {
      this.mindist = mindist;
      this.node = node;
    }

    @Override
    public int compareTo(PrioritySearchBranch o) {
      return Double.compare(this.mindist, o.mindist);
    }
  }

//  public class MVPTreePrioritySearcher implements PrioritySearcher<O> {
//
//    /**
//     * Min heap for searching.
//     */
//    private ComparableMinHeap<PrioritySearchBranch> heap = new ComparableMinHeap<>();
//
//    /**
//     * Current query object.
//     */
//    private O query;
//
//    /**
//     * Stopping threshold.
//     */
//    private double threshold;
//
//    /**
//     * Current search position.
//     */
//    private PrioritySearchBranch cur;
//
//    @Override
//    public PrioritySearcher<O> search(O query) {
//      this.query = query;
//      this.threshold = Double.POSITIVE_INFINITY;
//      this.heap.clear();
//      this.heap.add(new PrioritySearchBranch(0, root));
//      return advance();
//    }
//
//    @Override
//    public PrioritySearcher<O> advance() {
//      if(heap.isEmpty()) {
//        cur = null;
//        return this;
//      }
//      // Get next
//      cur = heap.poll();
//      if(cur.mindist > threshold) {
//        cur = null;
//        return this;
//      }
//      // border value
//      double middist = (cur.node.leftHighBound + cur.node.rightLowBound) / 2;
//
//      // Distance to axis:
//      final double delta = middist - distQuery.distance(query, cur.node.vp);
//      countDistanceComputation();
//      final double mindist = distFunc.isSquared() ? delta * delta : Math.abs(delta);
//
//      // Next axis:
//      final double ldist = delta < 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
//      if(cur.node.leftChild != null && ldist <= threshold) {
//        heap.add(new PrioritySearchBranch(ldist, cur.node.leftChild));
//      }
//      final double rdist = delta > 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
//      if(cur.node.rightChild != null && rdist <= threshold) {
//        heap.add(new PrioritySearchBranch(rdist, cur.node.rightChild));
//      }
//      return this;
//    }
//
//    @Override
//    public int internalGetIndex() {
//      return cur.node.vp.internalGetIndex();
//    }
//
//    @Override
//    public boolean valid() {
//      return cur != null;
//    }
//
//    @Override
//    public PrioritySearcher<O> decreaseCutoff(double threshold) {
//      assert threshold <= this.threshold : "Thresholds must only decreasee.";
//      this.threshold = threshold;
//      return this;
//    }
//
//    @Override
//    public double computeExactDistance() {
//      return distQuery.distance(query, relation.get(cur.node.vp));
//    }
//
//    @Override
//    public double allLowerBound() {
//      return cur.mindist;
//    }
//
//    @Override
//    public double getLowerBound() {
//      return cur.mindist;
//    }
//  }
//
//  /**
//   * Count a single object access.
//   */
//  protected void countObjectAccess() {
//    if(objaccess != null) {
//      objaccess.increment();
//    }
//  }

  /**
   * Count a distance computation.
   */
  protected void countDistanceComputation() {
    if(distcalc != null) {
      distcalc.increment();
    }
  }

  @Override
  public void logStatistics() {
    if(objaccess != null) {
      LOG.statistics(objaccess);
    }
    if(distcalc != null) {
      LOG.statistics(distcalc);
    }
  }

  @Alias({ "vp" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O> {

    /**
     * Distance Function
     */
    Distance<O> distance;

    /**
     * Random factory
     */
    RandomFactory random;

    /**
     * Sample size
     */
    int sampleSize;

    /**
     * 
     * Constructor.
     *
     */
    @SuppressWarnings("unchecked")
    public Factory() {
      this((Distance<O>) EuclideanDistance.STATIC, RandomFactory.DEFAULT, 10);
    }

    /**
     * Constructor.
     *
     */
    public Factory(Distance<O> distFunc, RandomFactory random, int sampleSize) {
      super();
      this.distance = distFunc;
      this.random = random;
      this.sampleSize = sampleSize;
    }

    @Override
    public MVPTree<O> instantiate(Relation<O> relation) {
      return new MVPTree<>(relation, distance, random, sampleSize);
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
      public final static OptionID SAMPLE_SIZE_ID = new OptionID("vptree.sampleSize", "Size of sample to select vantage point from.");

      /**
       * Parameter to specify the random generator seed
       */
      public final static OptionID SEED_ID = new OptionID("vptree.seed", "The random number generator seed.");

      /**
       * Distance function
       */
      protected Distance<O> distance;

      /**
       * Random generator
       */
      protected RandomFactory random;

      /**
       * Sample size
       */
      protected int sampleSize;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<O>>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class)//
            .grab(config, x -> {
              this.distance = x;
            });
        new IntParameter(SAMPLE_SIZE_ID, 10) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)//
            .grab(config, x -> {
              this.sampleSize = x;
            });
        new RandomParameter(SEED_ID).grab(config, x -> random = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(distance, random, sampleSize);
      }
    }
  }
}
