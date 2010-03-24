package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique.CLIQUESubspace;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique.CLIQUEUnit;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p/>
 * Implementation of the CLIQUE algorithm, a grid-based algorithm to identify
 * dense clusters in subspaces of maximum dimensionality.
 * </p>
 * <p/>
 * The implementation consists of two steps: <br>
 * 1. Identification of subspaces that contain clusters <br>
 * 2. Identification of clusters
 * </p>
 * <p/>
 * The third step of the original algorithm (Generation of minimal description
 * for the clusters) is not (yet) implemented.
 * </p>
 * <p>
 * Reference: <br>
 * R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan:: Automatic Subspace
 * Clustering of High Dimensional Data for Data Mining Applications. <br>
 * In Proc. ACM SIGMOD Int. Conf. on Management of Data, Seattle, WA, 1998.
 * </p>
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("CLIQUE: Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications")
@Description("Grid-based algorithm to identify dense clusters in subspaces of maximum dimensionality.")
@Reference(authors = "R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan", title = "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications", booktitle = "Proc. SIGMOD Conference, Seattle, WA, 1998", url = "http://dx.doi.org/10.1145/276304.276314")
public class CLIQUE<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, Clustering<SubspaceModel<V>>> implements ClusteringAlgorithm<Clustering<SubspaceModel<V>>, V> {
  /**
   * OptionID for {@link #XSI_PARAM}
   */
  public static final OptionID XSI_ID = OptionID.getOrCreateOptionID("clique.xsi", "The number of intervals (units) in each dimension.");

  /**
   * Parameter to specify the number of intervals (units) in each dimension,
   * must be an integer greater than 0.
   * <p>
   * Key: {@code -clique.xsi}
   * </p>
   */
  private final IntParameter XSI_PARAM = new IntParameter(XSI_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #XSI_PARAM}.
   */
  private int xsi;

  /**
   * OptionID for {@link #TAU_PARAM}
   */
  public static final OptionID TAU_ID = OptionID.getOrCreateOptionID("clique.tau", "The density threshold for the selectivity of a unit, where the selectivity is" + "the fraction of total feature vectors contained in this unit.");

  /**
   * Parameter to specify the density threshold for the selectivity of a unit,
   * where the selectivity is the fraction of total feature vectors contained in
   * this unit, must be a double greater than 0 and less than 1.
   * <p>
   * Key: {@code -clique.tau}
   * </p>
   */
  private final DoubleParameter TAU_PARAM = new DoubleParameter(TAU_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.OPEN));

  /**
   * Holds the value of {@link #TAU_PARAM}.
   */
  private double tau;

  /**
   * OptionID for {@link #PRUNE_FLAG}
   */
  public static final OptionID PRUNE_ID = OptionID.getOrCreateOptionID("clique.prune", "Flag to indicate that only subspaces with large coverage " + "(i.e. the fraction of the database that is covered by the dense units) " + "are selected, the rest will be pruned.");

  /**
   * Flag to indicate that only subspaces with large coverage (i.e. the fraction
   * of the database that is covered by the dense units) are selected, the rest
   * will be pruned.
   * <p>
   * Key: {@code -clique.prune}
   * </p>
   */
  private final Flag PRUNE_FLAG = new Flag(PRUNE_ID);

  /**
   * Holds the value of {@link #PRUNE_FLAG}.
   */
  private boolean prune;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public CLIQUE(Parameterization config) {
    super(config);
    if(config.grab(XSI_PARAM)) {
      xsi = XSI_PARAM.getValue();
    }

    if(config.grab(TAU_PARAM)) {
      tau = TAU_PARAM.getValue();
    }
    if(config.grab(PRUNE_FLAG)) {
      prune = PRUNE_FLAG.getValue();
    }
    // logger.getWrappedLogger().setLevel(Level.FINE);
  }

