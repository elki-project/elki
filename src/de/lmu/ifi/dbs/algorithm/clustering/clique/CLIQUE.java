package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Description;
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
 * in subspaces of maximum dimensionality.
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
    this.debug = true;

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
   *                               // todo
   */
  protected void runInTime(Database<V> database) throws IllegalStateException {
    // 1. Identification of subspaces that contain clusters
    if (isVerbose()) {
      verbose("1. Identification of subspaces that contain clusters");
    }
    findOneDimensionalDenseSubspaceCandidates(database);

    // 2. Identification of clusters
    // 3. Generation of minimal decription for the clusters

    Integer[][] clusters = new Integer[0][0];
    result = new Clusters<V>(clusters, database);
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
   * Initializes and returns the one dimensional units.
   *
   * @param database the database to run the algorithm on
   * @return the created one dimensional units
   */
  private Collection<CLIQUEUnit<V>> initOneDimensionalUnits(Database<V> database) {
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
      debugFine(msg.toString());
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
      debugFine(msg.toString());
    }

    // build the 1 dimensional units
    List<CLIQUEUnit<V>> units = new ArrayList<CLIQUEUnit<V>>((xsi * dimensionality));
    for (int x = 0; x < xsi; x++) {
      for (int d = 0; d < dimensionality; d++) {
        units.add(new CLIQUEUnit<V>(new CLIQUEInterval(d, unit_bounds[x][d], unit_bounds[x + 1][d])));
      }
    }

    if (debug || isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   total number of 1-dim units: ").append(units.size());
      if (debug) debugFine(msg.toString());
      else verbose(msg.toString());
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
   * @return a collection of the one-dimensional dense subspace candidates
   */
  private Map<Integer, CLIQUESubspace<V>> findOneDimensionalDenseSubspaceCandidates(Database<V> database) {
    Collection<CLIQUEUnit<V>> units = initOneDimensionalUnits(database);
    Collection<CLIQUEUnit<V>> denseUnits = new ArrayList<CLIQUEUnit<V>>();
    Map<Integer, CLIQUESubspace<V>> denseSubspaces = new HashMap<Integer, CLIQUESubspace<V>>();

    // identify dense units
    double total = database.size();
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      V featureVector = database.get(it.next());
      for (CLIQUEUnit<V> unit : units) {
        unit.addFeatureVector(featureVector);
        // unit is a dense unit
        if (!it.hasNext() && unit.selectivity(total) >= tau) {
          denseUnits.add(unit);
          // add the dense unit its subspace
          int dim = unit.getIntervals().iterator().next().getDimension();
          CLIQUESubspace<V> subspace_d = denseSubspaces.get(dim);
          if (subspace_d == null) {
            subspace_d = new CLIQUESubspace<V>(dim);
            denseSubspaces.put(dim, subspace_d);
          }
          subspace_d.addDenseUnit(unit);
        }
      }
    }

    if (debug || isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   number of 1-dim dense units: ").append(denseUnits.size());
      msg.append("\n   number of 1-dim dense subspace candidates: ").append(denseSubspaces.size());
      if (debug) debugFine(msg.toString());
      else verbose(msg.toString());
    }

    return denseSubspaces;
  }

  /*
  private List pruneDenseSubspaces(Set<CLIQUESubspace<V>> denseSubspaces) {
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

    List result = new ArrayList();
    Iterator it = denseSubspaces.iterator();
    for (int i = 0; i <= min_i; i++) {
      result.add(it.next());
    }
    return result;
  }
  */

  /**
   * Computes the mean of the cover fractions of the specified dense subspaces.
   * @param subspaces the subspaces
   * @return
   */
  /*
  private int[][] computeMeans(List<CLIQUESubspace<V>> subspaces) {
    int n = denseSubspaces.size() - 1;
    CLIQUESubspace<V>[] subspaces = new CLIQUESubspace<V>[n + 1];
    subspaces = denseSubspaces.toArray(subspaces);

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
  */
}
