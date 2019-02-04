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

import de.lmu.ifi.dbs.elki.math.linearalgebra.SingularValueDecomposition;
import de.lmu.ifi.dbs.elki.math.statistics.dependence.DependenceMeasure;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Layout the axes by multi-dimensional scaling.
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
public class MultidimensionalScalingMSTLayout3DPC extends AbstractLayout3DPC<MultidimensionalScalingMSTLayout3DPC.Node> {
  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public MultidimensionalScalingMSTLayout3DPC(DependenceMeasure sim) {
    super(sim);
  }

  /**
   * Node class for this layout.
   * 
   * @author Erich Schubert
   */
  public static class Node extends AbstractLayout3DPC.AbstractNode<Node> {
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
  Node makeNode(int dim, List<Node> children) {
    return new Node(dim, children);
  }

  @Override
  public Layout layout(int dim, double[] mat) {
    // Find maximum of |cij|
    double max = 0;
    for(double v : mat) {
      v = (v > 0) ? v : -v;
      max = (v > max) ? v : max;
    }
    // Assume that "max - |cij|" is now a distance.
    // We use sqrt(v) instead of v*v, since this makes the method
    // less aggressive overall, and we are not using euclidean anyway.
    double means[] = new double[dim];
    double mean = 0.0;
    for(int y = 1, o = 0; y < dim; y++) {
      for(int x = 0; x < y; x++, o++) {
        double v = max - Math.abs(mat[o]);
        v = -.5 * FastMath.sqrt(v);
        means[x] += v;
        means[y] += v;
        mean += 2 * v;
      }
    }
    for(int i = 0; i < dim; i++) {
      means[i] /= dim;
    }
    mean /= (dim * dim);
    // Build double centered matrix:
    double[][] d = new double[dim][dim];
    for(int y = 1, o = 0; y < dim; y++) {
      d[y][y] = -2 * means[y] + mean;
      for(int x = 0; x < y; x++, o++) {
        double v = max - Math.abs(mat[o]);
        v = -.5 * FastMath.sqrt(v) - means[x] - means[y] + mean;
        d[x][y] = d[y][x] = v;
      }
    }

    SingularValueDecomposition svd = new SingularValueDecomposition(d);
    double[][] u = svd.getU();
    double[] lambda = svd.getSingularValues();
    lambda[0] = FastMath.sqrt(Math.abs(lambda[0]));
    lambda[1] = FastMath.sqrt(Math.abs(lambda[1]));

    Layout l = new Layout();
    buildSpanningTree(dim, mat, l);

    double maxabs = 0;
    for(int i = 0; i < dim; i++) {
      Node n = (Node) l.getNode(i);
      n.x = u[i][0] * lambda[0];
      n.y = u[i][1] * lambda[1];
      double v = n.x * n.x + n.y * n.y;
      if(v > maxabs) {
        maxabs = v;
      }
    }
    maxabs = 1. / FastMath.sqrt(maxabs);
    for(int i = 0; i < dim; i++) {
      Node n = (Node) l.getNode(i);
      n.x *= maxabs;
      n.y *= maxabs;
    }

    return l;
  }

  /**
   * Parameteriation class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractLayout3DPC.Parameterizer {
    @Override
    protected MultidimensionalScalingMSTLayout3DPC makeInstance() {
      return new MultidimensionalScalingMSTLayout3DPC(sim);
    }
  }
}
