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
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * DBSCAN provides the DBSCAN algorithm, an algorithm to find density-connected
 * sets in a database.
 * <p>
 * Reference: <br>
 * M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: A Density-Based Algorithm for
 * Discovering Clusters in Large Spatial Databases with Noise. <br>
 * In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96),
 * Portland, OR, 1996.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <O> the type of Object the algorithm is applied to
 * @param <D> the type of Distance used
 */
@Title("DBSCAN: Density-Based Clustering of Applications with Noise")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minpts' and 'epsilon' (specifying a volume). " + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu", title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996", url = "http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.71.1980")
public class DBSCAN<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DBSCAN.class);

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("dbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private D epsilon;

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("dbscan.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  protected int minpts;

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
   * Constructor with parameters.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts parameter
   */
  public DBSCAN(DistanceFunction<? super O, D> distanceFunction, D epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the DBSCAN algorithm on the given database.
   */
  public Clustering<Model> run(Relation<O> relation) {
    RangeQuery<O, D> rangeQuery = QueryUtil.getRangeQuery(relation, getDistanceFunction());
    final int size = relation.size();

    FiniteProgress objprog = logger.isVerbose() ? new FiniteProgress("Processing objects", size, logger) : null;
    IndefiniteProgress clusprog = logger.isVerbose() ? new IndefiniteProgress("Number of clusters", logger) : null;
    resultList = new ArrayList<ModifiableDBIDs>();
    noise = DBIDUtil.newHashSet();
    processedIDs = DBIDUtil.newHashSet(size);
    if(size >= minpts) {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(!processedIDs.contains(iditer)) {
          expandCluster(relation, rangeQuery, DBIDUtil.deref(iditer), objprog, clusprog);
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
        }
        if(processedIDs.size() == size) {
          break;
        }
      }
    }
    else {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        noise.add(iditer);
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(noise.size(), logger);
          clusprog.setProcessed(resultList.size(), logger);
        }
      }
    }
    // Finish progress logging
    if(objprog != null) {
      objprog.ensureCompleted(logger);
    }
    if(clusprog != null) {
      clusprog.setCompleted(logger);
    }

    Clustering<Model> result = new Clustering<Model>("DBSCAN Clustering", "dbscan-clustering");
    for(ModifiableDBIDs res : resultList) {
      Cluster<Model> c = new Cluster<Model>(res, ClusterModel.CLUSTER);
      result.addCluster(c);
    }

    Cluster<Model> n = new Cluster<Model>(noise, true, ClusterModel.CLUSTER);
    result.addCluster(n);

    return result;
  }

  /**
   * DBSCAN-function expandCluster.
   * <p/>
   * Border-Objects become members of the first possible cluster.
   * 
   * @param relation Database relation to run on
   * @param rangeQuery Range query to use
   * @param startObjectID potential seed of a new potential cluster
   * @param objprog the progress object for logging the current status
   */
  protected void expandCluster(Relation<O> relation, RangeQuery<O, D> rangeQuery, DBID startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    List<DistanceResultPair<D>> seeds = rangeQuery.getRangeForDBID(startObjectID, epsilon);

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
    for(DistanceResultPair<D> seed : seeds) {
      if(!processedIDs.contains(seed)) {
        currentCluster.add(seed);
        processedIDs.add(seed);
      }
      else if(noise.contains(seed)) {
        currentCluster.add(seed);
        noise.remove(seed);
      }
    }
    seeds.remove(0);

    while(seeds.size() > 0) {
      DistanceResultPair<D> o = seeds.remove(0);
      List<DistanceResultPair<D>> neighborhood = rangeQuery.getRangeForDBID(o, epsilon);

      if(neighborhood.size() >= minpts) {
        for(DistanceResultPair<D> neighbor : neighborhood) {
          boolean inNoise = noise.contains(neighbor);
          boolean unclassified = !processedIDs.contains(neighbor);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(neighbor);
            }
            currentCluster.add(neighbor);
            processedIDs.add(neighbor);
            if(inNoise) {
              noise.remove(neighbor);
            }
          }
        }
      }

      if(processedIDs.size() == relation.size() && noise.size() == 0) {
        break;
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), logger);
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters, logger);
      }
    }
    if(currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      noise.addDBIDs(currentCluster);
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
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
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected D epsilon = null;

    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DistanceParameter<D> epsilonP = new DistanceParameter<D>(EPSILON_ID, distanceFunction);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID);
      minptsP.addConstraint(new GreaterConstraint(0));
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected DBSCAN<O, D> makeInstance() {
      return new DBSCAN<O, D>(distanceFunction, epsilon, minpts);
    }
  }
}