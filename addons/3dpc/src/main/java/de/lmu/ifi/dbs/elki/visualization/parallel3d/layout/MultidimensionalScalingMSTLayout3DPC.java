package de.lmu.ifi.dbs.elki.visualization.parallel3d.layout;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SingularValueDecomposition;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Layout the axes by multi-dimensional scaling.
 * 
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br />
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br />
 * Proceedings of the 2013 ACM International Conference on Management of Data
 * (SIGMOD), New York City, NY, 2013.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", booktitle = "Proc. of the 2013 ACM International Conference on Management of Data (SIGMOD)", url = "http://dx.doi.org/10.1145/2463676.2463696")
public class MultidimensionalScalingMSTLayout3DPC extends AbstractLayout3DPC<MultidimensionalScalingMSTLayout3DPC.Node> {
  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public MultidimensionalScalingMSTLayout3DPC(DimensionSimilarity<NumberVector> sim) {
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
  public Layout layout(int dim, DimensionSimilarityMatrix mat) {
    // Find maximum of |cij|
    double max = 0;
    for (int i = 0; i < dim; i++) {
      for (int j = i + 1; j < dim; j++) {
        double v = Math.abs(mat.get(j, i));
        if (v > max) {
          max = v;
        }
      }
    }
    // Assume that "max - |cij|" is now a distance.
    // We use sqrt(v) instead of v*v, since this makes the method
    // less aggressive overall, and we are not using euclidean anyway.
    double means[] = new double[dim];
    double mean = 0.0;
    for (int i = 0; i < dim; i++) {
      for (int j = i + 1; j < dim; j++) {
        double v = max - Math.abs(mat.get(i, j));
        v = -.5 * Math.sqrt(v);
        means[i] += v;
        means[j] += v;
        mean += 2 * v;
      }
    }
    for (int i = 0; i < dim; i++) {
      means[i] /= dim;
    }
    mean /= (dim * dim);
    // Build double centered matrix:
    Matrix d = new Matrix(dim, dim);
    for (int i = 0; i < dim; i++) {
      d.set(i, i, -2 * means[i] + mean);
      for (int j = i + 1; j < dim; j++) {
        double v = max - Math.abs(mat.get(i, j));
        v = -.5 * Math.sqrt(v) - means[i] - means[j] + mean;
        d.set(i, j, v);
        d.set(j, i, v);
      }
    }

    SingularValueDecomposition svd = new SingularValueDecomposition(d);
    Matrix u = svd.getU();
    double[] lambda = svd.getSingularValues();
    lambda[0] = Math.sqrt(Math.abs(lambda[0]));
    lambda[1] = Math.sqrt(Math.abs(lambda[1]));

    Layout l = new Layout();
    buildSpanningTree(mat, l);

    double maxabs = 0;
    for (int i = 0; i < dim; i++) {
      Node n = (Node) l.getNode(i);
      n.x = u.get(i, 0) * lambda[0];
      n.y = u.get(i, 1) * lambda[1];
      double v = n.x * n.x + n.y * n.y;
      if (v > maxabs) {
        maxabs = v;
      }
    }
    maxabs = 1. / Math.sqrt(maxabs);
    for (int i = 0; i < dim; i++) {
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractLayout3DPC.Parameterizer {
    @Override
    protected MultidimensionalScalingMSTLayout3DPC makeInstance() {
      return new MultidimensionalScalingMSTLayout3DPC(sim);
    }
  }
}
