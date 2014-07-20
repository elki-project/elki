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
 * Implementation of likelihood functions taken from:
 * <p>
 * Qinpei Zhao, Mantao Xu, Pasi Fränti:<br />
 * Knee Point Detection on Bayesian Information Criterion<br />
 * 20th IEEE International Conference on Tools with Artificial Intelligence (2008): 431-438
 * </p> 
 * and:
 * Wikipedia: http://de.wikipedia.org/wiki/Informationskriterium
 * and the original code
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
  
  protected double squaredDistanceCluster(Relation<V> relation, Clustering<M> clustering, Cluster<M> cluster, DistanceFunction<? super V> distanceFunction) {
    
    NumberVector.Factory<V> factory = RelationUtil.getNumberVectorFactory(relation);
    DistanceQuery<V> distanceQuery = relation.getDatabase().getDistanceQuery(relation, distanceFunction);
    
    // center of this cluster
    V c_i = factory.newNumberVector(cluster.getModel().getMean());
    
    double d_i = 0.0;
    for (DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
      V x_j = relation.get(iter);
      d_i += /*Math.pow(*/distanceQuery.distance(x_j, c_i)/*, 2)*/;
    }
    
    return d_i;
  }
  
  protected double squaredDistanceClustering(Relation<V> relation, Clustering<M> clustering, DistanceFunction<? super V> distanceFunction) {
    double d = 0.0;
    for (Cluster<M> cluster : clustering.getAllClusters()) {
      d += squaredDistanceCluster(relation, clustering, cluster, distanceFunction);
    }
    return d;
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
    
    /*
     * TODO:
     * Store or compute number of points in the clustering
     */
    // number of all data points
    int n = 0; // owned
    for (Cluster<M> aCluster : clustering.getAllClusters()) {
      n += aCluster.size();
    }
    
    // number of data points in this cluster
    int n_i = cluster.size(); // owned_curr
    
    // number of clusters
    int m = clustering.getAllClusters().size(); // num_ctrs
  
    // dimensionality of data points
    int dim = RelationUtil.dimensionality(relation); // num_dims
    
    /* dist_curr: sum([x_i - u_i]^2) */
    double d_i = squaredDistanceCluster(relation, clustering, cluster, distanceFunction);  // dist_curr
    
    // Squared distance of all point to its nearest center
    double d = squaredDistanceClustering(relation, clustering, distanceFunction); // dist_all
    
    // variance
    /* variance_all = dist_all/(1.0*owned - num_ctrs); */
    double v = d / (n - m); // variance_all
    
    /* Excerpt of original implementation 
    double loglik_curr =
      owned_curr*log(owned_curr)
      - owned_curr*log(owned)
      - owned_curr/2.0*log(2*PI)
      - owned_curr*num_dims/2.0*log(variance_all)
      - dist_curr/(2*variance_all); */
    
    // likelihood of this cluster
    double logLikelihood_i = 0.0;
    
    if (n > 1) {
      logLikelihood_i =
          n_i * Math.log(n_i)
          - n_i * Math.log(n)
          - (n_i / 2.0) * Math.log(2 * Math.PI)
          - n_i * dim / 2.0 * Math.log(v)
          - d_i / (2*v);
    }
    
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
      double loglikelihood_i = logLikelihoodCluster(relation, clustering, cluster, distanceFunction);
          
      logLikelihood += loglikelihood_i;
    }
    
    return logLikelihood;
  }
  
  protected int numberOfFreeParameters(Relation<V> relation, Clustering<M> clustering) {

    // number of clusters
    int m = clustering.getAllClusters().size(); // num_ctrs
    
    // dimensionality of data points
    int dim = RelationUtil.dimensionality(relation); // num_dims
    
    // number of free parameters
    /* int num_parameters = (num_ctrs - 1) + // probabilities
        num_ctrs * num_dims +   // means
        num_ctrs;         // variance parms */
    int p =
        (m - 1)
        + m * dim
        + m;
    
    return p;
  }
}