  /**
   * Performs the CLIQUE algorithm on the given database.
   * 
   */
  @Override
  protected Clustering<SubspaceModel<V>> runInTime(Database<V> database) throws IllegalStateException {
    // 1. Identification of subspaces that contain clusters
    if(logger.isVerbose()) {
      logger.verbose("*** 1. Identification of subspaces that contain clusters ***");
    }
    SortedMap<Integer, List<CLIQUESubspace<V>>> dimensionToDenseSubspaces = new TreeMap<Integer, List<CLIQUESubspace<V>>>();
    List<CLIQUESubspace<V>> denseSubspaces = findOneDimensionalDenseSubspaces(database);
    dimensionToDenseSubspaces.put(0, denseSubspaces);
    if(logger.isVerbose()) {
      logger.verbose("    1-dimensional dense subspaces: " + denseSubspaces.size());
    }
    if(logger.isDebugging()) {
      for(CLIQUESubspace<V> s : denseSubspaces) {
        logger.debug(s.toString("      "));
      }
    }

    for(int k = 2; k <= database.dimensionality() && !denseSubspaces.isEmpty(); k++) {
      denseSubspaces = findDenseSubspaces(database, denseSubspaces);
      dimensionToDenseSubspaces.put(k - 1, denseSubspaces);
      if(logger.isVerbose()) {
        logger.verbose("    " + k + "-dimensional dense subspaces: " + denseSubspaces.size());
      }
      if(logger.isDebugging()) {
        for(CLIQUESubspace<V> s : denseSubspaces) {
          logger.debug(s.toString("      "));
        }
      }
    }

    // 2. Identification of clusters
    if(logger.isVerbose()) {
      logger.verbose("*** 2. Identification of clusters ***");
    }

    // List<Pair<Subspace<V>, Set<Integer>>> modelsAndClusters = new
    // ArrayList<Pair<Subspace<V>, Set<Integer>>>();
    Clustering<SubspaceModel<V>> result = new Clustering<SubspaceModel<V>>();
    for(Integer dim : dimensionToDenseSubspaces.keySet()) {
      List<CLIQUESubspace<V>> subspaces = dimensionToDenseSubspaces.get(dim);
      List<Pair<Subspace<V>, Set<Integer>>> modelsAndClusters = determineClusters(database, subspaces);

      if(logger.isVerbose()) {
        logger.verbose("    " + (dim + 1) + "-dimensional clusters: " + modelsAndClusters.size());
      }

      // build result
      Map<Subspace<V>, Integer> numClusters = new HashMap<Subspace<V>, Integer>();

      for(Pair<Subspace<V>, Set<Integer>> modelAndCluster : modelsAndClusters) {
        Integer num = numClusters.get(modelAndCluster.first);
        if(num == null) {
          num = 1;
        }
        else {
          num += 1;
        }
        numClusters.put(modelAndCluster.first, num);

        DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Set<Integer>>(modelAndCluster.second);
        Cluster<SubspaceModel<V>> newCluster = new Cluster<SubspaceModel<V>>(group);
        newCluster.setModel(new SubspaceModel<V>(modelAndCluster.first));
        newCluster.setName("subspace_" + modelAndCluster.first.dimensonsToString("-") + "_cluster_" + num);
        result.addCluster(newCluster);
      }

    }

    return result;
  }

  /**
   * Determines the clusters in the specified dense subspaces.
   * 
   * @param database the database to run the algorithm on
   * @param denseSubspaces the dense subspaces in reverse order by their
   *        coverage
   * @return the clusters in the specified dense subspaces and the corresponding
   *         cluster models
   */
  private List<Pair<Subspace<V>, Set<Integer>>> determineClusters(Database<V> database, List<CLIQUESubspace<V>> denseSubspaces) {
    List<Pair<Subspace<V>, Set<Integer>>> clusters = new ArrayList<Pair<Subspace<V>, Set<Integer>>>();

    for(CLIQUESubspace<V> subspace : denseSubspaces) {
      List<Pair<Subspace<V>, Set<Integer>>> clustersInSubspace = subspace.determineClusters(database);
      if(logger.isDebugging()) {
        logger.debugFine("Subspace " + subspace + " clusters " + clustersInSubspace.size());
      }
      clusters.addAll(clustersInSubspace);
    }
    return clusters;
  }

