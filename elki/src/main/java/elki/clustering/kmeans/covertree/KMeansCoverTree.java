/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2023
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

package elki.clustering.kmeans.covertree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import elki.data.NumberVector;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.tree.metrical.covertree.AbstractCoverTree;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.utilities.documentation.Reference;

/**
 * Abstract base class for cover tree variants.
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * Accelerating k-Means Clustering with Cover Trees<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2023
 * 
 * @author Erich Schubert
 * @author Andreas Lang
 *
 * @param <V> vector datatype
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "Accelerating k-Means Clustering with Cover Trees", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2023", //
    url = "https://doi.org/10.1007/978-3-031-46994-7_13", //
    bibkey = "DBLP:conf/sisap/LangS23")
public class KMeansCoverTree<V extends NumberVector> extends AbstractCoverTree<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KMeansCoverTree.class);

  private boolean meanstat;

  int size = 0;

  /**
   * Tree root.
   */
  private Node root = null;

  public KMeansCoverTree(Relation<V> relation, Distance<? super V> distance, double expansion, int truncate, boolean meanstat) {
    super(relation, distance, expansion, truncate);
    this.meanstat = meanstat;
  }

  /**
   * 
   * @return Root node of the tree
   */
  protected Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    Duration construction = LOG.newDuration(this.getClass().getName() + ".tree-construction").begin();
    bulkLoad(relation.getDBIDs());
    if(LOG.isVerbose()) {
      int[] counts = new int[5];
      checkCoverTree(root, counts, 0);
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".nodes", counts[0]));
      LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".avg-depth", counts[1] / (double) counts[0]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".max-depth", counts[2]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".singletons", counts[3]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".entries", counts[4]));
    }
    LOG.statistics(construction.end());
    if(meanstat) {
      Duration meanstat = LOG.newDuration(this.getClass().getName() + ".tree-meanstat").begin();
      calculateMeans(root, relation);
      LOG.statistics(meanstat.end());
    }
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".distance-computations", distComputations));

  }

  /**
   * Collects statistics on the tree
   * 
   * @param cur current node
   * @param counts accumulator for statistics
   * @param depth current depth
   */
  private void checkCoverTree(Node cur, int[] counts, int depth) {
    counts[0] += 1; // Node count
    counts[1] += depth; // Sum of depth
    counts[2] = depth > counts[2] ? depth : counts[2]; // Max depth
    counts[3] += cur.singletons.size() - 1;
    counts[4] += cur.singletons.size() - (cur.children.isEmpty() ? 0 : 1);
    if(!cur.children.isEmpty()) {
      ++depth;
      for(Node chi : cur.children) {
        checkCoverTree(chi, counts, depth);
      }
    }
  }

  /**
   * Bulk-load the index.
   *
   * @param ids IDs to load
   */
  private void bulkLoad(DBIDs ids) {
    if(ids.isEmpty()) {
      return;
    }
    assert root == null : "Tree already initialized.";
    DBIDIter it = ids.iter();
    DBID first = DBIDUtil.deref(it);
    // Compute distances to all neighbors:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList(ids.size() - 1);
    for(it.advance(); it.valid(); it.advance()) {
      candidates.add(distance(first, it), it);
    }
    root = bulkConstruct(first, Integer.MAX_VALUE, Double.POSITIVE_INFINITY, candidates);
  }

  /**
   * Bulk-load the cover tree.
   * <p>
   * This bulk-load is slightly simpler than the one used in the original
   * cover-tree source: We do not look back into the "far" set of candidates.
   *
   * @param cur Current routing object
   * @param maxScale Maximum scale
   * @param parentDist Distance to parent element
   * @param elems Candidates
   * @return Root node of subtree
   */
  protected Node bulkConstruct(DBIDRef cur, int maxScale, double parentDist, ModifiableDoubleDBIDList elems) {
    final double max = maxDistance(elems);
    final int scale = Math.min(distToScale(max) - 1, maxScale);
    final int nextScale = scale - 1;
    // Leaf node, because points coincide, we are too deep, or have too few
    // elements remaining:
    if(max <= 0 || scale <= scaleBottom || elems.size() < truncate) {
      size++;
      return new Node(cur, max, parentDist, elems);
    }
    // Find neighbors in the cover of the current object:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList();
    excludeNotCovered(elems, scaleToDist(scale), candidates);
    // If no elements were not in the cover, build a compact tree:
    if(candidates.isEmpty()) {
      LOG.warning("Scale not chosen appropriately? " + max + " " + scaleToDist(scale));
      return bulkConstruct(cur, nextScale, parentDist, elems);
    }
    // We will have at least one other child, so build the parent:
    size++;
    Node node = new Node(cur, max, parentDist);
    // Routing element now is a singleton:
    final boolean curSingleton = elems.isEmpty();
    if(!curSingleton) {
      // Add node for the routing object:
      node.children.add(bulkConstruct(cur, nextScale, 0, elems));
    }
    final double fmax = scaleToDist(nextScale);
    // Build additional cover nodes:
    for(DoubleDBIDListIter it = candidates.iter(); it.valid();) {
      assert it.getOffset() == 0;
      DBID t = DBIDUtil.deref(it);
      collectByCover(it, candidates, fmax, elems.clear());
      assert DBIDUtil.equal(t, it) : "First element in candidates must not change!";
      if(elems.isEmpty()) { // Singleton
        node.singletons.add(it.doubleValue(), it);
      }
      else {
        // Build a full child node:
        node.children.add(bulkConstruct(it, nextScale, it.doubleValue(), elems));
      }
      candidates.removeSwap(0);
    }
    assert candidates.isEmpty();
    // Routing object is not yet handled:
    if(curSingleton && !node.children.isEmpty()) {
      node.singletons.add(parentDist, cur); // Add as regular singleton.
    }
    // TODO: improve recycling of lists?
    return node;
  }

  /**
   * Collect Samples that belong to the subtree represented by this node
   * 
   * @param cur Node whose samples should be added
   * @param collect Collector List the Samples are added
   */
  protected void collectSubtree(Node cur, ModifiableDBIDs collect) {
    // ModifiableDBIDs collect = DBIDUtil.newHashSet(100); // TODO fix size
    DBIDIter it = cur.singletons.iter();
    // DBIDRef cid = DBIDUtil.deref(it); // check if id is already in the
    // collection
    if(!cur.children.isEmpty()) {
      it.advance();
    }
    for(; it.valid(); it.advance()) {
      collect.add(it);
    }
    for(Node next : cur.children) {
      collectSubtree(next, collect);
    }
  }

  /**
   * Calculates the mean value of a Node
   * 
   * @param cur Node
   * @param relation underlying relation
   */
  public void calculateMeans(Node cur, Relation<? extends NumberVector> relation) {
    DBIDIter it = cur.singletons.iter();
    NumberVector fv = relation.get(it);
    int d = fv.getDimensionality();
    cur.meansum = new double[d];
    if(cur.children.isEmpty()) {
      cur.size++;
      for(int i = 0; i < d; i++) {
        cur.meansum[i] += fv.doubleValue(i);
      }
    }
    else {
      Iterator<Node> nit = cur.children.iterator();
      while(nit.hasNext()) {
        Node child = nit.next();
        calculateMeans(child, relation);
        cur.size += child.size;
        for(int i = 0; i < d; i++) {
          cur.meansum[i] += child.meansum[i];
        }

      }
    }
    it.advance();
    for(; it.valid(); it.advance()) {
      cur.size++;
      fv = relation.get(it);
      for(int i = 0; i < d; i++) {
        cur.meansum[i] += fv.doubleValue(i);
      }
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Node object.
   *
   * @author Erich Schubert
   * @author Andreas Lang
   */
  static final class Node implements DBIDRef {
    /**
     * Objects in this node. Except for the first, which is the routing
     * object.
     */
    ModifiableDoubleDBIDList singletons;

    /**
     * Maximum distance to descendants.
     */
    double maxDist = 0.;

    /**
     * Distance to parent.
     */
    double parentDist = 0.;

    /**
     * Sum of represented data points
     */
    double[] meansum;

    /**
     * Number of Data points represented by the node
     */
    int size;

    /**
     * Child nodes.
     */
    List<Node> children;

    /**
     * Constructor.
     *
     * @param r Reference object
     * @param maxDist Maximum distance to any descendant
     * @param parentDist Distance from parent
     */
    public Node(DBIDRef r, double maxDist, double parentDist) {
      this.singletons = DBIDUtil.newDistanceDBIDList();
      this.singletons.add(0., r); // TODO, keep one
      this.children = new ArrayList<>();
      this.maxDist = maxDist;
      this.parentDist = parentDist;
    }

    /**
     * Constructor for leaf node.
     *
     * @param r Reference object
     * @param maxDist Maximum distance to any descendant
     * @param parentDist Distance from parent
     * @param singletons Singletons
     */
    public Node(DBIDRef r, double maxDist, double parentDist, DoubleDBIDList singletons) {
      assert !singletons.contains(r);
      this.singletons = DBIDUtil.newDistanceDBIDList(singletons.size() + 1);
      this.singletons.add(0., r); // TODO, keep one
      for(DoubleDBIDListIter it = singletons.iter(); it.valid(); it.advance()) {
        this.singletons.add(it.doubleValue(), it);
      }
      this.children = Collections.emptyList();
      this.maxDist = maxDist;
      this.parentDist = parentDist;
    }

    /**
     * Constructor for leaf node.
     *
     * @param r Reference object
     * @param parentDist Distance from parent
     * @param mean Mean Value of the node
     * @param weight Number of Datapoints in the subtree
     */
    public Node(DBIDRef r, double parentDist, double[] mean, int weight) {
      this.children = Collections.emptyList();
      this.singletons = DBIDUtil.newDistanceDBIDList(1);
      this.singletons.add(0., r);
      this.meansum = mean;
      this.size = weight;
    }

    @Override
    public int internalGetIndex() {
      return this.singletons.internalGetIndex(0);
    }
  }
}
