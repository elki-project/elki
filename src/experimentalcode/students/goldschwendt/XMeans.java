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

import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.MutableProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 *
 *
 * Reference:<br>
 * <p>
 * Dan Pelleg, Andrew Moore:<br />
 * X-means: Extending K-means with Efficient Estimation on the Number of Clusters<br />
 * International Conference on Machine Learning (ICML) 17: 727-734
 * </p>
 * 
 * @author Tibor Goldschwendt
 * 
 * @param <V> Vector type
 * @param <M> Model type
 */
@Title("X-Means")
//@Description("Finds a partitioning into k clusters.")
//@Reference(authors = "S. Lloyd", title = "Least squares quantization in PCM", booktitle = "IEEE Transactions on Information Theory 28 (2): 129–137.", url = "http://dx.doi.org/10.1109/TIT.1982.1056489")
public class XMeans<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>>, DistanceBasedAlgorithm<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(XMeans.class);

  /**
   * Variant of kMeans for the bisecting step.
   */
  private KMeans<V, M> initialKMeans;
  private KMeans<V, M> splitKMeans;

  /**
   * Computed number of clusters. Output value
   */
  // TODO: how to return k?
  private int k;
  
  private int k_min;
  private int k_max;
  
  XMeansSplitKMeansInitialization<M> splitInitializer;

  /**
   * Constructor.
   * 
   * @param k_min k_min parameter - minimum number of result clusters
   * @param k_max k_max parameter - maximum number of result clusters
   * @param innerkMeans KMeans variant parameter - for split step
   */
  public XMeans(int k_min, int k_max, KMeans<V, M> initialKMeans, KMeans<V, M> splitKMeans, XMeansSplitKMeansInitialization<M> splitInitializer) {
    super();
    this.k_min = k_min;
    this.k_max = k_max;
    this.k = k_min;
    this.initialKMeans    = initialKMeans;
    this.splitKMeans      = splitKMeans;
    this.splitInitializer = splitInitializer;
  }
  
  private Clustering<M> splitCluster(Relation<V> relation, Cluster<M> parentCluster, Database database) {
    
    ProxyDatabase proxyDB = new ProxyDatabase(parentCluster.getIDs(), database);
    
    //determineChildCentroids(relation, parentCluster, database);
    
    // TODO: Throws exception when there are too few data points in the child cluster
    splitInitializer.setParentCluster(parentCluster);
    Clustering<M> childClustering = splitKMeans.run(proxyDB);
    
    // Transform parent cluster into a clustering
    LinkedList<Cluster<M>> parentClusterList = new LinkedList<Cluster<M>>();
    parentClusterList.add(parentCluster);
    Clustering<M> parentClustering = new Clustering<>(parentCluster.getName(), parentCluster.getName(), parentClusterList);
    
    double bicParent   = bic(relation, parentClustering);
    double bicChildren = bic(relation, childClustering);
    
    LOG.log(Logging.Level.FINE, "BIC parent: " + bicParent);
    LOG.log(Logging.Level.FINE, "BIC children: " + bicChildren);
    
    // Check if split is an improvement
    if (bicChildren < bicParent) {
      // Split does not improve clustering.
      // Return old cluster
      return parentClustering;
    }
    else {
      // Split improves clustering
      // Return the new clusters
      k++;
      return childClustering;
      //return parentClustering;
    }
  }
  
  public Clustering<M> run(Database database, Relation<V> relation) {
    
    // TODO: debug output does not appear
    LOG.debugFinest("start");
    
    ProxyDatabase proxyDB = new ProxyDatabase(relation.getDBIDs(), database);

    MutableProgress prog = LOG.isVerbose() ? new MutableProgress("X-means", k_max, LOG) : null;

    // Run initial k-means to find at least k_min clusters
    Clustering<M> initialClustering = initialKMeans.run(proxyDB);
    
    if (prog != null) {
      prog.setProcessed(k_min, LOG);
    }
    
    LinkedList<Cluster<M>> resultClusterList = new LinkedList<>(initialClustering.getAllClusters());
    LinkedList<Cluster<M>> toSplitClusterList = new LinkedList<>(resultClusterList);
    
    while (toSplitClusterList.size() > 0) {
      
      LinkedList<Cluster<M>> currentClusterList = new LinkedList<>(toSplitClusterList);
      toSplitClusterList.clear();
      
      for (int i = 0; i < currentClusterList.size() && k < k_max; i++) {
        
        Cluster<M> cluster = currentClusterList.get(i);
        
        // Split all clusters
        List<Cluster<M>> childClusterList = splitCluster(relation, cluster, database).getAllClusters();
        
        // If splitting improves clustering quality replace parent cluster with child clusters
        // and add child clusters to the list of clusters that should be split again
        if (childClusterList.size() > 1) {
          resultClusterList.remove(cluster);
          resultClusterList.addAll(childClusterList);
          toSplitClusterList.addAll(childClusterList);
          
          LOG.incrementProcessed(prog);
        }
      }
    }
    
    // TODO: progress is not set properly
    if (prog != null) {
      prog.setTotal(k);
    }

    // add all current clusters to the result
    return new Clustering<>("X-Means Result", "X-Means", resultClusterList);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return initialKMeans.getInputTypeRestriction();
  }

  @Override
  public DistanceFunction<? super V> getDistanceFunction() {
    return initialKMeans.getDistanceFunction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
  
  private double maxLikelihoodCluster(Relation<V> relation, Clustering<M> clustering, Cluster<M> cluster) {
    
    NumberVector.Factory<V> factory = RelationUtil.getNumberVectorFactory(relation);
    
    // number of data points in this cluster
    int n_i = cluster.size();
    
    // number of clusters
    int m = clustering.getAllClusters().size();
    
    // center of this cluster
    V c_i = factory.newNumberVector(cluster.getModel().getMean());
    
    // TODO: best way to get distance?
    DistanceQuery<V> distanceQuery = relation.getDatabase().getDistanceQuery(relation, initialKMeans.getDistanceFunction());
    
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
   * Computes log likelihood for a single cluster of a clustering
   *
   * @param relation
   * @param clustering
   * @param cluster
   * @return
   */
  private double logLikelihoodCluster(Relation<V> relation, Clustering<M> clustering, Cluster<M> cluster) {
    
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
        (n_i / 2) * Math.log(maxLikelihoodCluster(relation, clustering, cluster)) -
        (n_i - m) / 2;
    
    return logLikelihood_i;
  }

  /**
   * Computes log likelihood for a cluster
   * 
   * @param relation
   * @param clustering
   * @return
   */
  private double logLikelihoodClustering(Relation<V> relation, Clustering<M> clustering) {
    
    // log likelihood of this clustering
    double logLikelihood = 0.0;
    
    // add up the log-likelihood of all clusters
    for (Cluster<M> cluster : clustering.getAllClusters()) {
      logLikelihood += logLikelihoodCluster(relation, clustering, cluster);
    }
    
    return logLikelihood;
  }
  
  
  private double bic(Relation<V> relation, Clustering<M> clustering) {
    
    // number of all data points
    int n = 0;
    for (Cluster<M> aCluster : clustering.getAllClusters()) {
      n += aCluster.size();
    }
    
    // number of clusters
    int m = clustering.getAllClusters().size();
    
    // bayes information criterion for this clustering
    double bic =
      logLikelihoodClustering(relation, clustering) -
      (m * Math.log(n)) / 2;
    
    return bic;
  }

  /**
   * Parameterization class.
   * 
   * @author Tibor Goldschwendt
   * 
   * @param <V> Vector type
   * @param <M> Model type
   */
  public static class Parameterizer<V extends NumberVector, M extends KMeansModel> extends AbstractParameterizer {
    /**
     * Parameter to specify the kMeans variant.
     */
    public static final OptionID INITIAL_KMEANS_ID = new OptionID("xmeans.initial-kmeans-variant", "Initial kMeans variant");
    // TODO: Good idea?
    public static final OptionID SPLIT_KMEANS_ID = new OptionID("xmeans.split-kmeans-variant", "Split kMeans variant");

    public static final OptionID K_MIN_ID = new OptionID("xmeans.k_min", "The minimum number of clusters to find.");
    public static final OptionID K_MAX_ID = new OptionID("xmeans.k_max", "The maximum number of clusters to find.");
    
    /**
     * Variant of kMeans
     */
    protected KMeans<V, M> initialKMeansVariant;
    protected KMeans<V, M> splitKMeansVariant;
    
    protected XMeansSplitKMeansInitialization<M> splitInitializer;

    /**
     * Mimimum and maximum number of result clusters.
     */
    protected int k_min;
    protected int k_max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kMinP = new IntParameter(K_MIN_ID);
      IntParameter kMaxP = new IntParameter(K_MAX_ID);
      
      kMinP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      // TODO: k_max should be equal or greater than k_min
      kMaxP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      
      if (config.grab(kMinP) &&
          config.grab(kMaxP)) {
        k_min = kMinP.intValue();
        k_max = kMaxP.intValue();
      }
      
      RandomParameter rndP = new RandomParameter(KMeans.SEED_ID);
      ObjectParameter<KMeans<V, M>> initialKMeansVariantP = new ObjectParameter<>(INITIAL_KMEANS_ID, KMeans.class, KMeansLloyd.class);
      ObjectParameter<KMeans<V, M>> splitKMeansVariantP   = new ObjectParameter<>(SPLIT_KMEANS_ID, KMeans.class, KMeansLloyd.class);
      
      if (config.grab(initialKMeansVariantP) &&
          config.grab(splitKMeansVariantP) &&
          config.grab(rndP)) {
        
        ListParameterization initialKMeansVariantParameters = new ListParameterization();
        ListParameterization splitKMeansVariantParameters   = new ListParameterization();

        splitInitializer = new XMeansSplitKMeansInitialization<>(rndP.getValue());
        
        initialKMeansVariantParameters.addParameter(KMeans.K_ID, k_min);
        initialKMeansVariantParameters.addParameter(KMeans.SEED_ID, rndP.getValue());
        splitKMeansVariantParameters.addParameter(KMeans.K_ID, 2);
        splitKMeansVariantParameters.addParameter(KMeans.INIT_ID, splitInitializer);
        splitKMeansVariantParameters.addParameter(KMeans.SEED_ID, rndP.getValue());
        
        ChainedParameterization combinedConfig = new ChainedParameterization(initialKMeansVariantParameters, config);
        combinedConfig.errorsTo(config);
        initialKMeansVariant = initialKMeansVariantP.instantiateClass(combinedConfig);
        
        combinedConfig = new ChainedParameterization(splitKMeansVariantParameters, config);
        combinedConfig.errorsTo(config);
        splitKMeansVariant = splitKMeansVariantP.instantiateClass(combinedConfig);
      }
    }

    @Override
    protected XMeans<V, M> makeInstance() {
      return new XMeans<>(k_min, k_max, initialKMeansVariant, splitKMeansVariant, splitInitializer);
    }
  }
}
