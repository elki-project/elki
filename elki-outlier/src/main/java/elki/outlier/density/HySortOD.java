/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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

import java.util.*;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDArrayIter;
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
import elki.utilities.Alias;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

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
 * Cabral, Eugênio F., Robson L.F. Cordeiro<br>
 * Fast and Scalable Outlier Detection with Sorted Hypercubes<br>
 * Proc. 29th ACM Int. Conf. on Information & Knowledge Management (CIKM'20)
 * 
 * @author Cabral, Eugênio F. (Original Code)
 * @author Braulio V.S. Vinces (ELKIfication)
 *
 * @param <V> Vector type
 */
@Title("HySortOD: Hypercube-Based Outlier Detection")
@Description("Algorithm that uses an efficient hypercube-ordering-and-searching strategy for fast outlier detection.")
@Reference(authors = "Cabral, Eugênio F., Robson L.F. Cordeiro", //
    title = "Fast and Scalable Outlier Detection with Sorted Hypercubes", //
    booktitle = "Proc. 29th ACM Int. Conf. on Information & Knowledge Management (CIKM'20)", //
    url = "https://doi.org/10.1145/3340531.3412033")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.HySortOD", "hysort" })
public class HySortOD<V extends NumberVector> implements OutlierAlgorithm {

  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HySortOD.class);

  /**
   * Number of bins.
   */
  protected int b;

  /**
   * Threshold to balance the trade-off between the number of hypercubes to
   * scan during the search and the granularity of mapping.
   */
  protected int minSplit;

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
    this.minSplit = minSplit;
    if(this.minSplit > 0) {
      this.strategy = new TreeStrategy(this.minSplit);
    }
    else {
      this.strategy = new NaiveStrategy();
    }
  }

  public OutlierResult run(Database db, Relation<V> relation) {

    // Output data storage
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;

    LOG.beginStep(stepprog, 1, "Ordering of hypercubes lexicographically according to their coordinates.");
    final List<Hypercube> H = getSortedHypercubes(relation);

    LOG.beginStep(stepprog, 2, "Obtaining densities per hypercube.");
    final int[] W = this.strategy.buildIndex(H).getDensities();

    // Track minimum and maximum scores
    DoubleMinMax minmax = new DoubleMinMax();
    {
      // compute score by hypercube
      LOG.beginStep(stepprog, 3, "Computing hypercube scores");

      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("HySortOD scores", relation.size(), LOG) : null;
      DBIDArrayIter iter = (DBIDArrayIter) relation.iterDBIDs();
      for(int hypercube = 0; hypercube < H.size(); hypercube++) {
        final IntegerArray instances = H.get(hypercube).getInstances();
        final double hypercubeScore = score(W[hypercube]);
        minmax.put(hypercubeScore);
        for(int instance = 0; instance < instances.size; instance++) {
          final int index = instances.get(instance);
          scores.putDouble(iter.seek(index), hypercubeScore);
          LOG.incrementProcessed(prog);
        }
      }
      LOG.ensureCompleted(prog);
    }

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
  private List<Hypercube> getSortedHypercubes(Relation<V> relation) {
    TreeSet<Hypercube> sorted = new TreeSet<HySortOD<V>.Hypercube>();

    // Iterate over all objects
    for(DBIDArrayIter iter = (DBIDArrayIter) relation.iterDBIDs(); iter.valid(); iter.advance()) {
      Hypercube h = new Hypercube(relation.get(iter).toArray(), this.l);
      if(!sorted.contains(h)) {
        h.add(iter.getOffset());
        sorted.add(h);
      }
      else {
        sorted.ceiling(h).add(iter.getOffset());
      }
    }

    int n = sorted.size();
    List<Hypercube> H = new ArrayList<Hypercube>(n);

    for(Hypercube h : sorted) {
      H.add(h);
    }

    return H;
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
  class Hypercube implements Comparable<Hypercube> {

    /**
     * Hypercube coordinates.
     */
    final int[] coords;

    /**
     * Holds a set of instances within the hypercube.
     */
    final IntegerArray instances;

    /**
     * Hypercube constructor.
     *
     * @param values Vector representation of an instance
     * @param lenght Hypercube's length
     */
    public Hypercube(double[] values, double lenght) {
      super();
      this.coords = new int[values.length];
      this.instances = new IntegerArray();

      for(int d = 0; d < values.length; d++) {
        this.coords[d] = (int) FastMath.floor(values[d] / lenght);
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
      if(Objects.isNull(obj)) {
        return false;
      }

      if(getClass() != obj.getClass()) {
        return false;
      }

      @SuppressWarnings("unchecked")
      final Hypercube other = (Hypercube) obj;

      if(coords.length != other.coords.length) {
        return false;
      }

      for(int i = 0; i < coords.length; i++) {
        if(coords[i] != other.coords[i]) {
          return false;
        }
      }

      return true;
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder();
      str.append("(" + coords[0]);
      for(int i = 1; i < coords.length; i++) {
        str.append(", " + coords[i]);
      }
      str.append(")");
      return str.toString();
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

    public IntegerArray getInstances() {
      return instances;
    }

    public void add(int instance) {
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
   *
   */
  abstract class DensityStrategy {
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
      return (double) this.Wmax;
    }

    /**
     * Validate if Hypercube hk is immediate neighbor of Hypercube hi.
     * 
     * @param hi Hypercube hi
     * @param hk Hypercube hk
     * @return
     */
    protected boolean isImmediate(Hypercube hi, Hypercube hk) {
      final int[] p = hi.getCoords();
      final int[] q = hk.getCoords();
      for(int j = p.length - 1; j >= 0; j--) {
        if(FastMath.abs(p[j] - q[j]) > 1) {
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
      return FastMath.abs(hi.getCoordAt(col) - hk.getCoordAt(col)) <= 1;
    }
  }

  /**
   * Naive strategy for computing density.
   * 
   * @author Cabral, Eugênio F.
   *
   */
  class NaiveStrategy extends DensityStrategy {

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

        Wmax = FastMath.max(Wmax, W[i]);
      }

      return W;
    }

  }

  /**
   * Tree strategy for computing density.
   * 
   * @author Cabral, Eugênio F.
   *
   */
  class TreeStrategy extends DensityStrategy {
    /**
     * Tree root.
     */
    Node root;

    /**
     * Minimum number of rows to allow sub-mapping.
     */
    final int minSplit;

    /**
     * Maximum number of dimensions to map.
     */
    int numMappedDimensions;

    /**
     * Default constructor of the tree strategy.
     */
    public TreeStrategy() {
      this(100);
    }

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
      if(parent.end - parent.begin < this.minSplit)
        return;

      // Get the first value from the given range (minRowIdx, maxRowIdx)
      int value = this.H.get(parent.begin).getCoordAt(col);

      // Initialise the next range
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
      int n = H.size();
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
      int density = 0;

      if(parent.childs.isEmpty()) {
        for(int k = parent.begin; k <= parent.end; k++) {
          if(isImmediate(this.H.get(i), this.H.get(k))) {
            density += H.get(k).getDensity();
          }
        }
      }
      else {

        int lftVal = this.H.get(i).getCoordAt(col) - 1;
        int midVal = this.H.get(i).getCoordAt(col);
        int rgtVal = this.H.get(i).getCoordAt(col) + 1;

        Node lftNode = parent.childs.get(lftVal);
        Node midNode = parent.childs.get(midVal);
        Node rgtNode = parent.childs.get(rgtVal);

        int nextCol = Math.min(col + 1, this.H.get(i).getNumDimensions() - 1);

        if(Objects.nonNull(lftNode)) {
          density += density(i, lftNode, nextCol);
        }
        if(Objects.nonNull(midNode)) {
          density += density(i, midNode, nextCol);
        }
        if(Objects.nonNull(rgtNode)) {
          density += density(i, rgtNode, nextCol);
        }
      }

      return density;
    }

    /**
     * Tree node.
     * 
     * @author Cabral, Eugênio F.
     *
     */
    private class Node {

      /**
       * Index information.
       */
      public final int value, begin, end;

      /**
       * Childs of the node.
       */
      public final HashMap<Integer, Node> childs;

      /**
       * Constructor.
       *
       * @param value
       * @param begin
       * @param end
       */
      public Node(int value, int begin, int end) {
        this.childs = new HashMap<>();
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
        if(Objects.nonNull(node)) {
          childs.put(node.value, node);
        }
      }
    }
  }

  /**
   * Parameterization class
   * 
   * @hidden
   * 
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Parameter for number of bins.
     */
    public static final OptionID B_ID = new OptionID("hysortod.b", "Number of bins to use.");

    /**
     * Parameter for the predefined threshold.
     */
    public static final OptionID MIN_SPLIT_ID = new OptionID("hysortod.minsplit", "Predefined threshold to use.");

    protected int b = 5;

    protected int minSplit = 100;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(B_ID, 5) //
          .grab(config, x -> b = x);
      new IntParameter(MIN_SPLIT_ID, 100) //
          .grab(config, x -> minSplit = x);
    }

    @Override
    public HySortOD<V> make() {
      return new HySortOD<>(b, minSplit);
    }
  }

}
