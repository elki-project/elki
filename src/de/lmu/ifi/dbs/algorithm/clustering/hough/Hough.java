package de.lmu.ifi.dbs.algorithm.clustering.hough;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.HoughResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.SubspaceClusterMap;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Associations;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.varianceanalysis.AbstractPCA;
import de.lmu.ifi.dbs.varianceanalysis.FirstNEigenPairFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Subspace clustering algorithm based on the hough transform.
 * todo hierarchy
 *
 * @author Elke Achtert
 */
public class Hough extends AbstractAlgorithm<ParameterizationFunction> {
  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "threshold for minumum number of points in a cluster.";

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter maxlevel.
   */
  public static final String MAXLEVEL_D = "the maximum level for splitting the hypercube.";

  /**
   * Parameter maxlevel.
   */
  public static final String MAXLEVEL_P = "maxlevel";

  /**
   * The default value for mindim parameter.
   */
  public static final int DEFAULT_MINDIM = 1;

  /**
   * Description for parameter mindim.
   */
  public static final String MINDIM_D = "the minimum dimensionality of the subspaces to be found, " +
                                        "default:" + DEFAULT_MINDIM + ".";

  /**
   * Parameter mindim.
   */
  public static final String MINDIM_P = "mindim";

  /**
   * Description for parameter jitter.
   */
  public static final String JITTER_D = "the maximum jitter for distance values.";

  /**
   * Parameter jitter.
   */
  public static final String JITTER_P = "jitter";

  /**
   * Description for flag adjust
   */
  public static final String ADJUSTMENT_D = "flag indicating that an adjustment of the" +
                                            "applied heuristic for choosing an interval " +
                                            "is performed after an interval is selected, " +
                                            "default: no adjustment";

  /**
   * Flag adjust.
   */
  public static final String ADJUSTMENT_F = "adjust";


  /**
   * The result.
   */
  private HoughResult result;

  /**
   * Minimum points.
   */
  private int minPts;

  /**
   * Flag indicating that an adjustment of the
   * applied heuristic for choosing an interval
   * is performed after an interval is selected.
   */
  private boolean adjust;

  /**
   * The maximum level for splitting the hypercube.
   */
  private int maxLevel;

  /**
   * The minmum dimensionality for the subspaces to be found.
   */
  private int minDim;

  /**
   * The maximum allowed jitter for distance values.
   */
  private double jitter;

  /**
   * Holds the dimensionality for noise.
   */
  private int noiseDim;

  /**
   * Holds a set of processed ids.
   */
  private Set<Integer> processedIDs;

  private Database<ParameterizationFunction> database;

  /**
   * Provides a new Hough algorithm.
   */
  public Hough() {
    super();

    //parameter minpts
    optionHandler.put(new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));

    //parameter maxLevel
    optionHandler.put(new IntParameter(MAXLEVEL_P, MAXLEVEL_D, new GreaterConstraint(0)));

    //parameter minDim
    IntParameter minDim = new IntParameter(MINDIM_P, MINDIM_D, new GreaterEqualConstraint(1));
    minDim.setDefaultValue(DEFAULT_MINDIM);
    optionHandler.put(minDim);

    //parameter jitter
    optionHandler.put(new DoubleParameter(JITTER_P, JITTER_D, new GreaterConstraint(0)));

