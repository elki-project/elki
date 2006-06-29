package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalCluster;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalClusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Algorithm for detecting supspace hierarchies.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSH<O extends RealVector> extends AbstractAlgorithm<O> {

  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The optics algorithm to determine the cluster order.
   */
  private OPTICS<O, PreferenceVectorBasedCorrelationDistance> optics;

  /**
   * Holds the result;
   */
  private Result<O> result;

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    if (isVerbose()) {
      logger.info("\nRun OPTICS algorithm.\n");
    }
    optics.run(database);

    if (isVerbose()) {
      logger.info("\n\nCompute Clusters.\n");
    }
    computeClusters(database, (ClusterOrder<O, PreferenceVectorBasedCorrelationDistance>) optics.getResult());
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<O> getResult() {
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
    "DiSH",
    "Detecting Subsapace Clusters",
    "Algorithm to find hierarchical correlation clusters in subspaces.",
    "unpublished :-(");
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    List<String> opticsParameters = new ArrayList<String>();
    // distance function
    opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    opticsParameters.add(PreferenceVectorBasedCorrelationDistanceFunction.class.getName());
    // omit flag
    opticsParameters.add(OptionHandler.OPTION_PREFIX + PreferenceVectorBasedCorrelationDistanceFunction.OMIT_PREPROCESSING_F);
    // epsilon for OPTICS
    opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    opticsParameters.add(PreferenceVectorBasedCorrelationDistanceFunction.INFINITY_PATTERN);
    // preprocessor
    opticsParameters.add(OptionHandler.OPTION_PREFIX + PreferenceVectorBasedCorrelationDistanceFunction.PREPROCESSOR_CLASS_P);
    opticsParameters.add(DiSHPreprocessor.class.getName());
    // verbose
    if (isVerbose()) {
      opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.VERBOSE_F);
    }
    // time
    if (isTime()) {
      opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.TIME_F);
    }

    for (String parameter : remainingParameters) {
      opticsParameters.add(parameter);
    }
    optics = new OPTICS<O, PreferenceVectorBasedCorrelationDistance>();
    remainingParameters = optics.setParameters(opticsParameters.toArray(new String[opticsParameters.size()]));

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * This method goes through the result to find clusters and
   * assign each point to one of these clusters
   */
  private void computeClusters(Database<O> database, ClusterOrder<O, PreferenceVectorBasedCorrelationDistance> clusterOrder) {
    Progress progress = new Progress("Compute Clusters", database.size());
    int processed = 0;

    //noinspection unchecked
    PreferenceVectorBasedCorrelationDistanceFunction distanceFunction = (PreferenceVectorBasedCorrelationDistanceFunction) optics.getDistanceFunction();

    // compute clusters
    Map<BitSet, List<HierarchicalCluster>> clusterMap = new HashMap<BitSet, List<HierarchicalCluster>>();
    int dimensionality = database.dimensionality();
    int[] clustersInLevel = new int[dimensionality];
    for (Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();)
    {
      ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
      BitSet preferenceVector = entry.getReachability().getCommonPreferenceVector();

      // get the list of (parallel) clusters for the preference vector
      List<HierarchicalCluster> clusters = clusterMap.get(preferenceVector);
      if (clusters == null) {
        clusters = new ArrayList<HierarchicalCluster>();
        clusterMap.put(preferenceVector, clusters);
      }
      // look for the proper cluster
      HierarchicalCluster cluster = null;
      for (HierarchicalCluster c : clusters) {
        PreferenceVectorBasedCorrelationDistance dist = distanceFunction.distance(entry.getObjectID(), c.getIds().get(0));
        if (dist.getCorrelationValue() == entry.getReachability().getCorrelationValue()) {
          cluster = c;
          break;
        }
      }
      if (cluster == null) {
//        String label = Util.format(dimensionality, preferenceVector) + "_" + clusters.size();
        String label = "["+Util.format(dimensionality, preferenceVector)+"]";
        int level = preferenceVector.cardinality();
        cluster = new HierarchicalCluster(preferenceVector, label, level, clustersInLevel[level]++);
        clusters.add(cluster);
      }

      cluster.addID(entry.getObjectID());

      if (isVerbose()) {
        progress.setProcessed(++processed);
        logger.log(new ProgressLogRecord(Level.INFO, Util.status(progress), progress.getTask(), progress.status()));
      }
    }

    List<HierarchicalCluster> clusters = new ArrayList<HierarchicalCluster>();
    for (List<HierarchicalCluster> c : clusterMap.values()) {
      clusters.addAll(c);
    }
    Collections.sort(clusters);

    // build hierarchy
    //noinspection unchecked
    for (HierarchicalCluster c_i : clusters) {
      int corrDim_i = dimensionality - c_i.getLevel();

      for (HierarchicalCluster c_j : clusters) {
        int corrDim_j = dimensionality - c_j.getLevel();

        if (corrDim_i < corrDim_j) {
          PreferenceVectorBasedCorrelationDistance distance = distanceFunction.distance(c_i.getIds().get(0), c_j.getIds().get(0));
//          if (distance.getEuklideanValue() < alpha) {
          if (distance.getCorrelationValue() <= dimensionality - c_j.getLevel()) {
            c_j.addChild(c_i);
          }
        }
      }
    }

    result = new HierarchicalClusters<O, PreferenceVectorBasedCorrelationDistance>(clusters.get(0), clusterOrder, database);
  }

}
