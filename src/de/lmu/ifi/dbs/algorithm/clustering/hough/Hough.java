package de.lmu.ifi.dbs.algorithm.clustering.hough;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.HoughResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.SubspaceClusterMap;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.*;

/**
 * Subspace clustering algorithm based on the hough transform.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
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
   * The result.
   */
  private HoughResult result;

  /**
   * Minimum points.
   */
  private int minPts;

  /**
   * The maximum level for splitting the hypercube.
   */
  private int maxLevel;

  /**
   * Holds the dimensionality for noise.
   */
  private int noiseDim;

  /**
   * Holds a set of processed ids.
   */
  protected Set<Integer> processedIDs;

  /**
   * Provides a new Hough algorithm.
   */
  public Hough() {
    super();
//    this.debug = true;
    optionHandler.put(Hough.MINPTS_P, new IntParameter(Hough.MINPTS_P, Hough.MINPTS_D, new GreaterConstraint(0)));
    optionHandler.put(Hough.MAXLEVEL_P, new IntParameter(Hough.MAXLEVEL_P, Hough.MAXLEVEL_D, new GreaterConstraint(0)));
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
    try {
      processedIDs = new HashSet<Integer>(database.size());
      noiseDim = database.get(database.iterator().next()).getDimensionality();

      Progress progress = new Progress("Clustering", database.size());
      if (isVerbose()) {
        progress.setProcessed(0);
        progress(new ProgressLogRecord(LogLevel.PROGRESS,
                                       Util.status(progress),
                                       progress.getTask(),
                                       progress.status()));
      }


      SubspaceClusterMap clusters = doRun(database, progress);
      result = new HoughResult(database, clusters, noiseDim);

      if (isVerbose()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n\nclusters: " + clusters.subspaceDimensionalities());
        for (Integer id : clusters.subspaceDimensionalities()) {
          msg.append("\n         subspaceDim = " + id + ": " + clusters.getCluster(id).size() + " cluster(s)");
          msg.append(" [");
          for (Set<Integer> c : clusters.getCluster(id)) {
            msg.append(c.size() + " ");
          }
          msg.append("objects]");
        }
        verbose(msg.toString());
      }
    }
    catch (UnableToComplyException e) {
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
    String minptsString = optionHandler.getOptionValue(Hough.MINPTS_P);
    try {
      minPts = Integer.parseInt(minptsString);
      if (minPts <= 0) {
        throw new WrongParameterValueException(Hough.MINPTS_P, minptsString,
                                               Hough.MINPTS_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(Hough.MINPTS_P, minptsString,
                                             Hough.MINPTS_D, e);
    }

    // maxlevel
    String maxLevelString = optionHandler.getOptionValue(Hough.MAXLEVEL_P);
    try {
      maxLevel = Integer.parseInt(maxLevelString);
      if (maxLevel <= 0) {
        throw new WrongParameterValueException(Hough.MAXLEVEL_P, minptsString,
                                               Hough.MAXLEVEL_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(Hough.MAXLEVEL_P, minptsString,
                                             Hough.MAXLEVEL_D, e);
    }

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(Hough.MINPTS_P, Integer.toString(minPts));
    mySettings.addSetting(Hough.MAXLEVEL_P, Integer.toString(maxLevel));

    return attributeSettings;
  }

  /**
   * Runs the hough algorithm on the specified database, this method is recursively called
   * until only noise is left.
   *
   * @param database the current database to run the hough algorithm on
   * @param progress the progress object for verbose messages
   * @return a mapping of subspace dimensionalites to clusters
   * @throws UnableToComplyException
   */
  private SubspaceClusterMap doRun(Database<ParameterizationFunction> database, Progress progress) throws UnableToComplyException {
    int dim = database.get(database.iterator().next()).getDimensionality();
    SubspaceClusterMap clusterMap = new SubspaceClusterMap(dim);

    // init heap
    HoughInterval rootInterval = buildRootInterval(database, dim);
    DefaultHeap<Integer, HoughInterval> heap = new DefaultHeap<Integer, HoughInterval>(false);
    heap.addNode(new DefaultHeapNode<Integer, HoughInterval>(rootInterval.priority(), rootInterval));

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
      msg.append("\nXXXX dim " + dim);
      msg.append("\nXXXX database.size " + database.size());
      msg.append("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
      msg.append("\nrootInterval " + rootInterval);
      debugFine(msg.toString());
    }

    // get the ''best'' d-dimensional intervals at max level
    Set<Integer> noiseIDs = getDatabaseIDs(database);
    while (! heap.isEmpty()) {
      HoughInterval interval = determineNextIntervalAtMaxLevel(heap);
      if (debug) {
        debugFine("\nnext interval in dim " + dim + ": " + interval);
      }

      // only noise left
      if (interval == null) break;

      // do a dim-1 dimensional run
      Set<Integer> clusterIDs = new HashSet<Integer>();
      if (dim > 2) {
        Database<ParameterizationFunction> db = buildDB(dim, interval, database);
        SubspaceClusterMap clusterMap_dim_minus_1 = doRun(db, progress);

        // add result of dim-1 to this result
        for (Integer d : clusterMap_dim_minus_1.subspaceDimensionalities()) {
          List<Set<Integer>> clusters_d = clusterMap_dim_minus_1.getCluster(d);
          for (Set<Integer> clusters : clusters_d) {
            clusterMap.add(d, clusters);
            noiseIDs.removeAll(clusters);
            clusterIDs.addAll(clusters);
            processedIDs.addAll(clusters);
          }
        }
      }
      // dim = 2
      else {
        clusterMap.add(dim - 1, interval.getIDs());
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
        progress(new ProgressLogRecord(LogLevel.PROGRESS,
                                       Util.status(progress),
                                       progress.getTask(),
                                       progress.status()));
      }
    }

    // put noise to clusters
    if (! noiseIDs.isEmpty()) {
      if (dim == noiseDim) {
        clusterMap.addToNoise(noiseIDs);
      }
      else if (noiseIDs.size() >= minPts) {
        clusterMap.add(dim, noiseIDs);
      }
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nnoise fuer dim " + dim + ": " + noiseIDs.size());
      msg.append("\nclusters fuer dim= " + dim + ": " + clusterMap.subspaceDimensionalities());
      for (Integer id : clusterMap.subspaceDimensionalities()) {
        msg.append("         corrDim = " + id + ": " + clusterMap.getCluster(id).size() + " cluster(s)");
        msg.append(" [");
        for (Set<Integer> c : clusterMap.getCluster(id)) {
          msg.append(c.size() + " ");
        }
        msg.append("\nobjects]");
      }
      debugFine(msg.toString());
    }



    return clusterMap;
  }

  /**
   * Returns the root interval for the specified database.
   *
   * @param database the database storing the paramterization functions
   * @param dim      the dimensionality of the database
   * @return the root interval for the specified database
   */
  private HoughInterval buildRootInterval(Database<ParameterizationFunction> database, int dim) {
    // determine minimum and maximum function value of all functions
    double[] minMax = determineMinMax(database, dim);
    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("d_min " + minMax[0]);
      msg.append("d_max " + minMax[1]);
      debugFiner(msg.toString());
    }

    // build root
    double[] min = new double[dim];
    double[] max = new double[dim];
    Arrays.fill(max, Math.PI);
    min[0] = minMax[0];
    max[0] = minMax[1];
    HoughIntervalSplit split = new HoughIntervalSplit(database, minPts);
    HoughInterval rootInterval = new HoughInterval(min, max, split, getDatabaseIDs(database), 0, 0);

    return rootInterval;
  }

  /**
   * Builds a dim-1 dimensional database where the objects are projected into the specified subspace.
   *
   * @param dim      the dimensionality of the database
   * @param interval the interval describing the subspace
   * @param database the database storing the paramterization functions
   * @return a dim-1 dimensional database where the objects are projected into the specified subspace
   * @throws UnableToComplyException
   */
  private Database<ParameterizationFunction> buildDB(int dim, HoughInterval interval, Database<ParameterizationFunction> database) throws UnableToComplyException {
    // build objects and associations
    List<ObjectAndAssociations<ParameterizationFunction>> oaas = new ArrayList<ObjectAndAssociations<ParameterizationFunction>>(interval.getIDs().size());
    double[] alpha = interval.centroid(2, dim);
    Matrix b = determineBasis(alpha);

    for (Integer id : interval.getIDs()) {
      ParameterizationFunction f = project(b, database.get(id));

      Map<AssociationID, Object> associations = database.getAssociations(id);
      ObjectAndAssociations<ParameterizationFunction> oaa = new ObjectAndAssociations<ParameterizationFunction>(f, associations);
      oaas.add(oaa);
    }

    // insert into db
    Database<ParameterizationFunction> result = new SequentialDatabase<ParameterizationFunction>();
    result.insert(oaas);

    if (debug) {
      debugFine("\ndb fuer dim " + (dim - 1) + ": " + result.size());
    }

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
    Matrix m = new Matrix(new double[][]{f.getPointCoordinates()}).times(basis);
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
    Matrix b = n.completeToOrthonormalBasis();
    return b;
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
      if (heap.isEmpty()) return null;
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
  private HoughInterval doDetermineNextIntervalAtMaxLevel(Heap<Integer, HoughInterval> heap) {
    HoughInterval interval = heap.getMinNode().getValue();
    int dim = interval.getDimensionality();
    while (true) {
      if (debug) {
        debugFiner("\nsplit " + interval.toString());
      }
      interval.split();

      // max level is reached or noise
      if (! interval.hasChildren()) {
        if (interval.getLevel() >= maxLevel && interval.getMaxSplitDimension() == dim) {
          return interval;
        }
        else {
          return null;
        }
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
  private double[] determineMinMax(Database<ParameterizationFunction> database, int dimensionality) {
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

  private void runDerivator() {

  }

}