  /**
   * Determines the one dimensional dense subspaces and performs a pruning if
   * this option is chosen.
   * 
   * @param database the database to run the algorithm on
   * @return the one dimensional dense subspaces reverse ordered by their
   *         coverage
   */
  private List<CLIQUESubspace<V>> findOneDimensionalDenseSubspaces(Database<V> database) {
    List<CLIQUESubspace<V>> denseSubspaceCandidates = findOneDimensionalDenseSubspaceCandidates(database);

    if(prune) {
      return pruneDenseSubspaces(denseSubspaceCandidates);
    }

    return denseSubspaceCandidates;
  }

  /**
   * Determines the {@code k}-dimensional dense subspaces and performs a pruning
   * if this option is chosen.
   * 
   * @param database the database to run the algorithm on
   * @param denseSubspaces the {@code (k-1)}-dimensional dense subspaces
   * @return a list of the {@code k}-dimensional dense subspaces sorted in
   *         reverse order by their coverage
   */
  private List<CLIQUESubspace<V>> findDenseSubspaces(Database<V> database, List<CLIQUESubspace<V>> denseSubspaces) {
    List<CLIQUESubspace<V>> denseSubspaceCandidates = findDenseSubspaceCandidates(database, denseSubspaces);

    if(prune) {
      return pruneDenseSubspaces(denseSubspaceCandidates);
    }

    return denseSubspaceCandidates;
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
    for(int d = 0; d < dimensionality; d++) {
      maxima[d] = -Double.MAX_VALUE;
      minima[d] = Double.MAX_VALUE;
    }
    // update minima and maxima
    for(Iterator<Integer> it = database.iterator(); it.hasNext();) {
      V featureVector = database.get(it.next());
      updateMinMax(featureVector, minima, maxima);
    }
    for(int i = 0; i < maxima.length; i++) {
      maxima[i] += 0.0001;
    }

    // determine the unit length in each dimension
    double[] unit_lengths = new double[dimensionality];
    for(int d = 0; d < dimensionality; d++) {
      unit_lengths[d] = (maxima[d] - minima[d]) / xsi;
    }

    if(logger.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   minima: ").append(FormatUtil.format(minima, ", ", 2));
      msg.append("\n   maxima: ").append(FormatUtil.format(maxima, ", ", 2));
      msg.append("\n   unit lengths: ").append(FormatUtil.format(unit_lengths, ", ", 2));
      logger.debugFiner(msg.toString());
    }

    // determine the boundaries of the units
    double[][] unit_bounds = new double[xsi + 1][dimensionality];
    for(int x = 0; x <= xsi; x++) {
      for(int d = 0; d < dimensionality; d++) {
        if(x < xsi) {
          unit_bounds[x][d] = minima[d] + x * unit_lengths[d];
        }
        else {
          unit_bounds[x][d] = maxima[d];
        }
      }
    }
    if(logger.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   unit bounds ").append(new Matrix(unit_bounds).toString("   "));
      logger.debugFiner(msg.toString());
    }

    // build the 1 dimensional units
    List<CLIQUEUnit<V>> units = new ArrayList<CLIQUEUnit<V>>((xsi * dimensionality));
    for(int x = 0; x < xsi; x++) {
      for(int d = 0; d < dimensionality; d++) {
        units.add(new CLIQUEUnit<V>(new Interval(d, unit_bounds[x][d], unit_bounds[x + 1][d])));
      }
    }

    if(logger.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   total number of 1-dim units: ").append(units.size());
      logger.debugFiner(msg.toString());
    }

    return units;
  }

  /**
   * Updates the minima and maxima array according to the specified feature
   * vector.
   * 
   * @param featureVector the feature vector
   * @param minima the array of minima
   * @param maxima the array of maxima
   */
  private void updateMinMax(V featureVector, double[] minima, double[] maxima) {
    if(minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors differ in length.");
    }
    for(int d = 1; d <= featureVector.getDimensionality(); d++) {
      if((featureVector.doubleValue(d)) > maxima[d - 1]) {
        maxima[d - 1] = (featureVector.doubleValue(d));
      }
      if((featureVector.doubleValue(d)) < minima[d - 1]) {
        minima[d - 1] = (featureVector.doubleValue(d));
      }
    }
  }

