package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.CorrelationClusterMap;
import de.lmu.ifi.dbs.algorithm.result.clustering.HoughResult;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.tree.interval.IntervalTree;
import de.lmu.ifi.dbs.tree.interval.IntervalTreeSplit;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.util.*;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Hough extends AbstractAlgorithm<ParameterizationFunction> implements IntervalTreeSplit {
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
  private int minpts;

  /**
   * The maximum level for splitting the hypercube.
   */
  private int maxLevel;

  /**
   * Stores temporary function values, used for better split performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_minima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();

  /**
   * Stores temporary function values, used for better split performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_maxima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();

  /**
   * Holds the current database.
   */
  private Database<ParameterizationFunction> currentDatabase;

  /**
   * Holds the dimensionality for noise.
   */
  private int noiseDim;


  /**
   * Provides a new Hough algorithm.
   */
  public Hough() {
    super();
//    this.debug = true;
    //TODO default parameter values??
    optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
    optionHandler.put(MAXLEVEL_P, new IntParameter(MAXLEVEL_P, MAXLEVEL_D, new GreaterConstraint(0)));
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
      noiseDim = database.get(database.iterator().next()).getDimensionality();
      CorrelationClusterMap clusters = doRun(database);
      result = new HoughResult(database, clusters, noiseDim);
    }
    catch (UnableToComplyException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private CorrelationClusterMap doRun(Database<ParameterizationFunction> database) throws IllegalStateException, UnableToComplyException {
    currentDatabase = database;
    int dim = database.get(database.iterator().next()).getDimensionality();
    CorrelationClusterMap clusterMap = new CorrelationClusterMap(dim);

    StringBuffer msg = new StringBuffer();
    System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    System.out.println("XXXX dim " + dim);
    System.out.println("XXXX database.size " + database.size());
    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

    // init heap
    HyperBoundingBox rootInterval = buildRootInterval(database, dim);
    IntervalTree root = new IntervalTree(rootInterval, new BitSet(), getDatabaseIDs(database), 0);
    DefaultHeap<Integer, IntervalTree> heap = new DefaultHeap<Integer, IntervalTree>(false);
    heap.addNode(new DefaultHeapNode<Integer, IntervalTree>(root.getPriority(), root));

    // get the ''best'' d-dimensional intervals at max level
    Set<Integer> noiseIDs = getDatabaseIDs(database);
    while (! heap.isEmpty()) {
      currentDatabase = database;
      IntervalTree interval = determineNextIntervalAtMaxLevel(heap);

      // only noise left
      if (interval == null) break;

      System.out.println("next interval in dim " + dim + ": " + Format.format(interval.getInterval().centroid()));
      // do a dim-1 dimensional run
      Set<Integer> clusterIDs = new HashSet<Integer>();
      if (dim > 2) {
        Database<ParameterizationFunction> db = buildDB(dim, interval, database);
        System.out.println("db fuer dim " + (dim - 1) + ": " + db.size());
//        if (dim - 1 == 2) {
//          for (Iterator<Integer> it = db.iterator();it.hasNext();) {
//            System.out.println(Format.format(db.get(it.next()).getPointCoordinates()));
//          }
//        }
        CorrelationClusterMap clusterMap_dMinus1 = doRun(db);

        // add result of dim-1 to this result
        for (Integer d : clusterMap_dMinus1.correlationDimensionalities()) {
          List<Set<Integer>> clusters_d = clusterMap_dMinus1.getCluster(d);
          for (Set<Integer> clusters : clusters_d) {
            clusterMap.add(d, clusters);
            noiseIDs.removeAll(clusters);
            clusterIDs.addAll(clusters);
            Integer id = clusters.iterator().next();
            System.out.println("xxxxxx clusters " +
                               database.getAssociation(AssociationID.LABEL, id)+
                               " " + clusters.size() + " objects");
          }
        }
      }
      // dim = 2
      else {
        clusterMap.add(dim - 1, interval.getIds());
        noiseIDs.removeAll(interval.getIds());
        clusterIDs.addAll(interval.getIds());
      }

      // reorganize heap
      Vector<HeapNode<Integer, IntervalTree>> heapVector = heap.copy();
      heap.clear();
      for (HeapNode<Integer, IntervalTree> heapNode : heapVector) {
        IntervalTree tree = heapNode.getValue();
        tree.removeIDs(clusterIDs);
        if (tree.getIds().size() >= minpts) {
          heap.addNode(heapNode);
        }
      }
    }
    System.out.println("");
    System.out.println("noise fuer dim " + dim + ": " + noiseIDs.size());

    // put noise to clusters
    if (! noiseIDs.isEmpty()) {
      if (dim == noiseDim) {
        clusterMap.addToNoise(noiseIDs);
      }
      else if (noiseIDs.size() >= minpts) {
        clusterMap.add(dim, noiseIDs);
      }
    }

    System.out.println("clusters fuer dim= " + dim + ": " + clusterMap.correlationDimensionalities());
    for (Integer id : clusterMap.correlationDimensionalities()) {
      System.out.print("         corrDim = " + id + ": " + clusterMap.getCluster(id).size() + " cluster(s)");
      System.out.print(" [");
      for (Set<Integer> c : clusterMap.getCluster(id)) {
        System.out.print(c.size() + " ");
      }
      System.out.println("objects]");
    }

    if (debug) {
      debugFine(msg.toString());
    }

    return clusterMap;
  }

  private HyperBoundingBox buildRootInterval(Database<ParameterizationFunction> database, int dim) {
    StringBuffer msg = new StringBuffer();
    // determine minimum and maximum function value of all functions
    double[] minMax = determineMinMax(database, dim);
    if (debug) {
      msg.append("d_min " + minMax[0]);
      msg.append("d_max " + minMax[1]);
    }

    // build root
    double[] min = new double[dim];
    double[] max = new double[dim];
    Arrays.fill(max, Math.PI);
    min[0] = minMax[0];
    max[0] = minMax[1];
    HyperBoundingBox rootInterval = new HyperBoundingBox(min, max);
    if (debug) {
      debugFine("rootInterval " + rootInterval);
    }
    return rootInterval;
  }

  private Database<ParameterizationFunction> buildDB(int dim, IntervalTree interval, Database<ParameterizationFunction> database) throws UnableToComplyException {
    // build objects and associations
    List<ObjectAndAssociations<ParameterizationFunction>> oaas = new ArrayList<ObjectAndAssociations<ParameterizationFunction>>(interval.getIds().size());
    double[] alpha = interval.getInterval().centroid(2, dim);
    Matrix b = determineBasis(alpha);

    for (Integer id : interval.getIds()) {
      ParameterizationFunction f = transorm(b, database.get(id));

      Map<AssociationID, Object> associations = database.getAssociations(id);
      ObjectAndAssociations<ParameterizationFunction> oaa = new ObjectAndAssociations<ParameterizationFunction>(f, associations);
      oaas.add(oaa);
    }

    // insert into db
    Database<ParameterizationFunction> result = new SequentialDatabase<ParameterizationFunction>();
    result.insert(oaas);

    return result;
  }

  private ParameterizationFunction transorm(Matrix b, ParameterizationFunction f) {
    Matrix m = new Matrix(new double[][]{f.getPointCoordinates()}).times(b);
    ParameterizationFunction f_t = new ParameterizationFunction(m.getColumnPackedCopy());
    f_t.setID(f.getID());
    return f_t;
  }

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

  private IntervalTree determineNextIntervalAtMaxLevel(DefaultHeap<Integer, IntervalTree> heap) {
    IntervalTree next = doDetermineNextIntervalAtMaxLevel(heap);
    // noise path was chosen
    while (next == null) {
      if (heap.isEmpty()) return null;
      next = doDetermineNextIntervalAtMaxLevel(heap);
    }

//    Set<Integer> nextIds = next.getIds();

    // reorganize heap
//    Vector<HeapNode<Integer, IntervalTree>> heapVector = heap.copy();
//    heap.clear();
//    for (HeapNode<Integer, IntervalTree> heapNode : heapVector) {
//      IntervalTree tree = heapNode.getValue();
//      tree.removeIDs(nextIds);
//      if (tree.getIds().size() >= minpts) {
//        heap.addNode(heapNode);
//      }
//    }

    return next;
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
   * todo
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
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0) {
        throw new WrongParameterValueException(MINPTS_P, minptsString,
                                               MINPTS_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString,
                                             MINPTS_D, e);
    }

    // maxlevel
    String maxLevelString = optionHandler.getOptionValue(MAXLEVEL_P);
    try {
      maxLevel = Integer.parseInt(maxLevelString);
      if (maxLevel <= 0) {
        throw new WrongParameterValueException(MAXLEVEL_P, minptsString,
                                               MAXLEVEL_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MAXLEVEL_P, minptsString,
                                             MAXLEVEL_D, e);
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
    mySettings.addSetting(MINPTS_P, Integer.toString(minpts));
    mySettings.addSetting(MAXLEVEL_P, Integer.toString(maxLevel));

    return attributeSettings;
  }

  /**
   * Returns the list of ids which would be associated with the
   * subtree representing the specified interval. If the split criterion is not met,
   * null will be returned.
   *
   * @param parentIDs     the ids belonging to the parent subtree of the given interval
   * @param childInterval the interval represented by the new subtree
   * @return the list of ids which would be associated with the
   *         subtree representing the specified interval or null
   */
  public Set<Integer> split(Set<Integer> parentIDs, HyperBoundingBox childInterval, int childLevel) {
    if (childLevel > maxLevel) {
      return null;
    }
    StringBuffer msg = new StringBuffer();
    if (debug) {
      msg.append("\ninterval " + childInterval);
    }

    HyperBoundingBox alphaInterval = alphaInterval(childInterval);
    Set<Integer> childIDs = new HashSet<Integer>(parentIDs.size());
    double d_min = childInterval.getMin(1);
    double d_max = childInterval.getMax(1);

    Map<Integer, Double> minima = f_minima.get(alphaInterval);
    Map<Integer, Double> maxima = f_maxima.get(alphaInterval);

    if (minima == null || maxima == null) {
      minima = new HashMap<Integer, Double>();
      f_minima.put(alphaInterval, minima);
      maxima = new HashMap<Integer, Double>();
      f_maxima.put(alphaInterval, maxima);
      for (Integer id : parentIDs) {
        ParameterizationFunction f = currentDatabase.get(id);

        HyperBoundingBox minMax = f.determineAlphaMinMax(alphaInterval);
        double f_min = f.function(minMax.getMin());
        double f_max = f.function(minMax.getMax());
        minima.put(id, f_min);
        maxima.put(id, f_max);

        if (debug) {
          msg.append("\n\nf " + f);
          msg.append("\nf_min " + f_min);
          msg.append("\nf_max " + f_max);
          msg.append("\nd_min " + d_min);
          msg.append("\nd_max " + d_max);
          msg.append("\nf extremum minimum " + f.isExtremumMinimum());
          msg.append("\nglobal alpha_min " + Format.format(f.getGlobalAlphaExtremum()));
          msg.append("\nf(alpha_min) " + f.getGlobalExtremum());
          msg.append("\nlocal alpha_min " + Format.format(minMax.getMin()));
          msg.append("\nlocal alpha_max " + Format.format(minMax.getMax()));
        }

        if (f_min > f_max) {
          throw new IllegalArgumentException("Houston, we have a problem!");
        }


        if (f_min <= d_max && f_max >= d_min) {
          childIDs.add(id);
          if (debug) {
            msg.append("\nid " + id + " appended");
          }
        }

        else {
          if (debug) {
            msg.append("\nXXXXXXXXX id " + id + " NOT appended");
          }
        }
      }
    }
    else {
      for (Integer id : parentIDs) {
        double f_min = minima.get(id);
        double f_max = maxima.get(id);
        if (debug) {
          msg.append("\n\nf_min " + f_min);
          msg.append("\nf_max " + f_max);
          msg.append("\nd_min " + d_min);
          msg.append("\nd_max " + d_max);
        }
        if (f_min <= d_max && f_max >= d_min) {
          childIDs.add(id);
          if (debug) {
            msg.append("\nid " + id + " appended");
          }
        }

        else {
          if (debug) {
            msg.append("\nXXXXXXXXX id " + id + " NOT appended");
          }
        }
      }
    }
    if (debug) {
      msg.append("\nchildIds " + childIDs.size());
      debugFine(msg.toString());
    }
    if (childIDs.size() < minpts) {
      return null;
    }
    else {
      return childIDs;
    }
  }

  private Set<Integer> getDatabaseIDs(Database<ParameterizationFunction> database) {
    Set<Integer> result = new HashSet<Integer>(database.size());
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

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

  /**
   * Computes the alpha interval from the given interval.
   *
   * @param interval the interval
   * @return the alpha interval from the given interval
   */
  private HyperBoundingBox alphaInterval(HyperBoundingBox interval) {
    double[] alpha_min = new double[interval.getDimensionality() - 1];
    double[] alpha_max = new double[interval.getDimensionality() - 1];
    for (int d = 1; d < interval.getDimensionality(); d++) {
      alpha_min[d - 1] = interval.getMin(d + 1);
      alpha_max[d - 1] = interval.getMax(d + 1);
    }
    return new HyperBoundingBox(alpha_min, alpha_max);
  }

  private IntervalTree doDetermineNextIntervalAtMaxLevel(Heap<Integer, IntervalTree> heap) {
//    System.out.println("heap_XXX " + heap);
    IntervalTree node = heap.getMinNode().getValue();
    while (true) {
//      System.out.println("split " + node);
      node.performSplit(this);
      f_minima.clear();
      f_maxima.clear();

      // max level is reached or noise
      if (node.numChildren() == 0) {
        if (node.getLevel() >= maxLevel) {
          return node;
        }
        else {
          return null;
        }
      }

      IntervalTree bestNode = node.getChild(0);
      for (int i = 1; i < node.numChildren(); i++) {
        IntervalTree currentNode = node.getChild(i);
        if (bestNode.compareTo(currentNode) > 0) {
          heap.addNode(new DefaultHeapNode<Integer, IntervalTree>(bestNode.getPriority(), bestNode));
          bestNode = currentNode;
        }
        else {
          heap.addNode(new DefaultHeapNode<Integer, IntervalTree>(currentNode.getPriority(), currentNode));
        }
      }
      node = bestNode;
//      System.out.println("");
//      System.out.println("heap " + heap);
    }
  }
}
