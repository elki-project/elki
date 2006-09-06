package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.HoughResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.tree.interval.IntervalTree;
import de.lmu.ifi.dbs.tree.interval.IntervalTreeSplit;
import de.lmu.ifi.dbs.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

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
   * The result.
   */
  private HoughResult result;

  /**
   * Minimum points.
   */
  private int minpts;

  private int maxLevel = 30;

  /**
   * Holds the database on which the algorithm is running;
   */
  private Database<ParameterizationFunction> database;

  /**
   * Stores temporary function values, used for better split performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_minima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();

  /**
   * Stores temporary function values, used for better split performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_maxima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();

  /**
   * Provides a new Hough algorithm.
   */
  public Hough() {
    super();
    optionHandler.put(MINPTS_P, new Parameter(MINPTS_P, MINPTS_D, Parameter.Types.INT));
//    this.debug = true;
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
    System.out.println("database.size " + database.size());

    // determine minimum and maximum function value of all functions
    int dim = database.get(database.iterator().next()).getDimensionality();
    double[] minMax = determineMinMax(dim);
    double d_min = minMax[0];
    double d_max = minMax[1];

    System.out.println("d_min " + d_min);
    System.out.println("d_max " + d_max);

    // build root
    double[] min = new double[dim];
    double[] max = new double[dim];
    Arrays.fill(max, Math.PI);
    min[0] = d_min;
    max[0] = d_max;
    HyperBoundingBox rootInterval = new HyperBoundingBox(min, max);
    System.out.println("rootInterval " + rootInterval);
    IntervalTree root = new IntervalTree(rootInterval, new BitSet(), getDatabaseIDs(), 0);


    Queue<IntervalTree> queue = new LinkedList<IntervalTree>();
    queue.offer(root);

    while (! queue.isEmpty()) {
      IntervalTree tree = queue.remove();
      System.out.println("split " + tree);
      tree.performSplit(this);
      f_minima.clear();
      f_maxima.clear();
      for (int i = 0; i < tree.numChildren(); i++) {
        queue.add(tree.getChild(i));
      }
    }

    BreadthFirstEnumeration<IntervalTree> bfs = root.breadthFirstEnumeration();
    while (bfs.hasMoreElements()) {
      System.out.println("\n"+bfs.nextElement());
    }


//    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
//      Integer id = it.next();
//      ParameterizationFunction f = database.get(id);
//      if (id == 1) System.out.println("dim " + f.getDimensionality());
//    }


    result = new HoughResult(database);
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
  public List<Integer> split(List<Integer> parentIDs, HyperBoundingBox childInterval, int childLevel) {
    if (childLevel >= maxLevel) return null;
    StringBuffer msg = new StringBuffer();

    List<Integer> childIDs = new ArrayList<Integer>(parentIDs.size());
    double[] alpha_min = new double[childInterval.getDimensionality() - 1];
    double[] alpha_max = new double[childInterval.getDimensionality() - 1];
    for (int d = 1; d < childInterval.getDimensionality(); d++) {
      alpha_min[d - 1] = childInterval.getMin(d + 1);
      alpha_max[d - 1] = childInterval.getMax(d + 1);
    }
    HyperBoundingBox alphaInterval = new HyperBoundingBox(alpha_min, alpha_max);
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
        ParameterizationFunction f = database.get(id);
        HyperBoundingBox minMax = f.determineAlphaMinMax(alphaInterval);
        double f_min = f.function(minMax.getMin());
        double f_max = f.function(minMax.getMax());
        minima.put(id, f_min);
        maxima.put(id, f_max);

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
      debugFine(msg.toString());
    }
    if (childIDs.isEmpty() || childIDs.size() < minpts) return null;
    else return childIDs;
  }

  private List<Integer> getDatabaseIDs() {
    List<Integer> result = new ArrayList<Integer>(database.size());
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

  private double[] determineMinMax(int dimensionality) {
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

//      System.out.println("f_min " + f_min);
//      System.out.println("f_max " + f_max);
//      System.out.println("f_ext " + f.getExtremum());
//      System.out.println("alpha_min " + Util.format(minMax.getMin()));
//      System.out.println("alpha_max " + Util.format(minMax.getMax()));
//      System.out.println("alpha_ext " + Util.format(f.getAlphaExtremum()));
//      System.out.println("min? "+f.isExtremumMinimum());
//      System.out.println("");

      d_min = Math.min(d_min, f_min);
      d_max = Math.max(d_max, f_max);
    }
    return new double[]{d_min, d_max};
  }
}
