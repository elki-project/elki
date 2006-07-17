package de.lmu.ifi.dbs.algorithm.clustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.algorithm.result.HierarchicalCluster;
import de.lmu.ifi.dbs.algorithm.result.HierarchicalClusters;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Algorithm for detecting supspace hierarchies.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSH extends AbstractAlgorithm<RealVector> {

 

  /**
   * The optics algorithm to determine the cluster order.
   */
  private OPTICS<RealVector, PreferenceVectorBasedCorrelationDistance> optics;

  /**
   * Holds the result;
   */
  private Result<RealVector> result;

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<RealVector> database) throws IllegalStateException {
    if (isVerbose()) {
    	verbose("\nRun OPTICS algorithm.");
//      logger.info("\nRun OPTICS algorithm.\n");
    }
    optics.run(database);

    if (isVerbose()) {
    	verbose("\n\nCompute Clusters.");
//      logger.info("\n\nCompute Clusters.\n");
    }
    computeClusters(database, (ClusterOrder<RealVector, PreferenceVectorBasedCorrelationDistance>) optics.getResult());
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<RealVector> getResult() {
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
    optics = new OPTICS<RealVector, PreferenceVectorBasedCorrelationDistance>();
    remainingParameters = optics.setParameters(opticsParameters.toArray(new String[opticsParameters.size()]));

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    settings.addAll(optics.getAttributeSettings());
    return settings;
  }

  /**
   * Computes the hierarchical clusters according to the cluster order.
   *
   * @param database     the database holding the objects
   * @param clusterOrder the cluster order
   */
  private void computeClusters(Database<RealVector> database, ClusterOrder<RealVector, PreferenceVectorBasedCorrelationDistance> clusterOrder) {
    int dimensionality = database.dimensionality();

    //noinspection unchecked
    PreferenceVectorBasedCorrelationDistanceFunction distanceFunction = (PreferenceVectorBasedCorrelationDistanceFunction) optics.getDistanceFunction();

    // extract clusters
    Map<BitSet, List<HierarchicalCluster>> clustersMap = extractClusters(database, distanceFunction, clusterOrder);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\nStep 1");
      for (List<HierarchicalCluster> clusterList : clustersMap.values()) {
        for (HierarchicalCluster c : clusterList) {
          msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
        }
      }
      verbose(msg.toString());
//      logger.info(msg.toString());
    }

    // check if there are clusters < minpts
    checkClusters(database, distanceFunction, clustersMap);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\n\nStep 2");
      for (List<HierarchicalCluster> clusterList : clustersMap.values()) {
        for (HierarchicalCluster c : clusterList) {
          msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
        }
      }
      verbose(msg.toString());
//      logger.info(msg.toString());
    }

    // actualize the levels and indices and sort the clusters
    List<HierarchicalCluster> clusters = sortClusters(clustersMap, dimensionality);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\n\nStep 3");
      for (HierarchicalCluster c : clusters) {
        msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
      }
      verbose(msg.toString());
//      logger.info(msg.toString());
    }

    // build the hierarchy
    buildHierarchy(database, distanceFunction, clusters, dimensionality);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\n\nStep 4");
      for (HierarchicalCluster c : clusters) {
        msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
      }
      verbose(msg.toString());
