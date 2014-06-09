package experimentalcode.students.goldschwendt;

import java.util.LinkedList;
import java.util.List;

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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
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

  private Clustering<M> splitCluster(Cluster<M> parentCluster, Database database) {
    
    ProxyDatabase proxyDB = new ProxyDatabase(parentCluster.getIDs(), database);
    
    innerkMeans.setK(2);
    Clustering<M> childClustering = innerkMeans.run(proxyDB);
    
    // Transform parent cluster into a clustering
    LinkedList<Cluster<M>> parentClusterList = new LinkedList<Cluster<M>>();
    parentClusterList.add(parentCluster);
    Clustering<M> parentClustering = new Clustering<>(parentCluster.getName(), parentCluster.getName(), parentClusterList);
    
    // Check if split is an improvement
    if (bic(childClustering) < bic(parentClustering)) {
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
        List<Cluster<M>> childClusterList = splitCluster(cluster, database).getAllClusters();
        
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
  
  private double bic(Clustering<M> clustering) {
    double bic = clustering.getAllClusters().size();
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
      
      kMinP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      kMaxP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      
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
