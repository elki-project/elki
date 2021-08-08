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
package elki.clustering.hierarchical.betula;

/**
 * Interface for TreeNode
 * 
 * @author Andreas Lang
 * 
 * @param <L> CF Model
 */
public interface CFNode<L extends CFInterface> extends CFInterface {
  /**
   * Set child with index i to CF cf
   * 
   * @param i Index
   * @param cf Clustering Feature
   */
  void setChild(int i, L cf);

  /**
   * Get CF from Index i
   * 
   * @param i Index
   * @return CF
   */
  L getChild(int i);

  /**
   * Get the node capacity.
   * 
   * @return Node capacity
   */
  int capacity();

  /**
   * Add CF to statistics of TreeNode an save at index i
   * 
   * @param i Index
   * @param cf CF
   */
  void addToStatistics(int i, L cf);

  /**
   * Add TreeNode to Statistics of TreeNode an save at index i
   * 
   * @param i Index
   * @param cf TreeNode
   */
  void addToStatistics(int i, CFNode<L> cf);

  /**
   * Add CF to TreeNode but not the statistics
   * 
   * @param other CF
   * @return success
   */
  boolean addCF(L other);

  /**
   * Add CF to TreeNode but not the statistics
   * 
   * @param other CF
   * @return success
   */
  boolean addCF(CFNode<L> other);
}
