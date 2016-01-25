package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionSelectingSubspaceDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * Implementation of the SUBCLU algorithm, an algorithm to detect arbitrarily
 * shaped and positioned clusters in subspaces. SUBCLU delivers for each
 * subspace the same clusters DBSCAN would have found, when applied to this
 * subspace separately.
 * </p>
 * <p>
 * Reference: <br>
 * K. Kailing, H.-P. Kriegel, P. Kröger:<br />
 * Density connected Subspace Clustering for High Dimensional Data<br />
 * In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.
 * </p>
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @apiviz.uses DBSCAN
 * @apiviz.uses DimensionSelectingSubspaceDistanceFunction
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
@Title("SUBCLU: Density connected Subspace Clustering")
@Description("Algorithm to detect arbitrarily shaped and positioned clusters in subspaces. "//
    + "SUBCLU delivers for each subspace the same clusters DBSCAN would have found, "//
    + "when applied to this subspace seperately.")
@Reference(authors = "K. Kailing, H.-P. Kriegel, P. Kröger", //
title = "Density connected Subspace Clustering for High Dimensional Data", //
booktitle = "Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004", //
url = "http://www.siam.org/meetings/sdm04/proceedings/sdm04_023.pdf")
public class SUBCLU<V extends NumberVector> extends AbstractAlgorithm<Clustering<SubspaceModel>> implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SUBCLU.class);

  /**
   * The distance function to determine the distance between database objects.
   * <p>
   * Default value: {@link SubspaceEuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -subclu.distancefunction}
   * </p>
   */
  public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("subclu.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to
   * {@link DimensionSelectingSubspaceDistanceFunction}.
   * <p>
   * Key: {@code -subclu.epsilon}
   * </p>
   */
  public static final OptionID EPSILON_ID = new OptionID("subclu.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -subclu.minpts}
   * </p>
   */
  public static final OptionID MINPTS_ID = new OptionID("subclu.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_ID}.
   */
  private DimensionSelectingSubspaceDistanceFunction<V> distanceFunction;

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private double epsilon;

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds the result;
   */
  private Clustering<SubspaceModel> result;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public SUBCLU(DimensionSelectingSubspaceDistanceFunction<V> distanceFunction, double epsilon, int minpts) {
    super();
    this.distanceFunction = distanceFunction;
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the SUBCLU algorithm on the given database.
   * 
   * @param relation Relation to process
   * @return Clustering result
   */
  public Clustering<SubspaceModel> run(Relation<V> relation) {
    final int dimensionality = RelationUtil.dimensionality(relation);

    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(dimensionality) : null;

    // Generate all 1-dimensional clusters
    LOG.beginStep(stepprog, 1, "Generate all 1-dimensional clusters.");

    // mapping of dimensionality to set of subspaces
    HashMap<Integer, List<Subspace>> subspaceMap = new HashMap<>();

    // list of 1-dimensional subspaces containing clusters
    List<Subspace> s_1 = new ArrayList<>();
    subspaceMap.put(0, s_1);

    // mapping of subspaces to list of clusters
    TreeMap<Subspace, List<Cluster<Model>>> clusterMap = new TreeMap<>(new Subspace.DimensionComparator());

    for(int d = 0; d < dimensionality; d++) {
      Subspace currentSubspace = new Subspace(d);
      List<Cluster<Model>> clusters = runDBSCAN(relation, null, currentSubspace);

      if(LOG.isDebuggingFiner()) {
        StringBuilder msg = new StringBuilder();
        msg.append('\n').append(clusters.size()).append(" clusters in subspace ").append(currentSubspace.dimensonsToString()).append(": \n");
        for(Cluster<Model> cluster : clusters) {
          msg.append("      " + cluster.getIDs() + "\n");
        }
        LOG.debugFiner(msg.toString());
      }

      if(!clusters.isEmpty()) {
        s_1.add(currentSubspace);
        clusterMap.put(currentSubspace, clusters);
      }
    }

    // Generate (d+1)-dimensional clusters from d-dimensional clusters
    for(int d = 0; d < dimensionality - 1; d++) {
      if(stepprog != null) {
        stepprog.beginStep(d + 2, "Generate " + (d + 2) + "-dimensional clusters from " + (d + 1) + "-dimensional clusters.", LOG);
      }

      List<Subspace> subspaces = subspaceMap.get(d);
      if(subspaces == null || subspaces.isEmpty()) {
        if(stepprog != null) {
          for(int dim = d + 1; dim < dimensionality - 1; dim++) {
            stepprog.beginStep(dim + 2, "Generation of" + (dim + 2) + "-dimensional clusters not applicable, because no more " + (d + 2) + "-dimensional subspaces found.", LOG);
          }
        }
        break;
      }

      List<Subspace> candidates = generateSubspaceCandidates(subspaces);
      List<Subspace> s_d = new ArrayList<>();

      for(Subspace candidate : candidates) {
        Subspace bestSubspace = bestSubspace(subspaces, candidate, clusterMap);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("best subspace of " + candidate.dimensonsToString() + ": " + bestSubspace.dimensonsToString());
        }

        List<Cluster<Model>> bestSubspaceClusters = clusterMap.get(bestSubspace);
        List<Cluster<Model>> clusters = new ArrayList<>();
        for(Cluster<Model> cluster : bestSubspaceClusters) {
          List<Cluster<Model>> candidateClusters = runDBSCAN(relation, cluster.getIDs(), candidate);
          if(!candidateClusters.isEmpty()) {
            clusters.addAll(candidateClusters);
          }
        }

        if(LOG.isDebuggingFine()) {
          StringBuilder msg = new StringBuilder();
          msg.append(clusters.size() + " cluster(s) in subspace " + candidate + ": \n");
          for(Cluster<Model> c : clusters) {
            msg.append("      " + c.getIDs() + "\n");
          }
          LOG.debugFine(msg.toString());
        }

        if(!clusters.isEmpty()) {
          s_d.add(candidate);
          clusterMap.put(candidate, clusters);
        }
      }

      if(!s_d.isEmpty()) {
        subspaceMap.put(d + 1, s_d);
      }
    }

    // build result
    int numClusters = 1;
    result = new Clustering<>("SUBCLU clustering", "subclu-clustering");
    for(Subspace subspace : clusterMap.descendingKeySet()) {
      List<Cluster<Model>> clusters = clusterMap.get(subspace);
      for(Cluster<Model> cluster : clusters) {
        Cluster<SubspaceModel> newCluster = new Cluster<>(cluster.getIDs());
        newCluster.setModel(new SubspaceModel(subspace, Centroid.make(relation, cluster.getIDs())));
        newCluster.setName("cluster_" + numClusters++);
        result.addToplevelCluster(newCluster);
      }
    }

    LOG.setCompleted(stepprog);
    return result;
  }

  /**
   * Returns the result of the algorithm.
   * 
   * @return the result of the algorithm
   */
  public Clustering<SubspaceModel> getResult() {
    return result;
  }

  /**
   * Runs the DBSCAN algorithm on the specified partition of the database in the
   * given subspace. If parameter {@code ids} is null DBSCAN will be applied to
   * the whole database.
   * 
   * @param relation the database holding the objects to run DBSCAN on
   * @param ids the IDs of the database defining the partition to run DBSCAN on
   *        - if this parameter is null DBSCAN will be applied to the whole
   *        database
   * @param subspace the subspace to run DBSCAN on
   * @return the clustering result of the DBSCAN run
   */
  private List<Cluster<Model>> runDBSCAN(Relation<V> relation, DBIDs ids, Subspace subspace) {
    // distance function
    distanceFunction.setSelectedDimensions(subspace.getDimensions());

    ProxyDatabase proxy;
    if(ids == null) {
      // TODO: in this case, we might want to use an index - the proxy below
      // will prevent this!
      ids = relation.getDBIDs();
    }

    proxy = new ProxyDatabase(ids, relation);

    DBSCAN<V> dbscan = new DBSCAN<>(distanceFunction, epsilon, minpts);
    // run DBSCAN
    if(LOG.isVerbose()) {
      LOG.verbose("\nRun DBSCAN on subspace " + subspace.dimensonsToString());
    }
    Clustering<Model> dbsres = dbscan.run(proxy);

    // separate cluster and noise
    List<Cluster<Model>> clusterAndNoise = dbsres.getAllClusters();
    List<Cluster<Model>> clusters = new ArrayList<>();
    for(Cluster<Model> c : clusterAndNoise) {
      if(!c.isNoise()) {
        clusters.add(c);
      }
    }
    return clusters;
  }

  /**
   * Generates {@code d+1}-dimensional subspace candidates from the specified
   * {@code d}-dimensional subspaces.
   * 
   * @param subspaces the {@code d}-dimensional subspaces
   * @return the {@code d+1}-dimensional subspace candidates
   */
  private List<Subspace> generateSubspaceCandidates(List<Subspace> subspaces) {
    List<Subspace> candidates = new ArrayList<>();

    if(subspaces.isEmpty()) {
      return candidates;
    }

    // Generate (d+1)-dimensional candidate subspaces
    int d = subspaces.get(0).dimensionality();

    StringBuilder msgFine = new StringBuilder("\n");
    if(LOG.isDebuggingFiner()) {
      msgFine.append("subspaces ").append(subspaces).append('\n');
    }

    for(int i = 0; i < subspaces.size(); i++) {
      Subspace s1 = subspaces.get(i);
      for(int j = i + 1; j < subspaces.size(); j++) {
        Subspace s2 = subspaces.get(j);
        Subspace candidate = s1.join(s2);

        if(candidate != null) {
          if(LOG.isDebuggingFiner()) {
            msgFine.append("candidate: ").append(candidate.dimensonsToString()).append('\n');
          }
          // prune irrelevant candidate subspaces
          List<Subspace> lowerSubspaces = lowerSubspaces(candidate);
          if(LOG.isDebuggingFiner()) {
            msgFine.append("lowerSubspaces: ").append(lowerSubspaces).append('\n');
          }
          boolean irrelevantCandidate = false;
          for(Subspace s : lowerSubspaces) {
            if(!subspaces.contains(s)) {
              irrelevantCandidate = true;
              break;
            }
          }
          if(!irrelevantCandidate) {
            candidates.add(candidate);
          }
        }
      }
    }

    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner(msgFine.toString());
    }
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append(d + 1).append("-dimensional candidate subspaces: ");
      for(Subspace candidate : candidates) {
        msg.append(candidate.dimensonsToString()).append(' ');
      }
      LOG.debug(msg.toString());
    }

    return candidates;
  }

  /**
   * Returns the list of all {@code (d-1)}-dimensional subspaces of the
   * specified {@code d}-dimensional subspace.
   * 
   * @param subspace the {@code d}-dimensional subspace
   * @return a list of all {@code (d-1)}-dimensional subspaces
   */
  private List<Subspace> lowerSubspaces(Subspace subspace) {
    int dimensionality = subspace.dimensionality();
    if(dimensionality <= 1) {
      return null;
    }

    // order result according to the dimensions
    List<Subspace> result = new ArrayList<>();
    long[] dimensions = subspace.getDimensions();
    for(int dim = BitsUtil.nextSetBit(dimensions, 0); dim >= 0; dim = BitsUtil.nextSetBit(dimensions, dim + 1)) {
      long[] newDimensions = dimensions.clone();
      BitsUtil.clearI(newDimensions, dim);
      result.add(new Subspace(newDimensions));
    }

    return result;
  }

  /**
   * Determines the {@code d}-dimensional subspace of the {@code (d+1)}
   * -dimensional candidate with minimal number of objects in the cluster.
   * 
   * @param subspaces the list of {@code d}-dimensional subspaces containing
   *        clusters
   * @param candidate the {@code (d+1)}-dimensional candidate subspace
   * @param clusterMap the mapping of subspaces to clusters
   * @return the {@code d}-dimensional subspace of the {@code (d+1)}
   *         -dimensional candidate with minimal number of objects in the
   *         cluster
   */
  private Subspace bestSubspace(List<Subspace> subspaces, Subspace candidate, TreeMap<Subspace, List<Cluster<Model>>> clusterMap) {
    Subspace bestSubspace = null;

    for(Subspace subspace : subspaces) {
      int min = Integer.MAX_VALUE;

      if(subspace.isSubspace(candidate)) {
        List<Cluster<Model>> clusters = clusterMap.get(subspace);
        for(Cluster<Model> cluster : clusters) {
          int clusterSize = cluster.size();
          if(clusterSize < min) {
            min = clusterSize;
            bestSubspace = subspace;
          }
        }
      }
    }

    return bestSubspace;
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    protected int minpts = 0;

    protected double epsilon;

    protected DimensionSelectingSubspaceDistanceFunction<V> distance = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DimensionSelectingSubspaceDistanceFunction<V>> param = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DimensionSelectingSubspaceDistanceFunction.class, SubspaceEuclideanDistanceFunction.class);
      if(config.grab(param)) {
        distance = param.instantiateClass(config);
      }

      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID);
      minptsP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected SUBCLU<V> makeInstance() {
      return new SUBCLU<>(distance, epsilon, minpts);
    }
  }
}
