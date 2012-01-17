package de.lmu.ifi.dbs.elki.algorithm.clustering;

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
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Shared nearest neighbor clustering.
 * </p>
 * <p>
 * Reference: L. Ertöz, M. Steinbach, V. Kumar: Finding Clusters of Different
 * Sizes, Shapes, and Densities in Noisy, High Dimensional Data. <br>
 * In: Proc. of SIAM Data Mining (SDM), 2003.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses SharedNearestNeighborSimilarityFunction
 * 
 * @param <O> the type of Object the algorithm is applied on
 */
@Title("SNN: Shared Nearest Neighbor Clustering")
@Description("Algorithm to find shared-nearest-neighbors-density-connected sets in a database based on the " + "parameters 'minPts' and 'epsilon' (specifying a volume). " + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "L. Ertöz, M. Steinbach, V. Kumar", title = "Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High Dimensional Data", booktitle = "Proc. of SIAM Data Mining (SDM), 2003", url = "http://www.siam.org/meetings/sdm03/proceedings/sdm03_05.pdf")
public class SNNClustering<O> extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SNNClustering.class);

  /**
   * Parameter to specify the minimum SNN density, must be an integer greater
   * than 0.
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("snn.epsilon", "The minimum SNN density.");

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private IntegerDistance epsilon;

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-SNN-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("snn.minpts", "Threshold for minimum number of points in " + "the epsilon-SNN-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds a list of clusters found.
   */
  protected List<ModifiableDBIDs> resultList;

  /**
   * Holds a set of noise.
   */
  protected ModifiableDBIDs noise;

  /**
   * Holds a set of processed ids.
   */
  protected ModifiableDBIDs processedIDs;

  /**
   * The similarity function for the shared nearest neighbor similarity.
   */
  private SharedNearestNeighborSimilarityFunction<O> similarityFunction;

  /**
   * Constructor.
   * 
   * @param similarityFunction Similarity function
   * @param epsilon Epsilon
   * @param minpts Minpts
   */
  public SNNClustering(SharedNearestNeighborSimilarityFunction<O> similarityFunction, IntegerDistance epsilon, int minpts) {
    super();
    this.similarityFunction = similarityFunction;
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Perform SNN clustering
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public Clustering<Model> run(Database database, Relation<O> relation) {
    SimilarityQuery<O, IntegerDistance> snnInstance = similarityFunction.instantiate(relation);

    FiniteProgress objprog = logger.isVerbose() ? new FiniteProgress("SNNClustering", relation.size(), logger) : null;
    IndefiniteProgress clusprog = logger.isVerbose() ? new IndefiniteProgress("Number of clusters", logger) : null;
    resultList = new ArrayList<ModifiableDBIDs>();
    noise = DBIDUtil.newHashSet();
    processedIDs = DBIDUtil.newHashSet(relation.size());
    if(relation.size() >= minpts) {
      for(DBID id : snnInstance.getRelation().iterDBIDs()) {
        if(!processedIDs.contains(id)) {
          expandCluster(snnInstance, id, objprog, clusprog);
          if(processedIDs.size() == relation.size() && noise.size() == 0) {
            break;
          }
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
        }
      }
    }
    else {
      for(DBID id : snnInstance.getRelation().iterDBIDs()) {
        noise.add(id);
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(noise.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
        }
      }
    }
    // Finish progress logging
    if(objprog != null && clusprog != null) {
      objprog.ensureCompleted(logger);
      clusprog.setCompleted(logger);
    }

    Clustering<Model> result = new Clustering<Model>("Shared-Nearest-Neighbor Clustering", "snn-clustering");
    for(Iterator<ModifiableDBIDs> resultListIter = resultList.iterator(); resultListIter.hasNext();) {
      result.addCluster(new Cluster<Model>(resultListIter.next(), ClusterModel.CLUSTER));
    }
    result.addCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));

    return result;
  }

  /**
   * Returns the shared nearest neighbors of the specified query object in the
   * given database.
   * 
   * @param snnInstance shared nearest neighbors
   * @param queryObject the query object
   * @return the shared nearest neighbors of the specified query object in the
   *         given database
   */
  protected ArrayModifiableDBIDs findSNNNeighbors(SimilarityQuery<O, IntegerDistance> snnInstance, DBID queryObject) {
    ArrayModifiableDBIDs neighbors = DBIDUtil.newArray();
    for(DBID id : snnInstance.getRelation().iterDBIDs()) {
      if(snnInstance.similarity(queryObject, id).compareTo(epsilon) >= 0) {
        neighbors.add(id);
      }
    }
    return neighbors;
  }

  /**
   * DBSCAN-function expandCluster adapted to SNN criterion.
   * <p/>
   * <p/>
   * Border-Objects become members of the first possible cluster.
   * 
   * @param snnInstance shared nearest neighbors
   * @param startObjectID potential seed of a new potential cluster
   * @param objprog the progress object to report about the progress of
   *        clustering
   */
  protected void expandCluster(SimilarityQuery<O, IntegerDistance> snnInstance, DBID startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    ArrayModifiableDBIDs seeds = findSNNNeighbors(snnInstance, startObjectID);

    // startObject is no core-object
    if(seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), logger);
        clusprog.setProcessed(resultList.size(), logger);
      }
      return;
    }

    // try to expand the cluster
    ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(DBID seed : seeds) {
      if(!processedIDs.contains(seed)) {
        currentCluster.add(seed);
        processedIDs.add(seed);
      }
      else if(noise.contains(seed)) {
        currentCluster.add(seed);
        noise.remove(seed);
      }
    }

    while(seeds.size() > 0) {
      DBID o = seeds.remove(seeds.size() - 1);
      ArrayModifiableDBIDs neighborhood = findSNNNeighbors(snnInstance, o);

      if(neighborhood.size() >= minpts) {
        for(DBID p : neighborhood) {
          boolean inNoise = noise.contains(p);
          boolean unclassified = !processedIDs.contains(p);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(p);
            }
            currentCluster.add(p);
            processedIDs.add(p);
            if(inNoise) {
              noise.remove(p);
            }
          }
        }
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), logger);
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters, logger);
      }

      if(processedIDs.size() == snnInstance.getRelation().size() && noise.size() == 0) {
        break;
      }
    }
    if(currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      for(DBID id : currentCluster) {
        noise.add(id);
      }
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(similarityFunction.getInputTypeRestriction());
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
   * 
   * @param <O> object type
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    protected IntegerDistance epsilon;

    protected int minpts;

    private SharedNearestNeighborSimilarityFunction<O> similarityFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Class<SharedNearestNeighborSimilarityFunction<O>> cls = ClassGenericsUtil.uglyCastIntoSubclass(SharedNearestNeighborSimilarityFunction.class);
      similarityFunction = config.tryInstantiate(cls);

      DistanceParameter<IntegerDistance> epsilonP = new DistanceParameter<IntegerDistance>(EPSILON_ID, IntegerDistance.FACTORY);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected SNNClustering<O> makeInstance() {
      return new SNNClustering<O>(similarityFunction, epsilon, minpts);
    }
  }
}