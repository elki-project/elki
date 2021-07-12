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
package elki.outlier.isolation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
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
import elki.utilities.Alias;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
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
 * This algorithm detects anomalies purely based on the concept of
 * isolation without employing any distance or density measure.
 * <p>
 * Reference:
 * <p>
 * Fei Tony Liu, Kai Ming Ting, Zhi-Hua Zhou<br>
 * Isolation-Based Anomaly Detection<br>
 * ACM Transanctions on Knowledge Discovery from Data (TKDD'12)
 * 
 * @author Fei Tony Liu et al. (Original Code)
 * @author Braulio V.S. Vinces (ELKIfication)
 *
 * @param <V> Vector type
 */
@Title("IsolationForest: Isolation-Based Anomaly Detection")
@Description("This algorithm detects anomalies purely based on the concept of isolation without employing any distance or density measure.")
@Reference(authors = "Fei Tony Liu, Kai Ming Ting, Zhi-Hua Zhou", //
    title = "Isolation-Based Anomaly Detection", //
    booktitle = "ACM Transanctions on Knowledge Discovery from Data (TKDD'12)", //
    url = "https://doi.org/10.1145/2133360.2133363", //
    bibkey = "DBLP:journals/tkdd/LiuTZ12")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.IsolationForest", "iforest" })
public class IsolationForest implements OutlierAlgorithm {

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(IsolationForest.class);

  /*
   * The set of trees
   */
  protected List<Tree> trees = null;

  /*
   * The number of trees
   */
  protected int numTrees = 100;

  /*
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

  public OutlierResult run(Database db, Relation<? extends NumberVector> relation) {
    // Output data storage
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    // Track minimum and maximum scores
    DoubleMinMax minmax = new DoubleMinMax();

    // Reduce sub sample size if data is too small
    if(relation.size() < subsampleSize) {
      subsampleSize = relation.size();
    }

    {
      StepProgress stepprog = LOG.isVerbose() ? new StepProgress(2) : null;

      LOG.beginStep(stepprog, 1, "Generating trees.");
      FiniteProgress prog1 = LOG.isVerbose() ? new FiniteProgress("IsolationForest progress", numTrees, LOG) : null;
      final Random random = rnd.getSingleThreadedRandom();
      // Generate trees
      trees = new ArrayList<>(numTrees);
      for(int i = 0; i < numTrees; i++) {
        ModifiableDBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), subsampleSize, rnd);
        trees.add(new Tree( //
            sample, //
            random, //
            0, //
            (int) FastMath.ceil(FastMath.log(relation.size()) / FastMath.log(2)), //
            RelationUtil.dimensionality(relation), //
            relation));
        LOG.incrementProcessed(prog1);
      }
      LOG.ensureCompleted(prog1);

      LOG.beginStep(stepprog, 2, "Computing distribution of scores.");
      FiniteProgress prog2 = LOG.isVerbose() ? new FiniteProgress("IsolationForest scores", relation.size(), LOG) : null;
      // Iterate over all objects
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        final double score = distributionForInstance(relation.get(iter));
        scores.putDouble(iter, score);
        minmax.put(score);
        LOG.incrementProcessed(prog2);
      }
      LOG.ensureCompleted(prog2);
    }

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
   * Returns the average path length of an unsuccessful search. Returns 0 if
   * argument is less than or equal to 1.
   */
  protected static double c(double n) {
    if(n <= 1.0) {
      return 0;
    }
    return 2 * (FastMath.log(n - 1) + MathUtil.EULERMASCHERONI) - (2 * (n - 1) / n);
  }

  /**
   * Returns distribution of scores.
   */
  protected double distributionForInstance(NumberVector vector) {
    double avgPathLength = 0;
    for(Tree m_tree : trees) {
      avgPathLength += m_tree.pathLength(vector);
    }
    avgPathLength /= trees.size();

    return FastMath.pow(2, -avgPathLength / c(subsampleSize));
  }

  /**
   * Inner class for building and using an isolation tree.
   */
  static class Tree {

    /**
     * The size of the node
     */
    private int size;

    /**
     * The split attribute
     */
    private int a;

    /**
     * The split point
     */
    private double q;

    /**
     * The successors
     */
    private List<Tree> successors;

    /**
     * Constructs a tree from data.
     *
     * @param data Subsample from data
     * @param r Random generator
     * @param height
     * @param maxHeight
     * @param dim Dimensionality of the relation.
     * @param relation Relation to index
     */
    protected Tree(ModifiableDBIDs data, Random r, int height, int maxHeight, int dim, Relation<? extends NumberVector> relation) {
      // Set size of node
      size = data.size();

      // Stop splitting if necessary
      if((size <= 1) || (height == maxHeight)) {
        return;
      }

      // Compute mins and maxs and eligible attributes
      double[] mins = new double[dim], maxs = new double[dim];
      Arrays.fill(mins, Double.MAX_VALUE);
      Arrays.fill(maxs, -Double.MAX_VALUE);
      for(DBIDIter iditer = data.iter(); iditer.valid(); iditer.advance()) {
        final NumberVector o = relation.get(iditer);
        for(int d = 0; d < dim; d++) {
          final double v = o.doubleValue(d);
          mins[d] = (v < mins[d]) ? v : mins[d];
          maxs[d] = (v > maxs[d]) ? v : maxs[d];
        }
      }

      IntegerArray al = new IntegerArray();
      for(int j = 0; j < dim; j++) {
        if(mins[j] < maxs[j]) {
          al.add(j);
        }
      }

      // Check whether any eligible attributes have been found
      if(al.isEmpty()) {
        return;
      }
      // Randomly pick an attribute and split point
      a = al.get(r.nextInt(al.size()));
      q = (r.nextDouble() * (maxs[a] - mins[a])) + mins[a];

      // Create sub trees
      successors = new ArrayList<>(2);
      ArrayModifiableDBIDs left = DBIDUtil.newArray();
      ArrayModifiableDBIDs right = DBIDUtil.newArray();
      for(DBIDIter iditer = data.iter(); iditer.valid(); iditer.advance()) {
        NumberVector vector = relation.get(iditer);
        if(vector.doubleValue(a) < q) {
          left.add(iditer);
        }
        if(vector.doubleValue(a) >= q) {
          right.add(iditer);
        }
      }
      successors.add(new Tree(left, r, height + 1, maxHeight, dim, relation));
      successors.add(new Tree(right, r, height + 1, maxHeight, dim, relation));
    }

    /**
     * Returns path length according to algorithm.
     */
    protected double pathLength(NumberVector vector) {
      List<Tree> _successors = successors;
      double depth = 0.;
      int _size = size;
      int _a = a;
      double _q = q;
      while(_successors != null) {
        int child = 1;
        if(vector.doubleValue(_a) < _q) {
          child = 0;
        }
        depth++;
        _size = _successors.get(child).size;
        _a = _successors.get(child).a;
        _q = _successors.get(child).q;
        _successors = _successors.get(child).successors;
      }
      return c(_size) + depth;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class
   * 
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for the number of trees
     */
    public static final OptionID NUM_TREES_ID = new OptionID("iforest.numtrees", "Number of trees to use.");

    /**
     * Parameter for the sub sample size
     */
    public static final OptionID SUBSAMPLE_SIZE_ID = new OptionID("iforest.subsample", "Sub sample size.");

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
