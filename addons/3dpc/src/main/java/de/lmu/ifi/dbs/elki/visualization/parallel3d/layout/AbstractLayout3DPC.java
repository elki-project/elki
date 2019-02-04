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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.math.statistics.dependence.CorrelationDependenceMeasure;
import de.lmu.ifi.dbs.elki.math.statistics.dependence.DependenceMeasure;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.Layout.Edge;

/**
 * Abstract class for dimension similarity based layouters.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
public abstract class AbstractLayout3DPC<N extends Layout.Node> implements SimilarityBasedLayouter3DPC {
  /**
   * Similarity measure
   */
  DependenceMeasure sim = CorrelationDependenceMeasure.STATIC;

  /**
   * Constructor.
   *
   * @param sim Similarity measure
   */
  public AbstractLayout3DPC(DependenceMeasure sim) {
    super();
    this.sim = sim;
  }

  @Override
  public DependenceMeasure getSimilarity() {
    return sim;
  }

  @Override
  public Layout layout(Relation<? extends NumberVector> rel) {
    return layout(RelationUtil.dimensionality(rel), computeSimilarityMatrix(sim, rel));
  }

  /**
   * Compute a column-wise dependency matrix for the given relation.
   * 
   * @param sim Dependence measure
   * @param rel Vector relation
   * @return Similarity matrix (lower triangular form)
   */
  public static double[] computeSimilarityMatrix(DependenceMeasure sim, Relation<? extends NumberVector> rel) {
    final int dim = RelationUtil.dimensionality(rel);
    // TODO: we could use less memory (no copy), but this would likely be
    // slower. Maybe as a fallback option?
    double[][] data = new double[dim][rel.size()];
    int r = 0;
    for(DBIDIter it = rel.iterDBIDs(); it.valid(); it.advance(), r++) {
      NumberVector v = rel.get(it);
      for(int d = 0; d < dim; d++) {
        data[d][r] = v.doubleValue(d);
      }
    }
    return sim.dependence(DoubleArrayAdapter.STATIC, Arrays.asList(data));
  }

  @Override
  public abstract Layout layout(final int dim, double[] mat);

  /**
   * Class to use a lower-triangular similarity matrix for distance-based Prim's
   * spanning tree.
   * <p>
   * Implementation note: the edges are given as an array of 2*e values.
   * 
   * @author Erich Schubert
   */
  private static class LowerTriangularAdapter implements PrimsMinimumSpanningTree.Adapter<double[]> {
    int dim;

    public LowerTriangularAdapter(int dim) {
      this.dim = dim;
    }

    @Override
    public double distance(double[] data, int i, int j) {
      // Ensure i < j
      return (i == j) ? 0. : (j < i) ? distance(data, j, i) : //
          -Math.abs(data[((j * (j - 1)) >> 1) + i]);
    }

    @Override
    public int size(double[] data) {
      return dim;
    }
  }

  /**
   * Build the minimum spanning tree.
   *
   * @param mat Similarity matrix
   * @param layout Layout to write to
   * @return Root node id
   */
  protected N buildSpanningTree(int dim, double[] mat, Layout layout) {
    assert (layout.edges == null || layout.edges.size() == 0);
    int[] iedges = PrimsMinimumSpanningTree.processDense(mat, new LowerTriangularAdapter(dim));
    int root = findOptimalRoot(iedges);

    // Convert edges:
    ArrayList<Edge> edges = new ArrayList<>(iedges.length >> 1);
    for(int i = 1; i < iedges.length; i += 2) {
      edges.add(new Edge(iedges[i - 1], iedges[i]));
    }
    layout.edges = edges;

    // Prefill nodes array with nulls.
    ArrayList<N> nodes = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
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
    for(int i = 1; i < msg.length; i += 2) {
      if((msg[i - 1] == cur && msg[i] != parent) || (msg[i] == cur && msg[i - 1] != parent)) {
        c++;
      }
    }
    // Build children:
    List<N> children = Collections.emptyList();
    if(c > 0) {
      children = new ArrayList<>(c);
      for(int i = 1; i < msg.length; i += 2) {
        if(msg[i - 1] == cur && msg[i] != parent) {
          children.add(buildTree(msg, msg[i], cur, nodes));
        }
        else if(msg[i] == cur && msg[i - 1] != parent) {
          children.add(buildTree(msg, msg[i - 1], cur, nodes));
        }
      }
    }
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
    for(int i = 0; i < node.numChildren(); i++) {
      depth = Math.max(depth, maxDepth(node.getChild(i)));
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
   * <p>
   * This uses a simple message passing approach. Every node that has only one
   * unset neighbor will emit a message to this neighbor. The node last to emit
   * wins.
   *
   * @param msg Minimum spanning graph.
   * @return optimal root node.
   */
  public static int findOptimalRoot(int[] msg) {
    final int size = (msg.length >> 1) + 1;
    int[] depth = new int[size], missing = new int[size];

    // We shouldn't need more iterations in any case ever.
    int root = -1;
    for(int i = 1; i < size; i++) {
      boolean active = false;
      for(int e = 1; e < msg.length; e += 2) {
        if(depth[msg[e - 1]] == 0) {
          missing[msg[e]]++;
        }
        if(depth[msg[e]] == 0) {
          missing[msg[e - 1]]++;
        }
      }
      for(int n = 0; n < size; n++) {
        if(depth[n] == 0 && missing[n] <= 1) {
          depth[n] = i;
          root = n;
          active = true;
        }
      }
      if(!active) {
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
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Similarity measure
     */
    DependenceMeasure sim;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DependenceMeasure> simP = new ObjectParameter<>(SIM_ID, DependenceMeasure.class, CorrelationDependenceMeasure.class);
      if(config.grab(simP)) {
        sim = simP.instantiateClass(config);
      }
    }
  }
}
