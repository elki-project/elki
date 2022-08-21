/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.outlier.density;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Hypercube-Based Outlier Detection.
 * <p>
 * Algorithm that uses an efficient hypercube-ordering-and-searching strategy
 * for fast outlier detection.
 * Its main focus is the analysis of data with many instances and a
 * low-to-moderate number of dimensions.
 * <p>
 * Reference:
 * <p>
 * Eugênio F. Cabral and Robson L.F. Cordeiro<br>
 * Fast and Scalable Outlier Detection with Sorted Hypercubes<br>
 * Proc. 29th ACM Int. Conf. on Information and Knowledge Management (CIKM'20)
 * 
 * @author Cabral, Eugênio F. (Original Code)
 * @author Braulio V.S. Vinces (ELKIfication)
 * @since 0.8.0
 */
@Title("HySortOD: Hypercube-Based Outlier Detection")
@Description("Algorithm that uses an efficient hypercube-ordering-and-searching strategy for fast outlier detection.")
@Reference(authors = "Eugênio F. Cabral, and Robson L.F. Cordeiro", //
    title = "Fast and Scalable Outlier Detection with Sorted Hypercubes", //
    booktitle = "Proc. 29th ACM Int. Conf. on Information & Knowledge Management (CIKM'20)", //
    url = "https://doi.org/10.1145/3340531.3412033")
public class HySortOD implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HySortOD.class);

  /**
   * Number of bins.
   */
  private int b;

  /**
   * Hypercube's length.
   */
  private final double l;

  /**
   * Strategy for hypercubes density search.
   */
  private DensityStrategy strategy;

  /**
   * Constructor with parameters.
   * 
   * @param b Number of bins
   * @param minSplit Threshold to balance the tree strategy
   */
  public HySortOD(int b, int minSplit) {
    super();
    this.b = b;
    this.l = 1 / (double) this.b;
    this.strategy = minSplit > 0 ? new TreeStrategy(minSplit) : new NaiveStrategy();
  }

  public OutlierResult run(Database db, Relation<? extends NumberVector> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;

    LOG.beginStep(stepprog, 1, "Ordering of hypercubes lexicographically according to their coordinates.");
    final List<Hypercube> H = getSortedHypercubes(relation);

    LOG.beginStep(stepprog, 2, "Obtaining densities per hypercube.");
    final int[] W = this.strategy.buildIndex(H).getDensities();

    // Track minimum and maximum scores
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax minmax = new DoubleMinMax();
    // compute score by hypercube
    LOG.beginStep(stepprog, 3, "Computing hypercube scores");

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("HySortOD scores", relation.size(), LOG) : null;
    for(int hypercube = 0; hypercube < H.size(); hypercube++) {
      final double hypercubeScore = score(W[hypercube]);
      minmax.put(hypercubeScore);
      for(DBIDIter iter = H.get(hypercube).getInstances().iter(); iter.valid(); iter.advance()) {
        scores.putDouble(iter, hypercubeScore);
        LOG.incrementProcessed(prog);
      }
    }
    LOG.ensureCompleted(prog);

    // Wrap the result in the standard containers
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(
        // Actually observed minimum and maximum values
        minmax.getMin(), minmax.getMax(),
        // Theoretical minimum and maximum: no variance to infinite variance
        0, Double.POSITIVE_INFINITY);
    DoubleRelation rel = new MaterializedDoubleRelation("HySortOD", relation.getDBIDs(), scores);
    return new OutlierResult(meta, rel);
  }

  /**
   * Create and sort hypercubes considering their coordinates.
   * 
   * @param relation Data to process
   * @return Hypercubes sorted
   */
  private List<Hypercube> getSortedHypercubes(Relation<? extends NumberVector> relation) {
    TreeSet<Hypercube> sorted = new TreeSet<HySortOD.Hypercube>();

    // Iterate over all objects
    for(DBIDArrayIter iter = (DBIDArrayIter) relation.iterDBIDs(); iter.valid(); iter.advance()) {
      Hypercube h = new Hypercube(relation.get(iter), this.l);
      if(!sorted.contains(h)) {
        h.add(iter);
        sorted.add(h);
      }
      else {
        sorted.ceiling(h).add(iter);
      }
    }
    return new ArrayList<>(sorted);
  }

  /**
   * Compute score according to hypercube neighborhood density.
   * 
   * @param density Hypercube density
   * @return score
   */
  private double score(int density) {
    return 1 - (density / this.strategy.getMaxDensity());
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Bounded regions of the space where at least one instance exists.
   * 
   * @author Cabral, Eugênio F.
   */
  private static class Hypercube implements Comparable<Hypercube> {
    /**
     * Hypercube coordinates.
     */
    final int[] coords;

    /**
     * Holds a set of instances within the hypercube.
     */
    ArrayModifiableDBIDs instances;

    /**
     * Hypercube constructor.
     *
     * @param values Vector representation of an instance
     * @param length Hypercube's length
     */
    public Hypercube(NumberVector values, double length) {
      super();
      int[] coords = this.coords = new int[values.getDimensionality()];
      for(int d = 0; d < coords.length; d++) {
        coords[d] = (int) Math.floor(values.doubleValue(d) / length);
      }
    }

    @Override
    public int compareTo(Hypercube o) {
      for(int i = 0; i < coords.length; i++) {
        int d = this.coords[i] - o.coords[i];
        if(d != 0) {
          return d;
        }
      }
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && getClass() == obj.getClass() && Arrays.equals(coords, ((Hypercube) obj).coords);
    }
    
    @Override
    public int hashCode() {
      return Arrays.hashCode(coords) ^ instances.hashCode();
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder(coords.length * 10 + 2).append("(").append(coords[0]);
      for(int i = 1; i < coords.length; i++) {
        str.append(", ").append(coords[i]);
      }
      return str.append(")").toString();
    }

    public int getCoordAt(int j) {
      return coords[j];
    }

    public int[] getCoords() {
      return coords;
    }

    public int getNumDimensions() {
      return coords.length;
    }

    public DBIDs getInstances() {
      return instances;
    }

    public void add(DBIDRef instance) {
      if(instances == null) {
        instances = DBIDUtil.newArray();
      }
      instances.add(instance);
    }

    public int getDensity() {
      return instances.size();
    }
  }

  /**
   * Strategy for compute density.
   * 
   * @author Cabral, Eugênio F.
   */
  private static abstract class DensityStrategy {
    /**
     * Max hypercube neighborhood density
     */
    int Wmax;

    /**
     * Local reference for search
     */
    List<Hypercube> H;

    /**
     * Construct the index for compute hypercube density according to a
     * strategy.
     * 
     * @param H List of hypercubes
     * @return DensityStrategy Strategy instance
     */
    abstract DensityStrategy buildIndex(List<Hypercube> H);

    /**
     * @return Densities of all hypercubes
     */
    abstract int[] getDensities();

    /**
     * @return Maximum density
     */
    double getMaxDensity() {
      return this.Wmax;
    }

    /**
     * Validate if Hypercube hk is immediate neighbor of Hypercube hi.
     * 
     * @param hi Hypercube hi
     * @param hk Hypercube hk
     * @return
     */
    protected boolean isImmediate(Hypercube hi, Hypercube hk) {
      final int[] p = hi.getCoords(), q = hk.getCoords();
      for(int j = p.length - 1; j >= 0; j--) {
        if(Math.abs(p[j] - q[j]) > 1) {
          return false;
        }
      }
      return true;
    }

    /**
     * Validate if Hypercube hk is immediate neighbor of Hypercube hi using a
     * the same coordinate.
     * 
     * @param hi Hypercube hi
     * @param hk Hypercube hk
     * @param col Hypercube coordinate
     * @return
     */
    protected boolean isProspective(Hypercube hi, Hypercube hk, int col) {
      return Math.abs(hi.getCoordAt(col) - hk.getCoordAt(col)) <= 1;
    }
  }

  /**
   * Naive strategy for computing density.
   * 
   * @author Cabral, Eugênio F.
   */
  private static class NaiveStrategy extends DensityStrategy {
    @Override
    DensityStrategy buildIndex(List<Hypercube> H) {
      this.H = H;
      this.Wmax = 0;
      return this;
    }

    @Override
    public int[] getDensities() {
      int n = H.size();
      int[] W = new int[n];

      for(int i = 0; i < n; i++) {
        W[i] = H.get(i).getDensity();

        for(int k = i - 1; k >= 0; k--) {
          if(!isProspective(H.get(i), H.get(k), 0)) {
            break;
          }
          if(isImmediate(H.get(i), H.get(k))) {
            W[i] += H.get(k).getDensity();
          }
        }

        for(int k = i + 1; k < n; k++) {
          if(!isProspective(H.get(i), H.get(k), 0)) {
            break;
          }
          if(isImmediate(H.get(i), H.get(k))) {
            W[i] += H.get(k).getDensity();
          }
        }
        Wmax = Math.max(Wmax, W[i]);
      }
      return W;
    }
  }

  /**
   * Tree strategy for computing density.
   * 
   * @author Cabral, Eugênio F.
   */
  private static class TreeStrategy extends DensityStrategy {
    /**
     * Tree root.
     */
    Node root;

    /**
     * Minimum number of rows to allow sub-mapping.
     */
    final int minSplit;

    /**
     * Constructor of the tree strategy.
     *
     * @param minSplit
     */
    public TreeStrategy(int minSplit) {
      // Set the minimum number of rows to allow sub-mapping
      // When the value is 0 this parameter has no effect
      this.minSplit = minSplit;
    }

    @Override
    DensityStrategy buildIndex(List<Hypercube> H) {
      this.H = H;
      this.Wmax = 0;

      // The root node maps the whole dataset
      this.root = new Node(-1, 0, H.size() - 1);

      // Start recursive mapping from the first dimension
      buildIndex(this.root, 0);
      return this;
    }

    /**
     * Recursive build index.
     * 
     * @param parent Node of the tree strategy
     * @param col
     */
    private void buildIndex(Node parent, int col) {
      // Stop sub-mapping when the parent node map less than minSplit hypercubes
      if(parent.end - parent.begin < this.minSplit) {
        return;
      }

      // Get the first value from the given range (minRowIdx, maxRowIdx)
      int value = this.H.get(parent.begin).getCoordAt(col);

      // Initialize the next range
      int begin = parent.begin;
      int end = -1;

      // map the values in the current range
      int i = parent.begin;
      for(; i <= parent.end; i++) {
        // when the value change the node is created
        if(this.H.get(i).getCoordAt(col) != value) {
          // mark the end of the current value
          end = i - 1;

          // create node for 'value' in 'col'
          Node child = new Node(value, begin, end);
          parent.add(child);

          // map child values in the next dimension
          buildIndex(child, col + 1);

          // start new range
          begin = i;

          // update value
          value = this.H.get(i).getCoordAt(col);
        }
      }

      // map last value
      end = i - 1;
      Node child = new Node(value, begin, end);
      parent.add(child);

      buildIndex(child, col + 1);
    }

    @Override
    int[] getDensities() {
      final int n = H.size();
      int[] W = new int[n];

      for(int i = 0; i < n; i++) {
        W[i] = density(i, root, 0);
        Wmax = Math.max(Wmax, W[i]);
      }
      return W;
    }

    /**
     * Recursive computing of density.
     * 
     * @param i Index of the hypercube
     * @param parent Node of the tree
     * @param col Coordinate of the hypercube
     * @return Density
     */
    private int density(int i, Node parent, int col) {
      if(parent.children == null) {
        int density = 0;
        for(int k = parent.begin; k <= parent.end; k++) {
          if(isImmediate(this.H.get(i), this.H.get(k))) {
            density += H.get(k).getDensity();
          }
        }
        return density;
      }
      final int midVal = this.H.get(i).getCoordAt(col);
      final int nextCol = Math.min(col + 1, this.H.get(i).getNumDimensions() - 1);
      final Node lftNode = parent.children.get(midVal - 1);
      final Node midNode = parent.children.get(midVal);
      final Node rgtNode = parent.children.get(midVal + 1);
      return (lftNode != null ? density(i, lftNode, nextCol) : 0) //
          + (midNode != null ? density(i, midNode, nextCol) : 0) //
          + (rgtNode != null ? density(i, rgtNode, nextCol) : 0);
    }

    /**
     * Tree node.
     * 
     * @author Cabral, Eugênio F.
     */
    private static class Node {
      /**
       * Index information.
       */
      public final int value, begin, end;

      /**
       * Childs of the node.
       */
      public Int2ObjectOpenHashMap<Node> children;

      /**
       * Constructor.
       *
       * @param value
       * @param begin
       * @param end
       */
      public Node(int value, int begin, int end) {
        this.value = value;
        this.begin = begin;
        this.end = end;
      }

      @Override
      public String toString() {
        return "(" + begin + "," + end + ")";
      }

      /**
       * Add a child.
       * 
       * @param node Child
       */
      public void add(Node node) {
        if(node != null) {
          if(children == null) {
            children = new Int2ObjectOpenHashMap<>();
          }
          children.put(node.value, node);
        }
      }
    }
  }

  /**
   * Parameterization class
   *
   * @hidden
   *
   * @author Braulio V.S. Vinces
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for number of bins.
     */
    public static final OptionID B_ID = new OptionID("hysortod.b", "Number of bins to use.");

    /**
     * Parameter for the predefined threshold.
     */
    public static final OptionID MIN_SPLIT_ID = new OptionID("hysortod.minsplit", "Predefined threshold to use.");

    /**
     * Number of bins.
     */
    protected int b = 5;

    /**
     * Threshold to balance the tree strategy.
     */
    protected int minSplit = 100;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(B_ID, 5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> b = x);
      new IntParameter(MIN_SPLIT_ID, 100) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> minSplit = x);
    }

    @Override
    public HySortOD make() {
      return new HySortOD(b, minSplit);
    }
  }
}
