package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.HierarchicalCluster;
import de.lmu.ifi.dbs.algorithm.result.*;
import de.lmu.ifi.dbs.algorithm.result.HierarchicalClusters;
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
   * Computes the hierarchical clusters according to the cluster order.
   *
   * @param database     the database holding the objects
   * @param clusterOrder the cluster order
   */
  private void computeClusters(Database<O> database, ClusterOrder<O, PreferenceVectorBasedCorrelationDistance> clusterOrder) {
    Progress progress = new Progress("Compute Clusters", database.size());
    int processed = 0;

    //noinspection unchecked
    PreferenceVectorBasedCorrelationDistanceFunction distanceFunction = (PreferenceVectorBasedCorrelationDistanceFunction) optics.getDistanceFunction();

    // compute clusters
    Map<BitSet, List<HierarchicalCluster>> clustersMap = new HashMap<BitSet, List<HierarchicalCluster>>();
    int dimensionality = database.dimensionality();
    for (Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();)
    {
      ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
      BitSet preferenceVector = entry.getReachability().getCommonPreferenceVector();

      // get the list of (parallel) clusters for the preference vector
      List<HierarchicalCluster> parallelClusters = clustersMap.get(preferenceVector);
      if (parallelClusters == null) {
        parallelClusters = new ArrayList<HierarchicalCluster>();
        clustersMap.put(preferenceVector, parallelClusters);
      }
      // look for the proper cluster
      HierarchicalCluster cluster = null;
      for (HierarchicalCluster c : parallelClusters) {
        PreferenceVectorBasedCorrelationDistance dist = distanceFunction.distance(entry.getObjectID(), c.getIds().get(0));
        if (dist.getCorrelationValue() == entry.getReachability().getCorrelationValue()) {
          double d = distanceFunction.weightedDistance(entry.getObjectID(), c.getIds().get(0), dist.getCommonPreferenceVector());
          if (d <= 2 * ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getEpsilon().getDoubleValue()) {
            cluster = c;
            break;
          }
        }
      }
      if (cluster == null) {
        cluster = new HierarchicalCluster(preferenceVector);
        parallelClusters.add(cluster);
      }
      cluster.addID(entry.getObjectID());

      if (isVerbose()) {
        progress.setProcessed(++processed);
        logger.log(new ProgressLogRecord(Level.INFO, Util.status(progress), progress.getTask(), progress.status()));
      }
    }

    System.out.println("Step 1");
    for (List<HierarchicalCluster> clusterList : clustersMap.values()) {
      for (HierarchicalCluster c : clusterList) {
        System.out.println(Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIds().size());
      }
    }

    // check if there are clusters < minpts
    // if so, add to noise
    int minpts = ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getMinpts();
    HierarchicalCluster noise;
    if (clustersMap.containsKey(new BitSet())) {
      noise = clustersMap.get(new BitSet()).get(0);
    }
    else {
      noise = new HierarchicalCluster(new BitSet());
    }
    for (Iterator<BitSet> it = clustersMap.keySet().iterator(); it.hasNext();) {
      BitSet pv = it.next();
      List<HierarchicalCluster> parallelClusters = clustersMap.get(pv);
      List<HierarchicalCluster> toRemove = new ArrayList<HierarchicalCluster>();
      for (int i = 0; i < parallelClusters.size(); i++) {
        HierarchicalCluster c = parallelClusters.get(i);
        if (c.getIds().size() < minpts) {
          toRemove.add(c);
          for (Integer id : c.getIds())
            noise.addID(id);
        }
      }
      for (HierarchicalCluster c: toRemove) {
        parallelClusters.remove(c);
      }
    }
    if (! noise.getIds().isEmpty() && ! clustersMap.containsKey(new BitSet())) {
      List<HierarchicalCluster> noiseList = new ArrayList<HierarchicalCluster>();
      noiseList.add(noise);
      clustersMap.put(noise.getPreferenceVector(), noiseList);
    }

    System.out.println("");
    System.out.println("Step 2");
    for (List<HierarchicalCluster> clusterList : clustersMap.values()) {
      for (HierarchicalCluster c : clusterList) {
        System.out.println(Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIds().size());
      }
    }

    // actualize the levels and indices
    int[] clustersInLevel = new int[dimensionality];
    List<HierarchicalCluster> clusters = new ArrayList<HierarchicalCluster>();
    for (Iterator<BitSet> it = clustersMap.keySet().iterator(); it.hasNext();) {
      BitSet pv = it.next();
      int level = pv.cardinality();
      List<HierarchicalCluster> parallelClusters = clustersMap.get(pv);
      for (int i = 0; i < parallelClusters.size(); i++) {
        HierarchicalCluster c = parallelClusters.get(i);
        c.setLevel(level);
        c.setLevelIndex(clustersInLevel[level]++);
        if (parallelClusters.size() > 1) {
          c.setLabel("["+Util.format(dimensionality, pv)+"_"+i+"]");
        }
        else {
          c.setLabel("["+Util.format(dimensionality, pv)+"]");
        }
        clusters.add(c);
      }
    }
    Collections.sort(clusters);


    System.out.println("");
    System.out.println("Step 3");
    for (List<HierarchicalCluster> clusterList : clustersMap.values()) {
      for (HierarchicalCluster c : clusterList) {
        System.out.println(c + " ids " + c.getIds().size() + " " + c.getLevel() + " " + c.getLevelIndex());
      }
    }

    // build the hierarchy
    Map<HierarchicalCluster, Integer> parentLevels = new HashMap<HierarchicalCluster, Integer>();
    for (
      int i = 0;
      i < clusters.size(); i++)

    {
      HierarchicalCluster c_i = clusters.get(i);
      int corrDim_i = dimensionality - c_i.getLevel();

      for (int j = i; j < clusters.size(); j++) {
        HierarchicalCluster c_j = clusters.get(j);
        int corrDim_j = dimensionality - c_j.getLevel();

        if (corrDim_i < corrDim_j) {
          PreferenceVectorBasedCorrelationDistance distance = distanceFunction.distance(c_i.getIds().get(0), c_j.getIds().get(0));
          if (distance.getCorrelationValue() <= dimensionality - c_j.getLevel()) {
            double d = distanceFunction.weightedDistance(c_i.getIds().get(0), c_j.getIds().get(0), distance.getCommonPreferenceVector());
            if (d <= 2 * ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getEpsilon().getDoubleValue()) {
              Integer parentLevel = parentLevels.get(c_i);
              if (parentLevel == null) {
                parentLevels.put(c_i, c_j.getLevel());
                c_j.addChild(c_i);
              }
              else if (parentLevel == c_j.getLevel()) {
                c_j.addChild(c_i);
              }
            }
          }
        }
      }
    }

    result = new HierarchicalClusters<O, PreferenceVectorBasedCorrelationDistance>(clusters.get(clusters.size() - 1), clusterOrder, database);
  }


}
