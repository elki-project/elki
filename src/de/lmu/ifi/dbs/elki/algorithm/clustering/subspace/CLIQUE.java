package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique.CLIQUESubspace;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique.CLIQUEUnit;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
 * R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan: Automatic Subspace
 * Clustering of High Dimensional Data for Data Mining Applications. <br>
 * In Proc. ACM SIGMOD Int. Conf. on Management of Data, Seattle, WA, 1998.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has SubspaceModel
 * @apiviz.has CLIQUESubspace
 * @apiviz.uses CLIQUEUnit
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("CLIQUE: Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications")
@Description("Grid-based algorithm to identify dense clusters in subspaces of maximum dimensionality.")
@Reference(authors = "R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan", title = "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications", booktitle = "Proc. SIGMOD Conference, Seattle, WA, 1998", url = "http://dx.doi.org/10.1145/276304.276314")
public class CLIQUE<V extends NumberVector<?>> extends AbstractAlgorithm<Clustering<SubspaceModel<V>>> implements SubspaceClusteringAlgorithm<SubspaceModel<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CLIQUE.class);

  /**
   * Parameter to specify the number of intervals (units) in each dimension,
   * must be an integer greater than 0.
   * <p>
   * Key: {@code -clique.xsi}
   * </p>
   */
  public static final OptionID XSI_ID = OptionID.getOrCreateOptionID("clique.xsi", "The number of intervals (units) in each dimension.");

  /**
   * Parameter to specify the density threshold for the selectivity of a unit,
   * where the selectivity is the fraction of total feature vectors contained in
   * this unit, must be a double greater than 0 and less than 1.
   * <p>
   * Key: {@code -clique.tau}
   * </p>
   */
  public static final OptionID TAU_ID = OptionID.getOrCreateOptionID("clique.tau", "The density threshold for the selectivity of a unit, where the selectivity is" + "the fraction of total feature vectors contained in this unit.");

  /**
   * Flag to indicate that only subspaces with large coverage (i.e. the fraction
   * of the database that is covered by the dense units) are selected, the rest
   * will be pruned.
   * <p>
   * Key: {@code -clique.prune}
   * </p>
   */
  public static final OptionID PRUNE_ID = OptionID.getOrCreateOptionID("clique.prune", "Flag to indicate that only subspaces with large coverage " + "(i.e. the fraction of the database that is covered by the dense units) " + "are selected, the rest will be pruned.");

  /**
   * Holds the value of {@link #XSI_ID}.
   */
  private int xsi;

  /**
   * Holds the value of {@link #TAU_ID}.
   */
  private double tau;

  /**
   * Holds the value of {@link #PRUNE_ID}.
   */
  private boolean prune;

  /**
   * Constructor.
   * 
   * @param xsi Xsi value
   * @param tau Tau value
   * @param prune Prune flag
   */
  public CLIQUE(int xsi, double tau, boolean prune) {
    super();
    this.xsi = xsi;
    this.tau = tau;
    this.prune = prune;
  }

  /**
   * Performs the CLIQUE algorithm on the given database.
   * 
   * @param relation Data relation to process
   * @return Clustering result
   */
  public Clustering<SubspaceModel<V>> run(Relation<V> relation) {
    // 1. Identification of subspaces that contain clusters
    // TODO: use step logging.
    if(LOG.isVerbose()) {
      LOG.verbose("*** 1. Identification of subspaces that contain clusters ***");
    }
    SortedMap<Integer, List<CLIQUESubspace<V>>> dimensionToDenseSubspaces = new TreeMap<Integer, List<CLIQUESubspace<V>>>();
    List<CLIQUESubspace<V>> denseSubspaces = findOneDimensionalDenseSubspaces(relation);
    dimensionToDenseSubspaces.put(0, denseSubspaces);
    if(LOG.isVerbose()) {
      LOG.verbose("    1-dimensional dense subspaces: " + denseSubspaces.size());
    }
    if(LOG.isDebugging()) {
      for(CLIQUESubspace<V> s : denseSubspaces) {
        LOG.debug(s.toString("      "));
      }
    }

    int dimensionality = RelationUtil.dimensionality(relation);
    for(int k = 2; k <= dimensionality && !denseSubspaces.isEmpty(); k++) {
      denseSubspaces = findDenseSubspaces(relation, denseSubspaces);
      dimensionToDenseSubspaces.put(k - 1, denseSubspaces);
      if(LOG.isVerbose()) {
        LOG.verbose("    " + k + "-dimensional dense subspaces: " + denseSubspaces.size());
      }
      if(LOG.isDebugging()) {
        for(CLIQUESubspace<V> s : denseSubspaces) {
          LOG.debug(s.toString("      "));
        }
      }
    }

    // 2. Identification of clusters
    if(LOG.isVerbose()) {
      LOG.verbose("*** 2. Identification of clusters ***");
    }
    // build result
    int numClusters = 1;
    Clustering<SubspaceModel<V>> result = new Clustering<SubspaceModel<V>>("CLIQUE clustering", "clique-clustering");
    for(Integer dim : dimensionToDenseSubspaces.keySet()) {
      List<CLIQUESubspace<V>> subspaces = dimensionToDenseSubspaces.get(dim);
      List<Pair<Subspace, ModifiableDBIDs>> modelsAndClusters = determineClusters(subspaces);

      if(LOG.isVerbose()) {
        LOG.verbose("    " + (dim + 1) + "-dimensional clusters: " + modelsAndClusters.size());
      }

      for(Pair<Subspace, ModifiableDBIDs> modelAndCluster : modelsAndClusters) {
        Cluster<SubspaceModel<V>> newCluster = new Cluster<SubspaceModel<V>>(modelAndCluster.second);
        newCluster.setModel(new SubspaceModel<V>(modelAndCluster.first, Centroid.make(relation, modelAndCluster.second).toVector(relation)));
        newCluster.setName("cluster_" + numClusters++);
        result.addCluster(newCluster);
      }
    }

    return result;
  }

  /**
   * Determines the clusters in the specified dense subspaces.
   * 
   * @param denseSubspaces the dense subspaces in reverse order by their
   *        coverage
   * @return the clusters in the specified dense subspaces and the corresponding
   *         cluster models
   */
  private List<Pair<Subspace, ModifiableDBIDs>> determineClusters(List<CLIQUESubspace<V>> denseSubspaces) {
    List<Pair<Subspace, ModifiableDBIDs>> clusters = new ArrayList<Pair<Subspace, ModifiableDBIDs>>();

    for(CLIQUESubspace<V> subspace : denseSubspaces) {
      List<Pair<Subspace, ModifiableDBIDs>> clustersInSubspace = subspace.determineClusters();
      if(LOG.isDebugging()) {
        LOG.debugFine("Subspace " + subspace + " clusters " + clustersInSubspace.size());
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
  private List<CLIQUESubspace<V>> findOneDimensionalDenseSubspaces(Relation<V> database) {
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
  private List<CLIQUESubspace<V>> findDenseSubspaces(Relation<V> database, List<CLIQUESubspace<V>> denseSubspaces) {
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
  private Collection<CLIQUEUnit<V>> initOneDimensionalUnits(Relation<V> database) {
    int dimensionality = RelationUtil.dimensionality(database);
    // initialize minima and maxima
    double[] minima = new double[dimensionality];
    double[] maxima = new double[dimensionality];
    for(int d = 0; d < dimensionality; d++) {
      maxima[d] = -Double.MAX_VALUE;
      minima[d] = Double.MAX_VALUE;
    }
    // update minima and maxima
    for(DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      V featureVector = database.get(it);
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

    if(LOG.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   minima: ").append(FormatUtil.format(minima, ", ", 2));
      msg.append("\n   maxima: ").append(FormatUtil.format(maxima, ", ", 2));
      msg.append("\n   unit lengths: ").append(FormatUtil.format(unit_lengths, ", ", 2));
      LOG.debugFiner(msg.toString());
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
    if(LOG.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   unit bounds ").append(FormatUtil.format(new Matrix(unit_bounds), "   "));
      LOG.debugFiner(msg.toString());
    }

    // build the 1 dimensional units
    List<CLIQUEUnit<V>> units = new ArrayList<CLIQUEUnit<V>>((xsi * dimensionality));
    for(int x = 0; x < xsi; x++) {
      for(int d = 0; d < dimensionality; d++) {
        units.add(new CLIQUEUnit<V>(new Interval(d, unit_bounds[x][d], unit_bounds[x + 1][d])));
      }
    }

    if(LOG.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   total number of 1-dim units: ").append(units.size());
      LOG.debugFiner(msg.toString());
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
  private List<CLIQUESubspace<V>> findOneDimensionalDenseSubspaceCandidates(Relation<V> database) {
    Collection<CLIQUEUnit<V>> units = initOneDimensionalUnits(database);
    // identify dense units
    double total = database.size();
    for(DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      V featureVector = database.get(it);
      for(CLIQUEUnit<V> unit : units) {
        unit.addFeatureVector(it, featureVector);
      }
    }

    Collection<CLIQUEUnit<V>> denseUnits = new ArrayList<CLIQUEUnit<V>>();
    Map<Integer, CLIQUESubspace<V>> denseSubspaces = new HashMap<Integer, CLIQUESubspace<V>>();
    for(CLIQUEUnit<V> unit : units) {
      // unit is a dense unit
      if(unit.selectivity(total) >= tau) {
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

    if(LOG.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("   number of 1-dim dense units: ").append(denseUnits.size());
      msg.append("\n   number of 1-dim dense subspace candidates: ").append(denseSubspaces.size());
      LOG.debugFine(msg.toString());
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
  private List<CLIQUESubspace<V>> findDenseSubspaceCandidates(Relation<V> database, List<CLIQUESubspace<V>> denseSubspaces) {
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    protected int xsi;

    protected double tau;

    protected boolean prune;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter xsiP = new IntParameter(XSI_ID, new GreaterConstraint(0));
      if(config.grab(xsiP)) {
        xsi = xsiP.getValue();
      }

      DoubleParameter tauP = new DoubleParameter(TAU_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.OPEN));
      if(config.grab(tauP)) {
        tau = tauP.getValue();
      }

      Flag pruneF = new Flag(PRUNE_ID);
      if(config.grab(pruneF)) {
        prune = pruneF.getValue();
      }
    }

    @Override
    protected CLIQUE<V> makeInstance() {
      return new CLIQUE<V>(xsi, tau, prune);
    }
  }
}