  /**
   * Determines the one-dimensional dense subspace candidates by making a pass
   * over the database.
   * 
   * @param database the database to run the algorithm on
   * @return the one-dimensional dense subspace candidates reverse ordered by
   *         their coverage
   */
  private List<CLIQUESubspace<V>> findOneDimensionalDenseSubspaceCandidates(Database<V> database) {
    Collection<CLIQUEUnit<V>> units = initOneDimensionalUnits(database);
    Collection<CLIQUEUnit<V>> denseUnits = new ArrayList<CLIQUEUnit<V>>();
    Map<Integer, CLIQUESubspace<V>> denseSubspaces = new HashMap<Integer, CLIQUESubspace<V>>();

    // identify dense units
    double total = database.size();
    for(Iterator<Integer> it = database.iterator(); it.hasNext();) {
      V featureVector = database.get(it.next());
      for(CLIQUEUnit<V> unit : units) {
        unit.addFeatureVector(featureVector);
        // unit is a dense unit
        if(!it.hasNext() && unit.selectivity(total) >= tau) {
          denseUnits.add(unit);
          // add the dense unit to its subspace
          int dim = unit.getIntervals().iterator().next().getDimension();
          CLIQUESubspace<V> subspace_d = denseSubspaces.get(dim);
          if(subspace_d == null) {
            subspace_d = new CLIQUESubspace<V>(dim);
            denseSubspaces.put(dim, subspace_d);
          }
          subspace_d.addDenseUnit(unit);
        }
      }
    }

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   number of 1-dim dense units: ").append(denseUnits.size());
      msg.append("\n   number of 1-dim dense subspace candidates: ").append(denseSubspaces.size());
      logger.debugFine(msg.toString());
    }

