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
package elki.visualization.parallel3d.layout;

import java.util.List;

import elki.math.MathUtil;
import elki.math.statistics.dependence.Dependence;
import elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Simple circular layout based on the minimum spanning tree.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br>
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br>
 * Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - Node
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", //
    booktitle = "Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)", //
    url = "https://doi.org/10.1145/2463676.2463696", //
    bibkey = "DBLP:conf/sigmod/AchtertKSZ13")
public class SimpleCircularMSTLayout3DPC extends AbstractLayout3DPC<SimpleCircularMSTLayout3DPC.Node> {
  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public SimpleCircularMSTLayout3DPC(Dependence sim) {
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
  public Layout layout(int dim, double[] mat) {
    Layout l = new Layout();
    Node rootnode = buildSpanningTree(dim, mat, l);
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
    for(Node child : node.children) {
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
    node.x = FastMath.sin(aoff + awid * .5) * r;
    node.y = FastMath.cos(aoff + awid * .5) * r;

    double cpos = aoff;
    double cwid = awid / node.weight;
    for(Node c : node.children) {
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
   */
  public static class Par extends AbstractLayout3DPC.Par {
    @Override
    public SimpleCircularMSTLayout3DPC make() {
      return new SimpleCircularMSTLayout3DPC(sim);
    }
  }
}
