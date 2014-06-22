package experimentalcode.students.goldschwendt;

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

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

/**
 * Base class for evaluating clusterings by information criterions (such as AIC or BIC).
 * Provides helper functions (e.g. max likelihood calculation) to its subclasses.
 * 
 * @author Tibor Goldschwendt
 *
 * @param <V>
 * @param <M>
 */
public abstract class InformationCriterion<V extends NumberVector, M extends MeanModel> {

  /**
   * Evaluates the clustering 
   * 
   * @param relation
   * @param clustering
   * @param distanceFunction
   * @return The evaluation. Higher value means better fit.
   */
  abstract public double evaluate(Relation<V> relation, Clustering<M> clustering, DistanceFunction<? super V> distanceFunction);
  
  /**
   * Computes max likelihood of a single cluster of a clustering
   * 
   * @param relation
   * @param clustering
   * @param cluster
   * @param distanceFunction
   * @return
   */
  protected double maxLikelihoodCluster(Relation<V> relation, Clustering<M> clustering, Cluster<M> cluster, DistanceFunction<? super V> distanceFunction) {
    
    NumberVector.Factory<V> factory = RelationUtil.getNumberVectorFactory(relation);
    
    // number of data points in this cluster
    int n_i = cluster.size();
    
    // number of clusters
    int m = clustering.getAllClusters().size();
    
    // center of this cluster
    V c_i = factory.newNumberVector(cluster.getModel().getMean());
    
    // TODO: best way to get distance?
    DistanceQuery<V> distanceQuery = relation.getDatabase().getDistanceQuery(relation, distanceFunction);
    
    // max likelihood of this cluster
    double maxLikelihood_i = 0;
    for (DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
      V x_j = relation.get(iter);
      maxLikelihood_i += Math.pow(distanceQuery.distance(x_j, c_i), 2);
      
    }
    maxLikelihood_i /= n_i - m;
    
    return maxLikelihood_i;
  }
  
  /**
   * Computes log likelihood of a single cluster of a clustering
   *
   * @param relation
   * @param clustering
   * @param cluster
   * @return
   */
  protected double logLikelihoodCluster(Relation<V> relation, Clustering<M> clustering, Cluster<M> cluster, DistanceFunction<? super V> distanceFunction) {
    
    // number of all data points
    int n = 0;
    for (Cluster<M> aCluster : clustering.getAllClusters()) {
      n += aCluster.size();
    }
    
    // number of data points in this cluster
    int n_i = cluster.size();
    
    // number of clusters
    int m = clustering.getAllClusters().size();
  
    // dimensionality of data points
    int d = RelationUtil.dimensionality(relation);
    
    // likelihood of this cluster
    double logLikelihood_i =
        n_i * Math.log(n_i) -
        n_i * Math.log(n) -
        ((n_i * d) / 2) * Math.log(2 * Math.PI) -
        (n_i / 2) * Math.log(maxLikelihoodCluster(relation, clustering, cluster, distanceFunction)) -
        (n_i - m) / 2;
    
    return logLikelihood_i;
  }

  /**
   * Computes log likelihood of an entire clustering
   * 
   * @param relation
   * @param clustering
   * @return
   */
  protected double logLikelihoodClustering(Relation<V> relation, Clustering<M> clustering, DistanceFunction<? super V> distanceFunction) {
    
    // log likelihood of this clustering
    double logLikelihood = 0.0;
    
    // add up the log-likelihood of all clusters
    for (Cluster<M> cluster : clustering.getAllClusters()) {
      logLikelihood += logLikelihoodCluster(relation, clustering, cluster, distanceFunction);
    }
    
    return logLikelihood;
  }
}
