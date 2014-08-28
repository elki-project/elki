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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality.KMeansQualityMeasure;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.MutableProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters.
 * 
 * Note: this implementation does currently <em>not</em> use a k-d-tree for
 * acceleration.
 *
 * Reference:<br>
 * <p>
 * D. Pelleg, A. Moore:<br />
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br />
 * In: Proceedings of the 17th International Conference on Machine Learning
 * (ICML 2000)
 * </p>
 * 
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <M> Model type
 */
@Reference(authors = "D. Pelleg, A. Moore", //
booktitle = "X-means: Extending K-means with Efficient Estimation on the Number of Clusters", //
title = "Proceedings of the 17th International Conference on Machine Learning (ICML 2000)", //
url = "http://www.pelleg.org/shared/hp/download/xmeans.ps")
public class XMeans<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>>, DistanceBasedAlgorithm<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(XMeans.class);

  private KMeans<V, M> initialKMeans;

  private KMeans<V, M> splitKMeans;

  /**
   * Effective number of clusters, minimum and maximum.
   */
  private int k, k_min, k_max;

  XMeansSplitKMeansInitialization<M> splitInitializer;

  KMeansQualityMeasure<V> informationCriterion;

  /**
   * Constructor.
   * 
   * @param k_min k_min parameter - minimum number of result clusters
   * @param k_max k_max parameter - maximum number of result clusters
   * @param initialKMeans K-Means variant parameter - for initial clustering
   * @param splitKMeans K-Means variant parameter - for split step
   * @param splitInitializer K Means initializer for the splitting step
   * @param informationCriterion The information criterion used for the
   *        splitting step
   */
  public XMeans(int k_min, int k_max, KMeans<V, M> initialKMeans, KMeans<V, M> splitKMeans, XMeansSplitKMeansInitialization<M> splitInitializer, KMeansQualityMeasure<V> informationCriterion) {
    super();
    this.k_min = k_min;
    this.k_max = k_max;
    this.k = k_min;
    this.initialKMeans = initialKMeans;
    this.splitKMeans = splitKMeans;
    this.splitInitializer = splitInitializer;
    this.informationCriterion = informationCriterion;
  }

  public Clustering<M> run(Database database, Relation<V> relation) {
    if(!(initialKMeans.getDistanceFunction() instanceof PrimitiveDistanceFunction)) {
      throw new AbortException("K-Means results can only be evaluated for primitive distance functions, got: " + initialKMeans.getDistanceFunction().getClass());
    }
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super NumberVector> df = (PrimitiveDistanceFunction<? super NumberVector>) initialKMeans.getDistanceFunction();

    ProxyDatabase proxyDB = new ProxyDatabase(relation.getDBIDs(), database);

    MutableProgress prog = LOG.isVerbose() ? new MutableProgress("X-means", k_max, LOG) : null;

    // Run initial k-means to find at least k_min clusters
    Clustering<M> initialClustering = initialKMeans.run(proxyDB);

    if(prog != null) {
      prog.setProcessed(k_min, LOG);
    }

    LinkedList<Cluster<M>> resultClusterList = new LinkedList<>(initialClustering.getAllClusters());
    LinkedList<Cluster<M>> toSplitClusterList = new LinkedList<>(resultClusterList);

    while(toSplitClusterList.size() > 0) {
      LinkedList<Cluster<M>> currentClusterList = new LinkedList<>(toSplitClusterList);
      toSplitClusterList.clear();

      for(int i = 0; i < currentClusterList.size() && k < k_max; i++) {
        Cluster<M> cluster = currentClusterList.get(i);

        // Split all clusters
        List<Cluster<M>> childClusterList = splitCluster(relation, cluster, database, df).getAllClusters();

        // If splitting improves clustering quality replace parent cluster with
        // child clusters and add child clusters to the list of clusters that
        // should be split again
        if(childClusterList.size() > 1) {
          resultClusterList.remove(cluster);
          resultClusterList.addAll(childClusterList);
          toSplitClusterList.addAll(childClusterList);
          k++;

          LOG.incrementProcessed(prog);
        }
      }
    }

    if(prog != null) {
      prog.setTotal(k);
    }

    if(LOG.isDebugging()) {
      LOG.debug("X-means returned k=" + k + " clusters.");
    }

    // add all current clusters to the result
    Clustering<M> result = new Clustering<>("X-Means Result", "X-Means", resultClusterList);
    return result;
  }

  /**
   * Conditionally splits the clusters based on the information criterion.
   * 
   * @param relation
   * @param parentCluster
   * @param database
   * @return Parent cluster when split decreases clustering quality or child
   *         clusters when split improves clustering.
   */
  private Clustering<M> splitCluster(Relation<V> relation, Cluster<M> parentCluster, Database database, PrimitiveDistanceFunction<? super NumberVector> dist) {
    // Transform parent cluster into a clustering
    LinkedList<Cluster<M>> parentClusterList = new LinkedList<Cluster<M>>();
    parentClusterList.add(parentCluster);
    Clustering<M> parentClustering = new Clustering<>(parentCluster.getName(), parentCluster.getName(), parentClusterList);

    if(parentCluster.size() < 2) {
      // Split is not possbile
      return parentClustering;
    }

    ProxyDatabase proxyDB = new ProxyDatabase(parentCluster.getIDs(), database);

    splitInitializer.setParentCluster(parentCluster);
    Clustering<M> childClustering = splitKMeans.run(proxyDB);

    double parentEvaluation = informationCriterion.quality(parentClustering, dist, relation);
    double childrenEvaluation = informationCriterion.quality(childClustering, dist, relation);

    if(LOG.isDebugging()) {
      LOG.debug("parentEvaluation: " + parentEvaluation);
      LOG.debug("childrenEvaluation: " + childrenEvaluation);
    }

    // Check if split is an improvement:
    return (childrenEvaluation > parentEvaluation) ^ informationCriterion.ascending() ? parentClustering : childClustering;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return initialKMeans.getInputTypeRestriction();
  }

  @Override
  public DistanceFunction<? super V> getDistanceFunction() {
    return splitKMeans.getDistanceFunction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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

    public static final OptionID SPLIT_KMEANS_ID = new OptionID("xmeans.split-kmeans-variant", "Split kMeans variant");

    public static final OptionID K_MIN_ID = new OptionID("xmeans.k_min", "The minimum number of clusters to find.");

    public static final OptionID K_MAX_ID = new OptionID("xmeans.k_max", "The maximum number of clusters to find.");

    public static final OptionID INFORMATION_CRITERION_ID = new OptionID("xmeans.information-criterion", "The information to evaluate splits (e.g. AIC, BIC)");

    /**
     * Variant of kMeans
     */
    protected KMeans<V, M> initialKMeansVariant;

    protected KMeans<V, M> splitKMeansVariant;

    protected XMeansSplitKMeansInitialization<M> splitInitializer;

    protected KMeansQualityMeasure<V> informationCriterion;

    /**
     * Mimimum and maximum number of result clusters.
     */
    protected int k_min;

    protected int k_max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kMinP = new IntParameter(K_MIN_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kMinP)) {
        k_min = kMinP.intValue();
      }
      IntParameter kMaxP = new IntParameter(K_MAX_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kMaxP)) {
        k_max = kMaxP.intValue();
      }
      config.checkConstraint(new LessGlobalConstraint<>(kMinP, kMaxP));

      ObjectParameter<KMeans<V, M>> initialKMeansVariantP = new ObjectParameter<>(INITIAL_KMEANS_ID, KMeans.class, KMeansLloyd.class);
      ObjectParameter<KMeans<V, M>> splitKMeansVariantP = new ObjectParameter<>(SPLIT_KMEANS_ID, KMeans.class, KMeansLloyd.class);
      RandomParameter rndP = new RandomParameter(KMeans.SEED_ID);
      if(config.grab(initialKMeansVariantP) && config.grab(splitKMeansVariantP) && config.grab(rndP)) {
        ListParameterization initialKMeansVariantParameters = new ListParameterization();
        ListParameterization splitKMeansVariantParameters = new ListParameterization();

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

      ObjectParameter<KMeansQualityMeasure<V>> informationCriterionP = new ObjectParameter<>(INFORMATION_CRITERION_ID, KMeansQualityMeasure.class);
      if(config.grab(informationCriterionP)) {
        informationCriterion = informationCriterionP.instantiateClass(config);
      }
    }

    @Override
    protected XMeans<V, M> makeInstance() {
      return new XMeans<>(k_min, k_max, initialKMeansVariant, splitKMeansVariant, splitInitializer, informationCriterion);
    }
  }
}
