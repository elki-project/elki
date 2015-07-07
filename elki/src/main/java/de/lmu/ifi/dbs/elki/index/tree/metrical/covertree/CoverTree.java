package de.lmu.ifi.dbs.elki.index.tree.metrical.covertree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Cover tree data structure (in-memory). This is a <i>metrical</i> data
 * structure that is similar to the M-tree, but not as balanced and
 * disk-oriented. However, by not having these requirements it does not require
 * the expensive splitting procedures of M-tree.
 * 
 * Reference:
 * <p>
 * A. Beygelzimer, S. Kakade, J. Langford<br />
 * Cover trees for nearest neighbor<br />
 * In Proc. 23rd International Conference on Machine Learning (ICML).
 * </p>
 * 
 * TODO: allow insertions and removals, as in the original publication.
 * 
 * @author Erich Schubert
 */
@Reference(authors = "A. Beygelzimer, S. Kakade, J. Langford", //
title = "Cover trees for nearest neighbor",//
booktitle = "In Proc. 23rd International Conference on Machine Learning (ICML)",//
url = "http://dx.doi.org/10.1145/1143844.1143857")
public class CoverTree<O> extends AbstractIndex<O> implements RangeIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CoverTree.class);

  /**
   * Constant expansion rate. 2 would be the intuitive value, but the original
   * version used 1.3, so we copy this. This means that in every level, the
   * cover radius shrinks by 1.3.
   */
  final double BASE = 1.3f;

  /**
   * Logarithm base.
   */
  final double INV_LOG_BASE = 1. / Math.log(BASE);

  /**
   * Tree leaf.
   */
  final int SCALE_LEAF = Integer.MAX_VALUE;

  /**
   * Remaining points are likely identical. For 1.3 this yields: -2700
   */
  final int SCALE_BOTTOM = (int) Math.ceil(Math.log(Double.MIN_NORMAL) * INV_LOG_BASE);

  /**
   * Tree root.
   */
  private Node root = null;

  /**
   * Holds the instance of the trees distance function.
   */
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Distance query, on the data relation.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * Distance computations performed.
   */
  long distComputations = 0L;

  /**
   * Constructor.
   *
   * @param relation data relation
   * @param distanceFunction distance function
   */
  public CoverTree(Relation<O> relation, DistanceFunction<? super O> distanceFunction) {
    super(relation);
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceFunction.instantiate(relation);
  }

  /**
   * Convert a scaling factor to a distance.
   * 
   * @param s Scaling factor
   * @return Distance
   */
  final double scaleToDist(int s) {
    return Math.pow(BASE, s);
  }

  /**
   * Convert a distance to an upper scaling bound-
   * 
   * @param d Distance
   * @return Scaling bound
   */
  final int distToScale(double d) {
    return (int) Math.ceil(Math.log(d) * INV_LOG_BASE);
  }

  @Override
  public void initialize() {
    bulkLoad(relation.getDBIDs());
  }

  /**
   * Bulk-load the index.
   * 
   * @param ids IDs to load
   */
  public void bulkLoad(DBIDs ids) {
    if(ids.size() == 0) {
      return;
    }
    assert (root == null) : "Tree already initialized.";
    DBIDIter it = ids.iter();
    DBID first = DBIDUtil.deref(it);
    // Compute distances to all neighbors:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList(ids.size() - 1);
    for(it.advance(); it.valid(); it.advance()) {
      candidates.add(distanceQuery.distance(first, it), it);
      ++distComputations;
    }
    double maxdist = maxDistance(candidates);
    int scale = distToScale(maxdist);
    root = bulkConstruct(first, scale, scale, candidates);
  }

  /**
   * Bulk-load the cover tree.
   * 
   * This bulk-load is slightly simpler than the one used in the original
   * cover-tree source: We do not look back into the "far" set of candidates.
   * 
   * @param cur Current routing object
   * @param maxScale Maximum scale
   * @param topScale Topmost scale, used for depth.
   * @param elems Candidates
   * @return Root node of subtree
   */
  protected Node bulkConstruct(DBIDRef cur, int maxScale, int topScale, ModifiableDoubleDBIDList elems) {
    // No more children
    if(elems == null || elems.size() == 0) {
      return new Node(cur);
    }
    double max = maxDistance(elems);
    final int nextScale = Math.min(maxScale - 1, distToScale(max));
    // The remaining points coincide, build a leaf node:
    if(nextScale <= SCALE_BOTTOM) {
      Node r = new Node(cur, elems);
      r.maxdist = max;
      r.scale = SCALE_BOTTOM;
      return r;
    }
    // Find neighbors in the current cover:
    ModifiableDoubleDBIDList extra = DBIDUtil.newDistanceDBIDList();
    filterByCover(elems, maxScale, extra);
    // If no elements were not in the cover, build a compact tree:
    if(extra.size() == 0) {
      return bulkConstruct(cur, nextScale, topScale, elems);
    }
    // We will have at least one other child, so build the parent:
    Node n = new Node(cur);
    n.maxdist = maxDistance(extra);
    n.scale = topScale - maxScale;
    ArrayList<Node> children = n.children = new ArrayList<>();
    // Recursively build the cover tree on these contents:
    children.add(bulkConstruct(cur, nextScale, topScale, elems));
    // Build additional cover nodes:
    ModifiableDoubleDBIDList collect = DBIDUtil.newDistanceDBIDList();
    for(DoubleDBIDListIter it = extra.iter(); it.valid();) {
      assert (it.getOffset() == 0);
      collect(it, extra, maxScale, collect);
      if(collect.size() == 0) {
        n.ids.add(it); // TODO: preserve distance information?
      }
      else {
        // Build a child node:
        Node n2 = bulkConstruct(it, nextScale, topScale, collect);
        n2.parentDist = it.doubleValue();
        children.add(n2);
      }
      extra.removeSwap(it.getOffset());
    }
    assert (extra.size() == 0);
    // TODO: improve recycling of lists
    return n;
  }

  /**
   * Collect all elements within the current cover.
   * 
   * @param candidates Candidates
   * @param maxScale Maximum cover radius
   * @param collect Far neighbors
   */
  void filterByCover(ModifiableDoubleDBIDList candidates, int maxScale, ModifiableDoubleDBIDList collect) {
    final double fmax = distToScale(maxScale);
    for(DoubleDBIDListIter it = candidates.iter(); it.valid();) {
      if(it.doubleValue() > fmax) {
        collect.add(it.doubleValue(), it);
        candidates.removeSwap(it.getOffset());
      }
      else {
        it.advance(); // Keep in candidates
      }
    }
  }

  /**
   * Collect all elements with respect to a new routing object.
   * 
   * @param cur Routing object
   * @param candidates Candidate list
   * @param maxScale Maximum scale
   * @param collect Output list
   */
  private void collect(DBIDRef cur, ModifiableDoubleDBIDList candidates, int maxScale, ModifiableDoubleDBIDList collect) {
    final double fmax = distToScale(maxScale);
    for(DoubleDBIDListIter it = candidates.iter().seek(1); it.valid();) {
      assert (!DBIDUtil.equal(cur, it));
      final double dist = distanceQuery.distance(cur, it);
      ++distComputations;
      if(dist <= fmax) { // Collect
        collect.add(dist, it);
        candidates.removeSwap(it.getOffset());
      }
      else {
        it.advance(); // Keep in candidates, too far.
      }
    }
  }

  /**
   * Find maximum in a list via scanning.
   * 
   * @param elems Elements
   * @return Maximum distance
   */
  private double maxDistance(DoubleDBIDList elems) {
    double max = 0;
    for(DoubleDBIDListIter it = elems.iter(); it.valid(); it.advance()) {
      final double v = it.doubleValue();
      max = max > v ? max : v;
    }
    return max;
  }

  /**
   * Node object.
   * 
   * @author Erich Schubert
   */
  public class Node {
    /**
     * Objects in this node. The first is the routing object.
     */
    ModifiableDBIDs ids;

    /**
     * Maximum distance to grandchildren.
     */
    double maxdist = 0.;

    /**
     * Distance to parent.
     */
    double parentDist = 0.;

    /**
     * Child nodes.
     */
    ArrayList<Node> children = null;

    /**
     * Expansion scale.
     */
    int scale = SCALE_LEAF;

    /**
     * Constructor.
     *
     * @param r Object.
     */
    public Node(DBIDRef r) {
      ids = DBIDUtil.newArray();
      ids.add(r);
    }

    /**
     * Constructor, with additional children
     *
     * @param r Object.
     * @param children Children.
     */
    public Node(DBIDRef r, DBIDs children) {
      ids = DBIDUtil.newArray(children.size() + 1);
      ids.add(r);
      ids.addDBIDs(children);
    }
  }

  @Override
  public void logStatistics() {
    if(LOG.isStatistics() && root != null) {
      int[] counts = new int[4];
      collectStatistics(root, counts, 0);
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".nodes", counts[0]));
      LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".avg-depth", counts[1] / (double) counts[0]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".max-depth", counts[2]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".simple", counts[3]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".distance-computations", distComputations));
    }
  }

  /**
   * Collect some statistics on the tree.
   * 
   * @param cur Current node
   * @param counts Counter set
   * @param depth Current depth
   */
  private void collectStatistics(Node cur, int[] counts, int depth) {
    counts[0] += 1; // Node count
    counts[1] += depth; // Sum of depth
    counts[2] = depth > counts[2] ? depth : counts[2]; // Max depth
    counts[3] += cur.ids.size() - 1;
    if(cur.children != null) {
      ++depth;
      for(Node chi : cur.children) {
        collectStatistics(chi, counts, depth);
      }
    }
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(LOG.isDebugging()) {
        LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    DistanceQuery<O> dq = distanceFunction.instantiate(relation);
    return new CoverTreeRangeQuery(dq);
  }

  /**
   * Range query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeRangeQuery extends AbstractDistanceRangeQuery<O> implements RangeQuery<O> {
    /**
     * Constructor.
     *
     * @param distanceQuery Distance query
     */
    public CoverTreeRangeQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public DoubleDBIDList getRangeForObject(O obj, double range) {
      ModifiableDoubleDBIDList ret = DBIDUtil.newDistanceDBIDList();
      ArrayList<Node> open = new ArrayList<Node>();
      open.add(root);
      while(!open.isEmpty()) {
        Node cur = open.remove(open.size() - 1);
        DBIDIter it = cur.ids.iter();
        double d = distanceQuery.distance(obj, it);
        ++distComputations;
        // Cover not in range:
        if(d >= cur.maxdist + range) {
          continue;
        }
        if(cur.children != null) {
          for(Node c : cur.children) {
            // TODO: usse parentDist for pruning
            open.add(c);
          }
        }
        else if(d < range) {
          ret.add(d, it); // First is a candidate now
        }
        for(it.advance(); it.valid(); it.advance()) {
          double d2 = distanceQuery.distance(obj, it);
          ++distComputations;
          if(d2 < range) {
            ret.add(d2, it);
          }
        }
        continue;
      }
      ret.sort();
      return ret;
    }
  }

  @Override
  public String getLongName() {
    return "Cover Tree";
  }

  @Override
  public String getShortName() {
    return "cover-tree";
  }

  /**
   * Index factory.
   * 
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Factory<O> implements IndexFactory<O, CoverTree<O>> {
    /**
     * Holds the instance of the trees distance function.
     */
    protected DistanceFunction<? super O> distanceFunction;

    /**
     * Constructor.
     *
     * @param distanceFunction Distance function
     */
    public Factory(DistanceFunction<? super O> distanceFunction) {
      super();
      this.distanceFunction = distanceFunction;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distanceFunction.getInputTypeRestriction();
    }

    @Override
    public CoverTree<O> instantiate(Relation<O> relation) {
      return new CoverTree<O>(relation, distanceFunction);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    public static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Parameter to specify the distance function to determine the distance
       * between database objects, must extend
       * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
       * <p>
       * Key: {@code -covertree.distancefunction}
       * </p>
       */
      public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("covertree.distancefunction", "Distance function to determine the distance between objects.");

      /**
       * Holds the instance of the trees distance function.
       */
      protected DistanceFunction<? super O> distanceFunction;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DistanceFunction.class);
        if(config.grab(distanceFunctionP)) {
          distanceFunction = distanceFunctionP.instantiateClass(config);
        }
      }

      @Override
      protected CoverTree.Factory<O> makeInstance() {
        return new CoverTree.Factory<>(distanceFunction);
      }

    }
  }
}
