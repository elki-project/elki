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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.CovarianceDimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.parallelcoord.layout.Layout.Edge;

/**
 * Abstract class for dimension similarity based layouters.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractLayout3DPC<N extends Layout.Node> implements Layouter3DPC<NumberVector<?>> {
  /**
   * Similarity measure
   */
  DimensionSimilarity<NumberVector<?>> sim = CovarianceDimensionSimilarity.STATIC;

  /**
   * Constructor.
   * 
   * @param sim Similarity measure
   */
  public AbstractLayout3DPC(DimensionSimilarity<NumberVector<?>> sim) {
    super();
    this.sim = sim;
  }

  @Override
  public Layout layout(Database database, Relation<? extends NumberVector<?>> rel) {
    int dim = RelationUtil.dimensionality(rel);
    DimensionSimilarityMatrix mat = DimensionSimilarityMatrix.make(dim);
    sim.computeDimensionSimilarites(database, rel, rel.getDBIDs(), mat);
    return process(dim, mat);
  }

  /**
   * Main analysis method.
   * 
   * @param dim Dimensionality
   * @param mat Similarity matrix
   * @return Layout
   */
  abstract Layout process(final int dim, DimensionSimilarityMatrix mat);

  /**
   * Build the minimum spanning tree.
   * 
   * @param mat Similarity matrix
   * @param layout Layout to write to
   * @return Root node id
   */
  protected N buildSpanningTree(DimensionSimilarityMatrix mat, Layout layout) {
    assert (layout.edges == null || layout.edges.size() == 0);
    int[] iedges = PrimsMinimumSpanningTree.processDense(mat, DimensionSimilarityMatrix.PRIM_ADAPTER);
    int root = findOptimalRoot(iedges);

    // Convert edges:
    ArrayList<Edge> edges = new ArrayList<>(iedges.length >> 1);
    for (int i = 0; i < iedges.length; i += 2) {
      edges.add(new Edge(iedges[i], iedges[i + 1]));
    }
    layout.edges = edges;

    // Prefill nodes array with nulls.
    ArrayList<N> nodes = new ArrayList<>(mat.size());
    for (int i = 0; i < mat.size(); i++) {
      nodes.add(null);
    }
    layout.nodes = nodes;
    N rootnode = buildTree(iedges, root, -1, nodes);

    return rootnode;
  }

  abstract N makeNode(int dim, List<N> children);

  /**
   * Recursive tree build method.
   * 
   * @param msg Minimum spanning graph
   * @param cur Current node
   * @param parent Parent node
   * @param nodes Nodes array to fill - must be preinitialized with nulls!
   * @return Tree of nodes
   */
  protected N buildTree(int[] msg, int cur, int parent, ArrayList<N> nodes) {
    // Count the number of children:
    int c = 0;
    for (int i = 0; i < msg.length; i += 2) {
      if (msg[i] == cur && msg[i + 1] != parent) {
        c++;
      }
      if (msg[i + 1] == cur && msg[i] != parent) {
        c++;
      }
    }
    // Build children:
    List<N> children;
    if (c > 0) {
      children = new ArrayList<>(c);
    } else {
      children = Collections.emptyList();
    }
    for (int i = 0; i < msg.length; i += 2) {
      if (msg[i] == cur && msg[i + 1] != parent) {
        c--;
        children.add(buildTree(msg, msg[i + 1], cur, nodes));
      }
      if (msg[i + 1] == cur && msg[i] != parent) {
        c--;
        children.add(buildTree(msg, msg[i], cur, nodes));
      }
    }
    assert (c == 0);
    N node = makeNode(cur, children);
    nodes.set(cur, node);
    return node;
  }

  /**
   * Compute the depth of the graph.
   * 
   * @param node Current node
   * @return Depth
   */
  protected int maxDepth(Layout.Node node) {
    int depth = 0;
    for (int i = 0; i < node.numChildren(); i++) {
      Layout.Node child = node.getChild(i);
      depth = Math.max(depth, maxDepth(child));
    }
    return depth + 1;
  }

  /**
   * Abstract node implementation.
   * 
   * @author Erich Schubert
   * 
   * @param <N> Final node type
   */
  public static class AbstractNode<N extends AbstractNode<N>> implements Layout.Node {
    /**
     * Dimension represented by this node.
     */
    public int dim;

    /**
     * Coordinates
     */
    public double x, y;

    /**
     * Children
     */
    public List<N> children;

    /**
     * Constructor.
     * 
     * @param dim Dimension number
     * @param children Children
     */
    public AbstractNode(int dim, List<N> children) {
      this.dim = dim;
      this.children = children;
    }

    @Override
    public int getDim() {
      return dim;
    }

    @Override
    public double getX() {
      return x;
    }

    @Override
    public double getY() {
      return y;
    }

    @Override
    public N getChild(int off) {
      return children.get(off);
    }

    @Override
    public int numChildren() {
      return children.size();
    }
  }

  /**
   * Find the "optimal" root of a spanning tree. Optimal in the sense of: one of
   * the most central nodes.
   * 
   * This uses a simple message passing approach. Every node that has only one
   * unset neighbor will emit a message to this neighbor. The node last to emit
   * wins.
   * 
   * @param msg Minimum spanning graph.
   * @return
   */
  public static int findOptimalRoot(int[] msg) {
    final int size = (msg.length >> 1) + 1;

    int[] depth = new int[size];
    int[] missing = new int[size];

    // We shouldn't need more iterations in any case ever.
    int root = -1;
    for (int i = 1; i < size; i++) {
      boolean active = false;
      for (int e = 0; e < msg.length; e += 2) {
        if (depth[msg[e]] == 0) {
          missing[msg[e + 1]]++;
        }
        if (depth[msg[e + 1]] == 0) {
          missing[msg[e]]++;
        }
      }
      for (int n = 0; n < size; n++) {
        if (depth[n] == 0 && missing[n] <= 1) {
          depth[n] = i;
          root = n;
          active = true;
        }
      }
      if (!active) {
        break;
      }
      Arrays.fill(missing, 0); // Clean up.
    }
    return root;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Option for similarity measure.
     */
    public static final OptionID SIM_ID = new OptionID("parallel3d.sim", "Similarity measure for spanning tree.");

    /**
     * Similarity measure
     */
    DimensionSimilarity<NumberVector<?>> sim;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DimensionSimilarity<NumberVector<?>>> simP = new ObjectParameter<>(SIM_ID, DimensionSimilarity.class);
      if (config.grab(simP)) {
        sim = simP.instantiateClass(config);
      }
    }
  }

}
