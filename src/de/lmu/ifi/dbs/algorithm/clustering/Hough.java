package de.lmu.ifi.dbs.algorithm.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalAxesParallelCluster;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalAxesParallelClusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.HoughResult;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.tree.interval.IntervalTree;
import de.lmu.ifi.dbs.tree.interval.IntervalTreeSplit;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.*;
import java.util.Queue;

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
   * Option string for parameter epsilon.
   */
  public static final String EPSILON_P = DiSHPreprocessor.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static String EPSILON_D = DiSHPreprocessor.EPSILON_D;

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
   * The epsilon value for the DiSH algorithm.
   */
  private double epsilon;

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
    //TODO default parameter values??
    optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D,new GreaterConstraint(0) ));

    optionHandler.put(MAXLEVEL_P, new IntParameter(MAXLEVEL_P, MAXLEVEL_D, new GreaterConstraint(0) ));

    ArrayList<ParameterConstraint> epsConstraints = new ArrayList<ParameterConstraint>();
    epsConstraints.add(new GreaterEqual(0));
    epsConstraints.add(new LessEqualConstraint(1));
    DoubleParameter eps = new DoubleParameter(EPSILON_P, EPSILON_D,epsConstraints);
    eps.setDefaultValue(DiSHPreprocessor.DEFAULT_EPSILON.getDoubleValue());
    optionHandler.put(EPSILON_P, eps);
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
      this.database = database;
      if (debug) {
        debugFine("database.size " + database.size());
      }

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

      // split the tree until maxlevel is reached
      IntervalTree root = new IntervalTree(rootInterval, new BitSet(), getDatabaseIDs(), 0);
      Queue<IntervalTree> queue = new LinkedList<IntervalTree>();
      queue.offer(root);
      while (! queue.isEmpty()) {
        IntervalTree tree = queue.remove();
        System.out.println("");
        System.out.println("split " + tree);
        tree.performSplit(this);
        f_minima.clear();
        f_maxima.clear();
        for (int i = 0; i < tree.numChildren(); i++) {
          queue.add(tree.getChild(i));
        }
      }

      // get the leafs at maxlevel
      List<IntervalTree> leafs = new ArrayList<IntervalTree>();
      for (BreadthFirstEnumeration<IntervalTree> bfs = root.breadthFirstEnumeration(); bfs.hasMoreElements();) {
        IntervalTree next = bfs.nextElement();
        // leaf
        if (next.getLevel() == maxLevel && next.numChildren() == 0) {
          leafs.add(next);
        }
      }

      // run DisH
      Database<RealVector> intervalDB = buildDishDB(leafs);
      System.out.println("intervalDB " + intervalDB.size());
      for (Iterator<Integer> it = intervalDB.iterator(); it.hasNext();) {
        Integer id = it.next();
        System.out.println("" + id + " " + intervalDB.get(id));
      }
      DiSH diSH = new DiSH();
      diSH.setParameters(dishParameters());
      diSH.run(intervalDB);

      HierarchicalAxesParallelClusters<RealVector, PreferenceVectorBasedCorrelationDistance> dishClusters = (HierarchicalAxesParallelClusters<RealVector, PreferenceVectorBasedCorrelationDistance>) diSH.getResult();
      BreadthFirstEnumeration<HierarchicalAxesParallelCluster> cluster_bfs = dishClusters.breadthFirstEnumeration();
      HashMap<Integer, Set<Integer>> ccc = new HashMap<Integer, Set<Integer>>();
      int key = 1;
      while (cluster_bfs.hasMoreElements()) {
        HierarchicalAxesParallelCluster c = cluster_bfs.nextElement();
        System.out.println("\nc " + c.toString());
//        System.out.println("" + c.getIDs());
        Set<Integer> idsInCluster = new HashSet<Integer>();
        for (Integer id : c.getIDs()) {
          IntervalTree tree = (IntervalTree) intervalDB.getAssociation(AssociationID.INTERVAL_TREE, id);
          idsInCluster.addAll(tree.getIds());
//          System.out.println("" + intervalDB.get(id));
//          System.out.println("" + tree.getInterval());
//          System.out.println("tree "+tree);
        }
        ccc.put(key++, idsInCluster);
      }

      for (Integer cId : ccc.keySet()) {
        System.out.println("");
        System.out.println("cluster " + cId);
        List<Integer> ids = new ArrayList<Integer>(ccc.get(cId));
        Collections.sort(ids);
        System.out.println("" + ids);
      }


      result = new HoughResult(database, null);
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

    // epsilon
    if (optionHandler.isSet(EPSILON_P)) {
      String epsString = optionHandler.getOptionValue(EPSILON_P);
      try {
        epsilon = Double.parseDouble(epsString);
        if (epsilon < 0 || epsilon > 1) {
          throw new WrongParameterValueException(EPSILON_P, epsString, EPSILON_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(EPSILON_P, epsString, EPSILON_D, e);
      }
    }
    else {
      epsilon = DiSHPreprocessor.DEFAULT_EPSILON.getDoubleValue();
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
    mySettings.addSetting(EPSILON_P, Double.toString(epsilon));

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
    if (childLevel > maxLevel) return null;
    StringBuffer msg = new StringBuffer();

    HyperBoundingBox alphaInterval = alphaInterval(childInterval);
    List<Integer> childIDs = new ArrayList<Integer>(parentIDs.size());
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
      debugFiner(msg.toString());
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

  /**
   * Computes the d interval from the given interval.
   *
   * @param interval the interval
   * @return the d interval from the given interval
   */
  private HyperBoundingBox dInterval(HyperBoundingBox interval) {
    double[] d_min = new double[]{interval.getMin(1)};
    double[] d_max = new double[]{interval.getMax(1)};
    return new HyperBoundingBox(d_min, d_max);
  }

  /**
   * Returns the parameters for the DiSH algorithm.
   *
   * @return the parameters for the DiSH algorithm
   */
  private String[] dishParameters() {
    List<String> dishParameters = new ArrayList<String>();

    // minpts for OPTICS
    dishParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    dishParameters.add("1");

    // minpts for preprocessor
    dishParameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.MINPTS_P);
    dishParameters.add("1");

    // epsilon for preprocessor
    dishParameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.EPSILON_P);
    dishParameters.add(Double.toString(epsilon));

    // strategy for preprocessor
    dishParameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.STRATEGY_P);
    dishParameters.add(DiSHPreprocessor.Strategy.MAX_INTERSECTION.toString());
//    dishParameters.add(DiSHPreprocessor.Strategy.APRIORI.toString());

    return dishParameters.toArray(new String[dishParameters.size()]);
  }

  private Database<RealVector> buildDishDB(List<IntervalTree> leafs) throws NonNumericFeaturesException, UnableToComplyException {
    // build objects and associations
    List<ObjectAndAssociations<RealVector>> oaas = new ArrayList<ObjectAndAssociations<RealVector>>(leafs.size());
    for (IntervalTree tree : leafs) {
      HyperBoundingBox interval = tree.getInterval();
      double[] alphaInterval = alphaInterval(interval).centroid();
      double dValue = dInterval(interval).centroid()[0];
      int dim = tree.getInterval().getDimensionality();
      double[] values = new double[dim];
      for (int i = 0; i < dim; i++) {
        double alpha_i_minus_1 = i == 0 ? 0 : alphaInterval[i - 1];
        values[i] = dValue * Math.cos(alpha_i_minus_1) * sinusProduct(i,dim-1, alphaInterval);
      }
      RealVector v = new DoubleVector(values);

      System.out.println(""+Util.format(interval.centroid(), ", ", 8));
      Map<AssociationID, Object> associations = new HashMap<AssociationID, Object>();
      associations.put(AssociationID.INTERVAL_TREE, tree);

      ObjectAndAssociations<RealVector> oaa = new ObjectAndAssociations<RealVector>(v, associations);
      oaas.add(oaa);
    }

    // normalize
//    Normalization<RealVector> normalization = new AttributeWiseRealVectorNormalization();
//    List<ObjectAndAssociations<RealVector>> normalizedOaas = normalization.normalizeObjects(oaas);

    // insert into db
    SequentialDatabase<RealVector> intervalDB = new SequentialDatabase<RealVector>();
    intervalDB.insert(oaas);
    return intervalDB;
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

}
