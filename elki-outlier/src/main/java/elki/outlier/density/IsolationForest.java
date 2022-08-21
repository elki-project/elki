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
import java.util.Random;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Isolation-Based Anomaly Detection.
 * <p>
 * This method uses an ensemble of randomized trees that serve as a simple
 * density estimator instead of using distances to estimate density.
 * <p>
 * Reference:
 * <p>
 * F. T. Liu, K. M. Ting, Z.-H. Zhou<br>
 * Isolation-Based Anomaly Detection<br>
 * Transactions on Knowledge Discovery from Data (TKDD)
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "F. T. Liu, K. M. Ting, Z.-H. Zhou", //
    title = "Isolation-Based Anomaly Detection", //
    booktitle = "Transactions on Knowledge Discovery from Data (TKDD)", //
    url = "https://doi.org/10.1145/2133360.2133363", //
    bibkey = "DBLP:journals/tkdd/LiuTZ12")
public class IsolationForest implements OutlierAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(IsolationForest.class);

  /**
   * The number of trees
   */
  protected int numTrees = 100;

  /**
   * The sub sample size
   */
  protected int subsampleSize = 256;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param numTrees
   * @param subsampleSize
   * @param rnd
   */
  public IsolationForest(int numTrees, int subsampleSize, RandomFactory rnd) {
    this.numTrees = numTrees;
    this.subsampleSize = subsampleSize;
    this.rnd = rnd;
  }

  /**
   * Run the isolation forest algorithm.
   * 
   * @param relation Data relation to index
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<? extends NumberVector> relation) {
    // Reduce sub sample size if data is too small
    if(relation.size() < subsampleSize) {
      subsampleSize = relation.size();
    }
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(2) : null;
    LOG.beginStep(stepprog, 1, "Generating isolation trees.");
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Isolation forest construction", numTrees, LOG) : null;
    // Generate trees
    final Random random = rnd.getSingleThreadedRandom();
    List<Node> trees = new ArrayList<>(numTrees);
    ForestBuilder builder = new ForestBuilder(relation, subsampleSize, random);
    for(int i = 0; i < numTrees; i++) {
      trees.add(builder.newTree());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    LOG.beginStep(stepprog, 2, "Computing isolation forest scores.");
    prog = LOG.isVerbose() ? new FiniteProgress("Isolation forest scores", relation.size(), LOG) : null;
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax minmax = new DoubleMinMax();
    final double f = -MathUtil.LOG2 / (trees.size() * c(subsampleSize));
    // Iterate over all objects
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final NumberVector v = relation.get(iter);
      // Score against each tree:
      double avgPathLength = 0;
      for(Node tree : trees) {
        avgPathLength += isolationScore(tree, v);
      }
      final double score = FastMath.exp(f * avgPathLength);
      scores.putDouble(iter, score);
      minmax.put(score);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    LOG.ensureCompleted(stepprog);

    // Wrap the result in the standard containers
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(
        // Actually observed minimum and maximum values
        minmax.getMin(), minmax.getMax(),
        // Theoretical minimum and maximum: no variance to infinite variance
        0, Double.POSITIVE_INFINITY);
    DoubleRelation rel = new MaterializedDoubleRelation("IsolationForest", relation.getDBIDs(), scores);
    return new OutlierResult(meta, rel);
  }

  /**
   * Returns the average path length of an unsuccessful search.
   * Returns 0 if the value is less than or equal to 1.
   * 
   * @param n Depth
   * @return Expected average
   */
  protected static double c(double n) {
    return n <= 1.0 ? 0 : 2 * (FastMath.log(n - 1) + MathUtil.EULERMASCHERONI) - (2. * (n - 1) / n);
  }

  /**
   * Search a vector in the tree, return depth (path length)
   *
   * @param n Node to start
   * @param v Vector to search
   * @return Isolation score based on depth and node size
   */
  protected double isolationScore(Node n, NumberVector v) {
    int depth = 0;
    while(n != null) {
      depth++;
      if(n.dim < 0) {
        return c(n.size) + depth;
      }
      n = v.doubleValue(n.dim) <= n.split ? n.left : n.right;
    }
    throw new IllegalStateException("Encountered unexpected null.");
  }

  /**
   * Class to build the forest
   *
   * @author Erich Schubert
   */
  protected static class ForestBuilder {
    /**
     * Data relation to use
     */
    Relation<? extends NumberVector> relation;

    /**
     * Array of current candidates
     */
    ArrayModifiableDBIDs ids;

    /**
     * Iterator into candidates
     */
    DBIDArrayMIter iter;

    /**
     * Current value range
     */
    double[] min, max;

    /**
     * Active dimensions (not constant)
     */
    int[] active;

    /**
     * Maximum height
     */
    int maxheight;

    /**
     * Random generator
     */
    Random rnd;

    /**
     * Subsample size to use for the trees.
     */
    int subsampleSize;

    /**
     * Constructor for the tree builder.
     *
     * @param relation Data relation
     * @param subsampleSize Sampling size
     * @param random Random generator
     */
    protected ForestBuilder(Relation<? extends NumberVector> relation, int subsampleSize, Random random) {
      this.relation = relation;
      final int dim = RelationUtil.dimensionality(relation);
      this.subsampleSize = subsampleSize;
      this.maxheight = (int) FastMath.ceil(FastMath.log2(subsampleSize));
      this.min = new double[dim];
      this.max = new double[dim];
      this.active = new int[dim];
      this.ids = DBIDUtil.newArray(relation.getDBIDs());
      this.iter = ids.iter();
      this.rnd = random;
    }

    /**
     * Build a new tree.
     *
     * @return New tree
     */
    protected Node newTree() {
      // New random sample, by reshuffling our scratch array
      DBIDUtil.randomShuffle(ids, rnd, subsampleSize);
      return build(0, subsampleSize, 0);
    }

    /**
     * Recursively build the tree
     * 
     * @param s Start range
     * @param e End range
     * @param h Height
     * @return Tree node
     */
    protected Node build(int s, int e, int h) {
      final int size = e - s;
      if(h >= maxheight || size <= 1) {
        return new Node(-1, Double.NaN, size, null, null); // Terminal
      }
      // Find minimum and maximum in the subset
      Arrays.fill(min, Double.MAX_VALUE);
      Arrays.fill(max, -Double.MAX_VALUE);
      final int dim = min.length;
      for(iter.seek(s); iter.getOffset() < e; iter.advance()) {
        final NumberVector o = relation.get(iter);
        for(int d = 0; d < dim; d++) {
          final double v = o.doubleValue(d);
          min[d] = (v < min[d]) ? v : min[d];
          max[d] = (v > max[d]) ? v : max[d];
        }
      }
      // Find allowed attributes
      int numactive = 0;
      for(int j = 0; j < dim; j++) {
        if(min[j] < max[j]) {
          active[numactive++] = j;
        }
      }
      if(numactive == 0) {
        return new Node(-1, Double.NaN, size, null, null); // Terminal
      }
      // Random split dimension
      final int d = active[rnd.nextInt(numactive)];
      // Random split value
      final double v = min[d] + (max[d] - min[d]) * rnd.nextDouble();
      // Quick split
      int i = s, j = e - 1;
      while(i < j) {
        while(i < j && relation.get(iter.seek(i)).doubleValue(d) <= v) {
          i++;
        }
        while(i < j && relation.get(iter.seek(j)).doubleValue(d) > v) {
          j--;
        }
        if(i < j) {
          ids.swap(i++, j--);
        }
      }
      return new Node(d, v, size, build(s, i, h + 1), build(i, e, h + 1));
    }
  }

  /**
   * Minimalistic tree node for the isolation forest.
   *
   * @author Erich Schubert
   */
  protected static class Node {
    /**
     * Dimension to split at.
     */
    int dim;

    /**
     * Split value
     */
    double split;

    /**
     * Subtree size
     */
    int size;

    /**
     * Left child, may be null
     */
    Node left;

    /**
     * Right child, may be null
     */
    Node right;

    /**
     * Node constructor.
     *
     * @param dim Split dimension
     * @param split Split value
     * @param size Size of subtree
     * @param left Left subtree
     * @param right Right subtree
     */
    public Node(int dim, double split, int size, Node left, Node right) {
      this.dim = dim;
      this.split = split;
      this.size = size;
      this.left = left;
      this.right = right;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class
   * 
   * @author Braulio V.S. Vinces
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for the number of trees
     */
    public static final OptionID NUM_TREES_ID = new OptionID("iforest.numtrees", "Number of trees to use.");

    /**
     * Parameter for the sub sample size
     */
    public static final OptionID SUBSAMPLE_SIZE_ID = new OptionID("iforest.subsample", "Subsampling size.");

    /**
     * Parameter to specify the seed to initialize Random.
     */
    public static final OptionID SEED_ID = new OptionID("iforest.seed", "The seed to use for initializing Random.");

    /**
     * Number of trees
     */
    protected int numTrees = 100;

    /**
     * Size of the sample set
     */
    protected int subsampleSize = 256;

    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(NUM_TREES_ID, 100) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> numTrees = x);
      new IntParameter(SUBSAMPLE_SIZE_ID, 256) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> subsampleSize = x);
      new RandomParameter(SEED_ID).grab(config, x -> this.rnd = x);
    }

    @Override
    public IsolationForest make() {
      return new IsolationForest(this.numTrees, this.subsampleSize, this.rnd);
    }
  }
}
