package experimentalcode.students.goldschwendt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.AbstractKMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.BestOfMultipleKMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansBisecting;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.NumberVector.Factory;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
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
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.MutableProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
public class XMeans<V extends NumberVector, M extends KMeansModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>>, DistanceBasedAlgorithm<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(XMeans.class);

  /**
   * Variant of kMeans for the bisecting step.
   */
  private KMeans<V, M> innerkMeans;

  /**
   * Computed number of clusters. Output value
   */
  // TODO: how to return k?
  private int k;
  
  private int k_min;
  private int k_max;
  

  /**
   * Constructor.
   * 
   * @param k_min k_min parameter - minimum number of result clusters
   * @param k_max k_max parameter - maximum number of result clusters
   * @param innerkMeans KMeans variant parameter - for split step
   */
  public XMeans(int k_min, int k_max, KMeans<V, M> innerkMeans) {
    super();
    this.k_min = k_min;
    this.k_max = k_max;
    this.k = k_min;
    this.innerkMeans = innerkMeans;
  }

  private List<V> determineChildCentroids(Relation<V> relation, Cluster<M> parentCluster, Database database) {
    
    // Variables necessary to determine child centroids
    NumberVector.Factory<V> vectorFactory = RelationUtil.getNumberVectorFactory(relation);
    Random random = RandomFactory.DEFAULT.getSingleThreadedRandom();
    Vector parentCentroid = parentCluster.getModel().getMean();
    
    // Compute size of cluster/region
    // TODO: best way to determine size of cluster?
    int parentK = parentCluster.size();
    DistanceQuery<V> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<V> knnQuery = database.getKNNQuery(distQuery, parentK);
    KNNList knns = knnQuery.getKNNForObject(vectorFactory.newNumberVector(parentCentroid), parentK);
    double clusterSize = knns.getKNNDistance();
    
    // Chose random vector
    final int dim = RelationUtil.dimensionality(relation);
    Vector randomVector = VectorUtil.randomVector(Vector.FACTORY, dim);
    
    // Set the vectors length between 0 and the cluster size (randomly)
    randomVector.normalize();
    randomVector = randomVector.times((0.5 + random.nextDouble() % 0.5) * clusterSize);
    
    // Get the new centroids
    Vector childCentroid1 = parentCentroid.plus(randomVector);
    Vector childCentroid2 = parentCentroid.plus(randomVector.times(-1));
    
    ArrayList<V> result = new ArrayList<V>(2);
    result.add(vectorFactory.newNumberVector(childCentroid1));
    result.add(vectorFactory.newNumberVector(childCentroid2));
    
    return result;
  }
  
  private Clustering<M> splitCluster(Relation<V> relation, Cluster<M> parentCluster, Database database) {
    
    ProxyDatabase proxyDB = new ProxyDatabase(parentCluster.getIDs(), database);
    
    determineChildCentroids(relation, parentCluster, database);
    
    innerkMeans.setK(2);
    // TODO: Throws exception when there are too few data points in the child cluster
    Clustering<M> childClustering = innerkMeans.run(proxyDB);
    
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
    }
  }
  
  public Clustering<M> run(Database database, Relation<V> relation) {
    
    // TODO: debug output does not appear
    LOG.debug("start");
    
    ProxyDatabase proxyDB = new ProxyDatabase(relation.getDBIDs(), database);

    MutableProgress prog = LOG.isVerbose() ? new MutableProgress("X-means", k_max, LOG) : null;

    // Run initial k-means to find at least k_min clusters
    Clustering<M> initialClustering = innerkMeans.run(proxyDB);
    
    prog.setProcessed(k_min, LOG);
    
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
          
          prog.incrementProcessed(LOG);
        }
      }
    }
    
    // TODO: progress is not set properly
    prog.setTotal(k);

    // add all current clusters to the result
    return new Clustering<>("X-Means Result", "X-Means", resultClusterList);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return innerkMeans.getInputTypeRestriction();
  }

  @Override
  public DistanceFunction<? super V> getDistanceFunction() {
    return innerkMeans.getDistanceFunction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
  
  private double maxLikelihood (Relation<V> relation, Cluster<M> cluster, int m) {
    
    int ni = cluster.size();
    V ci = (V) cluster.getModel().getMean();
    
    // TODO: best way to get distance?
    DistanceQuery<V> distanceQuery = relation.getDatabase().getDistanceQuery(relation, innerkMeans.getDistanceFunction());
    
    double maxLikelihood = 0;
    
    for (DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
      V xj = relation.get(iter);
      maxLikelihood += Math.pow(distanceQuery.distance(xj, ci), 2);
      
    }
    maxLikelihood = maxLikelihood / (ni - m);
    
    return maxLikelihood;
  }
  
  private double logLikelihood(Relation<V> relation, Cluster<M> cluster, int n, int m) {
    
    DBIDIter iter = relation.getDBIDs().iter();
    V vector = relation.get(iter);
    
    // TODO: best way to get dimensionality
    int d = vector.getDimensionality();
    int ni = cluster.size();
    
    double logLikelihood = ni * Math.log(ni) -
        ni * Math.log(n) -
        ((ni * d) / 2) * Math.log(2 * Math.PI) -
        (ni / 2) * Math.log(maxLikelihood(relation, cluster, m)) -
        (ni - m) / 2;
    
    return logLikelihood;
  }
  
  private double bic(Relation<V> relation, Clustering<M> clustering) {
    
    // compute number of points of clustering
    int n = 0;
    for (Cluster<M> cluster : clustering.getAllClusters()) {
      n += cluster.size();
    }
    
    int m = clustering.getAllClusters().size();
    
    // add up the log-likelihood of all clusters
    double bic = 0.0;
    for (Cluster<M> cluster : clustering.getAllClusters()) {
      bic += logLikelihood(relation, cluster, n, m);
    }
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
    public static final OptionID KMEANS_ID = new OptionID("xmeans.kmeansvariant", "KMeans variant");

    public static final OptionID K_MIN_ID = new OptionID("xmeans.k_min", "The minimum number of clusters to find.");
    public static final OptionID K_MAX_ID = new OptionID("xmeans.k_max", "The maximum number of clusters to find.");
    
    /**
     * Variant of kMeans
     */
    protected KMeans<V, M> kMeansVariant;

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
      
      
      ObjectParameter<KMeans<V, M>> kMeansVariantP = new ObjectParameter<>(KMEANS_ID, KMeans.class, KMeansLloyd.class);
      if (config.grab(kMeansVariantP)) {
        ListParameterization kMeansVariantParameters = new ListParameterization();

        kMeansVariantParameters.addParameter(KMeans.K_ID, k_min);

        ChainedParameterization combinedConfig = new ChainedParameterization(kMeansVariantParameters, config);
        combinedConfig.errorsTo(config);
        kMeansVariant = kMeansVariantP.instantiateClass(combinedConfig);
      }
    }

    @Override
    protected XMeans<V, M> makeInstance() {
      return new XMeans<>(k_min, k_max, kMeansVariant);
    }
  }
}