//      logger.info(msg.toString());
    }

    result = new HierarchicalClusters<RealVector, PreferenceVectorBasedCorrelationDistance>(clusters.get(clusters.size() - 1), clusterOrder, database);
  }

  /**
   * Extracts the clusters from the cluster order.
   *
   * @param database         the database storing the objects
   * @param distanceFunction the distance function
   * @param clusterOrder     the cluster order to extract the clusters from
   */
  private Map<BitSet, List<HierarchicalCluster>> extractClusters(Database<RealVector> database,
                                                                 PreferenceVectorBasedCorrelationDistanceFunction distanceFunction,
                                                                 ClusterOrder<RealVector, PreferenceVectorBasedCorrelationDistance> clusterOrder) {

    Progress progress = new Progress("Extract Clusters", database.size());
    int processed = 0;
    Map<BitSet, List<HierarchicalCluster>> clustersMap = new HashMap<BitSet, List<HierarchicalCluster>>();
    double epsilon = ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getEpsilon().getDoubleValue();
    for (Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();)
    {
      ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
      RealVector object = database.get(entry.getObjectID());
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
        RealVector c_centroid = Util.centroid(database, c.getIDs(), c.getPreferenceVector());
        PreferenceVectorBasedCorrelationDistance dist = distanceFunction.correlationDistance(object, c_centroid, preferenceVector, preferenceVector);
        if (dist.getCorrelationValue() == entry.getReachability().getCorrelationValue()) {
          double d = distanceFunction.weightedDistance(object, c_centroid, dist.getCommonPreferenceVector());
          if (d <= 2 * epsilon) {
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
        progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(progress), progress.getTask(), progress.status()));
      }
    }
    return clustersMap;
  }

  /**
   * Sets the levels and indices in the clusters and returns a sorted list of the clusters.
   *
   * @param clustersMap    the mapping of bits sets to clusters
   * @param dimensionality the dimensionality of the data
   * @return a sorted list of the clusters
   */
  private List<HierarchicalCluster> sortClusters(Map<BitSet, List<HierarchicalCluster>> clustersMap, int dimensionality) {
    // actualize the levels and indices
    int[] clustersInLevel = new int[dimensionality + 1];
    List<HierarchicalCluster> clusters = new ArrayList<HierarchicalCluster>();
    for (BitSet pv : clustersMap.keySet()) {
      int level = pv.cardinality();
      List<HierarchicalCluster> parallelClusters = clustersMap.get(pv);
      for (int i = 0; i < parallelClusters.size(); i++) {
        HierarchicalCluster c = parallelClusters.get(i);
        c.setLevel(level);
        c.setLevelIndex(clustersInLevel[level]++);
        if (parallelClusters.size() > 1) {
          c.setLabel("[" + Util.format(dimensionality, pv) + "_" + i + "]");
        }
        else {
          c.setLabel("[" + Util.format(dimensionality, pv) + "]");
        }
        clusters.add(c);
      }
    }
    Collections.sort(clusters);
    return clusters;
  }

  /**
   * Removes the clusters with size < minpts from the cluster map and adds them to their parents.
   *
   * @param database         the database storing the objects
   * @param distanceFunction the distance function
   * @param clustersMap      the map containing the clusters
   */
  private void checkClusters(Database<RealVector> database,
                             PreferenceVectorBasedCorrelationDistanceFunction distanceFunction,
                             Map<BitSet, List<HierarchicalCluster>> clustersMap) {

    // check if there are clusters < minpts
    // and add them to not assigned
    int minpts = ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getMinpts();
    List<HierarchicalCluster> notAssigned = new ArrayList<HierarchicalCluster>();
    Map<BitSet, List<HierarchicalCluster>> newClustersMap = new HashMap<BitSet, List<HierarchicalCluster>>();
    for (BitSet pv : clustersMap.keySet()) {
      List<HierarchicalCluster> parallelClusters = clustersMap.get(pv);
      List<HierarchicalCluster> newParallelClusters = new ArrayList<HierarchicalCluster>(parallelClusters.size());
      for (HierarchicalCluster c : parallelClusters) {
        if (c.getIDs().size() < minpts) {
          notAssigned.add(c);
        }
        else {
          newParallelClusters.add(c);
        }
      }
      newClustersMap.put(pv, newParallelClusters);
    }

    clustersMap.clear();
    clustersMap.putAll(newClustersMap);

    HierarchicalCluster noise = new HierarchicalCluster(new BitSet());
    for (HierarchicalCluster c : notAssigned) {
      HierarchicalCluster parent = findParent(database, distanceFunction, c, clustersMap);
      if (parent != null) {
        parent.addIDs(c.getIDs());
      }
      else {
        noise.addIDs(c.getIDs());
      }
    }
    if (! noise.getIDs().isEmpty()) {
      List<HierarchicalCluster> noiseList = new ArrayList<HierarchicalCluster>(1);
      noiseList.add(noise);
      clustersMap.put(noise.getPreferenceVector(), noiseList);
    }
  }

  /**
   * Returns the parent of the specified cluster
   *
   * @param database         the database storing the objects
   * @param distanceFunction the distance function
   * @param child            the child to search teh parent for
   * @param clustersMap      the map containing the clusters
   * @return the parent of the specified cluster
   */
  private HierarchicalCluster findParent(Database<RealVector> database,
                                         PreferenceVectorBasedCorrelationDistanceFunction distanceFunction,
                                         HierarchicalCluster child,
                                         Map<BitSet, List<HierarchicalCluster>> clustersMap) {
    RealVector child_centroid = Util.centroid(database, child.getIDs(), child.getPreferenceVector());
    double epsilon = ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getEpsilon().getDoubleValue();

    HierarchicalCluster result = null;
    int resultCardinality = -1;

    BitSet childPV = child.getPreferenceVector();
    int childCardinality = childPV.cardinality();
    for (BitSet parentPV : clustersMap.keySet()) {
      int parentCardinality = parentPV.cardinality();
      if (parentCardinality >= childCardinality) continue;
      if (resultCardinality != -1 && parentCardinality <= resultCardinality) continue;

      BitSet pv = (BitSet) childPV.clone();
      pv.and(parentPV);
//      System.out.println("pv " + pv);
//      System.out.println("parentPV " + parentPV);
//      System.out.println("pv.equals(parentPV) " + pv.equals(parentPV));
      if (pv.equals(parentPV)) {
        List<HierarchicalCluster> parentList = clustersMap.get(parentPV);
        for (HierarchicalCluster parent : parentList) {
          RealVector parent_centroid = Util.centroid(database, parent.getIDs(), parentPV);
          double d = distanceFunction.weightedDistance(child_centroid, parent_centroid, parentPV);
          if (d <= 2 * epsilon) {
            result = parent;
            resultCardinality = parentCardinality;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Builds the cluster hierarchy
   *
   * @param distanceFunction the distance function
   * @param clusters         the sorted list of clusters
   * @param dimensionality   the dimensionality of the data
   */
  private void buildHierarchy(Database<RealVector> database,
                              PreferenceVectorBasedCorrelationDistanceFunction distanceFunction,
                              List<HierarchicalCluster> clusters, int dimensionality) {

    double epsilon = ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getEpsilon().getDoubleValue();
    Map<HierarchicalCluster, Integer> parentLevels = new HashMap<HierarchicalCluster, Integer>();
    for (int i = 0; i < clusters.size(); i++) {
      HierarchicalCluster c_i = clusters.get(i);
      int subspaceDim_i = dimensionality - c_i.getLevel();
      RealVector ci_centroid = Util.centroid(database, c_i.getIDs(), c_i.getPreferenceVector());

      for (int j = i; j < clusters.size(); j++) {
        HierarchicalCluster c_j = clusters.get(j);
        int subspaceDim_j = dimensionality - c_j.getLevel();

        if (subspaceDim_i < subspaceDim_j) {
          RealVector cj_centroid = Util.centroid(database, c_j.getIDs(), c_j.getPreferenceVector());
          PreferenceVectorBasedCorrelationDistance distance = distanceFunction.correlationDistance(ci_centroid, cj_centroid, c_i.getPreferenceVector(), c_j.getPreferenceVector());
          double d = distanceFunction.weightedDistance(ci_centroid, cj_centroid, distance.getCommonPreferenceVector());
          if (c_j.getLevel() == 0 || distance.getCorrelationValue() <= subspaceDim_j) {
            if (c_j.getLevel() == 0 || d <= 2 * epsilon) {
              Integer parentLevel = parentLevels.get(c_i);
              if (parentLevel == null) {
                parentLevels.put(c_i, c_j.getLevel());
                c_j.addChild(c_i);
                c_i.addParent(c_j);
              }
              else if (parentLevel == c_j.getLevel()) {
                c_j.addChild(c_i);
                c_i.addParent(c_j);
              }
            }
          }
        }
      }
    }
  }

}
