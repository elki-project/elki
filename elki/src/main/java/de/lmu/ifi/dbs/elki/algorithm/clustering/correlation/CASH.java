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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.euclideanLength;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.getMatrix;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusTimesEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.scalarProduct;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transpose;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimes;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimesTimes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash.CASHInterval;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash.CASHIntervalSplit;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.model.LinearEquationModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.StandardCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparatorMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ObjectHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import net.jafama.FastMath;

/**
 * The CASH algorithm is a subspace clustering algorithm based on the Hough
 * transform.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Jörn David, Peer Kröger, Arthur Zimek<br>
 * Robust clustering in arbitrarily oriented subspaces.<br>
 * In Proc. 8th SIAM Int. Conf. on Data Mining (SDM'08)
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @has - - - CASHInterval
 * @has - - - ParameterizationFunction
 * @has - - - LinearEquationModel
 *
 * @param <V> Vector type
 */
// todo elke hierarchy (later)
@Title("CASH: Robust clustering in arbitrarily oriented subspaces")
@Description("Subspace clustering algorithm based on the Hough transform.")
@Reference(authors = "Elke Achtert, Christian Böhm, Jörn David, Peer Kröger, Arthur Zimek", //
    title = "Robust clustering in arbitraily oriented subspaces", //
    booktitle = "Proc. 8th SIAM Int. Conf. on Data Mining (SDM'08)", //
    url = "https://doi.org/10.1137/1.9781611972788.69", //
    bibkey = "DBLP:conf/sdm/AchtertBDKZ08")
