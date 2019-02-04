/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.CorrelationClusterOrder;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.GeneralizedOPTICS;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.DiSHPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectedCentroid;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.jafama.FastMath;

/**
 * Algorithm for detecting subspace hierarchies.
 * <p>
 * Reference:
 * <p>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek<br>
 * Detection and Visualization of Subspace Cluster Hierarchies<br>
 * Proc. 12th Int. Conf. on Database Systems for Advanced Applications (DASFAA).
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @assoc - - - DiSHPreferenceVectorIndex
 * @has - - - SubspaceModel
 * @has - - - DiSHClusterOrder
 *
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("DiSH: Detecting Subspace cluster Hierarchies")
@Description("Algorithm to find hierarchical correlation clusters in subspaces.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", //
    title = "Detection and Visualization of Subspace Cluster Hierarchies", //
    booktitle = "Proc. 12th Int. Conf. on Database Systems for Advanced Applications (DASFAA)", //
    url = "https://doi.org/10.1007/978-3-540-71703-4_15", //
    bibkey = "DBLP:conf/dasfaa/AchtertBKKMZ07")
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
  public Clustering<SubspaceModel> run(Database db, Relation<V> relation) {
    if(mu >= relation.size()) {
      throw new AbortException("Parameter mu is chosen unreasonably large. This won't yield meaningful results.");
    }
    DiSHClusterOrder opticsResult = new Instance(db, relation).run();

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
  private Clustering<SubspaceModel> computeClusters(Relation<V> database, DiSHClusterOrder clusterOrder) {
    final int dimensionality = RelationUtil.dimensionality(database);

    // extract clusters
    Object2ObjectOpenCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap = extractClusters(database, clusterOrder);
    logClusterSizes("Step 1: extract clusters", dimensionality, clustersMap);

    // check if there are clusters < minpts
    checkClusters(database, clustersMap);
    logClusterSizes("Step 2: check clusters", dimensionality, clustersMap);

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
        for(It<Cluster<SubspaceModel>> iter = clustering.getClusterHierarchy().iterParents(c); iter.valid(); iter.advance()) {
          msg.append("\n   parent ").append(iter.get());
        }
        for(It<Cluster<SubspaceModel>> iter = clustering.getClusterHierarchy().iterChildren(c); iter.valid(); iter.advance()) {
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
   * Log cluster sizes in verbose mode.
   * 
   * @param m Log message
   * @param dimensionality Dimensionality
   * @param clustersMap Cluster map
   */
  private void logClusterSizes(String m, int dimensionality, Object2ObjectOpenCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    if(LOG.isVerbose()) {
      final StringBuilder msg = new StringBuilder(1000).append(m).append('\n');
      for(ObjectIterator<Object2ObjectMap.Entry<long[], List<ArrayModifiableDBIDs>>> iter = clustersMap.object2ObjectEntrySet().fastIterator(); iter.hasNext();) {
        Object2ObjectMap.Entry<long[], List<ArrayModifiableDBIDs>> entry = iter.next();
        msg.append(BitsUtil.toStringLow(entry.getKey(), dimensionality)).append(" sizes:");
        for(ArrayModifiableDBIDs c : entry.getValue()) {
          msg.append(' ').append(c.size());
        }
        msg.append('\n');
      }
      LOG.verbose(msg.toString());
    }
  }

  /**
   * Extracts the clusters from the cluster order.
   *
   * @param relation the database storing the objects
   * @param clusterOrder the cluster order to extract the clusters from
   * @return the extracted clusters
   */
  private Object2ObjectOpenCustomHashMap<long[], List<ArrayModifiableDBIDs>> extractClusters(Relation<V> relation, DiSHClusterOrder clusterOrder) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extract Clusters", relation.size(), LOG) : null;
    Object2ObjectOpenCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap = new Object2ObjectOpenCustomHashMap<>(BitsUtil.FASTUTIL_HASH_STRATEGY);
    // Note clusterOrder currently contains DBID objects anyway.
    WritableDataStore<Pair<long[], ArrayModifiableDBIDs>> entryToClusterMap = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Pair.class);
    for(DBIDIter iter = clusterOrder.iter(); iter.valid(); iter.advance()) {
      V object = relation.get(iter);
      long[] preferenceVector = clusterOrder.getCommonPreferenceVector(iter);

      // get the list of (parallel) clusters for the preference vector
      List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(preferenceVector);
      if(parallelClusters == null) {
        parallelClusters = new ArrayList<>();
        clustersMap.put(preferenceVector, parallelClusters);
      }

      // look for the proper cluster
      ArrayModifiableDBIDs cluster = null;
      for(ArrayModifiableDBIDs c : parallelClusters) {
        NumberVector c_centroid = ProjectedCentroid.make(preferenceVector, relation, c);
        long[] commonPreferenceVector = BitsUtil.andCMin(preferenceVector, preferenceVector);
        int subspaceDim = subspaceDimensionality(object, c_centroid, preferenceVector, preferenceVector, commonPreferenceVector);
        if(subspaceDim == clusterOrder.getCorrelationValue(iter)) {
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
      cluster.add(iter);
      entryToClusterMap.put(iter, new Pair<>(preferenceVector, cluster));

      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);

    if(LOG.isDebuggingFiner()) {
      int dim = RelationUtil.dimensionality(relation);
      StringBuilder msg = new StringBuilder("Step 0");
      for(Map.Entry<long[], List<ArrayModifiableDBIDs>> clusterList : clustersMap.entrySet()) {
        for(ArrayModifiableDBIDs c : clusterList.getValue()) {
          msg.append('\n').append(BitsUtil.toStringLow(clusterList.getKey(), dim)).append(" ids ").append(c.size());
        }
      }
      LOG.debugFiner(msg.toString());
    }

    // add the predecessor to the cluster
    DBIDVar cur = DBIDUtil.newVar(), pre = DBIDUtil.newVar();
    for(long[] pv : clustersMap.keySet()) {
      List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(pv);
      for(ArrayModifiableDBIDs cluster : parallelClusters) {
        if(cluster.isEmpty()) {
          continue;
        }
        cluster.assignVar(0, cur);
        clusterOrder.getPredecessor(cur, pre);
        if(!pre.isSet() || DBIDUtil.equal(pre, cur)) {
          continue;
        }
        // parallel cluster
        if(BitsUtil.equal(clusterOrder.getCommonPreferenceVector(pre), clusterOrder.getCommonPreferenceVector(cur))) {
          continue;
        }
        if(clusterOrder.getCorrelationValue(pre) < clusterOrder.getCorrelationValue(cur) || //
            clusterOrder.getReachability(pre) < clusterOrder.getReachability(cur)) {
          continue;
        }

        Pair<long[], ArrayModifiableDBIDs> oldCluster = entryToClusterMap.get(pre);
        oldCluster.second.remove(pre);
        cluster.add(pre);
        entryToClusterMap.put(pre, new Pair<>(pv, cluster));
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
  private List<Cluster<SubspaceModel>> sortClusters(Relation<V> relation, Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    final int db_dim = RelationUtil.dimensionality(relation);
    // int num = 1;
    List<Cluster<SubspaceModel>> clusters = new ArrayList<>();
    for(long[] pv : clustersMap.keySet()) {
      List<ArrayModifiableDBIDs> parallelClusters = clustersMap.get(pv);
      for(int i = 0; i < parallelClusters.size(); i++) {
        ArrayModifiableDBIDs c = parallelClusters.get(i);
        Cluster<SubspaceModel> cluster = new Cluster<>(c);
        cluster.setModel(new SubspaceModel(new Subspace(pv), Centroid.make(relation, c).getArrayRef()));
        String subspace = BitsUtil.toStringLow(cluster.getModel().getSubspace().getDimensions(), db_dim);
        cluster.setName(parallelClusters.size() > 1 ? ("Cluster_" + subspace + "_" + i) : ("Cluster_" + subspace));
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
   * Removes the clusters with size &lt; minpts from the cluster map and adds them
   * to their parents.
   *
   * @param relation the relation storing the objects
   * @param clustersMap the map containing the clusters
   */
  private void checkClusters(Relation<V> relation, Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    final int dimensionality = RelationUtil.dimensionality(relation);
    // check if there are clusters < minpts
    // and add them to not assigned
    List<Pair<long[], ArrayModifiableDBIDs>> notAssigned = new ArrayList<>();
    Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> newClustersMap = new Object2ObjectOpenCustomHashMap<>(BitsUtil.FASTUTIL_HASH_STRATEGY);
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
      (parent != null ? parent : noise).second.addDBIDs(c.second);
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
  private Pair<long[], ArrayModifiableDBIDs> findParent(Relation<V> relation, Pair<long[], ArrayModifiableDBIDs> child, Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
    Centroid child_centroid = ProjectedCentroid.make(child.first, relation, child.second);

    Pair<long[], ArrayModifiableDBIDs> result = null;
    int resultCardinality = -1;

    long[] childPV = child.first;
    int childCardinality = BitsUtil.cardinality(childPV);
    for(long[] parentPV : clustersMap.keySet()) {
      int parentCardinality = BitsUtil.cardinality(parentPV);
      if(parentCardinality >= childCardinality || (resultCardinality != -1 && parentCardinality <= resultCardinality)) {
        continue;
      }

      long[] pv = BitsUtil.andCMin(childPV, parentPV);
      if(BitsUtil.equal(pv, parentPV)) {
        List<ArrayModifiableDBIDs> parentList = clustersMap.get(parentPV);
        for(ArrayModifiableDBIDs parent : parentList) {
          NumberVector parent_centroid = ProjectedCentroid.make(parentPV, relation, parent);
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
      NumberVector ci_centroid = ProjectedCentroid.make(s_i.getDimensions(), database, c_i.getIDs());
      long[] pv1 = s_i.getDimensions();

      for(int j = i + 1; j < clusters.size(); j++) {
        Cluster<SubspaceModel> c_j = clusters.get(j);
        final Subspace s_j = c_j.getModel().getSubspace();
        int subspaceDim_j = dimensionality - s_j.dimensionality();

        if(subspaceDim_i < subspaceDim_j) {
          if(msg != null) {
            msg.append("\n l_i=").append(subspaceDim_i).append(" pv_i=[").append(BitsUtil.toStringLow(s_i.getDimensions(), db_dim)).append(']') //
                .append("\n l_j=").append(subspaceDim_j).append(" pv_j=[").append(BitsUtil.toStringLow(s_j.getDimensions(), db_dim)).append(']');
          }

          // noise level reached
          if(s_j.dimensionality() == 0) {
            // no parents exists -> parent is noise
            if(hier.numParents(c_i) == 0) {
              clustering.addChildCluster(c_j, c_i);
              if(msg != null) {
                msg.append("\n [").append(BitsUtil.toStringLow(s_j.getDimensions(), db_dim)) //
                    .append("] is parent of [").append(BitsUtil.toStringLow(s_i.getDimensions(), db_dim)).append(']');
              }
            }
          }
          else {
            NumberVector cj_centroid = ProjectedCentroid.make(c_j.getModel().getDimensions(), database, c_j.getIDs());
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
                    msg.append("\n [").append(BitsUtil.toStringLow(s_j.getDimensions(), db_dim)) //
                        .append("] is parent of [") //
                        .append(BitsUtil.toStringLow(s_i.getDimensions(), db_dim)).append(']');
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
  private boolean isParent(Relation<V> relation, Cluster<SubspaceModel> parent, It<Cluster<SubspaceModel>> iter, int db_dim) {
    Subspace s_p = parent.getModel().getSubspace();
    NumberVector parent_centroid = ProjectedCentroid.make(s_p.getDimensions(), relation, parent.getIDs());
    int subspaceDim_parent = db_dim - s_p.dimensionality();

    for(; iter.valid(); iter.advance()) {
      Cluster<SubspaceModel> child = iter.get();
      Subspace s_c = child.getModel().getSubspace();
      NumberVector child_centroid = ProjectedCentroid.make(s_c.getDimensions(), relation, child.getIDs());
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
    return FastMath.sqrt(sqrDist);
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
   */
  private class Instance extends GeneralizedOPTICS.Instance<V, DiSHClusterOrder> {
    /**
     * Data relation.
     */
    private Relation<V> relation;

    /**
     * Cluster order.
     */
    private ArrayModifiableDBIDs clusterOrder;

    /**
     * Correlation value.
     */
    private WritableIntegerDataStore correlationValue;

    /**
     * Shared preference vectors.
     */
    private WritableDataStore<long[]> commonPreferenceVectors;

    /**
     * Temporary ids.
     */
    private ArrayModifiableDBIDs tmpIds;

    /**
     * Temporary storage of correlation values.
     */
    private WritableIntegerDataStore tmpCorrelation;

    /**
     * Temporary storage of distances.
     */
    private WritableDoubleDataStore tmpDistance;

    /**
     * Sort object by the temporary fields.
     */
    Comparator<DBIDRef> tmpcomp = new Sorter();

    /**
     * Index.
     */
    private DiSHPreferenceVectorIndex<V> index;

    /**
     * Temporary storage for new preference vectors.
     */
    private WritableDataStore<long[]> tmpPreferenceVectors;

    /**
     * Constructor.
     *
     * @param db Database
     * @param relation Relation
     */
    public Instance(Database db, Relation<V> relation) {
      super(db, relation);
      DBIDs ids = relation.getDBIDs();
      this.clusterOrder = DBIDUtil.newArray(ids.size());
      this.relation = relation;
      this.correlationValue = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_DB, Integer.MAX_VALUE);
      this.commonPreferenceVectors = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, long[].class);
      this.tmpIds = DBIDUtil.newArray(ids);
      this.tmpCorrelation = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
      this.tmpDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
      this.tmpPreferenceVectors = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, long[].class);
    }

    @Override
    public DiSHClusterOrder run() {
      // This will be lazily instantiated.
      this.index = dishPreprocessor.instantiate(relation);
      return super.run();
    }

    @Override
    protected DiSHClusterOrder buildResult() {
      return new DiSHClusterOrder("DiSH Cluster Order", "dish-cluster-order", //
          clusterOrder, reachability, predecessor, correlationValue, commonPreferenceVectors);
    }

    @Override
    protected void initialDBID(DBIDRef id) {
      correlationValue.put(id, Integer.MAX_VALUE);
      commonPreferenceVectors.put(id, new long[0]);
    }

    @Override
    protected void expandDBID(DBIDRef id) {
      clusterOrder.add(id);

      long[] pv1 = index.getPreferenceVector(id);
      V dv1 = relation.get(id);
      final int dim = dv1.getDimensionality();

      long[] ones = BitsUtil.ones(dim);
      long[] inverseCommonPreferenceVector = BitsUtil.ones(dim);

      DBIDArrayIter iter = tmpIds.iter();
      for(; iter.valid(); iter.advance()) {
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
        tmpCorrelation.put(iter, subspaceDim);
        tmpDistance.put(iter, orthogonalDistance);
        tmpPreferenceVectors.put(iter, commonPreferenceVector);
      }
      tmpIds.sort(tmpcomp);
      // Core-distance of OPTICS:
      // FIXME: what if there are less than mu points of smallest
      // dimensionality? Then this distance will not be meaningful.
      double coredist = tmpDistance.doubleValue(iter.seek(mu - 1));
      // This is a hack, but needed to enforce core-distance of OPTICS:
      for(iter.seek(0); iter.valid(); iter.advance()) {
        if(processedIDs.contains(iter)) {
          continue;
        }
        int prevcorr = correlationValue.intValue(iter);
        int curcorr = tmpCorrelation.intValue(iter);
        if(prevcorr < curcorr) {
          continue; // No improvement.
        }
        double currdist = MathUtil.max(tmpDistance.doubleValue(iter), coredist);
        if(prevcorr == curcorr) {
          double prevdist = reachability.doubleValue(iter);
          if(prevdist <= currdist) {
            continue; // No improvement.
          }
        }
        correlationValue.putInt(iter, curcorr);
        reachability.putDouble(iter, currdist);
        predecessor.putDBID(iter, id);
        commonPreferenceVectors.put(iter, tmpPreferenceVectors.get(iter));
        // Add to candidates if not yet seen:
        if(prevcorr == Integer.MAX_VALUE) {
          candidates.add(iter);
        }
      }
    }

    @Override
    public int compare(DBIDRef o1, DBIDRef o2) {
      int c1 = correlationValue.intValue(o1),
          c2 = correlationValue.intValue(o2);
      return (c1 < c2) ? -1 : (c1 > c2) ? +1 : //
          super.compare(o1, o2);
    }

    /**
     * Sort new candidates by their distance, for determining the core size.
     *
     * @author Erich Schubert
     */
    private final class Sorter implements Comparator<DBIDRef> {
      @Override
      public int compare(DBIDRef o1, DBIDRef o2) {
        int c1 = tmpCorrelation.intValue(o1), c2 = tmpCorrelation.intValue(o2);
        return (c1 < c2) ? -1 : (c1 > c2) ? +1 : //
            Double.compare(tmpDistance.doubleValue(o1), tmpDistance.doubleValue(o2));
      }
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * DiSH cluster order.
   *
   * @author Erich Schubert
   */
  public static class DiSHClusterOrder extends CorrelationClusterOrder {
    /**
     * Preference vectors.
     */
    private WritableDataStore<long[]> commonPreferenceVectors;

    /**
     * Constructor.
     *
     * @param name
     * @param shortname
     * @param ids
     * @param reachability
     * @param predecessor
     * @param corrdim
     * @param commonPreferenceVectors
     */
    public DiSHClusterOrder(String name, String shortname, //
        ArrayModifiableDBIDs ids, WritableDoubleDataStore reachability, //
        WritableDBIDDataStore predecessor, WritableIntegerDataStore corrdim, //
        WritableDataStore<long[]> commonPreferenceVectors) {
      super(name, shortname, ids, reachability, predecessor, corrdim);
      this.commonPreferenceVectors = commonPreferenceVectors;
    }

    /**
     * Get the common subspace.
     *
     * @param id Object id
     * @return common subspace
     */
    public long[] getCommonPreferenceVector(DBIDRef id) {
      return commonPreferenceVectors.get(id);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter that specifies the maximum radius of the neighborhood to be
     * considered in each dimension for determination of the preference vector,
     * must be a double equal to or greater than 0.
     */
    public static final OptionID EPSILON_ID = new OptionID("dish.epsilon", //
        "The maximum radius of the neighborhood to be considered in each " //
            + " dimension for determination of the preference vector.");

    /**
     * Parameter that specifies the a minimum number of points as a smoothing
     * factor to avoid the single-link-effect, must be an integer greater
     * than 0.
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

      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID, 0.001) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.doubleValue();
      }

      IntParameter muP = new IntParameter(MU_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
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
