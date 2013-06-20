package experimentalcode.shared.parallelcoord.layout;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;

/**
 * Simple circular layout based on the minimum spanning tree.
 * 
 * @author Erich Schubert
 */
public class SimpleCircularMSTLayout extends AbstractLayout3DPC<SimpleCircularMSTLayout.Node> {
  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public SimpleCircularMSTLayout(DimensionSimilarity<NumberVector<?>> sim) {
    super(sim);
  }

  /**
   * Node class for this layout.
   * 
   * @author Erich Schubert
   */
  public static class Node extends AbstractLayout3DPC.AbstractNode<Node> {
    /**
     * Weight (fanout needed)
     */
    public int weight;

    /**
     * Constructor.
     * 
     * @param dim Dimensionality
     * @param children Number of children
     */
    public Node(int dim, List<Node> children) {
      super(dim, children);
    }
  }

  @Override
  public Layout layout(int dim, DimensionSimilarityMatrix mat) {
    Layout l = new Layout();
    Node rootnode = buildSpanningTree(mat, l);
    int maxdepth = maxDepth(rootnode);
    computeWeights(rootnode);
    computePositions(rootnode, 0, 0, MathUtil.TWOPI, maxdepth);
    return l;
  }

  /**
   * Recursively assign node weights.
   * 
   * @param node Node to start with.
   */
  private void computeWeights(Node node) {
    int wsum = 0;
    for (Node child : node.children) {
      computeWeights(child);
      wsum += child.weight;
    }
    node.weight = Math.max(1, wsum);
  }

  /**
   * Compute the layout positions
   * 
   * @param node Node to start with
   * @param depth Depth of the node
   * @param aoff Angular offset
   * @param awid Angular width
   * @param maxdepth Maximum depth (used for radius computations)
   */
  public static void computePositions(Node node, int depth, double aoff, double awid, int maxdepth) {
    double r = depth / (maxdepth - 1.);
    node.x = Math.sin(aoff + awid * .5) * r;
    node.y = Math.cos(aoff + awid * .5) * r;

    double cpos = aoff;
    double cwid = awid / node.weight;
    for (Node c : node.children) {
      computePositions(c, depth + 1, cpos, cwid * c.weight, maxdepth);
      cpos += cwid * c.weight;
    }
  }

  @Override
  Node makeNode(int dim, List<Node> children) {
    return new Node(dim, children);
  }

  /**
   * Parameteriation class.
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractLayout3DPC.Parameterizer {
    @Override
    protected SimpleCircularMSTLayout makeInstance() {
      return new SimpleCircularMSTLayout(sim);
    }
  }
}
