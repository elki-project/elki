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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ProxyDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DiSHDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.AbstractDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.DiSHPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyReferenceLists;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * Algorithm for detecting subspace hierarchies.
 * </p>
 * <p>
 * Reference: <br>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek:
 * Detection and Visualization of Subspace Cluster Hierarchies. <br>
 * In Proc. 12th International Conference on Database Systems for Advanced
 * Applications (DASFAA), Bangkok, Thailand, 2007.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses DiSHPreferenceVectorIndex
 * @apiviz.uses DiSHDistanceFunction
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("DiSH: Detecting Subspace cluster Hierarchies")
@Description("Algorithm to find hierarchical correlation clusters in subspaces.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", title = "Detection and Visualization of Subspace Cluster Hierarchies", booktitle = "Proc. 12th International Conference on Database Systems for Advanced Applications (DASFAA), Bangkok, Thailand, 2007", url = "http://dx.doi.org/10.1007/978-3-540-71703-4_15")
public class DiSH<V extends NumberVector<V, ?>> extends AbstractAlgorithm<Clustering<SubspaceModel<V>>> implements SubspaceClusteringAlgorithm<SubspaceModel<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DiSH.class);

  /**
   * Parameter that specifies the maximum radius of the neighborhood to be
   * considered in each dimension for determination of the preference vector,
   * must be a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.001}
   * </p>
   * <p>
   * Key: {@code -dish.epsilon}
   * </p>
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("dish.epsilon", "The maximum radius of the neighborhood " + "to be considered in each dimension for determination of " + "the preference vector.");

  /**
   * Parameter that specifies the a minimum number of points as a smoothing
   * factor to avoid the single-link-effect, must be an integer greater than 0.
   * <p>
   * Default value: {@code 1}
   * </p>
   * <p>
   * Key: {@code -dish.mu}
   * </p>
   */
  public static final OptionID MU_ID = OptionID.getOrCreateOptionID("dish.mu", "The minimum number of points as a smoothing factor to avoid the single-link-effekt.");

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private double epsilon;

  /**
   * The distance function we use
   */
  private DiSHDistanceFunction dishDistance;

  /**
   * Parameters that were given to OPTICS
   */
  private Collection<Pair<OptionID, Object>> opticsAlgorithmParameters;

  /**
   * Constructor.
   * 
   * @param epsilon Epsilon value
   * @param dishDistance Distance function
   * @param opticsAlgorithmParameters OPTICS parameters
   */
  public DiSH(double epsilon, DiSHDistanceFunction dishDistance, Collection<Pair<OptionID, Object>> opticsAlgorithmParameters) {
    super();
    this.epsilon = epsilon;
    this.dishDistance = dishDistance;
    this.opticsAlgorithmParameters = opticsAlgorithmParameters;
  }

  /**
   * Performs the DiSH algorithm on the given database.
   * 
   * @param database Database to process
   * @param relation Relation to process
   */
  public Clustering<SubspaceModel<V>> run(Database database, Relation<V> relation) {
    // Instantiate DiSH distance (and thus run the preprocessor)
    if(logger.isVerbose()) {
      logger.verbose("*** Run DiSH preprocessor.");
    }
    DiSHDistanceFunction.Instance<V> dishDistanceQuery = dishDistance.instantiate(relation);
    // Configure and run OPTICS.
    if(logger.isVerbose()) {
      logger.verbose("*** Run OPTICS algorithm.");
    }
    ListParameterization opticsconfig = new ListParameterization(opticsAlgorithmParameters);
    opticsconfig.addParameter(OPTICS.DISTANCE_FUNCTION_ID, ProxyDistanceFunction.proxy(dishDistanceQuery));

    Class<OPTICS<V, PreferenceVectorBasedCorrelationDistance>> cls = ClassGenericsUtil.uglyCastIntoSubclass(OPTICS.class);
    OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics = null;
    optics = opticsconfig.tryInstantiate(cls);
    ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> opticsResult = optics.run(database, relation);

    if(logger.isVerbose()) {
      logger.verbose("*** Compute Clusters.");
    }
    return computeClusters(relation, opticsResult, dishDistanceQuery);
  }

  /**
   * Computes the hierarchical clusters according to the cluster order.
   * 
   * @param database the database holding the objects
   * @param clusterOrder the cluster order
   * @param distFunc Distance function
   */
  private Clustering<SubspaceModel<V>> computeClusters(Relation<V> database, ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> clusterOrder, DiSHDistanceFunction.Instance<V> distFunc) {
    int dimensionality = DatabaseUtil.dimensionality(database);
    int minpts = dishDistance.getMinpts();

    // extract clusters
    Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> clustersMap = extractClusters(database, distFunc, clusterOrder);

    if(logger.isVerbose()) {
      StringBuffer msg = new StringBuffer("Step 1: extract clusters");
      for(List<Pair<BitSet, ArrayModifiableDBIDs>> clusterList : clustersMap.values()) {
        for(Pair<BitSet, ArrayModifiableDBIDs> c : clusterList) {
          msg.append("\n").append(FormatUtil.format(dimensionality, c.first)).append(" ids ").append(c.second.size());
        }
      }
      logger.verbose(msg.toString());
    }

    // check if there are clusters < minpts
    checkClusters(database, distFunc, clustersMap, minpts);
    if(logger.isVerbose()) {
      StringBuffer msg = new StringBuffer("Step 2: check clusters");
      for(List<Pair<BitSet, ArrayModifiableDBIDs>> clusterList : clustersMap.values()) {
        for(Pair<BitSet, ArrayModifiableDBIDs> c : clusterList) {
          msg.append("\n").append(FormatUtil.format(dimensionality, c.first)).append(" ids ").append(c.second.size());
        }
      }
      logger.verbose(msg.toString());
    }

    // sort the clusters
    List<Cluster<SubspaceModel<V>>> clusters = sortClusters(database, clustersMap);
    if(logger.isVerbose()) {
      StringBuffer msg = new StringBuffer("Step 3: sort clusters");
      for(Cluster<SubspaceModel<V>> c : clusters) {
        msg.append("\n").append(FormatUtil.format(dimensionality, c.getModel().getSubspace().getDimensions())).append(" ids ").append(c.size());
      }
      logger.verbose(msg.toString());
    }

    // build the hierarchy
    buildHierarchy(database, distFunc, clusters, dimensionality);
    if(logger.isVerbose()) {
      StringBuffer msg = new StringBuffer("Step 4: build hierarchy");
      for(Cluster<SubspaceModel<V>> c : clusters) {
        msg.append("\n").append(FormatUtil.format(dimensionality, c.getModel().getDimensions())).append(" ids ").append(c.size());
        for(Cluster<SubspaceModel<V>> cluster : c.getParents()) {
          msg.append("\n   parent ").append(cluster);
        }
        for(Cluster<SubspaceModel<V>> cluster : c.getChildren()) {
          msg.append("\n   child ").append(cluster);
        }
      }
      logger.verbose(msg.toString());
    }

    // build result
    Clustering<SubspaceModel<V>> result = new Clustering<SubspaceModel<V>>("DiSH clustering", "dish-clustering");
    for(Cluster<SubspaceModel<V>> c : clusters) {
      if(c.getParents() == null || c.getParents().isEmpty()) {
        result.addCluster(c);
      }
    }
    return result;
  }

  /**
   * Extracts the clusters from the cluster order.
   * 
   * @param database the database storing the objects
   * @param distFunc the distance function
   * @param clusterOrder the cluster order to extract the clusters from
   * @return the extracted clusters
   */
  private Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> extractClusters(Relation<V> database, DiSHDistanceFunction.Instance<V> distFunc, ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> clusterOrder) {
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Extract Clusters", database.size(), logger) : null;
    int processed = 0;
    Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> clustersMap = new HashMap<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>>();
    Map<DBID, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> entryMap = new HashMap<DBID, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>>();
    Map<DBID, Pair<BitSet, ArrayModifiableDBIDs>> entryToClusterMap = new HashMap<DBID, Pair<BitSet, ArrayModifiableDBIDs>>();
    for(Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();) {
      ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
      entryMap.put(entry.getID(), entry);

      V object = database.get(entry.getID());
      BitSet preferenceVector = entry.getReachability().getCommonPreferenceVector();

      // get the list of (parallel) clusters for the preference vector
      List<Pair<BitSet, ArrayModifiableDBIDs>> parallelClusters = clustersMap.get(preferenceVector);
      if(parallelClusters == null) {
        parallelClusters = new ArrayList<Pair<BitSet, ArrayModifiableDBIDs>>();
        clustersMap.put(preferenceVector, parallelClusters);
      }

      // look for the proper cluster
      Pair<BitSet, ArrayModifiableDBIDs> cluster = null;
      for(Pair<BitSet, ArrayModifiableDBIDs> c : parallelClusters) {
        V c_centroid = DatabaseUtil.centroid(database, c.second, c.first);
        PreferenceVectorBasedCorrelationDistance dist = distFunc.correlationDistance(object, c_centroid, preferenceVector, preferenceVector);
        if(dist.getCorrelationValue() == entry.getReachability().getCorrelationValue()) {
          double d = distFunc.weightedDistance(object, c_centroid, dist.getCommonPreferenceVector());
          if(d <= 2 * epsilon) {
            cluster = c;
            break;
          }
        }
      }
      if(cluster == null) {
        cluster = new Pair<BitSet, ArrayModifiableDBIDs>(preferenceVector, DBIDUtil.newArray());
        parallelClusters.add(cluster);
      }
      cluster.second.add(entry.getID());
      entryToClusterMap.put(entry.getID(), cluster);

      if(progress != null) {
        progress.setProcessed(++processed, logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    if(logger.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer("Step 0");
      for(List<Pair<BitSet, ArrayModifiableDBIDs>> clusterList : clustersMap.values()) {
        for(Pair<BitSet, ArrayModifiableDBIDs> c : clusterList) {
          msg.append("\n").append(FormatUtil.format(DatabaseUtil.dimensionality(database), c.first)).append(" ids ").append(c.second.size());
        }
      }
      logger.debugFiner(msg.toString());
    }

    // add the predecessor to the cluster
    for(BitSet pv : clustersMap.keySet()) {
      List<Pair<BitSet, ArrayModifiableDBIDs>> parallelClusters = clustersMap.get(pv);
      for(Pair<BitSet, ArrayModifiableDBIDs> cluster : parallelClusters) {
        if(cluster.second.isEmpty()) {
          continue;
        }
        DBID firstID = cluster.second.get(0);
        ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = entryMap.get(firstID);
        DBID predecessorID = entry.getPredecessorID();
        if(predecessorID == null) {
          continue;
        }
        ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> predecessor = entryMap.get(predecessorID);
        // parallel cluster
        if(predecessor.getReachability().getCommonPreferenceVector().equals(entry.getReachability().getCommonPreferenceVector())) {
          continue;
        }
        if(predecessor.getReachability().compareTo(entry.getReachability()) < 0) {
          continue;
        }

        Pair<BitSet, ArrayModifiableDBIDs> oldCluster = entryToClusterMap.get(predecessorID);
        oldCluster.second.remove(predecessorID);
        cluster.second.add(predecessorID);
        entryToClusterMap.remove(predecessorID);
        entryToClusterMap.put(predecessorID, cluster);
      }
    }

    return clustersMap;
  }

  /**
   * Returns a sorted list of the clusters w.r.t. the subspace dimensionality in
   * descending order.
   * 
   * @param database the database storing the objects
   * @param clustersMap the mapping of bits sets to clusters
   * @return a sorted list of the clusters
   */
  private List<Cluster<SubspaceModel<V>>> sortClusters(Relation<V> database, Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> clustersMap) {
    final int db_dim = DatabaseUtil.dimensionality(database);
    // int num = 1;
    List<Cluster<SubspaceModel<V>>> clusters = new ArrayList<Cluster<SubspaceModel<V>>>();
    for(BitSet pv : clustersMap.keySet()) {
      List<Pair<BitSet, ArrayModifiableDBIDs>> parallelClusters = clustersMap.get(pv);
      for(int i = 0; i < parallelClusters.size(); i++) {
        Pair<BitSet, ArrayModifiableDBIDs> c = parallelClusters.get(i);
        Cluster<SubspaceModel<V>> cluster = new Cluster<SubspaceModel<V>>(c.second);
        cluster.setModel(new SubspaceModel<V>(new Subspace<V>(c.first), DatabaseUtil.centroid(database, c.second)));
        cluster.setHierarchy(new HierarchyReferenceLists<Cluster<SubspaceModel<V>>>(cluster, new ArrayList<Cluster<SubspaceModel<V>>>(), new ArrayList<Cluster<SubspaceModel<V>>>()));
        // cluster.setName("Cluster_" + num++);
        String subspace = FormatUtil.format(cluster.getModel().getSubspace().getDimensions(), db_dim, "");
        if(parallelClusters.size() > 1) {
          cluster.setName("Cluster_" + subspace + "_" + i);
        }
        else {
          cluster.setName("Cluster_" + subspace);
        }
        clusters.add(cluster);
      }
    }
    // sort the clusters w.r.t. lambda
    Comparator<Cluster<SubspaceModel<V>>> comparator = new Comparator<Cluster<SubspaceModel<V>>>() {
      @Override
      public int compare(Cluster<SubspaceModel<V>> c1, Cluster<SubspaceModel<V>> c2) {
        return c2.getModel().getSubspace().dimensionality() - c1.getModel().getSubspace().dimensionality();
      }

    };
    Collections.sort(clusters, comparator);
    return clusters;
  }

  /**
   * Removes the clusters with size < minpts from the cluster map and adds them
   * to their parents.
   * 
   * @param database the database storing the objects
   * @param distFunc the distance function
   * @param clustersMap the map containing the clusters
   * @param minpts MinPts
   */
  private void checkClusters(Relation<V> database, DiSHDistanceFunction.Instance<V> distFunc, Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> clustersMap, int minpts) {
    // check if there are clusters < minpts
    // and add them to not assigned
    List<Pair<BitSet, ArrayModifiableDBIDs>> notAssigned = new ArrayList<Pair<BitSet, ArrayModifiableDBIDs>>();
    Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> newClustersMap = new HashMap<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>>();
    Pair<BitSet, ArrayModifiableDBIDs> noise = new Pair<BitSet, ArrayModifiableDBIDs>(new BitSet(), DBIDUtil.newArray());
    for(BitSet pv : clustersMap.keySet()) {
      // noise
      if(pv.cardinality() == 0) {
        List<Pair<BitSet, ArrayModifiableDBIDs>> parallelClusters = clustersMap.get(pv);
        for(Pair<BitSet, ArrayModifiableDBIDs> c : parallelClusters) {
          noise.second.addDBIDs(c.second);
        }
      }
      // clusters
      else {
        List<Pair<BitSet, ArrayModifiableDBIDs>> parallelClusters = clustersMap.get(pv);
        List<Pair<BitSet, ArrayModifiableDBIDs>> newParallelClusters = new ArrayList<Pair<BitSet, ArrayModifiableDBIDs>>(parallelClusters.size());
        for(Pair<BitSet, ArrayModifiableDBIDs> c : parallelClusters) {
          if(!pv.equals(new BitSet()) && c.second.size() < minpts) {
            notAssigned.add(c);
          }
          else {
            newParallelClusters.add(c);
          }
        }
        newClustersMap.put(pv, newParallelClusters);
      }
    }

    clustersMap.clear();
    clustersMap.putAll(newClustersMap);

    for(Pair<BitSet, ArrayModifiableDBIDs> c : notAssigned) {
      if(c.second.isEmpty()) {
        continue;
      }
      Pair<BitSet, ArrayModifiableDBIDs> parent = findParent(database, distFunc, c, clustersMap);
      if(parent != null) {
        parent.second.addDBIDs(c.second);
      }
      else {
        noise.second.addDBIDs(c.second);
      }
    }

    List<Pair<BitSet, ArrayModifiableDBIDs>> noiseList = new ArrayList<Pair<BitSet, ArrayModifiableDBIDs>>(1);
    noiseList.add(noise);
    clustersMap.put(noise.first, noiseList);
  }

  /**
   * Returns the parent of the specified cluster
   * 
   * @param database the database storing the objects
   * @param distFunc the distance function
   * @param child the child to search the parent for
   * @param clustersMap the map containing the clusters
   * @return the parent of the specified cluster
   */
  private Pair<BitSet, ArrayModifiableDBIDs> findParent(Relation<V> database, DiSHDistanceFunction.Instance<V> distFunc, Pair<BitSet, ArrayModifiableDBIDs> child, Map<BitSet, List<Pair<BitSet, ArrayModifiableDBIDs>>> clustersMap) {
    V child_centroid = DatabaseUtil.centroid(database, child.second, child.first);

    Pair<BitSet, ArrayModifiableDBIDs> result = null;
    int resultCardinality = -1;

    BitSet childPV = child.first;
    int childCardinality = childPV.cardinality();
    for(BitSet parentPV : clustersMap.keySet()) {
      int parentCardinality = parentPV.cardinality();
      if(parentCardinality >= childCardinality) {
        continue;
      }
      if(resultCardinality != -1 && parentCardinality <= resultCardinality) {
        continue;
      }

      BitSet pv = (BitSet) childPV.clone();
      pv.and(parentPV);
      if(pv.equals(parentPV)) {
        List<Pair<BitSet, ArrayModifiableDBIDs>> parentList = clustersMap.get(parentPV);
        for(Pair<BitSet, ArrayModifiableDBIDs> parent : parentList) {
          V parent_centroid = DatabaseUtil.centroid(database, parent.second, parentPV);
          double d = distFunc.weightedDistance(child_centroid, parent_centroid, parentPV);
          if(d <= 2 * epsilon) {
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
   * Builds the cluster hierarchy.
   * 
   * @param distFunc the distance function
   * @param clusters the sorted list of clusters
   * @param dimensionality the dimensionality of the data
   * @param database the database containing the data objects
   */
  private void buildHierarchy(Relation<V> database, DiSHDistanceFunction.Instance<V> distFunc, List<Cluster<SubspaceModel<V>>> clusters, int dimensionality) {
    StringBuffer msg = new StringBuffer();
    final int db_dim = DatabaseUtil.dimensionality(database);

    for(int i = 0; i < clusters.size() - 1; i++) {
      Cluster<SubspaceModel<V>> c_i = clusters.get(i);
      int subspaceDim_i = dimensionality - c_i.getModel().getSubspace().dimensionality();
      V ci_centroid = DatabaseUtil.centroid(database, c_i.getIDs(), c_i.getModel().getDimensions());

      for(int j = i + 1; j < clusters.size(); j++) {
        Cluster<SubspaceModel<V>> c_j = clusters.get(j);
        int subspaceDim_j = dimensionality - c_j.getModel().getSubspace().dimensionality();

        if(subspaceDim_i < subspaceDim_j) {
          if(logger.isDebugging()) {
            msg.append("\n l_i=").append(subspaceDim_i).append(" pv_i=[").append(FormatUtil.format(db_dim, c_i.getModel().getSubspace().getDimensions())).append("]");
            msg.append("\n l_j=").append(subspaceDim_j).append(" pv_j=[").append(FormatUtil.format(db_dim, c_j.getModel().getSubspace().getDimensions())).append("]");
          }

          // noise level reached
          if(c_j.getModel().getSubspace().dimensionality() == 0) {
            // no parents exists -> parent is noise
            if(c_i.getParents().isEmpty()) {
              c_j.getChildren().add(c_i);
              c_i.getParents().add(c_j);
              if(logger.isDebugging()) {
                msg.append("\n [").append(FormatUtil.format(db_dim, c_j.getModel().getSubspace().getDimensions()));
                msg.append("] is parent of [").append(FormatUtil.format(db_dim, c_i.getModel().getSubspace().getDimensions()));
                msg.append("]");
              }
            }
          }
          else {
            V cj_centroid = DatabaseUtil.centroid(database, c_j.getIDs(), c_j.getModel().getDimensions());
            PreferenceVectorBasedCorrelationDistance distance = distFunc.correlationDistance(ci_centroid, cj_centroid, c_i.getModel().getSubspace().getDimensions(), c_j.getModel().getSubspace().getDimensions());
            double d = distFunc.weightedDistance(ci_centroid, cj_centroid, distance.getCommonPreferenceVector());
            if(logger.isDebugging()) {
              msg.append("\n dist = ").append(distance.getCorrelationValue());
            }

            if(distance.getCorrelationValue() == subspaceDim_j) {
              if(logger.isDebugging()) {
                msg.append("\n d = ").append(d);
              }
              if(d <= 2 * epsilon) {
                // no parent exists or c_j is not a parent of the already
                // existing parents
                if(c_i.getParents().isEmpty() || !isParent(database, distFunc, c_j, c_i.getParents())) {
                  c_j.getChildren().add(c_i);
                  c_i.getParents().add(c_j);
                  if(logger.isDebugging()) {
                    msg.append("\n [").append(FormatUtil.format(db_dim, c_j.getModel().getSubspace().getDimensions()));
                    msg.append("] is parent of [");
                    msg.append(FormatUtil.format(db_dim, c_i.getModel().getSubspace().getDimensions()));
                    msg.append("]");
                  }
                }
              }
              else {
                throw new RuntimeException("Should never happen: d = " + d);
              }
            }
          }
        }
      }
    }
    if(logger.isDebugging()) {
      logger.debug(msg.toString());
    }
  }

  /**
   * Returns true, if the specified parent cluster is a parent of one child of
   * the children clusters.
   * 
   * @param database the database containing the objects
   * @param distFunc the distance function for distance computation between the
   *        clusters
   * @param parent the parent to be tested
   * @param children the list of children to be tested
   * @return true, if the specified parent cluster is a parent of one child of
   *         the children clusters, false otherwise
   */
  private boolean isParent(Relation<V> database, DiSHDistanceFunction.Instance<V> distFunc, Cluster<SubspaceModel<V>> parent, List<Cluster<SubspaceModel<V>>> children) {
    V parent_centroid = DatabaseUtil.centroid(database, parent.getIDs(), parent.getModel().getDimensions());
    int dimensionality = DatabaseUtil.dimensionality(database);
    int subspaceDim_parent = dimensionality - parent.getModel().getSubspace().dimensionality();

    for(Cluster<SubspaceModel<V>> child : children) {
      V child_centroid = DatabaseUtil.centroid(database, child.getIDs(), child.getModel().getDimensions());
      PreferenceVectorBasedCorrelationDistance distance = distFunc.correlationDistance(parent_centroid, child_centroid, parent.getModel().getSubspace().getDimensions(), child.getModel().getSubspace().getDimensions());
      if(distance.getCorrelationValue() == subspaceDim_parent) {
        return true;
      }
    }

    return false;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected double epsilon = 0.0;

    protected int mu = 1;

    protected DiSHDistanceFunction dishDistance;

    protected Collection<Pair<OptionID, Object>> opticsO;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID, new GreaterEqualConstraint(0), 0.001);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter muP = new IntParameter(MU_ID, new GreaterConstraint(0), 1);
      if(config.grab(muP)) {
        mu = muP.getValue();
      }

      configDiSHDistance(config, epsilon, mu);

      configOPTICS(config, mu, dishDistance);
    }

    public void configDiSHDistance(Parameterization config, double epsilon, int minpts) {
      ListParameterization dishParameters = new ListParameterization();
      dishParameters.addParameter(DiSHDistanceFunction.EPSILON_ID, epsilon);
      dishParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, DiSHPreferenceVectorIndex.Factory.class);
      dishParameters.addParameter(DiSHPreferenceVectorIndex.Factory.EPSILON_ID, Double.toString(epsilon));
      dishParameters.addParameter(DiSHPreferenceVectorIndex.Factory.MINPTS_ID, minpts);
      ChainedParameterization dishchain = new ChainedParameterization(dishParameters, config);
      dishchain.errorsTo(config);

      dishDistance = dishchain.tryInstantiate(DiSHDistanceFunction.class);
    }

    /**
     * Get the parameters for embedded OPTICS.
     * 
     * @param config Parameterization
     * @param minpts MinPts value
     * @param dishDistance DiSH distance function
     */
    public void configOPTICS(Parameterization config, final int minpts, final DiSHDistanceFunction dishDistance) {
      // Configure OPTICS. Tracked parameters
      ListParameterization opticsParameters = new ListParameterization();
      opticsParameters.addParameter(OPTICS.EPSILON_ID, AbstractDistance.INFINITY_PATTERN);
      opticsParameters.addParameter(OPTICS.MINPTS_ID, minpts);
      // Configure OPTICS. Untracked parameters
      ListParameterization opticsUntrackedParameters = new ListParameterization();
      opticsUntrackedParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, dishDistance);
      ChainedParameterization optchain = new ChainedParameterization(opticsParameters, config);
      TrackParameters trackpar = new TrackParameters(optchain);

      ChainedParameterization optchain2 = new ChainedParameterization(opticsUntrackedParameters, trackpar);
      optchain2.errorsTo(config);

      // Instantiate OPTICS for parameterization
      optchain2.tryInstantiate(OPTICS.class);
      // store parameters
      opticsO = trackpar.getGivenParameters();
    }

    @Override
    protected DiSH<V> makeInstance() {
      return new DiSH<V>(epsilon, dishDistance, opticsO);
    }
  }
}