public class CASH<V extends NumberVector> extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CASH.class);

  /**
   * Threshold for minimum number of points in a cluster
   */
  protected int minPts;

  /**
   * Maximum level for splitting the hypercube.
   */
  protected int maxLevel;

  /**
   * Minimum dimensionality of the subspaces to be found
   */
  protected int minDim;

  /**
   * Maximum jitter for distance values.
   */
  protected double jitter;

  /**
   * Apply adjustment heuristic for interval choosing.
   */
  protected boolean adjust;

  /**
   * Holds the dimensionality for noise.
   */
  private int noiseDim;

  /**
   * Holds a set of processed ids.
   */
  private ModifiableDBIDs processedIDs;

  /**
   * The entire relation.
   */
  private Relation<ParameterizationFunction> fulldatabase;

  /**
   * Constructor.
   *
   * @param minPts MinPts parameter
   * @param maxLevel Maximum level
   * @param minDim Minimum dimensionality
   * @param jitter Jitter
   * @param adjust Adjust
   */
  public CASH(int minPts, int maxLevel, int minDim, double jitter, boolean adjust) {
    super();
    this.minPts = minPts;
    this.maxLevel = maxLevel;
    this.minDim = minDim;
    this.jitter = jitter;
    this.adjust = adjust;
  }

  /**
   * Run CASH on the relation.
   *
   * @param rel Relation
   * @return Clustering result
   */
  public Clustering<Model> run(Relation<V> rel) {
    fulldatabase = preprocess(rel);
    processedIDs = DBIDUtil.newHashSet(fulldatabase.size());
    noiseDim = dimensionality(fulldatabase);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("CASH Clustering", fulldatabase.size(), LOG) : null;
    Clustering<Model> result = doRun(fulldatabase, progress);
    LOG.ensureCompleted(progress);

    if(LOG.isVerbose()) {
      StringBuilder msg = new StringBuilder(1000);
      for(Cluster<Model> c : result.getAllClusters()) {
        if(c.getModel() instanceof LinearEquationModel) {
          LinearEquationModel s = (LinearEquationModel) c.getModel();
          msg.append("\n Cluster: Dim: " + s.getLes().subspacedim() + " size: " + c.size());
        }
        else {
          msg.append("\n Cluster: " + c.getModel().getClass().getName() + " size: " + c.size());
        }
      }
      LOG.verbose(msg.toString());
    }
    return result;
  }

  /**
   * Preprocess the dataset, precomputing the parameterization functions.
   *
   * @param vrel Vector relation
   * @return Preprocessed relation
   */
  private Relation<ParameterizationFunction> preprocess(Relation<V> vrel) {
    DBIDs ids = vrel.getDBIDs();
    SimpleTypeInformation<ParameterizationFunction> type = new SimpleTypeInformation<>(ParameterizationFunction.class);
    WritableDataStore<ParameterizationFunction> prep = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT, ParameterizationFunction.class);

    // Project
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      prep.put(iter, new ParameterizationFunction(vrel.get(iter)));
    }
    return new MaterializedRelation<>(type, ids, null, prep);
  }

  /**
   * Runs the CASH algorithm on the specified database, this method is
   * recursively called until only noise is left.
   *
   * @param relation the Relation to run the CASH algorithm on
   * @param progress the progress object for verbose messages
   * @return a mapping of subspace dimensionalities to clusters
   */
  private Clustering<Model> doRun(Relation<ParameterizationFunction> relation, FiniteProgress progress) {
    Clustering<Model> res = new Clustering<>("CASH clustering", "cash-clustering");
    final int dim = dimensionality(relation);

    // init heap
    ObjectHeap<CASHInterval> heap = new ComparatorMaxHeap<>(new Comparator<CASHInterval>() {
      @Override
      public int compare(CASHInterval o1, CASHInterval o2) {
        return Integer.compare(o1.priority(), o2.priority());
      }
    });
    ModifiableDBIDs noiseIDs = DBIDUtil.newHashSet(relation.getDBIDs());
    initHeap(heap, relation, dim, noiseIDs);

    if(LOG.isVerbose()) {
      LOG.verbose(new StringBuilder().append("dim ").append(dim).append(" database.size ").append(relation.size()).toString());
    }

    // get the ''best'' d-dimensional intervals at max level
    while(!heap.isEmpty()) {
      CASHInterval interval = determineNextIntervalAtMaxLevel(heap);
      if(LOG.isVerbose()) {
        LOG.verbose("next interval in dim " + dim + ": " + interval);
      }

      // only noise left
      if(interval == null) {
        break;
      }

      // do a dim-1 dimensional run
      ModifiableDBIDs clusterIDs = DBIDUtil.newHashSet();
      if(dim > minDim + 1) {
        ModifiableDBIDs ids;
        double[][] basis_dim_minus_1;
        if(adjust) {
          ids = DBIDUtil.newHashSet();
          basis_dim_minus_1 = runDerivator(relation, dim, interval, ids);
        }
        else {
          ids = interval.getIDs();
          basis_dim_minus_1 = determineBasis(SpatialUtil.centroid(interval));
        }

        if(ids.size() != 0) {
          MaterializedRelation<ParameterizationFunction> db = buildDB(dim, basis_dim_minus_1, ids, relation);
          // add result of dim-1 to this result
          Clustering<Model> res_dim_minus_1 = doRun(db, progress);
          for(Cluster<Model> cluster : res_dim_minus_1.getAllClusters()) {
            res.addToplevelCluster(cluster);
            noiseIDs.removeDBIDs(cluster.getIDs());
            clusterIDs.addDBIDs(cluster.getIDs());
            processedIDs.addDBIDs(cluster.getIDs());
          }
        }
      }
      // dim == minDim
      else {
        LinearEquationSystem les = runDerivator(relation, dim - 1, interval.getIDs());
        Cluster<Model> c = new Cluster<Model>(interval.getIDs(), new LinearEquationModel(les));
        res.addToplevelCluster(c);
        noiseIDs.removeDBIDs(interval.getIDs());
        clusterIDs.addDBIDs(interval.getIDs());
        processedIDs.addDBIDs(interval.getIDs());
      }

      // Rebuild heap
      ArrayList<CASHInterval> heapVector = new ArrayList<>(heap.size());
      for(ObjectHeap.UnsortedIter<CASHInterval> iter = heap.unsortedIter(); iter.valid(); iter.advance()) {
        heapVector.add(iter.get());
      }
      heap.clear();
      for(CASHInterval currentInterval : heapVector) {
        currentInterval.removeIDs(clusterIDs);
        if(currentInterval.getIDs().size() >= minPts) {
          heap.add(currentInterval);
        }
      }

      if(progress != null) {
        progress.setProcessed(processedIDs.size(), LOG);
      }
    }

    // put noise to clusters
    if(!noiseIDs.isEmpty()) {
      if(dim == noiseDim) {
        res.addToplevelCluster(new Cluster<Model>(noiseIDs, true, ClusterModel.CLUSTER));
        processedIDs.addDBIDs(noiseIDs);
      }
      else if(noiseIDs.size() >= minPts) {
        LinearEquationSystem les = runDerivator(fulldatabase, dim - 1, noiseIDs);
        res.addToplevelCluster(new Cluster<Model>(noiseIDs, true, new LinearEquationModel(les)));
        processedIDs.addDBIDs(noiseIDs);
      }
    }

    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append("noise fuer dim ").append(dim).append(": ").append(noiseIDs.size());

      for(Cluster<Model> c : res.getAllClusters()) {
        if(c.getModel() instanceof LinearEquationModel) {
          msg.append("\n Cluster: Dim: ").append(((LinearEquationModel) c.getModel()).getLes().subspacedim());
        }
        else {
          msg.append("\n Cluster: ").append(c.getModel().getClass().getName());
        }
        msg.append(" size: ").append(c.size());
      }
      LOG.debugFine(msg.toString());
    }

    if(progress != null) {
      progress.setProcessed(processedIDs.size(), LOG);
    }
    return res;
  }

  /**
   * Get the dimensionality of a vector field.
   *
   * @param relation Relation
   * @return Dimensionality
   */
  private static int dimensionality(Relation<ParameterizationFunction> relation) {
    return relation.get(relation.iterDBIDs()).getDimensionality();
  }

  /**
   * Initializes the heap with the root intervals.
   *
   * @param heap the heap to be initialized
   * @param relation the database storing the parameterization functions
   * @param dim the dimensionality of the database
   * @param ids the ids of the database
   */
  private void initHeap(ObjectHeap<CASHInterval> heap, Relation<ParameterizationFunction> relation, int dim, DBIDs ids) {
    CASHIntervalSplit split = new CASHIntervalSplit(relation, minPts);

    // determine minimum and maximum function value of all functions
    double[] minMax = determineMinMaxDistance(relation, dim);

    double d_min = minMax[0], d_max = minMax[1];
    double dIntervalLength = d_max - d_min;
    int numDIntervals = (int) FastMath.ceil(dIntervalLength / jitter);
    double dIntervalSize = dIntervalLength / numDIntervals;
    double[] d_mins = new double[numDIntervals],
        d_maxs = new double[numDIntervals];

    if(LOG.isVerbose()) {
      LOG.verbose(new StringBuilder().append("d_min ").append(d_min)//
          .append("\nd_max ").append(d_max)//
          .append("\nnumDIntervals ").append(numDIntervals)//
          .append("\ndIntervalSize ").append(dIntervalSize).toString());
    }

    // alpha intervals
    double[] alphaMin = new double[dim - 1], alphaMax = new double[dim - 1];
    Arrays.fill(alphaMax, Math.PI);

    for(int i = 0; i < numDIntervals; i++) {
      d_mins[i] = (i == 0) ? d_min : d_maxs[i - 1];
      d_maxs[i] = (i < numDIntervals - 1) ? d_mins[i] + dIntervalSize : d_max - d_mins[i];

      HyperBoundingBox alphaInterval = new HyperBoundingBox(alphaMin, alphaMax);
      ModifiableDBIDs intervalIDs = split.determineIDs(ids, alphaInterval, d_mins[i], d_maxs[i]);
      if(intervalIDs != null && intervalIDs.size() >= minPts) {
        heap.add(new CASHInterval(alphaMin, alphaMax, split, intervalIDs, -1, 0, d_mins[i], d_maxs[i]));
      }
    }

    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner(new StringBuilder().append("heap.size: ").append(heap.size()).toString());
    }
  }

  /**
   * Builds a dim-1 dimensional database where the objects are projected into
   * the specified subspace.
   *
   * @param dim the dimensionality of the database
   * @param basis the basis defining the subspace
   * @param ids the ids for the new database
   * @param relation the database storing the parameterization functions
   * @return a dim-1 dimensional database where the objects are projected into
   *         the specified subspace
   */
  private MaterializedRelation<ParameterizationFunction> buildDB(int dim, double[][] basis, DBIDs ids, Relation<ParameterizationFunction> relation) {
    ProxyDatabase proxy = new ProxyDatabase(ids);
    SimpleTypeInformation<ParameterizationFunction> type = new SimpleTypeInformation<>(ParameterizationFunction.class);
    WritableDataStore<ParameterizationFunction> prep = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT, ParameterizationFunction.class);

    // Project
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      prep.put(iter, project(basis, relation.get(iter)));
    }

    if(LOG.isDebugging()) {
      LOG.debugFine("db fuer dim " + (dim - 1) + ": " + ids.size());
    }
    MaterializedRelation<ParameterizationFunction> prel = new MaterializedRelation<>(type, ids, null, prep);
    proxy.addRelation(prel);
    return prel;
  }

  /**
   * Projects the specified parameterization function into the subspace
   * described by the given basis.
   *
   * @param basis the basis defining he subspace
   * @param f the parameterization function to be projected
   * @return the projected parameterization function
   */
  private ParameterizationFunction project(double[][] basis, ParameterizationFunction f) {
    // Matrix m = new Matrix(new
    // double[][]{f.getPointCoordinates()}).times(basis);
    double[] m = transposeTimes(basis, f.getColumnVector());
    return new ParameterizationFunction(DoubleVector.wrap(m));
  }

  /**
   * Determines a basis defining a subspace described by the specified alpha
   * values.
   *
   * @param alpha the alpha values
   * @return a basis defining a subspace described by the specified alpha values
   */
  private double[][] determineBasis(double[] alpha) {
    final int dim = alpha.length;
    // Primary vector:
    double[] nn = new double[dim + 1];
    for(int i = 0; i < nn.length; i++) {
      double alpha_i = i == alpha.length ? 0 : alpha[i];
      nn[i] = ParameterizationFunction.sinusProduct(0, i, alpha) * FastMath.cos(alpha_i);
    }
    timesEquals(nn, 1. / euclideanLength(nn)); // Normalize
    // Find orthogonal system, in transposed form:
    double[][] basis = new double[dim][];
    int found = 0;
    for(int i = 0; i < nn.length && found < dim; i++) {
      // ith unit vector.
      final double[] e_i = new double[nn.length];
      e_i[i] = 1.0;
      minusTimesEquals(e_i, nn, scalarProduct(e_i, nn));
      double len = euclideanLength(e_i);
      // Make orthogonal to earlier (normal) basis vectors:
      for(int j = 0; j < found; j++) {
        if(len < 1e-9) { // Disappeared, probably linear dependent
          break;
        }
        minusTimesEquals(e_i, basis[j], scalarProduct(e_i, basis[j]));
        len = euclideanLength(e_i);
      }
      if(len < 1e-9) {
        continue;
      }
      timesEquals(e_i, 1. / len); // Normalize
      basis[found++] = e_i;
    }
    if(found < dim) {
      // Likely some numerical instability, should not happen.
      for(int i = found; i < dim; i++) {
        basis[i] = new double[nn.length]; // Append zero vectors
      }
    }
    return transpose(basis);
  }

  /**
   * Determines the next ''best'' interval at maximum level, i.e. the next
   * interval containing the most unprocessed objects.
   *
   * @param heap the heap storing the intervals
   * @return the next ''best'' interval at maximum level
   */
  private CASHInterval determineNextIntervalAtMaxLevel(ObjectHeap<CASHInterval> heap) {
    CASHInterval next = doDetermineNextIntervalAtMaxLevel(heap);
    // noise path was chosen
    while(next == null) {
      if(heap.isEmpty()) {
        return null;
      }
      next = doDetermineNextIntervalAtMaxLevel(heap);
    }
    return next;
  }

  /**
   * Recursive helper method to determine the next ''best'' interval at maximum
   * level, i.e. the next interval containing the most unprocessed objects
   *
   * @param heap the heap storing the intervals
   * @return the next ''best'' interval at maximum level
   */
  private CASHInterval doDetermineNextIntervalAtMaxLevel(ObjectHeap<CASHInterval> heap) {
    CASHInterval interval = heap.poll();
    int dim = interval.getDimensionality();
    while(true) {
      // max level is reached
      if(interval.getLevel() >= maxLevel && interval.getMaxSplitDimension() == (dim - 1)) {
        return interval;
      }

      if(heap.size() % 10000 == 0 && LOG.isVerbose()) {
        LOG.verbose("heap size " + heap.size());
      }

      if(heap.size() >= 40000) {
        LOG.warning("Heap size > 40.000! Stopping.");
        heap.clear();
        return null;
      }

      if(LOG.isDebuggingFiner()) {
        LOG.debugFiner("split " + interval.toString() + " " + interval.getLevel() + "-" + interval.getMaxSplitDimension());
      }
      interval.split();

      // noise
      if(!interval.hasChildren()) {
        return null;
      }

      CASHInterval bestInterval;
      if(interval.getLeftChild() != null && interval.getRightChild() != null) {
        int comp = interval.getLeftChild().compareTo(interval.getRightChild());
        if(comp < 0) {
          bestInterval = interval.getRightChild();
          heap.add(interval.getLeftChild());
        }
        else {
          bestInterval = interval.getLeftChild();
          heap.add(interval.getRightChild());
        }
      }
      else if(interval.getLeftChild() == null) {
        bestInterval = interval.getRightChild();
      }
      else {
        bestInterval = interval.getLeftChild();
      }

      interval = bestInterval;
    }
  }

  /**
   * Determines the minimum and maximum function value of all parameterization
   * functions stored in the specified database.
   *
   * @param relation the database containing the parameterization functions.
   * @param dimensionality the dimensionality of the database
   * @return an array containing the minimum and maximum function value of all
   *         parameterization functions stored in the specified database
   */
  private double[] determineMinMaxDistance(Relation<ParameterizationFunction> relation, int dimensionality) {
    double[] min = new double[dimensionality - 1];
    double[] max = new double[dimensionality - 1];
    Arrays.fill(max, Math.PI);
    HyperBoundingBox box = new HyperBoundingBox(min, max);

    double d_min = Double.POSITIVE_INFINITY, d_max = Double.NEGATIVE_INFINITY;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      ParameterizationFunction f = relation.get(iditer);
      HyperBoundingBox minMax = f.determineAlphaMinMax(box);
      double f_min = f.function(SpatialUtil.getMin(minMax));
      double f_max = f.function(SpatialUtil.getMax(minMax));

      d_min = Math.min(d_min, f_min);
      d_max = Math.max(d_max, f_max);
    }
    return new double[] { d_min, d_max };
  }

  /**
   * Runs the derivator on the specified interval and assigns all points having
   * a distance less then the standard deviation of the derivator model to the
   * model to this model.
   *
   * @param relation the database containing the parameterization functions
   * @param interval the interval to build the model
   * @param dim the dimensionality of the database
   * @param ids an empty set to assign the ids
   * @return a basis of the found subspace
   */
  private double[][] runDerivator(Relation<ParameterizationFunction> relation, int dim, CASHInterval interval, ModifiableDBIDs ids) {
    Database derivatorDB = buildDerivatorDB(relation, interval);

    PCARunner pca = new PCARunner(new StandardCovarianceMatrixBuilder());
    EigenPairFilter filter = new FirstNEigenPairFilter(dim - 1);
    DependencyDerivator<DoubleVector> derivator = new DependencyDerivator<>(null, FormatUtil.NF4, pca, filter, 0, false);

    CorrelationAnalysisSolution<DoubleVector> model = derivator.run(derivatorDB);

    double[][] weightMatrix = model.getSimilarityMatrix();
    double[] centroid = model.getCentroid();
    double eps = .25;

    ids.addDBIDs(interval.getIDs());
    // Search for nearby vectors in original database
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] v = minusEquals(relation.get(iditer).getColumnVector(), centroid);
      double d = transposeTimesTimes(v, weightMatrix, v);
      if(d <= eps) {
        ids.add(iditer);
      }
    }

    double[][] basis = model.getStrongEigenvectors();
    return getMatrix(basis, 0, basis.length, 0, dim - 1);
  }

  /**
   * Builds a database for the derivator consisting of the ids in the specified
   * interval.
   *
   * @param relation the database storing the parameterization functions
   * @param interval the interval to build the database from
   * @return a database for the derivator consisting of the ids in the specified
   *         interval
   */
  private Database buildDerivatorDB(Relation<ParameterizationFunction> relation, CASHInterval interval) {
    DBIDs ids = interval.getIDs();
    ProxyDatabase proxy = new ProxyDatabase(ids);
    int dim = dimensionality(relation);
    SimpleTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    WritableDataStore<DoubleVector> prep = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT, DoubleVector.class);

    // Project
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      prep.put(iter, DoubleVector.wrap(relation.get(iter).getColumnVector()));
    }

    if(LOG.isDebugging()) {
      LOG.debugFine("db fuer derivator : " + ids.size());
    }

    MaterializedRelation<DoubleVector> prel = new MaterializedRelation<>(type, ids, null, prep);
    proxy.addRelation(prel);
    return proxy;
  }

  /**
   * Runs the derivator on the specified interval and assigns all points having
   * a distance less then the standard deviation of the derivator model to the
   * model to this model.
   *
   * @param relation the database containing the parameterization functions
   * @param ids the ids to build the model
   * @param dimensionality the dimensionality of the subspace
   * @return a basis of the found subspace
   */
  private LinearEquationSystem runDerivator(Relation<ParameterizationFunction> relation, int dimensionality, DBIDs ids) {
    try {
      // build database for derivator
      Database derivatorDB = buildDerivatorDB(relation, ids);

      PCARunner pca = new PCARunner(new StandardCovarianceMatrixBuilder());
      EigenPairFilter filter = new FirstNEigenPairFilter(dimensionality);
      DependencyDerivator<DoubleVector> derivator = new DependencyDerivator<>(null, FormatUtil.NF4, pca, filter, 0, false);

      CorrelationAnalysisSolution<DoubleVector> model = derivator.run(derivatorDB);
      LinearEquationSystem les = model.getNormalizedLinearEquationSystem(null);
      return les;
    }
    catch(NonNumericFeaturesException e) {
      throw new IllegalStateException("Error during normalization" + e);
    }
  }

  /**
   * Builds a database for the derivator consisting of the ids in the specified
   * interval.
   *
   * @param relation the database storing the parameterization functions
   * @param ids the ids to build the database from
   * @return a database for the derivator consisting of the ids in the specified
   *         interval
   */
  private Database buildDerivatorDB(Relation<ParameterizationFunction> relation, DBIDs ids) {
    ProxyDatabase proxy = new ProxyDatabase(ids);
    int dim = dimensionality(relation);
    SimpleTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    MaterializedRelation<DoubleVector> prep = new MaterializedRelation<>(type, ids);
    proxy.addRelation(prep);

    // Project
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      prep.insert(iter, DoubleVector.wrap(relation.get(iter).getColumnVector()));
    }
    return proxy;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the threshold for minimum number of points in a
     * cluster, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("cash.minpts", "Threshold for minimum number of points in a cluster.");

    /**
     * Parameter to specify the maximum level for splitting the hypercube, must
     * be an integer greater than 0.
     */
    public static final OptionID MAXLEVEL_ID = new OptionID("cash.maxlevel", "The maximum level for splitting the hypercube.");

    /**
     * Parameter to specify the minimum dimensionality of the subspaces to be
     * found, must be an integer greater than 0.
     */
    public static final OptionID MINDIM_ID = new OptionID("cash.mindim", "The minimum dimensionality of the subspaces to be found.");

    /**
     * Parameter to specify the maximum jitter for distance values, must be a
     * double greater than 0.
     */
    public static final OptionID JITTER_ID = new OptionID("cash.jitter", "The maximum jitter for distance values.");

    /**
     * Flag to indicate that an adjustment of the applied heuristic for choosing
     * an interval is performed after an interval is selected.
     */
    public static final OptionID ADJUST_ID = new OptionID("cash.adjust", "Flag to indicate that an adjustment of the applied heuristic for choosing an interval " + "is performed after an interval is selected.");

    /**
     * Threshold for minimum number of points in a cluster
     */
    protected int minPts;

    /**
     * Maximum level for splitting the hypercube.
     */
    protected int maxLevel;

    /**
     * Minimum dimensionality of the subspaces to be found
     */
    protected int minDim;

    /**
     * Maximum jitter for distance values.
     */
    protected double jitter;

    /**
     * Apply adjustment heuristic for interval choosing.
     */
    protected boolean adjust;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter minptsP = new IntParameter(MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minPts = minptsP.getValue();
      }
      IntParameter maxlevelP = new IntParameter(MAXLEVEL_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(maxlevelP)) {
        maxLevel = maxlevelP.getValue();
      }
      IntParameter mindimP = new IntParameter(MINDIM_ID, 1)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(mindimP)) {
        minDim = mindimP.getValue();
      }
      DoubleParameter jitterP = new DoubleParameter(JITTER_ID)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(jitterP)) {
        jitter = jitterP.getValue();
      }
      Flag adjustF = new Flag(ADJUST_ID);
      if(config.grab(adjustF)) {
        adjust = adjustF.getValue();
      }
    }

    @Override
    protected CASH<NumberVector> makeInstance() {
      return new CASH<>(minPts, maxLevel, minDim, jitter, adjust);
    }
  }
}