    //flag adjust
    optionHandler.put(new Flag(ADJUSTMENT_F, ADJUSTMENT_D));
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<ParameterizationFunction> database) throws IllegalStateException {
    this.database = database;
    if (isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nDB size: ").append(database.size());
      msg.append("\nmin Dim: ").append(minDim);
      verbose(msg.toString());
    }

    try {
      processedIDs = new HashSet<Integer>(database.size());
      noiseDim = database.get(database.iterator().next()).getDimensionality();

      Progress progress = new Progress("Clustering", database.size());
      if (isVerbose()) {
        progress.setProcessed(0);
        progress(progress);
      }

      SubspaceClusterMap clusters = doRun(database, progress);
      if (isVerbose()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n\nclusters: ").append(clusters.subspaceDimensionalities());
        for (Integer id : clusters.subspaceDimensionalities()) {
          msg.append("\n         subspaceDim = ").append(id).append(": ").append(clusters.getCluster(id).size()).append(" cluster(s)");
          msg.append(" [");
          for (Set<Integer> c : clusters.getCluster(id)) {
            msg.append(c.size()).append(" ");
          }
          msg.append("objects]");
        }
        verbose(msg.toString());
      }

      result = new HoughResult(database, clusters, noiseDim);
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    catch (ParameterException e) {
      throw new IllegalStateException(e);
    }
    catch (NonNumericFeaturesException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<ParameterizationFunction> getResult() {
    return result;
  }

  /**
   * // todo
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("AdvancedHough",
                           "",
                           "",
                           "unpublished :-(");
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // minpts
    minPts = (Integer) optionHandler.getOptionValue(MINPTS_P);

    // maxlevel
    maxLevel = (Integer) optionHandler.getOptionValue(MAXLEVEL_P);

    // mindim
    minDim = (Integer) optionHandler.getOptionValue(MINDIM_P);

    // jitter
    jitter = (Double) optionHandler.getOptionValue(JITTER_P);

    // adjust
    adjust = optionHandler.isSet(ADJUSTMENT_F);

    return remainingParameters;
  }

  /**
   * Runs the hough algorithm on the specified database, this method is recursively called
   * until only noise is left.
   *
   * @param database the current database to run the hough algorithm on
   * @param progress the progress object for verbose messages
   * @return a mapping of subspace dimensionalites to clusters
   * @throws UnableToComplyException if an error according to the database occurs
   * @throws ParameterException if the parameter setting is wrong
   * @throws NonNumericFeaturesException if non numeric feature vectors are used
   */
  private SubspaceClusterMap doRun(Database<ParameterizationFunction> database,
                                   Progress progress) throws UnableToComplyException, ParameterException, NonNumericFeaturesException {


    int dim = database.get(database.iterator().next()).getDimensionality();
    SubspaceClusterMap clusterMap = new SubspaceClusterMap(dim);

    // init heap
    DefaultHeap<Integer, HoughInterval> heap = new DefaultHeap<Integer, HoughInterval>(false);
    Set<Integer> noiseIDs = getDatabaseIDs(database);
    initHeap(heap, database, dim, noiseIDs);

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
      msg.append("\nXXXX dim ").append(dim);
      msg.append("\nXXXX database.size ").append(database.size());
      msg.append("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
      debugFine(msg.toString());
    }
    else if (isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nXXXX dim ").append(dim).append(" database.size ").append(database.size());
      verbose(msg.toString());
    }

    // get the ''best'' d-dimensional intervals at max level
    while (! heap.isEmpty()) {
      HoughInterval interval = determineNextIntervalAtMaxLevel(heap);
      if (debug) {
        debugFine("\nnext interval in dim " + dim + ": " + interval);
      }
      else if (isVerbose()) {
        verbose("\nnext interval in dim " + dim + ": " + interval);
      }

      // only noise left
      if (interval == null) {
        break;
      }

      // do a dim-1 dimensional run
      Set<Integer> clusterIDs = new HashSet<Integer>();
      if (dim > minDim + 1) {
        Set<Integer> ids;
        Matrix basis_dim_minus_1;
        if (adjust) {
          ids = new HashSet<Integer>();
          basis_dim_minus_1 = runDerivator(database, dim, interval, ids);
//          System.out.println("ids " + ids.size());
//          System.out.println("basis (fuer dim " + (dim - 1) + ") " + basis_dim_minus_1);
        }
        else {
          ids = interval.getIDs();
          basis_dim_minus_1 = determineBasis(interval.centroid());
        }

        Database<ParameterizationFunction> db = buildDB(dim, basis_dim_minus_1, ids, database);
        if (db.size() != 0) {
          SubspaceClusterMap clusterMap_dim_minus_1 = doRun(db, progress);

          // add result of dim-1 to this result
          for (Integer d : clusterMap_dim_minus_1.subspaceDimensionalities()) {
            List<Set<Integer>> clusters_d = clusterMap_dim_minus_1.getCluster(d);
            for (Set<Integer> clusters : clusters_d) {
              clusterMap.add(d, clusters, this.database);
              noiseIDs.removeAll(clusters);
              clusterIDs.addAll(clusters);
              processedIDs.addAll(clusters);
            }
          }
        }
      }
      // dim == minDim
      else {
        clusterMap.add(dim - 1, interval.getIDs(), this.database);
        noiseIDs.removeAll(interval.getIDs());
        clusterIDs.addAll(interval.getIDs());
        processedIDs.addAll(interval.getIDs());
      }

      // reorganize heap
      Vector<HeapNode<Integer, HoughInterval>> heapVector = heap.copy();
      heap.clear();
      for (HeapNode<Integer, HoughInterval> heapNode : heapVector) {
        HoughInterval currentInterval = heapNode.getValue();
        currentInterval.removeIDs(clusterIDs);
        if (currentInterval.getIDs().size() >= minPts) {
          heap.addNode(new DefaultHeapNode<Integer, HoughInterval>(currentInterval.priority(), currentInterval));
        }
      }

      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        progress(progress);
      }
    }

    // put noise to clusters
    if (! noiseIDs.isEmpty()) {
      if (dim == noiseDim) {
        clusterMap.addToNoise(noiseIDs);
        processedIDs.addAll(noiseIDs);
      }
      else if (noiseIDs.size() >= minPts) {
        clusterMap.add(dim, noiseIDs, this.database);
        processedIDs.addAll(noiseIDs);
      }
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nnoise fuer dim ").append(dim).append(": ").append(noiseIDs.size());
      msg.append("\nclusters fuer dim= ").append(dim).append(": ").append(clusterMap.subspaceDimensionalities());
      for (Integer id : clusterMap.subspaceDimensionalities()) {
        msg.append("         corrDim = ");
        msg.append(id);
        msg.append(": ");
        msg.append(clusterMap.getCluster(id).size());
        msg.append(" cluster(s)");
        msg.append(" [");
        for (Set<Integer> c : clusterMap.getCluster(id)) {
          msg.append(c.size()).append(" ");
        }
        msg.append("\nobjects]");
      }
      debugFine(msg.toString());
    }

    if (isVerbose()) {
      progress.setProcessed(processedIDs.size());
      progress(progress);
    }


    return clusterMap;
  }

  /**
   * Initializes the heap with the root intervals.
   *
   * @param heap     the heap to be initialized
   * @param database the database storing the paramterization functions
   * @param dim      the dimensionality of the database
   * @param ids      the ids of the database
   */
  private void initHeap(DefaultHeap<Integer, HoughInterval> heap, Database<ParameterizationFunction> database, int dim, Set<Integer> ids) {
    HoughIntervalSplit split = new HoughIntervalSplit(database, minPts);

    // determine minimum and maximum function value of all functions
    double[] minMax = determineMinMaxDistance(database, dim);


    double d_min = minMax[0];
    double d_max = minMax[1];
    double dIntervalLength = d_max - d_min;
    int numDIntervals = (int) Math.ceil(dIntervalLength / jitter);
    double dIntervalSize = dIntervalLength / numDIntervals;
    double[] d_mins = new double[numDIntervals];
    double[] d_maxs = new double[numDIntervals];

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nd_min ").append(d_min);
      msg.append("\nd_max ").append(d_max);
      msg.append("\nnumDIntervals ").append(numDIntervals);
      msg.append("\ndIntervalSize ").append(dIntervalSize);
      debugFine(msg.toString());
    }
    else if (isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nd_min ").append(d_min);
      msg.append("\nd_max ").append(d_max);
      msg.append("\nnumDIntervals ").append(numDIntervals);
      msg.append("\ndIntervalSize ").append(dIntervalSize);
      verbose(msg.toString());
    }

    // alpha intervals
    double[] alphaMin = new double[dim - 1];
    double[] alphaMax = new double[dim - 1];
    Arrays.fill(alphaMax, Math.PI);

    for (int i = 0; i < numDIntervals; i++) {
      if (i == 0) {
        d_mins[i] = d_min;
      }
      else {
        d_mins[i] = d_maxs[i - 1];
      }

      if (i < numDIntervals - 1) {
        d_maxs[i] = d_mins[i] + dIntervalSize;
      }
      else {
        d_maxs[i] = d_max - d_mins[i];
      }

      HyperBoundingBox alphaInterval = new HyperBoundingBox(alphaMin, alphaMax);
      Set<Integer> intervalIDs = split.determineIDs(ids, alphaInterval, d_mins[i], d_maxs[i]);
      // todo: nur die mit allen punkten?
      if (intervalIDs != null && intervalIDs.size() >= minPts) {
//      if (intervalIDs != null && intervalIDs.size() >= database.size()) {
        HoughInterval rootInterval = new HoughInterval(alphaMin, alphaMax, split, intervalIDs, 0, 0, d_mins[i], d_maxs[i]);
        heap.addNode(new DefaultHeapNode<Integer, HoughInterval>(rootInterval.priority(), rootInterval));
      }
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nheap.size ").append(heap.size());
      debugFiner(msg.toString());
    }
  }

  /**
   * Builds a dim-1 dimensional database where the objects are projected into the specified subspace.
   *
   * @param dim      the dimensionality of the database
   * @param basis    the basis defining the subspace
   * @param ids      the ids for the new database
   * @param database the database storing the paramterization functions
   * @return a dim-1 dimensional database where the objects are projected into the specified subspace
   * @throws UnableToComplyException if an error according to the database occurs
   */
  private Database<ParameterizationFunction> buildDB(int dim,
                                                     Matrix basis,
                                                     Set<Integer> ids,
                                                     Database<ParameterizationFunction> database) throws UnableToComplyException {
    // build objects and associations
    List<ObjectAndAssociations<ParameterizationFunction>> oaas = new ArrayList<ObjectAndAssociations<ParameterizationFunction>>(ids.size());

    for (Integer id : ids) {
      ParameterizationFunction f = project(basis, database.get(id));

      Associations associations = database.getAssociations(id);
      ObjectAndAssociations<ParameterizationFunction> oaa = new ObjectAndAssociations<ParameterizationFunction>(f, associations);
      oaas.add(oaa);
    }

    // insert into db
    Database<ParameterizationFunction> result = new SequentialDatabase<ParameterizationFunction>();
    result.insert(oaas);

    if (debug) {
      debugFine("\ndb fuer dim " + (dim - 1) + ": " + result.size());
    }

//    if (dim - 1 == 2 || dim -1 == 3) {
//      System.out.println("#################################################################");
//      for (Iterator<Integer> it = result.iterator(); it.hasNext();) {
//        Integer id = it.next();
//        System.out.print("" + Format.format(result.get(id).getPointCoordinates(), " "));
//        System.out.println(" " + database.getAssociation(AssociationID.LABEL, id));
//      }
//    }

    return result;
  }

  /**
   * Projects the specified parametrization function into the subspace
   * described by the given basis.
   *
   * @param basis the basis defining he subspace
   * @param f     the parametrization function to be projected
   * @return the projected parametrization function
   */
  private ParameterizationFunction project(Matrix basis, ParameterizationFunction f) {
//    Matrix m = new Matrix(new double[][]{f.getPointCoordinates()}).times(basis);
    Matrix m = f.getRowVector().times(basis);
    ParameterizationFunction f_t = new ParameterizationFunction(m.getColumnPackedCopy());
    f_t.setID(f.getID());
    return f_t;
  }

  /**
   * Determines a basis defining a subspace described by the specified alpha values.
   *
   * @param alpha the alpha values
   * @return a basis defining a subspace described by the specified alpha values
   */
  private Matrix determineBasis(double[] alpha) {
    double[] nn = new double[alpha.length + 1];
    for (int i = 0; i < nn.length; i++) {
      double alpha_i = i == alpha.length ? 0 : alpha[i];
      nn[i] = sinusProduct(0, i, alpha) * Math.cos(alpha_i);
    }
    Matrix n = new Matrix(nn, alpha.length + 1);
    return n.completeToOrthonormalBasis();
  }

  /**
   * Computes the product of all sinus values of the specified angles
   * from start to end index.
   *
   * @param start the index to start
   * @param end   the index to end
   * @param alpha the array of angles
   * @return the product of all sinus values of the specified angles
   *         from start to end index
   */
  private double sinusProduct(int start, int end, double[] alpha) {
    double result = 1;
    for (int j = start; j < end; j++) {
      result *= Math.sin(alpha[j]);
    }
    return result;
  }

  /**
   * Determines the next ''best'' interval at maximum level, i.e. the next interval containing the
   * most unprocessed obejcts.
   *
   * @param heap the heap storing the intervals
   * @return the next ''best'' interval at maximum level
   */
  private HoughInterval determineNextIntervalAtMaxLevel(DefaultHeap<Integer, HoughInterval> heap) {
    HoughInterval next = doDetermineNextIntervalAtMaxLevel(heap);
    // noise path was chosen
    while (next == null) {
      if (heap.isEmpty()) {
        return null;
      }
      next = doDetermineNextIntervalAtMaxLevel(heap);
    }

    return next;
  }

  /**
   * Recursive helper method to determine the next ''best'' interval at maximum level,
   * i.e. the next interval containing the most unprocessed obejcts
   *
   * @param heap the heap storing the intervals
   * @return the next ''best'' interval at maximum level
   */
  private HoughInterval doDetermineNextIntervalAtMaxLevel(DefaultHeap<Integer, HoughInterval> heap) {
    HoughInterval interval = heap.getMinNode().getValue();
    int dim = interval.getDimensionality();
    while (true) {
      // max level is reached
      if (interval.getLevel() >= maxLevel && interval.getMaxSplitDimension() == dim) {
        return interval;
      }

      if (heap.size() % 10000 == 0 && isVerbose()) {
        verbose("heap size " + heap.size());
      }

      if (heap.size() >= 40000) {
        warning("Heap size > 50.000!!!");
        heap.clear();
        return null;
      }

      if (debug) {
        debugFiner("\nsplit " + interval.toString() + " " + interval.getLevel() + "-" + interval.getMaxSplitDimension());
      }
      interval.split();

      // noise
      if (! interval.hasChildren()) {
        return null;
      }

      HoughInterval bestInterval;
      if (interval.getLeftChild() != null && interval.getRightChild() != null) {
        int comp = interval.getLeftChild().compareTo(interval.getRightChild());
        if (comp < 0) {
          bestInterval = interval.getRightChild();
          heap.addNode(new DefaultHeapNode<Integer, HoughInterval>(interval.getLeftChild().priority(), interval.getLeftChild()));
        }
        else {
          bestInterval = interval.getLeftChild();
          heap.addNode(new DefaultHeapNode<Integer, HoughInterval>(interval.getRightChild().priority(), interval.getRightChild()));
        }
      }
      else if (interval.getLeftChild() == null) {
        bestInterval = interval.getRightChild();
      }
      else {
        bestInterval = interval.getLeftChild();
      }

      interval = bestInterval;
    }
  }

  /**
   * Returns the set of ids belonging to the specified database.
   *
   * @param database the database containing the parametrization functions.
   * @return the set of ids belonging to the specified database
   */
  private Set<Integer> getDatabaseIDs(Database<ParameterizationFunction> database) {
    Set<Integer> result = new HashSet<Integer>(database.size());
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

  /**
   * Determines the minimum and maximum function value of all parametrization functions
   * stored in the specified database.
   *
   * @param database       the database containing the parametrization functions.
   * @param dimensionality the dimensionality of the database
   * @return an array containing the minimum and maximum function value of all parametrization functions
   *         stored in the specified database
   */
  private double[] determineMinMaxDistance(Database<ParameterizationFunction> database, int dimensionality) {
    double[] min = new double[dimensionality - 1];
    double[] max = new double[dimensionality - 1];
    Arrays.fill(max, Math.PI);
    HyperBoundingBox box = new HyperBoundingBox(min, max);

    double d_min = Double.POSITIVE_INFINITY;
    double d_max = Double.NEGATIVE_INFINITY;
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      Integer id = it.next();
      ParameterizationFunction f = database.get(id);
      HyperBoundingBox minMax = f.determineAlphaMinMax(box);
      double f_min = f.function(minMax.getMin());
      double f_max = f.function(minMax.getMax());

      d_min = Math.min(d_min, f_min);
      d_max = Math.max(d_max, f_max);
    }
    return new double[]{d_min, d_max};
  }

  /**
   * Runs the derivator on the specified inerval and assigns all points
   * having a distance less then the standard deviation of the derivator model
   * to the model to this model.
   *
   * @param database the database containing the parametrization functions
   * @param interval the interval to build the model
   * @param dim      the dimensinality of the database
   * @param ids      an empty set to assign the ids
   * @return a basis of the found subspace
   * @throws UnableToComplyException if an error according to the database occurs
   * @throws ParameterException if the parameter setting is wrong
   *
   */
  private Matrix runDerivator(Database<ParameterizationFunction> database,
                              int dim,
                              HoughInterval interval,
                              Set<Integer> ids) throws UnableToComplyException, ParameterException {
    // build database for derivator
    Database<RealVector> derivatorDB = buildDerivatorDB(database, interval);

    DependencyDerivator derivator = new DependencyDerivator();

    List<String> params = new ArrayList<String>();
    params.add(OptionHandler.OPTION_PREFIX + AbstractPCA.EIGENPAIR_FILTER_P);
    params.add(FirstNEigenPairFilter.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + FirstNEigenPairFilter.N_P);
    params.add(Integer.toString(dim - 1));
    derivator.setParameters(params.toArray(new String[params.size()]));

    //noinspection unchecked
    derivator.run(derivatorDB);
    CorrelationAnalysisSolution model = derivator.getResult();

    Matrix weightMatrix = model.getSimilarityMatrix();
    RealVector centroid = new DoubleVector(model.getCentroid());
    DistanceFunction<RealVector, DoubleDistance> df = new WeightedDistanceFunction(weightMatrix);
    DoubleDistance eps = df.valueOf("0.25");

    ids.addAll(interval.getIDs());
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      Integer id = it.next();
      RealVector v = new DoubleVector(database.get(id).getRowVector().getRowPackedCopy());
      DoubleDistance d = df.distance(v, centroid);
      if (d.compareTo(eps) < 0) {
        ids.add(id);
      }
    }

    Matrix basis = model.getStrongEigenvectors();
    return basis.getMatrix(0, basis.getRowDimensionality() - 1, 0, dim - 2);
  }

  /**
   * Builds a database for the derivator consisting of the ids
   * in the specified interval.
   *
   * @param database the database storing the paramterization functions
   * @param interval the interval to build the database from
   * @return a database for the derivator consisting of the ids
   *         in the specified interval
   * @throws UnableToComplyException if an error according to the database occurs
   */
  private Database<RealVector> buildDerivatorDB(Database<ParameterizationFunction> database,
                                                HoughInterval interval) throws UnableToComplyException {
    // build objects and associations
    List<ObjectAndAssociations<RealVector>> oaas = new ArrayList<ObjectAndAssociations<RealVector>>(database.size());

    for (Integer id : interval.getIDs()) {
      Associations associations = database.getAssociations(id);
      RealVector v = new DoubleVector(database.get(id).getRowVector().getRowPackedCopy());
      ObjectAndAssociations<RealVector> oaa = new ObjectAndAssociations<RealVector>(v, associations);
      oaas.add(oaa);
    }

    // insert into db
    Database<RealVector> result = new SequentialDatabase<RealVector>();
    result.insert(oaas);

    if (debug) {
      debugFine("\ndb fuer derivator : " + result.size());
    }

    return result;
  }
}
