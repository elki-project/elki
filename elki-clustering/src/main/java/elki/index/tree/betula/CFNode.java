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
package elki.index.tree.betula;

import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.ClusterFeature;

/**
 * Interface for TreeNode
 * 
 * @author Andreas Lang
 * @since 0.8.0
 * 
 * @param <L> Cluster feature type
 */
public class CFNode<L extends ClusterFeature> implements AsClusterFeature {
  /**
   * Cluster feature
   */
  private L cf;

  /**
   * Children of the TreeNode
   */
  private Object[] children;

  /**
   * Constructor
   * 
   * @param capacity Fanout of the Tree
   */
  public CFNode(L cf, int capacity) {
    super();
    this.cf = cf;
    this.children = new Object[capacity];
  }

  @Override
  public L getCF() {
    return cf;
  }

  /**
   * Set child with index i to CF cf
   * 
   * @param i Index
   * @param cf Clustering Feature
   */
  public void setChild(int i, AsClusterFeature cf) {
    children[i] = cf;
  }

  /**
   * Add a child without statistics
   * 
   * @param node child
   * @return success
   */
  public boolean setChild(AsClusterFeature node) {
    for(int i = 0; i < children.length; i++) {
      if(children[i] == null) {
        children[i] = node;
        return true;
      }
    }
    return false;
  }

  /**
   * Get CF from Index i
   * 
   * @param i Index
   * @return CF
   */
  public AsClusterFeature getChild(int i) {
    return i < children.length ? (AsClusterFeature) children[i] : null;
  }

  /**
   * Add a subtree
   * 
   * @param node Subtree
   * @return success
   */
  public boolean add(AsClusterFeature node) {
    for(int i = 0; i < children.length; i++) {
      if(children[i] == null) {
        children[i] = node;
        cf.addToStatistics(node.getCF());
        return true;
      }
    }
    return false;
  }

  /**
   * Add a subtree.
   * 
   * @param i Index
   * @param node Tree node
   */
  public void add(int i, AsClusterFeature node) {
    children[i] = node;
    cf.addToStatistics(node.getCF());
  }

  /**
   * Get the node capacity.
   * 
   * @return Node capacity
   */
  public int capacity() {
    return children.length;
  }
}
