/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.subspace;

import java.util.*;

import elki.clustering.optics.CorrelationClusterOrder;
import elki.clustering.optics.GeneralizedOPTICS;
import elki.data.*;
import elki.data.model.SubspaceModel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.database.datastore.*;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.subspace.OnedimensionalDistance;
import elki.itemsetmining.APRIORI;
import elki.itemsetmining.Itemset;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.Duration;
import elki.logging.statistics.MillisTimeDuration;
import elki.math.MathUtil;
import elki.math.linearalgebra.Centroid;
import elki.math.linearalgebra.ProjectedCentroid;
import elki.result.FrequentItemsetsResult;
import elki.result.Metadata;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.pairs.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

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
 * @has - - - SubspaceMode
 * @has - - - DiSHClusterOrder
 */
@Title("DiSH: Detecting Subspace cluster Hierarchies")
@Description("Algorithm to find hierarchical correlation clusters in subspaces.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", //
    title = "Detection and Visualization of Subspace Cluster Hierarchies", //
    booktitle = "Proc. 12th Int. Conf. on Database Systems for Advanced Applications (DASFAA)", //
    url = "https://doi.org/10.1007/978-3-540-71703-4_15", //
    bibkey = "DBLP:conf/dasfaa/AchtertBKKMZ07")
public class DiSH implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DiSH.class);

  /**
   * Available strategies for determination of the preference vector.
   */
  public enum Strategy {
    /**
     * Apriori strategy.
     */
    APRIORI,
    /**
     * Max intersection strategy.
     */
    MAX_INTERSECTION
  }

  /**
   * Holds the value of {@link Par#EPSILON_ID}.
   */
  private double epsilon;

  /**
   * OPTICS minPts parameter.
   */
  private int minpts;

  /**
   * DiSH strategy.
   */
  private Strategy strategy;

  /**
   * Constructor.
   *
   * @param epsilon Epsilon value
   * @param minpts Mu parameter (minPts)
   * @param strategy DiSH strategy
   */
  public DiSH(double epsilon, int minpts, DiSH.Strategy strategy) {
    super();
    this.epsilon = epsilon;
    this.minpts = minpts;
    this.strategy = strategy;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Performs the DiSH algorithm on the given database.
   *
   * @param relation Relation to process
   * @return Clustering
   */
  public Clustering<SubspaceModel> run(Relation<? extends NumberVector> relation) {
    if(minpts >= relation.size()) {
      throw new AbortException("Parameter minpts is chosen unreasonably large. This won't yield meaningful results.");
    }
    DiSHClusterOrder opticsResult = new Instance(relation).run();

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
  private Clustering<SubspaceModel> computeClusters(Relation<? extends NumberVector> database, DiSHClusterOrder clusterOrder) {
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
    Clustering<SubspaceModel> clustering = new Clustering<>();
    Metadata.of(clustering).setLongName("DiSH clustering");
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
  private Object2ObjectOpenCustomHashMap<long[], List<ArrayModifiableDBIDs>> extractClusters(Relation<? extends NumberVector> relation, DiSHClusterOrder clusterOrder) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extract Clusters", relation.size(), LOG) : null;
    Object2ObjectOpenCustomHashMap<long[], List<ArrayModifiableDBIDs>> clustersMap = new Object2ObjectOpenCustomHashMap<>(BitsUtil.FASTUTIL_HASH_STRATEGY);
    // Note clusterOrder currently contains DBID objects anyway.
    WritableDataStore<Pair<long[], ArrayModifiableDBIDs>> entryToClusterMap = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Pair.class);
    for(DBIDIter iter = clusterOrder.iter(); iter.valid(); iter.advance()) {
      NumberVector object = relation.get(iter);
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
  private List<Cluster<SubspaceModel>> sortClusters(Relation<? extends NumberVector> relation, Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
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
    Collections.sort(clusters, (c1, c2) -> c2.getModel().getSubspace().dimensionality() - c1.getModel().getSubspace().dimensionality());
    return clusters;
  }

  /**
   * Removes the clusters with size &lt; minpts from the cluster map and adds
   * them to their parents.
   *
   * @param relation the relation storing the objects
   * @param clustersMap the map containing the clusters
   */
  private void checkClusters(Relation<? extends NumberVector> relation, Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
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
          if(!BitsUtil.isZero(pv) && c.size() < minpts) {
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
  private Pair<long[], ArrayModifiableDBIDs> findParent(Relation<? extends NumberVector> relation, Pair<long[], ArrayModifiableDBIDs> child, Object2ObjectMap<long[], List<ArrayModifiableDBIDs>> clustersMap) {
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
  private void buildHierarchy(Relation<? extends NumberVector> database, Clustering<SubspaceModel> clustering, List<Cluster<SubspaceModel>> clusters, int dimensionality) {
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
              if(d > 2 * epsilon) {
                throw new IllegalStateException("Should never happen: d = " + d + " > 2*" + epsilon);
              }
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
  private boolean isParent(Relation<? extends NumberVector> relation, Cluster<SubspaceModel> parent, It<Cluster<SubspaceModel>> iter, int db_dim) {
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
    return Math.sqrt(sqrDist);
  }

  /**
   * OPTICS variant used by DiSH internally.
   *
   * @author Erich Schubert
   */
  private class Instance extends GeneralizedOPTICS.Instance<DiSHClusterOrder> {
    /**
     * Data relation.
     */
    private Relation<? extends NumberVector> relation;

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
     * The precomputed preference vectors.
     */
    protected WritableDataStore<long[]> preferenceVectors;

    /**
     * Temporary storage for new preference vectors.
     */
    private WritableDataStore<long[]> tmpPreferenceVectors;

    /**
     * Constructor.
     *
     * @param relation Relation
     */
    public Instance(Relation<? extends NumberVector> relation) {
      super(relation.getDBIDs());
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
      preferenceVectors = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, long[].class);

      Duration dur = new MillisTimeDuration(this.getClass() + ".preprocessing-time").begin();
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Preprocessing preference vector", relation.size(), LOG) : null;

      final int dim = RelationUtil.dimensionality(relation);
      ArrayList<RangeSearcher<DBIDRef>> rangeQueries = new ArrayList<>(dim);
      for(int d = 0; d < dim; d++) {
        rangeQueries.add(new QueryBuilder<>(relation, new OnedimensionalDistance(d)).rangeByDBID(epsilon));
      }

      StringBuilder msg = LOG.isDebugging() ? new StringBuilder() : null;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        if(msg != null) {
          msg.setLength(0);
          msg.append("\nid = ").append(DBIDUtil.toString(it));
          // msg.append(" ").append(database.get(id));
          // msg.append(" ").append(database.getObjectLabelQuery().get(id));
        }

        // determine neighbors in each dimension
        ModifiableDBIDs[] allNeighbors = new ModifiableDBIDs[dim];
        for(int d = 0; d < dim; d++) {
          allNeighbors[d] = DBIDUtil.newHashSet(rangeQueries.get(d).getRange(it, epsilon));
        }

        if(msg != null) {
          for(int d = 0; d < dim; d++) {
            msg.append("\n neighbors [").append(d).append(']') //
                .append(" (").append(allNeighbors[d].size()).append(") = ").append(allNeighbors[d]);
          }
        }
        preferenceVectors.put(it, determinePreferenceVector(allNeighbors, msg));

        if(msg != null) {
          LOG.debugFine(msg.toString());
        }
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      LOG.statistics(dur.end());
      return super.run();
    }

    /**
     * Determines the preference vector according to the specified neighbor ids.
     *
     * @param neighborIDs the list of ids of the neighbors in each dimension
     * @param msg a string buffer for debug messages
     * @return the preference vector
     */
    private long[] determinePreferenceVector(ModifiableDBIDs[] neighborIDs, StringBuilder msg) {
      return strategy == Strategy.APRIORI ? //
          determinePreferenceVectorByApriori(neighborIDs, msg) : //
          determinePreferenceVectorByMaxIntersection(neighborIDs, msg);
    }

    /**
     * Determines the preference vector with the apriori strategy.
     *
     * @param neighborIDs the list of ids of the neighbors in each dimension
     * @param msg a string buffer for debug messages
     * @return the preference vector
     */
    private long[] determinePreferenceVectorByApriori(ModifiableDBIDs[] neighborIDs, StringBuilder msg) {
      int dimensionality = neighborIDs.length;

      ArrayList<BitVector> bvs = new ArrayList<>(relation.size());
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        long[] bits = BitsUtil.zero(dimensionality);
        boolean allFalse = true;
        for(int d = 0; d < dimensionality; d++) {
          if(neighborIDs[d].contains(it)) {
            BitsUtil.setI(bits, d);
            allFalse = false;
          }
        }
        if(!allFalse) {
          bvs.add(new BitVector(bits, dimensionality));
        }
      }
      // Build virtual relation for running apriori
      SimpleTypeInformation<BitVector> bitmeta = VectorFieldTypeInformation.typeRequest(BitVector.class, dimensionality, dimensionality);
      DBIDRange vids = DBIDFactory.FACTORY.generateStaticDBIDRange(0, bvs.size());
      MaterializedRelation<BitVector> bitvectorrel = new MaterializedRelation<>(bitmeta, vids);
      for(DBIDArrayIter it = vids.iter(); it.valid(); it.advance()) {
        bitvectorrel.insert(it, bvs.get(it.getOffset()));
      }
      APRIORI apriori = new APRIORI(minpts);
      FrequentItemsetsResult aprioriResult = apriori.run(bitvectorrel);

      // result of apriori
      List<Itemset> frequentItemsets = aprioriResult.getItemsets();
      if(msg != null) {
        msg.append("\n Frequent itemsets: ").append(frequentItemsets);
      }
      int maxSupport = 0, maxCardinality = 0;
      long[] preferenceVector = BitsUtil.zero(dimensionality);
      for(Itemset itemset : frequentItemsets) {
        if((maxCardinality < itemset.length()) || (maxCardinality == itemset.length() && maxSupport == itemset.getSupport())) {
          preferenceVector = Itemset.toBitset(itemset, BitsUtil.zero(dimensionality));
          maxCardinality = itemset.length();
          maxSupport = itemset.getSupport();
        }
      }

      if(msg != null) {
        LOG.debugFine(msg.append("\n preference ") //
            .append(BitsUtil.toStringLow(preferenceVector, dimensionality)) //
            .append('\n').toString());
      }
      return preferenceVector;
    }

    /**
     * Determines the preference vector with the max intersection strategy.
     *
     * @param neighborIDs the list of ids of the neighbors in each dimension
     * @param msg a string buffer for debug messages
     * @return the preference vector
     */
    private long[] determinePreferenceVectorByMaxIntersection(ModifiableDBIDs[] neighborIDs, StringBuilder msg) {
      int dimensionality = neighborIDs.length;
      long[] preferenceVector = BitsUtil.zero(dimensionality);

      Map<Integer, ModifiableDBIDs> candidates = new HashMap<>(dimensionality);
      for(int i = 0; i < dimensionality; i++) {
        ModifiableDBIDs s_i = neighborIDs[i];
        if(s_i.size() > minpts) {
          candidates.put(i, s_i);
        }
      }
      if(msg != null) {
        msg.append("\n candidates ").append(candidates.keySet());
      }

      if(!candidates.isEmpty()) {
        int i = max(candidates);
        ModifiableDBIDs intersection = candidates.remove(i);
        BitsUtil.setI(preferenceVector, i);
        while(!candidates.isEmpty()) {
          i = maxIntersection(candidates, intersection);
          candidates.remove(i);
          if(intersection.size() < minpts) {
            break;
          }
          BitsUtil.setI(preferenceVector, i);
        }
      }

      if(msg != null) {
        msg.append("\n preference ").append(BitsUtil.toStringLow(preferenceVector, dimensionality));
        LOG.debug(msg.toString());
      }

      return preferenceVector;
    }

    /**
     * Returns the set with the maximum size contained in the specified map.
     *
     * @param candidates the map containing the sets
     * @return the set with the maximum size
     */
    private int max(Map<Integer, ModifiableDBIDs> candidates) {
      int maxDim = -1, size = -1;
      for(Integer nextDim : candidates.keySet()) {
        int nextSet = candidates.get(nextDim).size();
        if(size < nextSet) {
          size = nextSet;
          maxDim = nextDim;
        }
      }
      return maxDim;
    }

    /**
     * Returns the index of the set having the maximum intersection set with the
     * specified set contained in the specified map.
     *
     * @param candidates the map containing the sets
     * @param set the set to intersect with and output the result to
     * @return the set with the maximum size
     */
    private int maxIntersection(Map<Integer, ModifiableDBIDs> candidates, ModifiableDBIDs set) {
      int maxDim = -1;
      ModifiableDBIDs maxIntersection = null;
      for(Integer nextDim : candidates.keySet()) {
        DBIDs nextSet = candidates.get(nextDim);
        ModifiableDBIDs nextIntersection = DBIDUtil.intersection(set, nextSet);
        if(maxDim < 0 || maxIntersection.size() < nextIntersection.size()) {
          maxIntersection = nextIntersection;
          maxDim = nextDim;
        }
      }
      if(maxDim >= 0) {
        set.clear().addDBIDs(maxIntersection);
      }
      return maxDim;
    }

    @Override
    protected DiSHClusterOrder buildResult() {
      DiSHClusterOrder result = new DiSHClusterOrder(clusterOrder, reachability, predecessor, correlationValue, commonPreferenceVectors);
      Metadata.of(result).setLongName("DiSH Cluster Order");
      return result;
    }

    @Override
    protected void initialDBID(DBIDRef id) {
      correlationValue.put(id, Integer.MAX_VALUE);
      commonPreferenceVectors.put(id, new long[0]);
    }

    @Override
    protected void expandDBID(DBIDRef id) {
      clusterOrder.add(id);

      long[] pv1 = preferenceVectors.get(id);
      NumberVector dv1 = relation.get(id);
      final int dim = dv1.getDimensionality();

      long[] ones = BitsUtil.ones(dim);
      long[] inverseCommonPreferenceVector = BitsUtil.ones(dim);

      DBIDArrayIter iter = tmpIds.iter();
      for(; iter.valid(); iter.advance()) {
        long[] pv2 = preferenceVectors.get(iter);
        NumberVector dv2 = relation.get(iter);
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
      // FIXME: what if there are less than minpts points of smallest
      // dimensionality? Then this distance will not be meaningful.
      double coredist = tmpDistance.doubleValue(iter.seek(minpts - 1));
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
      int c1 = correlationValue.intValue(o1);
      int c2 = correlationValue.intValue(o2);
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
     * @param ids Cluster order
     * @param reachability Reachability
     * @param predecessor Predecessors
     * @param corrdim Correlation dimensionality
     * @param commonPreferenceVectors Subspace preference
     */
    public DiSHClusterOrder(ArrayModifiableDBIDs ids, WritableDoubleDataStore reachability, //
        WritableDBIDDataStore predecessor, WritableIntegerDataStore corrdim, //
        WritableDataStore<long[]> commonPreferenceVectors) {
      super(ids, reachability, predecessor, corrdim);
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
  public static class Par implements Parameterizer {
    /**
     * The default value for epsilon.
     */
    public static final double DEFAULT_EPSILON = 0.001;

    /**
     * A comma separated list of positive doubles specifying the maximum radius
     * of the neighborhood to be considered in each dimension for determination
     * of the preference vector (default is {@link #DEFAULT_EPSILON} in each
     * dimension). If only one value is specified, this value will be used for
     * each dimension.
     */
    public static final OptionID EPSILON_ID = new OptionID("dish.epsilon", //
        "A comma separated list of positive doubles specifying the maximum radius " + //
            "of the neighborhood to be considered in each dimension for determination " + //
            "of the preference vector " + //
            "(default is " + DEFAULT_EPSILON + " in each dimension). " + //
            "If only one value is specified, this value will be used for each dimension.");

    /**
     * Positive threshold for minimum numbers of points in the
     * epsilon-neighborhood of a points.
     */
    public static final OptionID MINPTS_ID = new OptionID("dish.minpts", //
        "Positive threshold for minumum numbers of points in the epsilon-neighborhood of a point. " + //
            "The value of the preference vector in dimension d_i is set to 1 " + //
            "if the epsilon neighborhood contains more than dish.minpts points and the following condition holds: " + //
            "for all dimensions d_j: |neighbors(d_i) intersection neighbors(d_j)| >= dish.minpts.");

    /**
     * Default strategy.
     */
    public static final Strategy DEFAULT_STRATEGY = Strategy.MAX_INTERSECTION;

    /**
     * The strategy for determination of the preference vector, available
     * strategies are: {@link Strategy#APRIORI } and
     * {@link Strategy#MAX_INTERSECTION}.
     */
    public static final OptionID STRATEGY_ID = new OptionID("dish.strategy", //
        "The strategy for determination of the preference vector, " + //
            "available strategies are: [" + Strategy.APRIORI + "| " + Strategy.MAX_INTERSECTION + "]" + //
            "(default is " + DEFAULT_STRATEGY + ")");

    /**
     * Parameter that specifies the a minimum number of points as a smoothing
     * factor to avoid the single-link-effect, must be an integer greater
     * than 0.
     */
    public static final OptionID MU_ID = new OptionID("dish.mu", //
        "The minimum number of points as a smoothing factor to avoid the single-link-effekt.");

    /**
     * The epsilon value for each dimension.
     */
    protected double epsilon;

    /**
     * Threshold for minimum number of points in the neighborhood.
     */
    protected int minpts;

    /**
     * The strategy to determine the preference vector.
     */
    protected Strategy strategy;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(EPSILON_ID, 0.001) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> epsilon = x);
      new IntParameter(MU_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minpts = x);
      new EnumParameter<Strategy>(STRATEGY_ID, Strategy.class, DEFAULT_STRATEGY) //
          .grab(config, x -> strategy = x);
    }

    @Override
    public DiSH make() {
      return new DiSH(epsilon, minpts, strategy);
    }
  }
}
