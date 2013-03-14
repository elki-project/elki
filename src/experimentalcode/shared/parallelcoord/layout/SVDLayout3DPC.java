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
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SingularValueDecomposition;

public class SVDLayout3DPC extends AbstractLayout3DPC<SVDLayout3DPC.Node> {
  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public SVDLayout3DPC(DimensionSimilarity<NumberVector<?>> sim) {
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
  Layout process(int dim, DimensionSimilarityMatrix mat) {
    // Convert to a full matrix.
    Matrix m = mat.copyToFullMatrix();
    SingularValueDecomposition svd = new SingularValueDecomposition(m);
    Matrix u = svd.getU();

    Layout l = new Layout();
    /* Node rootnode = */buildSpanningTree(mat, l);

    for (int i = 0; i < dim; i++) {
      Node n = (Node) l.getNode(i);
      n.x = u.get(0, i);
      n.y = u.get(1, i);
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
    protected SVDLayout3DPC makeInstance() {
      return new SVDLayout3DPC(sim);
    }
  }
}