    List<CLIQUESubspace<V>> subspaceCandidates = new ArrayList<CLIQUESubspace<V>>(denseSubspaces.values());
    Collections.sort(subspaceCandidates, new CLIQUESubspace.CoverageComparator());
    return subspaceCandidates;
  }

  /**
   * Determines the {@code k}-dimensional dense subspace candidates from the
   * specified {@code (k-1)}-dimensional dense subspaces.
   * 
   * @param database the database to run the algorithm on
   * @param denseSubspaces the {@code (k-1)}-dimensional dense subspaces
   * @return a list of the {@code k}-dimensional dense subspace candidates
   *         reverse ordered by their coverage
   */
  private List<CLIQUESubspace<V>> findDenseSubspaceCandidates(Database<V> database, List<CLIQUESubspace<V>> denseSubspaces) {
    // sort (k-1)-dimensional dense subspace according to their dimensions
    List<CLIQUESubspace<V>> denseSubspacesByDimensions = new ArrayList<CLIQUESubspace<V>>(denseSubspaces);
    Collections.sort(denseSubspacesByDimensions, new Subspace.DimensionComparator());

    // determine k-dimensional dense subspace candidates
    double all = database.size();
    List<CLIQUESubspace<V>> denseSubspaceCandidates = new ArrayList<CLIQUESubspace<V>>();

    while(!denseSubspacesByDimensions.isEmpty()) {
      CLIQUESubspace<V> s1 = denseSubspacesByDimensions.remove(0);
      for(CLIQUESubspace<V> s2 : denseSubspacesByDimensions) {
        CLIQUESubspace<V> s = s1.join(s2, all, tau);
        if(s != null) {
          denseSubspaceCandidates.add(s);
        }
      }
    }

    // sort reverse by coverage
    Collections.sort(denseSubspaceCandidates, new CLIQUESubspace.CoverageComparator());
    return denseSubspaceCandidates;
  }

  /**
   * Performs a MDL-based pruning of the specified dense subspaces as described
   * in the CLIQUE algorithm.
   * 
   * @param denseSubspaces the subspaces to be pruned sorted in reverse order by
   *        their coverage
   * @return the subspaces which are not pruned reverse ordered by their
   *         coverage
   */
  private List<CLIQUESubspace<V>> pruneDenseSubspaces(List<CLIQUESubspace<V>> denseSubspaces) {
    int[][] means = computeMeans(denseSubspaces);
    double[][] diffs = computeDiffs(denseSubspaces, means[0], means[1]);
    double[] codeLength = new double[denseSubspaces.size()];
    double minCL = Double.MAX_VALUE;
    int min_i = -1;

    for(int i = 0; i < denseSubspaces.size(); i++) {
      int mi = means[0][i];
      int mp = means[1][i];
      double log_mi = mi == 0 ? 0 : StrictMath.log(mi) / StrictMath.log(2);
      double log_mp = mp == 0 ? 0 : StrictMath.log(mp) / StrictMath.log(2);
      double diff_mi = diffs[0][i];
      double diff_mp = diffs[1][i];
      codeLength[i] = log_mi + diff_mi + log_mp + diff_mp;

      if(codeLength[i] <= minCL) {
        minCL = codeLength[i];
        min_i = i;
      }
    }

    return denseSubspaces.subList(0, min_i + 1);
  }

  /**
   * The specified sorted list of dense subspaces is divided into the selected
   * set I and the pruned set P. For each set the mean of the cover fractions is
   * computed.
   * 
   * @param denseSubspaces the dense subspaces in reverse order by their
   *        coverage
   * @return the mean of the cover fractions, the first value is the mean of the
   *         selected set I, the second value is the mean of the pruned set P.
   */
  private int[][] computeMeans(List<CLIQUESubspace<V>> denseSubspaces) {
    int n = denseSubspaces.size() - 1;

    int[] mi = new int[n + 1];
    int[] mp = new int[n + 1];

    double resultMI = 0;
    double resultMP = 0;

    for(int i = 0; i < denseSubspaces.size(); i++) {
      resultMI += denseSubspaces.get(i).getCoverage();
      resultMP += denseSubspaces.get(n - i).getCoverage();
      mi[i] = (int) Math.ceil(resultMI / (i + 1));
      if(i != n) {
        mp[n - 1 - i] = (int) Math.ceil(resultMP / (i + 1));
      }
    }

    int[][] result = new int[2][];
    result[0] = mi;
    result[1] = mp;

    return result;
  }

  /**
   * The specified sorted list of dense subspaces is divided into the selected
   * set I and the pruned set P. For each set the difference from the specified
   * mean values is computed.
   * 
   * @param denseSubspaces denseSubspaces the dense subspaces in reverse order
   *        by their coverage
   * @param mi the mean of the selected sets I
   * @param mp the mean of the pruned sets P
   * @return the difference from the specified mean values, the first value is
   *         the difference from the mean of the selected set I, the second
   *         value is the difference from the mean of the pruned set P.
   */
  private double[][] computeDiffs(List<CLIQUESubspace<V>> denseSubspaces, int[] mi, int[] mp) {
    int n = denseSubspaces.size() - 1;

    double[] diff_mi = new double[n + 1];
    double[] diff_mp = new double[n + 1];

    double resultMI = 0;
    double resultMP = 0;

    for(int i = 0; i < denseSubspaces.size(); i++) {
      double diffMI = Math.abs(denseSubspaces.get(i).getCoverage() - mi[i]);
      resultMI += diffMI == 0.0 ? 0 : StrictMath.log(diffMI) / StrictMath.log(2);
      double diffMP = (i != n) ? Math.abs(denseSubspaces.get(n - i).getCoverage() - mp[n - 1 - i]) : 0;
      resultMP += diffMP == 0.0 ? 0 : StrictMath.log(diffMP) / StrictMath.log(2);
      diff_mi[i] = resultMI;
      if(i != n) {
        diff_mp[n - 1 - i] = resultMP;
      }
    }
    double[][] result = new double[2][];
    result[0] = diff_mi;
    result[1] = diff_mp;

    return result;
  }
}
