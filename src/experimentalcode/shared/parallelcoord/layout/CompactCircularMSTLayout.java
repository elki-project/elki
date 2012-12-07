package experimentalcode.shared.parallelcoord.layout;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
public class CompactCircularMSTLayout extends AbstractLayout3DPC<CompactCircularMSTLayout.Node> {
  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public CompactCircularMSTLayout(DimensionSimilarity<NumberVector<?>> sim) {
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
  Layout process(int dim, DimensionSimilarityMatrix mat) {
    Layout l = new Layout();
    Node rootnode = buildSpanningTree(mat, l);
    int maxdepth = maxDepth(rootnode);
    computeWeights(rootnode);
    computePositions(rootnode, 0, 0, MathUtil.TWOPI, 0.0, 1.0 / (maxdepth - 1));
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
   * @param radius Current radius
   * @param radiusinc Radius per depth step
   */
  public static void computePositions(Node node, int depth, double aoff, double awid, double radius, double radiusinc) {
    node.x = Math.sin(aoff) * radius;
    node.y = Math.cos(aoff) * radius;

    // Avoid overly wide angles - shrink radius if necessary.
    double cpos = aoff - awid * .5;
    double cwid = awid / node.weight;
    double cwid2 = cwid;
    if (node.weight > 1) {
      double s = Math.sin(awid * .5), c = Math.cos(awid * .5);
      double dx = s * (depth + 1), dy = c * (depth + 1) - depth;
      double d = Math.sqrt(dx * dx + dy * dy) / MathUtil.SQRT2;
      if (d > 1) {
        cwid2 /= d;
      }
    }
    for (Node c : node.children) {
      computePositions(c, depth + 1, cpos, cwid2 * c.weight, radius + radiusinc, radiusinc);
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
    protected CompactCircularMSTLayout makeInstance() {
      return new CompactCircularMSTLayout(sim);
    }
  }
}
