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
package de.lmu.ifi.dbs.elki.evaluation.clustering.internal;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

/**
 * Compute the Density-Based Clustering Validation Index.
 * <p>
 * Reference:
 * <p>
 * Davoud Moulavi, Pablo A. Jaskowiak, Ricardo J. G. B. Campello, Arthur Zimek,
 * Jörg Sander<br>
 * Density-Based Clustering Validation<br>
 * In: Proc. 14th SIAM International Conference on Data Mining (SDM).
 *
 * @author Stephan Baier
 * @since 0.7.5
 *
 * @param <O> Object type
 *
 * @assoc - analyzes - Clustering
 */
@Reference(authors = "Davoud Moulavi, Pablo A. Jaskowiak, Ricardo J. G. B. Campello, Arthur Zimek, Jörg Sander", //
    title = "Density-Based Clustering Validation", //
    booktitle = "Proc. 14th SIAM International Conference on Data Mining (SDM)", //
    url = "https://doi.org/10.1137/1.9781611973440.96", //
    bibkey = "DBLP:conf/sdm/MoulaviJCZS14")
public class EvaluateDBCV<O> implements Evaluator {
  /**
   * Distance function to use.
   */
  private DistanceFunction<? super O> distanceFunction;

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  public EvaluateDBCV(DistanceFunction<? super O> distance) {
    super();
    this.distanceFunction = distance;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param cl Clustering
   *
   * @return dbcv DBCV-index
   */
  public double evaluateClustering(Database db, Relation<O> rel, Clustering<?> cl) {
    final DistanceQuery<O> dq = rel.getDistanceQuery(distanceFunction);

    List<? extends Cluster<?>> clusters = cl.getAllClusters();
    final int numc = clusters.size();

    // DBCV needs a "dimensionality".
    @SuppressWarnings("unchecked")
    final Relation<? extends SpatialComparable> vrel = (Relation<? extends SpatialComparable>) rel;
    final int dim = RelationUtil.dimensionality(vrel);

    // precompute all core distances
    ArrayDBIDs[] cids = new ArrayDBIDs[numc];
    double[][] coreDists = new double[numc][];
    for(int c = 0; c < numc; c++) {
      Cluster<?> cluster = clusters.get(c);
      // Singletons are considered as Noise, because they have no sparseness
      if(cluster.isNoise() || cluster.size() < 2) {
        coreDists[c] = null;
        continue;
      }
      // Store for use below:
      ArrayDBIDs ids = cids[c] = DBIDUtil.ensureArray(cluster.getIDs());
      double[] clusterCoreDists = coreDists[c] = new double[ids.size()];
      for(DBIDArrayIter it = ids.iter(), it2 = ids.iter(); it.valid(); it.advance()) {
        double currentCoreDist = 0;
        int neighbors = 0;
        for(it2.seek(0); it2.valid(); it2.advance()) {
          if(DBIDUtil.equal(it, it2)) {
            continue;
          }
          double dist = dq.distance(it, it2);
          // Unfortunately, the DBCV definition has a division by zero.
          // We ignore such objects.
          if(dist > 0) {
            currentCoreDist += MathUtil.powi(1. / dist, dim);
            ++neighbors;
          }
        }
        // Average, and undo power.
        clusterCoreDists[it.getOffset()] = FastMath.pow(currentCoreDist / neighbors, -1. / dim);
      }
    }

    // compute density sparseness of all clusters
    int[][] clusterDegrees = new int[numc][];
    double[] clusterDscMax = new double[numc];
    // describes if a cluster contains any internal edges
    boolean[] internalEdges = new boolean[numc];
    for(int c = 0; c < numc; c++) {
      Cluster<?> cluster = clusters.get(c);
      if(cluster.isNoise() || cluster.size() < 2) {
        clusterDegrees[c] = null;
        clusterDscMax[c] = Double.NaN;
        continue;
      }
      double[] clusterCoreDists = coreDists[c];
      ArrayDBIDs ids = cids[c];
      double dscMax = 0; // Density Sparseness of the Cluster
      double[][] distances = new double[cluster.size()][cluster.size()];

      // create mutability distance matrix for Minimum Spanning Tree
      for(DBIDArrayIter it = ids.iter(), it2 = ids.iter(); it.valid(); it.advance()) {
        double currentCoreDist = clusterCoreDists[it.getOffset()];
        for(it2.seek(it.getOffset() + 1); it2.valid(); it2.advance()) {
          double mutualReachDist = MathUtil.max(currentCoreDist, clusterCoreDists[it2.getOffset()], dq.distance(it, it2));
          distances[it.getOffset()][it2.getOffset()] = mutualReachDist;
          distances[it2.getOffset()][it.getOffset()] = mutualReachDist;
        }
      }

      // generate Minimum Spanning Tree
      int[] nodes = PrimsMinimumSpanningTree.processDense(distances);

      // get degree of all nodes in the spanning tree
      int[] degree = new int[cluster.size()];
      for(int i = 0; i < nodes.length; i++) {
        degree[nodes[i]]++;
      }
      // check if cluster contains any internal edges
      for(int i = 0, e = nodes.length - 1; i < e; i += 2) {
        if(degree[nodes[i]] > 1 && degree[nodes[i + 1]] > 1) {
          internalEdges[c] = true;
        }
      }

      clusterDegrees[c] = degree;

      // find maximum sparseness in the Minimum Spanning Tree
      for(int i = 0, e = nodes.length - 1; i < e; i += 2) {
        final int n1 = nodes[i], n2 = nodes[i + 1];
        // We only consider edges where both vertices are internal nodes.
        // If a cluster has no internal nodes we consider all edges.
        if(distances[n1][n2] > dscMax && (!internalEdges[c] || (degree[n1] > 1 && degree[n2] > 1))) {
          dscMax = distances[n1][n2];
        }
      }
      clusterDscMax[c] = dscMax;
    }

    // compute density separation of all clusters
    double dbcv = 0;
    for(int c = 0; c < numc; c++) {
      Cluster<?> cluster = clusters.get(c);
      if(cluster.isNoise() || cluster.size() < 2) {
        continue;
      }
      double currentDscMax = clusterDscMax[c];
      double[] clusterCoreDists = coreDists[c];
      int[] currentDegree = clusterDegrees[c];

      // minimal Density Separation of the Cluster
      double dspcMin = Double.POSITIVE_INFINITY;
      for(DBIDArrayIter it = cids[c].iter(); it.valid(); it.advance()) {
        // We again ignore external nodes, if the cluster has any internal
        // nodes.
        if(currentDegree[it.getOffset()] < 2 && internalEdges[c]) {
          continue;
        }
        double currentCoreDist = clusterCoreDists[it.getOffset()];
        for(int oc = 0; oc < numc; oc++) {
          Cluster<?> ocluster = clusters.get(oc);
          if(ocluster.isNoise() || ocluster.size() < 2 || cluster == ocluster) {
            continue;
          }
          int[] oDegree = clusterDegrees[oc];
          double[] oclusterCoreDists = coreDists[oc];
          for(DBIDArrayIter it2 = cids[oc].iter(); it2.valid(); it2.advance()) {
            if(oDegree[it2.getOffset()] < 2 && internalEdges[oc]) {
              continue;
            }
            double mutualReachDist = MathUtil.max(currentCoreDist, oclusterCoreDists[it2.getOffset()], dq.distance(it, it2));
            dspcMin = mutualReachDist < dspcMin ? mutualReachDist : dspcMin;
          }
        }
      }

      // compute DBCV
      double vc = (dspcMin - currentDscMax) / MathUtil.max(dspcMin, currentDscMax);
      double weight = cluster.size() / (double) rel.size();
      dbcv += weight * vc;
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), cl, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Density Based Clustering Validation", dbcv, 0., Double.POSITIVE_INFINITY, 0., true);
    db.getHierarchy().resultChanged(ev);
    return dbcv;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(newResult);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(hier);
    TypeInformation typ = new CombinedTypeInformation(this.distanceFunction.getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
    Relation<O> rel = db.getRelation(typ);

    if(rel != null) {
      for(Clustering<?> cl : crs) {
        evaluateClustering(db, rel, cl);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("dbcv.distance", "Distance function to use for computing the dbcv.");

    /**
     * Distance function to use.
     */
    private DistanceFunction<? super O> distance;

    @Override
    protected void makeOptions(Parameterization config) {
      ObjectParameter<DistanceFunction<? super O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }
    }

    @Override
    protected EvaluateDBCV<O> makeInstance() {
      return new EvaluateDBCV<>(distance);
    }
  }
}
