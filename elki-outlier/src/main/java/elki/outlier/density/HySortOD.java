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
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.Alias;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

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
 * @since 0.7.5
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
   * Number of bins
   */
  protected int b;

  /**
   * Threshold to balance the trade-off between the number of hyper-cubes to
   * scan
   * during the search and the granularity of mapping
   */
  protected int minSplit;

  // hypercube's length
  private final double l;

  // to match each object with its hyper-cube
  // it's a little trick to deal with hypercube rearrangement
  private int[] invertedIndex;

  // strategy for hyper-cubes density search
  private DensityStrategy strategy;

  /**
   * Constructor
   * 
   * @param k
   * @param b Number of bins
   */
  public HySortOD(int b, int minSplit) {
    super();
    this.b = b;
    this.l = 1 / (double) this.b;
    this.minSplit = minSplit;
    if(this.minSplit > 0) 
      this.strategy = new TreeStrategy(this.minSplit);
     else 
      this.strategy = new NaiveStrategy();
  }

  public OutlierResult run(Database db, Relation<V> relation) {

    // Output data storage
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("HySortOD scores", relation.size(), LOG) : null;
    // Track minimum and maximum scores
    DoubleMinMax minmax = new DoubleMinMax();

    int[] W;
    List<Hypercube> H;

    {
      H = getSortedHypercubes(relation);
      W = this.strategy.buildIndex(H).getDensities();

      // Iterate over all objects
      int objectPosition = 0;
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance())
        scores.putDouble(iter, score(W[this.invertedIndex[objectPosition++]]));

      LOG.incrementProcessed(prog);
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

  private List<Hypercube> getSortedHypercubes(Relation<V> relation) {
    BinaryTree<Hypercube> sorted = new BinaryTree<>();

    int objectPosition = 0;
    // Iterate over all objects
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      Hypercube h = new Hypercube(relation.get(iter), this.l);
      Hypercube obj = sorted.get(h);

      if(Objects.isNull(obj)) {
        h.add(objectPosition);
        sorted.add(h);
      }
      else {
        obj.add(objectPosition);
      }

      objectPosition++;
    }

    int n = sorted.size();
    List<Hypercube> H = new ArrayList<Hypercube>(n);

    this.invertedIndex = new int[relation.size()];
    int hypercybePosition = 0;
    for(Hypercube h : sorted) {
      H.add(h);

      for(Integer _objectPosition : h.getInstances())
        this.invertedIndex[_objectPosition] = hypercybePosition;

      hypercybePosition++;
    }

    return H;
  }

  private double score(int density) {
    return 1 - (density / this.strategy.getMaxDensity());
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  class Hypercube implements Comparable<Hypercube>, Comparator<Hypercube> {

    int[] coords;

    ArrayList<Integer> instances;

    public Hypercube(V instance, double lenght) {
      super();
      this.coords = new int[instance.getDimensionality()];
      this.instances = new ArrayList<Integer>();

      for(int d = 0; d < instance.getDimensionality(); d++) {
        this.coords[d] = (int) Math.floor(instance.doubleValue(d) / lenght);
      }
    }

    @Override
    public int compare(Hypercube o1, Hypercube o2) {
      for(int i = 0; i < o1.coords.length; i++) {
        int d = o1.coords[i] - o2.coords[i];
        if(d != 0)
          return d;
      }
      return 0;
    }

    @Override
    public int compareTo(Hypercube o) {
      for(int i = 0; i < coords.length; i++) {
        int d = this.coords[i] - o.coords[i];
        if(d != 0)
          return d;
      }
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      if(Objects.isNull(obj))
        return false;

      if(getClass() != obj.getClass())
        return false;

      @SuppressWarnings("unchecked")
      final Hypercube other = (Hypercube) obj;

      if(coords.length != other.coords.length)
        return false;

      for(int i = 0; i < coords.length; i++)
        if(coords[i] != other.coords[i])
          return false;

      return true;
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder();
      str.append("(" + coords[0]);
      for(int i = 1; i < coords.length; i++)
        str.append(", " + coords[i]);
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

    public List<Integer> getInstances() {
      return instances;
    }

    public void add(Integer instance) {
      instances.add(instance);
    }

    public int getDensity() {
      return instances.size();
    }

  }

  /**
   * 
   * @source https://www.baeldung.com/java-binary-tree
   */
  class BinaryTree<T extends Comparable<T>> implements Iterable<T> {

    private Node<T> root;

    public void add(T value) {
      root = addRecursive(null, root, value);
    }

    private Node<T> addRecursive(Node<T> parent, Node<T> current, T value) {

      if(Objects.isNull(current)) {
        return new Node<T>(parent, value);
      }

      if(value.compareTo(current.value) < 0) {
        current.left = addRecursive(current, current.left, value);
      }
      else if(value.compareTo(current.value) > 0) {
        current.right = addRecursive(current, current.right, value);
      }

      return current;
    }

    public boolean isEmpty() {
      return Objects.isNull(root);
    }

    public int size() {
      return getSizeRecursive(root);
    }

    private int getSizeRecursive(Node<T> current) {
      return Objects.isNull(current) ? 0 : getSizeRecursive(current.left) + 1 + getSizeRecursive(current.right);
    }

    public boolean contains(T value) {
      return containsRecursive(root, value);
    }

    private boolean containsRecursive(Node<T> current, T value) {
      if(Objects.isNull(current)) {
        return false;
      }

      if(value.compareTo(current.value) == 0) {
        return true;
      }

      return value.compareTo(current.value) < 0 ? containsRecursive(current.left, value) : containsRecursive(current.right, value);
    }

    public T get(T value) {
      return getRecursive(root, value);
    }

    private T getRecursive(Node<T> current, T value) {
      if(Objects.isNull(current))
        return null;

      if(value.compareTo(current.value) == 0)
        return current.value;

      return value.compareTo(current.value) < 0 ? getRecursive(current.left, value) : getRecursive(current.right, value);
    }

    public void delete(T value) {
      root = deleteRecursive(root, value);
    }

    private Node<T> deleteRecursive(Node<T> current, T value) {
      if(Objects.isNull(current)) {
        return null;
      }

      if(value.compareTo(current.value) == 0) {
        // Case 1: no children
        if(current.left == null && current.right == null) {
          return null;
        }

        // Case 2: only 1 child
        if(current.right == null) {
          return current.left;
        }

        if(current.left == null) {
          return current.right;
        }

        // Case 3: 2 children
        T smallestValue = findSmallestValue(current.right);
        current.value = smallestValue;
        current.right = deleteRecursive(current.right, smallestValue);
        return current;
      }
      if(value.compareTo(current.value) < 0) {
        current.left = deleteRecursive(current.left, value);
        return current;
      }

      current.right = deleteRecursive(current.right, value);
      return current;
    }

    private T findSmallestValue(Node<T> root) {
      return root.left == null ? root.value : findSmallestValue(root.left);
    }

    @Override
    public Iterator<T> iterator() {
      return new TreeIterator(root);
    }

    /**
     * @source https://stackoverflow.com/a/12851421
     */
    private class TreeIterator implements Iterator<T> {
      private Node<T> next;

      public TreeIterator(Node<T> root) {
        next = root;
        if(next == null)
          return;

        while(next.left != null)
          next = next.left;
      }

      public boolean hasNext() {
        return Objects.nonNull(next);
      }

      public T next() {
        if(!hasNext())
          throw new NoSuchElementException();
        Node<T> r = next;

        // If you can walk right, walk right, then fully left.
        // otherwise, walk up until you come from left.
        if(Objects.nonNull(next.right)) {
          next = next.right;
          while(Objects.nonNull(next.left))
            next = next.left;
          return r.value;
        }

        while(true) {
          if(Objects.isNull(next.parent)) {
            next = null;
            return r.value;
          }
          if(next.parent.left == next) {
            next = next.parent;
            return r.value;
          }
          next = next.parent;
        }
      }
    }

    @SuppressWarnings("hiding")
    private class Node<T> {
      public T value;

      public Node<T> left;

      public Node<T> right;

      public Node<T> parent;

      public Node(Node<T> parent, T value) {
        this.right = null;
        this.left = null;
        this.parent = parent;
        this.value = value;
      }
    }
  }

  abstract class DensityStrategy {
    // Max hypercube neighborhood density
    int Wmax;

    // Local reference for search
    List<Hypercube> H;

    abstract DensityStrategy buildIndex(List<Hypercube> H);

    abstract int[] getDensities();

    double getMaxDensity() {
      return (double) this.Wmax;
    }

    protected boolean isImmediate(Hypercube hi, Hypercube hk) {
      final int[] p = hi.getCoords();
      final int[] q = hk.getCoords();
      for(int j = p.length - 1; j >= 0; j--)
        if(Math.abs(p[j] - q[j]) > 1)
          return false;
      return true;
    }

    protected boolean isProspective(Hypercube hi, Hypercube hk, int col) {
      return Math.abs(hi.getCoordAt(col) - hk.getCoordAt(col)) <= 1;
    }
  }
  
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

      for (int i = 0; i < n; i++) {
        
        W[i] = H.get(i).getDensity();
        
        for (int k = i - 1; k >= 0; k--) {
          if (!isProspective(H.get(i), H.get(k), 0))
            break;
          if (isImmediate(H.get(i), H.get(k)))
            W[i] += H.get(k).getDensity();
        }
        
        for (int k = i + 1; k < n; k++) {
          if (!isProspective(H.get(i), H.get(k), 0))
            break;
          if (isImmediate(H.get(i), H.get(k)))
            W[i] += H.get(k).getDensity();
        }
        
        Wmax = Math.max(Wmax, W[i]);
      }
      
      return W;
    }
    
  }

  class TreeStrategy extends DensityStrategy {
    Node root;

    // Minimum number of rows to allow sub-mapping
    final int minSplit;

    // Maximum number of dimensions to map
    int numMappedDimensions;

    public TreeStrategy() {
      this(100);
    }

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

    final class Node {

      // Index information
      public final int value, begin, end;

      public final HashMap<Integer, Node> childs;

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

