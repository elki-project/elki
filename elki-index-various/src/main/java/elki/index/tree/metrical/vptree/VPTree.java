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

import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.Index;
import elki.utilities.documentation.Reference;

/**
 * Vantage Point Tree with no further information
 *  
 *  WIP
 * 
 * @author Robert Gehde
 *
 * @param <O>
 *
 */
@Reference(authors = "Peter N. Yianilos", //
    booktitle = "Proceedings of the Fourth Annual {ACM/SIGACT-SIAM} Symposium on Discrete Algorithms, 25-27 January 1993, Austin, Texas, {USA}", //
    title = "Data Structures and Algorithms for Nearest Neighbor Search in General Metric Spaces", //
    url = "http://dl.acm.org/citation.cfm?id=313559.313789", //
    bibkey = "DBLP:conf/soda/Yianilos93")
public class VPTree<O> implements Index {

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  Distance<? super O> distFunc;

  DistanceQuery<? super O> distQuery;

  ModifiableDoubleDBIDList sorted;

  Node root;

  class Node {
    DBID vp;

    Node leftChild, rightChild;

    int leftBound, rightBound;

    double leftLowBound, leftHighBound, rightLowBound, rightHighBound;

    public Node(int left, int right) {
      this.leftBound = left;
      this.rightBound = right;
      this.leftLowBound = Double.MAX_VALUE;
      this.rightLowBound = Double.MAX_VALUE;
      this.leftHighBound = Double.MIN_VALUE;
      this.rightHighBound = Double.MIN_VALUE;
    }
  }

  public VPTree(Relation<O> relation, Distance<? super O> distance) {
    this.relation = relation;
    this.distFunc = distance;
    this.distQuery = distance.instantiate(relation);
  }

  @Override
  public void initialize() {
    sorted = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      sorted.add(Double.NaN, it);
    }
    root = new Node(0, relation.size());
    buildTree(root, 0, relation.size(), sorted.iter());
  }

  private void buildTree(Node current, int left, int right, DoubleDBIDListMIter iter) {
    // find vantage point
    DBID vantagePoint = findVantagePoint(left, right, iter);
    current.vp = vantagePoint;
    // find median in dist(vp, rel)
    int vantagePointOffset = 0;
    for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
      double d = distQuery.distance(iter, vantagePoint);
      // bound to [0,1]
      d = d / (d + 1);
      iter.setDouble(d);
      if(DBIDUtil.compare(iter, vantagePoint) == 0) {
        vantagePointOffset = iter.getOffset();
      }
    }
    int middle = (left + 1 + right) >>> 1;
    // sort left < median; right >= median
    // i dont know if quickselect fullfills this
    // also, the case of multiple middle-dists, when right has more elements
    // than left
    // this is not accurate to the paper currently
    // exclude current vantage point
    sorted.swap(left, vantagePointOffset);
    QuickSelectDBIDs.quickSelect(sorted, left + 1, right, middle);

    for(iter.seek(left); iter.getOffset() < middle; iter.advance()) {
      double d = iter.doubleValue();
      if(d < current.leftLowBound) {
        current.leftLowBound = d;
      }
      if(d > current.leftHighBound) {
        current.leftHighBound = d;
      }
    }
    for(iter.seek(middle); iter.getOffset() < right; iter.advance()) {
      double d = iter.doubleValue();
      if(d < current.rightLowBound) {
        current.rightLowBound = d;
      }
      if(d > current.rightHighBound) {
        current.rightHighBound = d;
      }
    }
    // construct child trees
    if(middle - (left + 1) > 0) {
      current.leftChild = new Node(left + 1, middle);
      buildTree(current.leftChild, left + 1, middle, iter);
    }
    if(right - middle > 0) {
      current.rightChild = new Node(middle, right);
      buildTree(current.rightChild, middle, right, iter);
    }
  }

  // this is not nice
  DBID result = null;

  double tau = 1;

  public DBID FindnearestNeighbor(DBID target) {
    result = null;
    tau = 1;
    searchTree(target, root);
    return DBIDUtil.deref(result);
  }

  private void searchTree(DBID target, Node tree) {
    if(tree == null) {
      return;
    }
    double x = distQuery.distance(tree.vp, target);
    if(x < tau) {
      result = tree.vp;
      tau = x;
    }
    double middle = (tree.leftHighBound + tree.rightLowBound) / 2;

    if(x < middle) {
      if(x > tree.leftLowBound - tau && x < tree.leftHighBound + tau) {
        searchTree(target, tree.leftChild);
      }
      if(x > tree.rightLowBound - tau && x < tree.rightHighBound + tau) {
        searchTree(target, tree.leftChild);
      }
    }
    else {
      if(x > tree.rightLowBound - tau && x < tree.rightHighBound + tau) {
        searchTree(target, tree.leftChild);
      }
      if(x > tree.leftLowBound - tau && x < tree.leftHighBound + tau) {
        searchTree(target, tree.leftChild);
      }
    }
  }

  /**
   * TODO size of random sample
   * TODO random seed
   * 
   * @return
   */
  private DBID findVantagePoint(int left, int right, DoubleDBIDListMIter iter) {
    // init, construct workset
    int s = 10;
    ArrayModifiableDBIDs workset = DBIDUtil.newArray(right - left);
    for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
      workset.add(iter);
    }
    // find vantage point (TODO random values will be replaced)
    ModifiableDBIDs candidates = DBIDUtil.randomSample(workset, s, 1337);
    DBID best = null;
    double bestSpread = Double.MIN_VALUE;
    for(DBIDMIter it = candidates.iter(); it.valid(); it.advance()) {
      DBIDs check = DBIDUtil.randomSample(workset, s, 72 + it.internalGetIndex());
      // calculate moment
      double spread = calcMoment(check, it, s);
      if(spread > bestSpread) {
        bestSpread = spread;
        best = DBIDUtil.deref(it);
      }
    }
    return best;
  }

  private double calcMoment(DBIDs check, DBIDIter it, int s) {
    // calc distances
    ModifiableDoubleDBIDList vals = DBIDUtil.newDistanceDBIDList(s);
    for(DBIDIter iter = check.iter(); iter.valid(); iter.advance()) {
      double d = distQuery.distance(it, iter);
      d = d / (d + 1);
      vals.add(d, iter);
    }
    // calc median
    int pos = QuickSelectDBIDs.median(vals);
    double median = vals.iter().seek(pos).doubleValue();
    // calc 2nd moment
    double moment = 0;
    for(DoubleDBIDListMIter iter = vals.iter(); iter.valid(); iter.advance()) {
      moment += Math.pow(iter.doubleValue() - median, 2);
    }
    return moment / s;
  }
}
