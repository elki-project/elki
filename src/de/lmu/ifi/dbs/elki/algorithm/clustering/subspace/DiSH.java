package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.procedure.TObjectObjectProcedure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.CorrelationClusterOrderEntry;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.GeneralizedOPTICS;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.DiSHPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectedCentroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
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
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("DiSH: Detecting Subspace cluster Hierarchies")
@Description("Algorithm to find hierarchical correlation clusters in subspaces.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", title = "Detection and Visualization of Subspace Cluster Hierarchies", booktitle = "Proc. 12th International Conference on Database Systems for Advanced Applications (DASFAA), Bangkok, Thailand, 2007", url = "http://dx.doi.org/10.1007/978-3-540-71703-4_15")
public class DiSH<V extends NumberVector> extends AbstractAlgorithm<Clustering<SubspaceModel>> implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DiSH.class);

  /**
   * Holds the value of {@link Parameterizer#EPSILON_ID}.
   */
  private double epsilon;

  /**
   * The DiSH preprocessor.
   */
  private DiSHPreferenceVectorIndex.Factory<V> dishPreprocessor;

  /**
   * OPTICS minPts parameter.
   */
  private int mu;

  /**
   * Constructor.
   * 
   * @param epsilon Epsilon value
   * @param mu Mu parameter (minPts)
   * @param dishPreprocessor DiSH preprocessor
   */
  public DiSH(double epsilon, int mu, DiSHPreferenceVectorIndex.Factory<V> dishPreprocessor) {
    super();
    this.epsilon = epsilon;
    this.mu = mu;
    this.dishPreprocessor = dishPreprocessor;
  }

  /**
   * Performs the DiSH algorithm on the given database.
   * 
   * @param relation Relation to process
   */
  public Clustering<SubspaceModel> run(Relation<V> relation) {
    if(LOG.isVerbose()) {
      LOG.verbose("Running the DiSH preprocessor.");
    }
    DiSHPreferenceVectorIndex<V> indexinst = dishPreprocessor.instantiate(relation);
    if(LOG.isVerbose()) {
      LOG.verbose("Running the OPTICS algorithm.");
    }

    ClusterOrderResult<DiSHClusterOrderEntry> opticsResult = new DiSHOPTICS(indexinst).run(relation);

    if(LOG.isVerbose()) {
      LOG.verbose("Compute Clusters.");
    }
    return computeClusters(relation, opticsResult);
  }

  /**
   * Computes the hierarchical clusters according to the cluster order.
   * 
   * @param database the database holding the objects
   * @param clusterOrder the cluster order
   */
  private Clustering<SubspaceModel> computeClusters(Relation<V> database, ClusterOrderResult<DiSHClusterOrderEntry> clusterOrder) {
    final int dimensionality = RelationUtil.dimensionality(database);

    // extract clusters
    TCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap = extractClusters(database, clusterOrder);

    if(LOG.isVerbose()) {
      final StringBuilder msg = new StringBuilder("Step 1: extract clusters\n");
      clustersMap.forEachEntry(new TObjectObjectProcedure<long[], List<ArrayModifiableDBIDs>>() {
        @Override
        public boolean execute(long[] key, List<ArrayModifiableDBIDs> clusters) {
          msg.append(BitsUtil.toStringLow(key, dimensionality)).append(" sizes:");
          for(ArrayModifiableDBIDs c : clusters) {
            msg.append(' ').append(c.size());
          }
          msg.append('\n');
          return true; // continue
        }
      });
      LOG.verbose(msg.toString());
    }

    // check if there are clusters < minpts
    checkClusters(database, clustersMap);
    if(LOG.isVerbose()) {
      final StringBuilder msg = new StringBuilder("Step 2: check clusters\n");
      clustersMap.forEachEntry(new TObjectObjectProcedure<long[], List<ArrayModifiableDBIDs>>() {
        @Override
        public boolean execute(long[] key, List<ArrayModifiableDBIDs> clusters) {
          msg.append(BitsUtil.toStringLow(key, dimensionality)).append(" sizes:");
          for(ArrayModifiableDBIDs c : clusters) {
            msg.append(' ').append(c.size());
          }
          msg.append('\n');
          return true; // continue
        }
      });
      LOG.verbose(msg.toString());
    }

    // sort the clusters
    List<Cluster<SubspaceModel>> clusters = sortClusters(database, clustersMap);
    if(LOG.isVerbose()) {
      StringBuilder msg = new StringBuilder("Step 3: sort clusters");
      for(Cluster<SubspaceModel> c : clusters) {
        msg.append('\n').append(BitsUtil.toStringLow(c.getModel().getSubspace().getDimensions(), dimensionality)).append(" ids ").append(c.size());
      }
      LOG.verbose(msg.toString());
    }

    // build the hierarchy
    Clustering<SubspaceModel> clustering = new Clustering<>("DiSH clustering", "dish-clustering");
    buildHierarchy(database, clustering, clusters, dimensionality);
    if(LOG.isVerbose()) {
      StringBuilder msg = new StringBuilder("Step 4: build hierarchy");
      for(Cluster<SubspaceModel> c : clusters) {
        msg.append('\n').append(BitsUtil.toStringLow(c.getModel().getSubspace().getDimensions(), dimensionality)).append(" ids ").append(c.size());
        for(Iter<Cluster<SubspaceModel>> iter = clustering.getClusterHierarchy().iterParents(c); iter.valid(); iter.advance()) {
          msg.append("\n   parent ").append(iter.get());
        }
        for(Iter<Cluster<SubspaceModel>> iter = clustering.getClusterHierarchy().iterChildren(c); iter.valid(); iter.advance()) {
          msg.append("\n   child ").append(iter.get());
        }
      }
      LOG.verbose(msg.toString());
    }

    // build result
    for(Cluster<SubspaceModel> c : clusters) {
      if(clustering.getClusterHierarchy().numParents(c) == 0) {
        clustering.addToplevelCluster(c);
      }
    }
    return clustering;
  }

  /**
   * Extracts the clusters from the cluster order.
   * 
   * @param database the database storing the objects
   * @param clusterOrder the cluster order to extract the clusters from
   * @return the extracted clusters
   */
  private TCustomHashMap<long[], List<ArrayModifiableDBIDs>> extractClusters(Relation<V> database, ClusterOrderResult<DiSHClusterOrderEntry> clusterOrder) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extract Clusters", database.size(), LOG) : null;
    int processed = 0;
    TCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap = new TCustomHashMap<>(BitsUtil.TROVE_HASH_STRATEGY);
    // Note clusterOrder currently contains DBID objects anyway.
    Map<DBID, DiSHClusterOrderEntry> entryMap = new HashMap<>();
    Map<DBID, Pair<long[], ArrayModifiableDBIDs>> entryToClusterMap = new HashMap<>();
    for(Iterator<DiSHClusterOrderEntry> it = clusterOrder.iterator(); it.hasNext();) {
      DiSHClusterOrderEntry entry = it.next();
      entryMap.put(entry.getID(), entry);

      V object = database.get(entry.getID());
      long[] preferenceVector = entry.getCommonPreferenceVector();

      // get the list of (parallel) clusters for the preference vector
      List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(preferenceVector);
      if(parallelClusters == null) {
        parallelClusters = new ArrayList<>();
        clustersMap.put(preferenceVector, parallelClusters);
      }

      // look for the proper cluster
      ArrayModifiableDBIDs cluster = null;
      for(ArrayModifiableDBIDs c : parallelClusters) {
        Vector c_centroid = ProjectedCentroid.make(preferenceVector, database, c);
        long[] commonPreferenceVector = BitsUtil.andCMin(preferenceVector, preferenceVector);
        int subspaceDim = subspaceDimensionality(object, c_centroid, preferenceVector, preferenceVector, commonPreferenceVector);
        if(subspaceDim == entry.getCorrelationValue()) {
          double d = weightedDistance(object, c_centroid, commonPreferenceVector);
          if(d <= 2 * epsilon) {
            cluster = c;
            break;
          }
        }
      }
      if(cluster == null) {
        cluster = DBIDUtil.newArray();
        parallelClusters.add(cluster);
      }
      cluster.add(entry.getID());
      entryToClusterMap.put(entry.getID(), new Pair<>(preferenceVector, cluster));

      if(progress != null) {
        progress.setProcessed(++processed, LOG);
      }
    }
    LOG.ensureCompleted(progress);

    if(LOG.isDebuggingFiner()) {
      int dim = RelationUtil.dimensionality(database);
      StringBuilder msg = new StringBuilder("Step 0");
      for(Map.Entry<long[], List<ArrayModifiableDBIDs>> clusterList : clustersMap.entrySet()) {
        for(ArrayModifiableDBIDs c : clusterList.getValue()) {
          msg.append('\n').append(BitsUtil.toStringLow(clusterList.getKey(), dim)).append(" ids ").append(c.size());
        }
      }
      LOG.debugFiner(msg.toString());
    }

    // add the predecessor to the cluster
    for(long[] pv : clustersMap.keySet()) {
      List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(pv);
      for(ArrayModifiableDBIDs cluster : parallelClusters) {
        if(cluster.isEmpty()) {
          continue;
        }
        DBID firstID = cluster.get(0);
        DiSHClusterOrderEntry entry = entryMap.get(firstID);
        DBID predecessorID = entry.getPredecessorID();
        if(predecessorID == null) {
          continue;
        }
        DiSHClusterOrderEntry predecessor = entryMap.get(predecessorID);
        // parallel cluster
        if(BitsUtil.equal(predecessor.getCommonPreferenceVector(), entry.getCommonPreferenceVector())) {
          continue;
        }
        if(predecessor.compareTo(entry) < 0) {
          continue;
        }

        Pair<long[], ArrayModifiableDBIDs> oldCluster = entryToClusterMap.get(predecessorID);
        oldCluster.second.remove(predecessorID);
        cluster.add(predecessorID);
        entryToClusterMap.remove(predecessorID);
        entryToClusterMap.put(predecessorID, new Pair<>(pv, cluster));
      }
    }

    return clustersMap;
  }

  /**
   * Returns a sorted list of the clusters w.r.t. the subspace dimensionality in
   * descending order.
   * 
   * @param relation the database storing the objects
   * @param clustersMap the mapping of bits sets to clusters
   * @return a sorted list of the clusters
   */
  private List<Cluster<SubspaceModel>> sortClusters(Relation<V> relation, TCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    final int db_dim = RelationUtil.dimensionality(relation);
    // int num = 1;
    List<Cluster<SubspaceModel>> clusters = new ArrayList<>();
    for(long[] pv : clustersMap.keySet()) {
      List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(pv);
      for(int i = 0; i < parallelClusters.size(); i++) {
        ArrayModifiableDBIDs c = parallelClusters.get(i);
        Cluster<SubspaceModel> cluster = new Cluster<>(c);
        cluster.setModel(new SubspaceModel(new Subspace(pv), Centroid.make(relation, c)));
        String subspace = BitsUtil.toStringLow(cluster.getModel().getSubspace().getDimensions(), db_dim);
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
    Comparator<Cluster<SubspaceModel>> comparator = new Comparator<Cluster<SubspaceModel>>() {
      @Override
      public int compare(Cluster<SubspaceModel> c1, Cluster<SubspaceModel> c2) {
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
   * @param relation the relation storing the objects
   * @param clustersMap the map containing the clusters
   */
  private void checkClusters(Relation<V> relation, TCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    final int dimensionality = RelationUtil.dimensionality(relation);
    // check if there are clusters < minpts
    // and add them to not assigned
    List<Pair<long[], ArrayModifiableDBIDs>> notAssigned = new ArrayList<>();
    TCustomHashMap<long[], List<ArrayModifiableDBIDs>> newClustersMap = new TCustomHashMap<>(BitsUtil.TROVE_HASH_STRATEGY);
    Pair<long[], ArrayModifiableDBIDs> noise = new Pair<>(BitsUtil.zero(dimensionality), DBIDUtil.newArray());
    for(long[] pv : clustersMap.keySet()) {
      // noise
      if(BitsUtil.cardinality(pv) == 0) {
        List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(pv);
        for(ArrayModifiableDBIDs c : parallelClusters) {
          noise.second.addDBIDs(c);
        }
      }
      // clusters
      else {
        List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(pv);
        List<ArrayModifiableDBIDs> newParallelClusters = new ArrayList<>(parallelClusters.size());
        for(ArrayModifiableDBIDs c : parallelClusters) {
          if(!BitsUtil.isZero(pv) && c.size() < mu) {
            notAssigned.add(new Pair<>(pv, c));
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

    for(Pair<long[], ArrayModifiableDBIDs> c : notAssigned) {
      if(c.second.isEmpty()) {
        continue;
      }
      Pair<long[], ArrayModifiableDBIDs> parent = findParent(relation, c, clustersMap);
      if(parent != null) {
        parent.second.addDBIDs(c.second);
      }
      else {
        noise.second.addDBIDs(c.second);
      }
    }

    List<ArrayModifiableDBIDs> noiseList = new ArrayList<>(1);
    noiseList.add(noise.second);
    clustersMap.put(noise.first, noiseList);
  }

  /**
   * Returns the parent of the specified cluster
   * 
   * @param relation the relation storing the objects
   * @param child the child to search the parent for
   * @param clustersMap the map containing the clusters
   * @return the parent of the specified cluster
   */
  private Pair<long[], ArrayModifiableDBIDs> findParent(Relation<V> relation, Pair<long[], ArrayModifiableDBIDs> child, TCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    Vector child_centroid = ProjectedCentroid.make(child.first, relation, child.second);

    Pair<long[], ArrayModifiableDBIDs> result = null;
    int resultCardinality = -1;

    long[] childPV = child.first;
    int childCardinality = BitsUtil.cardinality(childPV);
    for(long[] parentPV : clustersMap.keySet()) {
      int parentCardinality = BitsUtil.cardinality(parentPV);
      if(parentCardinality >= childCardinality) {
        continue;
      }
      if(resultCardinality != -1 && parentCardinality <= resultCardinality) {
        continue;
      }

      long[] pv = BitsUtil.andCMin(childPV, parentPV);
      if(pv.equals(parentPV)) {
        List<ArrayModifiableDBIDs> parentList = clustersMap.get(parentPV);
        for(ArrayModifiableDBIDs parent : parentList) {
          Vector parent_centroid = ProjectedCentroid.make(parentPV, relation, parent);
          double d = weightedDistance(child_centroid, parent_centroid, parentPV);
          if(d <= 2 * epsilon) {
            result = new Pair<>(parentPV, parent);
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
   * @param clustering Clustering we process
   * @param clusters the sorted list of clusters
   * @param dimensionality the dimensionality of the data
   * @param database the database containing the data objects
   */
  private void buildHierarchy(Relation<V> database, Clustering<SubspaceModel> clustering, List<Cluster<SubspaceModel>> clusters, int dimensionality) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder() : null;
    final int db_dim = RelationUtil.dimensionality(database);
    Hierarchy<Cluster<SubspaceModel>> hier = clustering.getClusterHierarchy();

    for(int i = 0; i < clusters.size() - 1; i++) {
      Cluster<SubspaceModel> c_i = clusters.get(i);
      final Subspace s_i = c_i.getModel().getSubspace();
      int subspaceDim_i = dimensionality - s_i.dimensionality();
      Vector ci_centroid = ProjectedCentroid.make(s_i.getDimensions(), database, c_i.getIDs());
      long[] pv1 = s_i.getDimensions();

      for(int j = i + 1; j < clusters.size(); j++) {
        Cluster<SubspaceModel> c_j = clusters.get(j);
        final Subspace s_j = c_j.getModel().getSubspace();
        int subspaceDim_j = dimensionality - s_j.dimensionality();

        if(subspaceDim_i < subspaceDim_j) {
          if(msg != null) {
            msg.append("\n l_i=").append(subspaceDim_i).append(" pv_i=[").append(BitsUtil.toStringLow(s_i.getDimensions(), db_dim)).append(']');
            msg.append("\n l_j=").append(subspaceDim_j).append(" pv_j=[").append(BitsUtil.toStringLow(s_j.getDimensions(), db_dim)).append(']');
          }

          // noise level reached
          if(s_j.dimensionality() == 0) {
            // no parents exists -> parent is noise
            if(hier.numParents(c_i) == 0) {
              clustering.addChildCluster(c_j, c_i);
              if(msg != null) {
                msg.append("\n [").append(BitsUtil.toStringLow(s_j.getDimensions(), db_dim));
                msg.append("] is parent of [").append(BitsUtil.toStringLow(s_i.getDimensions(), db_dim));
                msg.append(']');
              }
            }
          }
          else {
            Vector cj_centroid = ProjectedCentroid.make(c_j.getModel().getDimensions(), database, c_j.getIDs());
            long[] pv2 = s_j.getDimensions();
            long[] commonPreferenceVector = BitsUtil.andCMin(pv1, pv2);
            int subspaceDim = subspaceDimensionality(ci_centroid, cj_centroid, pv1, pv2, commonPreferenceVector);

            double d = weightedDistance(ci_centroid, cj_centroid, commonPreferenceVector);
            if(msg != null) {
              msg.append("\n dist = ").append(subspaceDim);
            }

            if(subspaceDim == subspaceDim_j) {
              if(msg != null) {
                msg.append("\n d = ").append(d);
              }
              if(d <= 2 * epsilon) {
                // no parent exists or c_j is not a parent of the already
                // existing parents
                if(hier.numParents(c_i) == 0 || !isParent(database, c_j, hier.iterParents(c_i), db_dim)) {
                  clustering.addChildCluster(c_j, c_i);
                  if(msg != null) {
                    msg.append("\n [").append(BitsUtil.toStringLow(s_j.getDimensions(), db_dim));
                    msg.append("] is parent of [");
                    msg.append(BitsUtil.toStringLow(s_i.getDimensions(), db_dim));
                    msg.append(']');
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
    if(msg != null) {
      LOG.debug(msg.toString());
    }
  }

  /**
   * Returns true, if the specified parent cluster is a parent of one child of
   * the children clusters.
   * 
   * @param relation the database containing the objects
   * @param parent the parent to be tested
   * @param iter the list of children to be tested
   * @param db_dim Database dimensionality
   * @return true, if the specified parent cluster is a parent of one child of
   *         the children clusters, false otherwise
   */
  private boolean isParent(Relation<V> relation, Cluster<SubspaceModel> parent, Iter<Cluster<SubspaceModel>> iter, int db_dim) {
    Subspace s_p = parent.getModel().getSubspace();
    Vector parent_centroid = ProjectedCentroid.make(s_p.getDimensions(), relation, parent.getIDs());
    int subspaceDim_parent = db_dim - s_p.dimensionality();

    for(; iter.valid(); iter.advance()) {
      Cluster<SubspaceModel> child = iter.get();
      Subspace s_c = child.getModel().getSubspace();
      Vector child_centroid = ProjectedCentroid.make(s_c.getDimensions(), relation, child.getIDs());
      long[] commonPreferenceVector = BitsUtil.andCMin(s_p.getDimensions(), s_c.getDimensions());
      int subspaceDim = subspaceDimensionality(parent_centroid, child_centroid, s_p.getDimensions(), s_c.getDimensions(), commonPreferenceVector);
      if(subspaceDim == subspaceDim_parent) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compute the common subspace dimensionality of two vectors.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @param pv1 First preference
   * @param pv2 Second preference
   * @param commonPreferenceVector Common preference
   * @return Usually, v1.dim - commonPreference.cardinality, unless either pv1
   *         and pv2 are a subset of the other.
   */
  private int subspaceDimensionality(NumberVector v1, NumberVector v2, long[] pv1, long[] pv2, long[] commonPreferenceVector) {
    // number of zero values in commonPreferenceVector
    int subspaceDim = v1.getDimensionality() - BitsUtil.cardinality(commonPreferenceVector);

    // special case: v1 and v2 are in parallel subspaces
    if(BitsUtil.equal(commonPreferenceVector, pv1) || BitsUtil.equal(commonPreferenceVector, pv2)) {
      double d = weightedDistance(v1, v2, commonPreferenceVector);
      if(d > 2 * epsilon) {
        subspaceDim++;
      }
    }
    return subspaceDim;
  }

  /**
   * Computes the weighted distance between the two specified vectors according
   * to the given preference vector.
   * 
   * @param v1 the first vector
   * @param v2 the second vector
   * @param weightVector the preference vector
   * @return the weighted distance between the two specified vectors according
   *         to the given preference vector
   */
  protected static double weightedDistance(NumberVector v1, NumberVector v2, long[] weightVector) {
    double sqrDist = 0;
    for(int i = BitsUtil.nextSetBit(weightVector, 0); i >= 0; i = BitsUtil.nextSetBit(weightVector, i + 1)) {
      double manhattanI = v1.doubleValue(i) - v2.doubleValue(i);
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
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
   * OPTICS variant used by DiSH internally.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public class DiSHOPTICS extends GeneralizedOPTICS<V, DiSHClusterOrderEntry> {
    /**
     * DiSH preprocessor instance.
     */
    private DiSHPreferenceVectorIndex<V> index;

    /**
     * Constructor.
     * 
     * @param indexinst Preprocessor instance.
     */
    public DiSHOPTICS(DiSHPreferenceVectorIndex<V> indexinst) {
      super(mu);
      this.index = indexinst;
    }

    @Override
    public Class<? super DiSHClusterOrderEntry> getEntryType() {
      return DiSHClusterOrderEntry.class;
    }

    @Override
    protected DiSHClusterOrderEntry makeSeedEntry(Relation<V> relation, DBID objectID) {
      return new DiSHClusterOrderEntry(objectID, null, Integer.MAX_VALUE, Double.POSITIVE_INFINITY, new long[0]);
    }

    @Override
    protected Collection<DiSHClusterOrderEntry> getNeighborsForDBID(Relation<V> relation, DBID id) {
      DBID id1 = DBIDUtil.deref(id);
      long[] pv1 = index.getPreferenceVector(id1);
      V dv1 = relation.get(id1);
      final int dim = dv1.getDimensionality();

      long[] ones = BitsUtil.ones(dim);
      long[] inverseCommonPreferenceVector = BitsUtil.ones(dim);

      ArrayList<DiSHClusterOrderEntry> result = new ArrayList<>();
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        long[] pv2 = index.getPreferenceVector(iter);
        V dv2 = relation.get(iter);
        // We need a copy of this for the distance.
        long[] commonPreferenceVector = BitsUtil.andCMin(pv1, pv2);

        // number of zero values in commonPreferenceVector
        int subspaceDim = dim - BitsUtil.cardinality(commonPreferenceVector);

        // special case: v1 and v2 are in parallel subspaces
        if(BitsUtil.equal(commonPreferenceVector, pv1) || BitsUtil.equal(commonPreferenceVector, pv2)) {
          double d = weightedDistance(dv1, dv2, commonPreferenceVector);
          if(d > 2 * epsilon) {
            subspaceDim++;
          }
        }

        // flip commonPreferenceVector for distance computation in common
        // subspace
        System.arraycopy(ones, 0, inverseCommonPreferenceVector, 0, ones.length);
        BitsUtil.xorI(inverseCommonPreferenceVector, commonPreferenceVector);

        final double orthogonalDistance = weightedDistance(dv1, dv2, inverseCommonPreferenceVector);
        result.add(new DiSHClusterOrderEntry(DBIDUtil.deref(iter), id1, subspaceDim, orthogonalDistance, commonPreferenceVector));
      }
      Collections.sort(result);
      // This is a hack, but needed to enforce core-distance of OPTICS:
      if(result.size() >= getMinPts()) {
        DiSHClusterOrderEntry coredist = result.get(getMinPts() - 1);
        for(int i = 0; i < getMinPts() - 1; i++) {
          final DiSHClusterOrderEntry prev = result.get(i);
          result.set(i, new DiSHClusterOrderEntry(prev.getID(), id1, coredist.getCorrelationValue(), coredist.getEuclideanValue(), coredist.commonPreferenceVector));
        }
      }
      return result;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
      return TypeUtil.array(TypeUtil.DOUBLE_VECTOR_FIELD);
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Cluster order entry for DiSH.
   * 
   * @author Elke Achtert
   * @author Erich Schubert
   */
  public static class DiSHClusterOrderEntry extends CorrelationClusterOrderEntry<DiSHClusterOrderEntry> {
    /**
     * The common preference vector of the two objects defining this distance.
     */
    private long[] commonPreferenceVector;

    /**
     * Constructs a new CorrelationDistance object.
     * 
     * @param correlationValue the correlation dimension to be represented by
     *        the CorrelationDistance
     * @param euclideanValue the Euclidean distance to be represented by the
     *        CorrelationDistance
     * @param commonPreferenceVector the common preference vector of the two
     *        objects defining this distance
     */
    public DiSHClusterOrderEntry(DBID objectID, DBID predecessorID, int correlationValue, double euclideanValue, long[] commonPreferenceVector) {
      super(objectID, predecessorID, correlationValue, euclideanValue);
      this.commonPreferenceVector = commonPreferenceVector;
    }

    /**
     * Returns the common preference vector of the two objects defining this
     * distance.
     * 
     * @return the common preference vector
     */
    public long[] getCommonPreferenceVector() {
      return commonPreferenceVector;
    }

    /**
     * Returns a string representation of this
     * PreferenceVectorBasedCorrelationDistance.
     * 
     * @return the correlation value, the Euclidean value and the common
     *         preference vector separated by blanks
     */
    @Override
    public String toString() {
      return super.toString() + SEPARATOR + commonPreferenceVector.toString();
    }

    @Override
    public int compareTo(DiSHClusterOrderEntry other) {
      return super.compareTo(other);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
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
    public static final OptionID EPSILON_ID = new OptionID("dish.epsilon", //
    "The maximum radius of the neighborhood to be considered in each " //
        + " dimension for determination of the preference vector.");

    /**
     * Parameter that specifies the a minimum number of points as a smoothing
     * factor to avoid the single-link-effect, must be an integer greater than
     * 0.
     * <p>
     * Default value: {@code 1}
     * </p>
     * <p>
     * Key: {@code -dish.mu}
     * </p>
     */
    public static final OptionID MU_ID = new OptionID("dish.mu", //
    "The minimum number of points as a smoothing factor to avoid the single-link-effekt.");

    protected double epsilon = 0.0;

    protected int mu = 1;

    /**
     * DiSH preprocessor.
     */
    protected DiSHPreferenceVectorIndex.Factory<V> dishPreprocessor;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID, 0.001);
      epsilonP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.doubleValue();
      }

      IntParameter muP = new IntParameter(MU_ID, 1);
      muP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(muP)) {
        mu = muP.intValue();
      }

      configDiSHPreprocessor(config, epsilon, mu);
    }

    public void configDiSHPreprocessor(Parameterization config, double epsilon, int minpts) {
      ListParameterization dishParameters = new ListParameterization();
      dishParameters.addParameter(DiSHPreferenceVectorIndex.Factory.EPSILON_ID, epsilon);
      dishParameters.addParameter(DiSHPreferenceVectorIndex.Factory.MINPTS_ID, minpts);
      ChainedParameterization dishchain = new ChainedParameterization(dishParameters, config);
      dishchain.errorsTo(config);

      final Class<DiSHPreferenceVectorIndex.Factory<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(DiSHPreferenceVectorIndex.Factory.class);
      dishPreprocessor = dishchain.tryInstantiate(cls);
    }

    @Override
    protected DiSH<V> makeInstance() {
      return new DiSH<>(epsilon, mu, dishPreprocessor);
    }
  }
}
