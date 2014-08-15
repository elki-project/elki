package experimentalcode.students.baierst.thesis;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the Desnity-Based Clustering Validation Index.
 * 
 * Reference:
 * <p>
 * D. Moulavi et al.<br />
 * Density-Based Clustering Validation<br />
 * In: Proceedings of the 14th SIAM International Conference on Data Mining
 * (SDM), Philadelphia, 2014
 * </p>
 * 
 * 
 * @author Stephan Baier
 * 
 * @param <O> Object type
 */
public class EvaluateDBCV<O> implements Evaluator {

  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateDBCV.class);

  /**
   * Distance function to use.
   */
  private PrimitiveDistanceFunction<? super NumberVector> distanceFunction;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   */
  public EvaluateDBCV(PrimitiveDistanceFunction<? super NumberVector> distance) {
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
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> cl) {

    List<? extends Cluster<?>> clusters = cl.getAllClusters();

    // precompute all core distances
    List<Double[]> coreDists = new ArrayList<Double[]>();
    for(Cluster<?> cluster : clusters) {
      // Singletons are considered as Noise, because they have no sparseness
      if(cluster.isNoise() || cluster.size() < 2) {
        coreDists.add(null);
        continue;
      }
      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      Double[] clusterCoreDists = new Double[ids.size()];
      for(int i = 0; i < ids.size(); i++) {
        double currentCoreDist = 0;
        NumberVector currentVector = rel.get(ids.get(i));
        int dim = currentVector.getDimensionality();
        DBIDArrayIter it = ids.iter();
        for(it.seek(0); it.valid(); it.advance()) {
          if(DBIDUtil.equal(ids.get(i), it)) {
            continue;
          }
          currentCoreDist += Math.pow((1. / distanceFunction.distance(currentVector, rel.get(it))), dim);
        }
        currentCoreDist = Math.pow((currentCoreDist / (cluster.size() - 1)), -1. / dim);
        clusterCoreDists[i] = currentCoreDist;
      }
      coreDists.add(clusterCoreDists);
    }

    // compute density sparseness of all clusters
    List<Integer[]> clusterDegrees = new ArrayList<Integer[]>();
    List<Double> clusterDscMax = new ArrayList<Double>();
    boolean[] internalEdges = new boolean[clusters.size()]; // describes if a
                                                            // cluster contains
                                                            // any internal
                                                            // edges
    for(int c = 0; c < clusters.size(); c++) {
      Cluster<?> cluster = clusters.get(c);
      if(cluster.isNoise() || cluster.size() < 2) {
        clusterDegrees.add(null);
        clusterDscMax.add(null);
        continue;
      }
      Double[] clusterCoreDists = coreDists.get(c);
      ArrayDBIDs ids = DBIDUtil.ensureArray(clusters.get(c).getIDs());
      double dscMax = 0; // Density Sparseness of the Cluster
      double[][] distances = new double[cluster.size()][cluster.size()];

      // create mutability distance matrix for Minimum Spanning Tree
      for(int i = 0; i < ids.size(); i++) {
        NumberVector currentVector = rel.get(ids.get(i));
        double currentCoreDist = clusterCoreDists[i];
        for(int j = i + 1; j < ids.size(); j++) {
          double mutualReachDist = Math.max(Math.max(currentCoreDist, clusterCoreDists[j]), distanceFunction.distance(currentVector, rel.get(ids.get(j))));
          distances[i][j] = mutualReachDist;
          distances[j][i] = mutualReachDist;
        }
      }

      // generate Minimum Spanning Tree
      int[] nodes = PrimsMinimumSpanningTree.processDense(distances);

      // get degree of all nodes in the spanning tree
      Integer[] degree = new Integer[cluster.size()];
      for(int i = 0; i < nodes.length; i++) {
        if(degree[nodes[i]] != null) {
          degree[nodes[i]]++;
        }
        else {
          degree[nodes[i]] = 1;
        }
      }
      // check if cluster contains any internal edges
      for(int i = 0; i < nodes.length; i=i+2) {
        if(degree[nodes[i]] > 1 && degree[nodes[i+1]] > 1) {
          internalEdges[c] = true;
        }
      }
      
      clusterDegrees.add(degree);

      // find maximum sparseness in the Minimum Spanning Tree
      for(int i = 0; i < nodes.length; i = i + 2) {
        if(distances[nodes[i]][nodes[i + 1]] > dscMax) {
          // We only consider edges where both vertices are internal nodes. If a
          // cluster has no internal nodes we consider all edges.
          if((internalEdges[c] && degree[nodes[i]] > 1 && degree[nodes[i + 1]] > 1) || (!internalEdges[c])) {
            dscMax = distances[nodes[i]][nodes[i + 1]];
          }
        }
      }
      clusterDscMax.add(dscMax);
    }

    // compute density separation of all clusters
    double dbcv = 0;
    for(int c = 0; c < clusters.size(); c++) {
      Cluster<?> cluster = clusters.get(c);
      if(cluster.isNoise() || cluster.size() < 2) {
        continue;
      }
      Double currentDscMax = clusterDscMax.get(c);
      double dspcMin = Double.POSITIVE_INFINITY; // minimal Density Separation
                                                 // of the Cluster
      ArrayDBIDs ids = DBIDUtil.ensureArray(clusters.get(c).getIDs());

      Double[] clusterCoreDists = coreDists.get(c);
      Integer[] currentDegree = clusterDegrees.get(c);

      for(int i = 0; i < ids.size(); i++) {
        // We again ignore external nodes, if the cluster has any internal
        // nodes.
        if(currentDegree[i] < 2 && internalEdges[c]) {
          continue;
        }
        NumberVector currentVector = rel.get(ids.get(i));
        double currentCoreDist = clusterCoreDists[i];
        for(int oc = 0; oc < clusters.size(); oc++) {
          Cluster<?> ocluster = clusters.get(oc);
          if(ocluster.isNoise() || ocluster.size() < 2) {
            continue;
          }
          if(cluster == ocluster) {
            continue;
          }
          Integer[] oDegree = clusterDegrees.get(oc);
          Double[] oclusterCoreDists = coreDists.get(oc);
          ArrayDBIDs oids = DBIDUtil.ensureArray(clusters.get(oc).getIDs());
          for(int j = 0; j < oids.size(); j++) {
            if(oDegree[j] < 2 && internalEdges[oc]) {
              continue;
            }
            double mutualReachDist = Math.max(Math.max(currentCoreDist, oclusterCoreDists[j]), distanceFunction.distance(currentVector, rel.get(oids.get(j))));
            if(mutualReachDist < dspcMin) {
              dspcMin = mutualReachDist;
            }
          }
        }
      }

      // compute DBCV
      double vc = (dspcMin - currentDscMax) / Math.max(dspcMin, currentDscMax);
      double weight = (double) cluster.size() / (double) rel.size();
      dbcv += weight * vc;
    }

    if(LOG.isVerbose()) {
      LOG.verbose("DBCV: " + dbcv);
    }
    
    return dbcv;

  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<? extends NumberVector> rel = db.getRelation(this.distanceFunction.getInputTypeRestriction());

    for(Clustering<?> cl : crs) {
      double dbcv = evaluateClustering(db, (Relation<? extends NumberVector>) rel, cl);
      
      // Build a primitive result attachment:
      Collection<DoubleVector> col = new ArrayList<>();
      col.add(new DoubleVector(new double[] { dbcv }));
      db.getHierarchy().add(cl, new CollectionResult<>("Density Based Clustering Validation", "dbcv", col));
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Stephan Baier
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("dbcv.distance", "Distance function to use for computing the dbcv.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
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
