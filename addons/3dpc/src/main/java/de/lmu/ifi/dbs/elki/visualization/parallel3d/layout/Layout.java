/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.visualization.parallel3d.layout;

import java.util.List;

/**
 * Layout class.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @composed - - - Node
 * @composed - - - Edge
 */
public class Layout {
  /**
   * Edge class
   * 
   * @author Erich Schubert
   */
  public static class Edge {
    /**
     * The two dimensions connected by this edge.
     */
    public int dim1, dim2;

    /**
     * Constructor.
     * 
     * @param dim1 First dimension
     * @param dim2 Second dimension
     */
    public Edge(int dim1, int dim2) {
      super();
      this.dim1 = dim1;
      this.dim2 = dim2;
    }
  }

  /**
   * Node of the layout tree.
   * 
   * @author Erich Schubert
   */
  public interface Node {
    /**
     * Get the dimension represented by this node.
     * 
     * @return Dimension
     */
    int getDim();

    /**
     * Get layout X position.
     * 
     * @return X position
     */
    double getX();

    /**
     * Get layout Y position.
     * 
     * @return Y position
     */
    double getY();

    /**
     * Get a child node.
     * 
     * @param off offset
     * @return Child node.
     */
    Node getChild(int off);

    /**
     * Get the number of children.
     * 
     * @return Number of children
     */
    int numChildren();
  }

  /**
   * Nodes
   */
  public List<? extends Node> nodes;

  /**
   * Edges
   */
  public List<? extends Edge> edges;

  // TODO: add support for triangles.

  /**
   * Get the node for the given dimension.
   * 
   * @param dim Dimension
   * @return Node
   */
  public Node getNode(int dim) {
    return nodes.get(dim);
  }
}
