package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.algorithm.result.clustering.CLIQUEModel;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Interval;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.*;


/**
 * Implementation of the CLIQUE algorithm, a grid-based algorithm to identify dense clusters
 * in subspaces of maximum dimensionality.<p>
 *
 * The implementation consists of two steps:br>
 * 1. Identification of subspaces that contain clusters<br>
 * 2. Identification of clusters<p>
 *
 * The third step of the original algorithm (Generation of minimal description
 * for the clusters) is not implemented. 
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUE<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V> {

  /**
   * Parameter xsi.
   */
  public static final String XSI_P = "xsi";

  /**
   * Description for parameter xsi.
   */
  public static final String XSI_D = "number of intervals (units) in each dimension";

  /**
   * Parameter tau.
   */
  public static final String TAU_P = "tau";

  /**
   * Description for parameter tau.
   */
  public static final String TAU_D = "density threshold for the selectivity of a unit, where the selectivity is" +
                                     "the fraction of total feature vectors contained in this unit";

  /**
   * Flag prune.
   */
  public static final String PRUNE_F = "prune";

  /**
   * Description for flag prune.
   */
  public static final String PRUNE_D = "flag indicating that only subspaces with large coverage " +
                                       "(i.e. the fraction of the database that is covered by the dense units) " +
                                       "are selected, the rest will be pruned, default: no pruning";


  /**
   * Number of units in each dimension.
   */
  private int xsi;

  /**
   * Threshold for the selectivity of a unit.
   */
  private double tau;

  /**
   * Flag indicating that subspaces with low coverage are pruned.
   */
  private boolean prune;

  /**
   * The result of the algorithm;
   */
  private Clusters<V> result;


  public CLIQUE() {
    super();
//    this.debug = true;

    //parameter xsi
    optionHandler.put(new IntParameter(XSI_P, XSI_D, new GreaterConstraint(0)));

    //parameter tau
    List<ParameterConstraint<Number>> tauConstraints = new ArrayList<ParameterConstraint<Number>>();
    tauConstraints.add(new GreaterConstraint(0));
    tauConstraints.add(new LessConstraint(1));
    optionHandler.put(new DoubleParameter(TAU_P, TAU_D, tauConstraints));

    //flag prune
    optionHandler.put(new Flag(PRUNE_F, PRUNE_D));
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public ClusteringResult<V> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("CLIQUE",
                           "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications",
                           "Grid-based algorithm to identify dense clusters in subspaces of maximum dimensionality. ",
                           "R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan: " +
                           "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications. " +
                           "In Proc. SIGMOD Conference, Seattle, WA, 1998.");
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized
   *                               properly (e.g. the setParameters(String[]) method has been failed
   *                               to be called).
   */
  protected void runInTime(Database<V> database) throws IllegalStateException {
    Map<CLIQUEModel<V>, Set<Integer>> modelsAndClusters = new HashMap<CLIQUEModel<V>, Set<Integer>>();

    // 1. Identification of subspaces that contain clusters
    if (isVerbose()) {
      verbose("*** 1. Identification of subspaces that contain clusters ***");
    }
    SortedMap<Integer, SortedSet<Subspace<V>>> dimensionToDenseSubspaces = new TreeMap<Integer, SortedSet<Subspace<V>>>();
    SortedSet<Subspace<V>> denseSubspaces = findOneDimensionalDenseSubspaces(database);
    dimensionToDenseSubspaces.put(0, denseSubspaces);
    if (isVerbose()) {
      verbose("    1-dimensional dense subspaces: " + denseSubspaces.size());
//      for (Subspace<V> denseSubspace : denseSubspaces) {
//        verbose("    Subspace \n" + denseSubspace.toString("      "));
//      }
    }

    for (int k = 2; k <= database.dimensionality() && !denseSubspaces.isEmpty(); k++) {
      denseSubspaces = findDenseSubspaces(database, denseSubspaces);
      dimensionToDenseSubspaces.put(k - 1, denseSubspaces);
      if (isVerbose()) {
        verbose("    " + k + "-dimensional dense subspaces: " + denseSubspaces.size());
//        for (Subspace<V> denseSubspace : denseSubspaces) {
//          verbose("      Subspace " + denseSubspace.toString("      "));
//        }
      }
    }

    // 2. Identification of clusters
    if (isVerbose()) {
      verbose("\n*** 2. Identification of clusters ***");
    }

    for (Integer dim : dimensionToDenseSubspaces.keySet()) {
      SortedSet<Subspace<V>> subspaces = dimensionToDenseSubspaces.get(dim);
      Map<CLIQUEModel<V>, Set<Integer>> modelsToClusters = determineClusters(database, subspaces);
      modelsAndClusters.putAll(modelsToClusters);

      if (isVerbose()) {
        verbose("    " + (dim + 1) + "-dimensionional clusters: " + modelsToClusters.size());
//        for (CLIQUEModel<V> model : modelsToClusters.keySet()) {
//          verbose("       " + model.getSubspace().getDimensions() + " ids " + modelsToClusters.get(model));
//        }
      }
    }

    Integer[][] clusters = new Integer[modelsAndClusters.size()][0];
    Iterator<Set<Integer>> valuesIt = modelsAndClusters.values().iterator();
    for (int i = 0; i < clusters.length; i++) {
      Set<Integer> ids = valuesIt.next();
      clusters[i] = ids.toArray(new Integer[ids.size()]);
    }

    result = new Clusters<V>(clusters, database);

    // append model
    Iterator<CLIQUEModel<V>> keysIt = modelsAndClusters.keySet().iterator();
    for (int i = 0; i < clusters.length; i++) {
      SimpleClassLabel label = new SimpleClassLabel();
      label.init(result.canonicalClusterLabel(i));
      result.appendModel(label, keysIt.next());
    }

  }

  /**
   * Sets the parameters xsi, tau and prune additionally to the parameters set
   * by the super-class' method.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    xsi = (Integer) optionHandler.getOptionValue(XSI_P);
    tau = (Double) optionHandler.getOptionValue(TAU_P);
    prune = optionHandler.isSet(PRUNE_F);

    return remainingParameters;
  }

  /**
   * Determines the clusters in the specified dense subspaces.
   *
   * @param database       the database to run the algorithm on
   * @param denseSubspaces the dense subspaces
   * @return the clusters in the specified dense subspaces and the corresponding
   *         cluster models
   */
  private Map<CLIQUEModel<V>, Set<Integer>> determineClusters(Database<V> database,
                                                              SortedSet<Subspace<V>> denseSubspaces) {
    Map<CLIQUEModel<V>, Set<Integer>> result = new HashMap<CLIQUEModel<V>, Set<Integer>>();

    for (Subspace<V> subspace : denseSubspaces) {
      Map<CLIQUEModel<V>, Set<Integer>> clusters = subspace.determineClusters(database);
      if (debug) {
        debugFine("Subspace " + subspace + " clusters " + clusters.size());
      }
      result.putAll(clusters);
    }
    return result;
  }

  /**
   * Determines the one dimensional dense subspaces and performs
   * a pruning if this option is chosen.
   *
   * @param database the database to run the algorithm on
   * @return the one dimensional dense subspaces
   */
  private SortedSet<Subspace<V>> findOneDimensionalDenseSubspaces(Database<V> database) {
    SortedSet<Subspace<V>> denseSubspaceCandidates = findOneDimensionalDenseSubspaceCandidates(database);

    if (prune)
      return pruneDenseSubspaces(denseSubspaceCandidates);

    return denseSubspaceCandidates;
  }

  /**
   * Determines the k>1 dimensional dense subspaces and performs
   * a pruning if this option is chosen.
   *
   * @param database       the database to run the algorithm on
   * @param denseSubspaces the (k-1)-dimensional dense subspaces
   * @return the k-dimensional dense subspaces
   */
  private SortedSet<Subspace<V>> findDenseSubspaces(Database<V> database, SortedSet<Subspace<V>> denseSubspaces) {
    SortedSet<Subspace<V>> denseSubspaceCandidates = findDenseSubspaceCandidates(database,
                                                                                 denseSubspaces);

    if (prune)
      return pruneDenseSubspaces(denseSubspaceCandidates);

    return denseSubspaceCandidates;
  }

  /**
   * Initializes and returns the one dimensional units.
   *
   * @param database the database to run the algorithm on
   * @return the created one dimensional units
   */
  private Collection<Unit<V>> initOneDimensionalUnits(Database<V> database) {
    int dimensionality = database.dimensionality();
    // initialize minima and maxima
    double[] minima = new double[dimensionality];
    double[] maxima = new double[dimensionality];
    for (int d = 0; d < dimensionality; d++) {
      maxima[d] = -Double.MAX_VALUE;
      minima[d] = Double.MAX_VALUE;
    }
    // update minima and maxima
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      V featureVector = database.get(it.next());
      updateMinMax(featureVector, minima, maxima);
    }
    for (int i = 0; i < maxima.length; i++) {
      maxima[i] += 0.0001;
    }

    // determine the unit length in each dimension
    double[] unit_lengths = new double[dimensionality];
    for (int d = 0; d < dimensionality; d++) {
      unit_lengths[d] = (maxima[d] - minima[d]) / xsi;
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   minima: ").append(Util.format(minima, ", ", 2));
      msg.append("\n   maxima: ").append(Util.format(maxima, ", ", 2));
      msg.append("\n   unit lengths: ").append(Util.format(unit_lengths, ", ", 2));
      debugFiner(msg.toString());
    }

    // determine the boundaries of the units
    double[][] unit_bounds = new double[xsi + 1][dimensionality];
    for (int x = 0; x <= xsi; x++) {
      for (int d = 0; d < dimensionality; d++) {
        if (x < xsi)
          unit_bounds[x][d] = minima[d] + x * unit_lengths[d];
        else
          unit_bounds[x][d] = maxima[d];
      }
    }
    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   unit bounds ").append(new Matrix(unit_bounds).toString("   "));
      debugFiner(msg.toString());
    }

    // build the 1 dimensional units
    List<Unit<V>> units = new ArrayList<Unit<V>>((xsi * dimensionality));
    for (int x = 0; x < xsi; x++) {
      for (int d = 0; d < dimensionality; d++) {
        units.add(new Unit<V>(new Interval(d, unit_bounds[x][d], unit_bounds[x + 1][d])));
      }
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   total number of 1-dim units: ").append(units.size());
      debugFiner(msg.toString());
    }

    return units;
  }

  /**
   * Updates the minima and maxima array according to the specified feature vector.
   *
   * @param featureVector the feature vector
   * @param minima        the array of minima
   * @param maxima        the array of maxima
   */
  private void updateMinMax(V featureVector, double[] minima, double[] maxima) {
    if (minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors differ in length.");
    }
    for (int d = 1; d <= featureVector.getDimensionality(); d++) {
      if ((featureVector.getValue(d).doubleValue()) > maxima[d - 1]) {
        maxima[d - 1] = (featureVector.getValue(d).doubleValue());
      }
      if ((featureVector.getValue(d).doubleValue()) < minima[d - 1]) {
        minima[d - 1] = (featureVector.getValue(d).doubleValue());
      }
    }
  }

  /**
   * Determines the one-dimensional dense subspace candidates by making a pass over the database.
   *
   * @param database the database to run the algorithm on
   * @return the one-dimensional dense subspace candidates
   */
  private SortedSet<Subspace<V>> findOneDimensionalDenseSubspaceCandidates(Database<V> database) {
    Collection<Unit<V>> units = initOneDimensionalUnits(database);
    Collection<Unit<V>> denseUnits = new ArrayList<Unit<V>>();
    Map<Integer, Subspace<V>> denseSubspaces = new HashMap<Integer, Subspace<V>>();

    // identify dense units
    double total = database.size();
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      V featureVector = database.get(it.next());
      for (Unit<V> unit : units) {
        unit.addFeatureVector(featureVector);
        // unit is a dense unit
        if (!it.hasNext() && unit.selectivity(total) >= tau) {
          denseUnits.add(unit);
          // add the dense unit to its subspace
          int dim = unit.getIntervals().iterator().next().getDimension();
          Subspace<V> subspace_d = denseSubspaces.get(dim);
          if (subspace_d == null) {
            subspace_d = new Subspace<V>(dim);
            denseSubspaces.put(dim, subspace_d);
          }
          subspace_d.addDenseUnit(unit);
        }
      }
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   number of 1-dim dense units: ").append(denseUnits.size());
      msg.append("\n   number of 1-dim dense subspace candidates: ").append(denseSubspaces.size());
      debugFine(msg.toString());
    }

    return new TreeSet<Subspace<V>>(denseSubspaces.values());
  }

  /**
   * Determines the k-dimensional dense subspace candidates.
   *
   * @param database       the database to run the algorithm on
   * @param denseSubspaces the (k-1)-dimensional dense subspace
   * @return the k-dimensional dense subspace candidates
   */
  private SortedSet<Subspace<V>> findDenseSubspaceCandidates(Database<V> database, Set<Subspace<V>> denseSubspaces) {
    List<Subspace<V>> denseSubspaceList = new ArrayList<Subspace<V>>();
    for (Subspace<V> s : denseSubspaces) {
      denseSubspaceList.add(s);
    }

    Comparator<Subspace<V>> comparator = new Comparator<Subspace<V>>() {
      public int compare(Subspace<V> s1, Subspace<V> s2) {
        SortedSet<Integer> dims1 = s1.getDimensions();
        SortedSet<Integer> dims2 = s2.getDimensions();

        if (dims1.size() != dims2.size()) {
          throw new IllegalArgumentException("different dimensions sizes!");
        }

        Iterator<Integer> it1 = dims1.iterator();
        Iterator<Integer> it2 = dims2.iterator();
        while (it1.hasNext()) {
          Integer d1 = it1.next();
          Integer d2 = it2.next();

          if (d1.equals(d2)) continue;
          return d1.compareTo(d2);
        }
        return 0;
      }
    };
    Collections.sort(denseSubspaceList, comparator);

    double all = database.size();
    TreeSet<Subspace<V>> subspaces = new TreeSet<Subspace<V>>();

    while (!denseSubspaceList.isEmpty()) {
      Subspace<V> s1 = denseSubspaceList.get(0);
      for (int j = 1; j < denseSubspaceList.size(); j++) {
        Subspace<V> s2 = denseSubspaceList.get(j);
        Subspace<V> s = s1.join(s2, all, tau);
        if (s != null) {
          subspaces.add(s);
        }
      }
      denseSubspaceList.remove(s1);
    }

    return subspaces;
  }

  /**
   * Performs a MDL-based pruning of the specified
   * dense sunspaces as described in the CLIQUE algorithm.
   *
   * @param denseSubspaces the subspaces to be pruned
   * @return the subspaces which are not pruned
   */
  private SortedSet<Subspace<V>> pruneDenseSubspaces(SortedSet<Subspace<V>> denseSubspaces) {
    int[][] means = computeMeans(denseSubspaces);
    double[][] diffs = computeDiffs(denseSubspaces, means[0], means[1]);
    double[] codeLength = new double[denseSubspaces.size()];
    double minCL = Double.MAX_VALUE;
    int min_i = -1;

    for (int i = 0; i < denseSubspaces.size(); i++) {
      int mi = means[0][i];
      int mp = means[1][i];
      double log_mi = mi == 0 ? 0 : Math.log(mi) / Math.log(2);
      double log_mp = mp == 0 ? 0 : Math.log(mp) / Math.log(2);
      double diff_mi = diffs[0][i];
      double diff_mp = diffs[1][i];
      codeLength[i] = log_mi + diff_mi + log_mp + diff_mp;

      if (codeLength[i] <= minCL) {
        minCL = codeLength[i];
        min_i = i;
      }
    }

    SortedSet<Subspace<V>> result = new TreeSet<Subspace<V>>();
    Iterator<Subspace<V>> it = denseSubspaces.iterator();
    for (int i = 0; i <= min_i; i++) {
      result.add(it.next());
    }
    return result;
  }

  /**
   * The specified sorted set of dense subspaces is divided into the selected set I
   * and the pruned set P. For each set the mean of the cover fractions
   * is computed.
   *
   * @param denseSubspaces the dense subspaces
   * @return the mean of the cover fractions, the first value is the mean of the
   *         selected set I, the second value is the mean of the pruned set P.
   */
  private int[][] computeMeans(SortedSet<Subspace<V>> denseSubspaces) {
    int n = denseSubspaces.size() - 1;
    Subspace[] subspaces = denseSubspaces.toArray(new Subspace[n + 1]);

    int[] mi = new int[n + 1];
    int[] mp = new int[n + 1];

    double resultMI = 0;
    double resultMP = 0;

    for (int i = 0; i < subspaces.length; i++) {
      resultMI += subspaces[i].getCoverage();
      resultMP += subspaces[n - i].getCoverage();
      mi[i] = (int) Math.ceil(resultMI / (i + 1));
      if (i != n) mp[n - 1 - i] = (int) Math.ceil(resultMP / (i + 1));
    }

    int[][] result = new int[2][];
    result[0] = mi;
    result[1] = mp;

    return result;
  }

  /**
   * The specified sorted set of dense subspaces is divided into the selected set I
   * and the pruned set P. For each set the difference from the specified mean values
   * is computed.
   *
   * @param denseSubspaces the dense subspaces
   * @param mi             the mean of the selected sets I
   * @param mp             the mean of the pruned sets P
   * @return the difference from the specified mean values, the first value is the
   *         difference from the mean of the selected set I,
   *         the second value is the difference from the mean of the pruned set P.
   */
  private double[][] computeDiffs(SortedSet<Subspace<V>> denseSubspaces, int[] mi, int[] mp) {
    int n = denseSubspaces.size() - 1;
    Subspace[] subspaces = denseSubspaces.toArray(new Subspace[n + 1]);

    double[] diff_mi = new double[n + 1];
    double[] diff_mp = new double[n + 1];

    double resultMI = 0;
    double resultMP = 0;

    for (int i = 0; i < subspaces.length; i++) {
      double diffMI = Math.abs(subspaces[i].getCoverage() - mi[i]);
      resultMI += diffMI == 0 ? 0 : Math.log(diffMI) / Math.log(2);
      double diffMP = (i != n) ? Math.abs(subspaces[n - i].getCoverage() - mp[n - 1 - i]) : 0;
      resultMP += diffMP == 0 ? 0 : Math.log(diffMP) / Math.log(2);
      diff_mi[i] = resultMI;
      if (i != n) diff_mp[n - 1 - i] = resultMP;
    }
    double[][] result = new double[2][];
    result[0] = diff_mi;
    result[1] = diff_mp;

    return result;
  }


}
