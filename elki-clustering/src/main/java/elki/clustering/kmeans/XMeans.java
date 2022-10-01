/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.kmeans;

import static elki.math.linearalgebra.VMath.normalize;
import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.Predefined;
import elki.clustering.kmeans.quality.BayesianInformationCriterionXMeans;
import elki.clustering.kmeans.quality.KMeansQualityMeasure;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.relation.ProxyView;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.MutableProgress;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.math.MathUtil;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters.
 * <p>
 * Note: this implementation does currently <em>not</em> use a k-d-tree for
 * acceleration. Also note that kmax is not a hard threshold - the algorithm
 * can return up to 2*kmax clusters!
 * <p>
 * Reference:
 * <p>
 * D. Pelleg, A. Moore<br>
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br>
 * Proc. 17th Int. Conf. on Machine Learning (ICML 2000)
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KMeans
 * @has - - - KMeansQualityMeasure
 *
 * @param <V> Vector type
 * @param <M> Model type
 */
@Title("X-means")
@Reference(authors = "D. Pelleg, A. Moore", //
    title = "X-means: Extending K-means with Efficient Estimation on the Number of Clusters", //
    booktitle = "Proc. 17th Int. Conf. on Machine Learning (ICML 2000)", //
    url = "http://www.pelleg.org/shared/hp/download/xmeans.ps", //
    bibkey = "DBLP:conf/icml/PellegM00")
public class XMeans<V extends NumberVector, M extends MeanModel> extends AbstractKMeans<V, M> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(XMeans.class);

  /**
   * Inner k-means algorithm.
   */
  protected KMeans<V, M> innerKMeans;

  /**
   * Effective number of clusters, minimum and maximum.
   */
  private int k_min, k_max;

  /**
   * Initializer for k-means.
   */
  Predefined splitInitializer;

  /**
   * Information criterion to choose the better split.
   */
  KMeansQualityMeasure<V> informationCriterion;

  /**
   * Random factory.
   */
  RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k_min k_min parameter - minimum number of result clusters
   * @param k_max k_max parameter - maximum number of result clusters
   * @param maxiter Maximum number of iterations each.
   * @param innerKMeans K-Means variant to use inside.
   * @param informationCriterion The information criterion used for the
   *        splitting step
   * @param random Random factory
   */
  public XMeans(NumberVectorDistance<? super V> distance, int k_min, int k_max, int maxiter, KMeans<V, M> innerKMeans, KMeansInitialization initializer, KMeansQualityMeasure<V> informationCriterion, RandomFactory random) {
    super(distance, k_min, maxiter, initializer);
    this.k_min = k_min;
    this.k_max = k_max;
    this.innerKMeans = innerKMeans;
    this.splitInitializer = new Predefined((double[][]) null);
    this.innerKMeans.setInitializer(this.splitInitializer);
    this.innerKMeans.setDistance(distance);
    this.informationCriterion = informationCriterion;
    this.rnd = random;
  }

  /**
   * Run the algorithm on a database and relation.
   *
   * @param relation Data relation
   * @return Clustering result.
   */
  @Override
  public Clustering<M> run(Relation<V> relation) {
    final String key = getClass().getName();
    MutableProgress prog = LOG.isVerbose() ? new MutableProgress("Number of clusters", k_max, LOG) : null;

    // Run initial k-means to find at least k_min clusters
    innerKMeans.setK(k_min);
    LOG.statistics(new StringStatistic(key + ".initialization", initializer.toString()));
    splitInitializer.setInitialMeans(initializer.chooseInitialMeans(relation, k_min, distance));
    Clustering<M> clustering = innerKMeans.run(relation);

    if(prog != null) {
      prog.setProcessed(k_min, LOG);
    }

    ArrayList<Cluster<M>> clusters = new ArrayList<>(clustering.getAllClusters());
    while(clusters.size() <= k_max) {
      // Improve-Structure:
      ArrayList<Cluster<M>> nextClusters = new ArrayList<>();
      for(Cluster<M> cluster : clusters) {
        // Try to split this cluster:
        List<Cluster<M>> childClusterList = splitCluster(cluster, relation);
        nextClusters.addAll(childClusterList);
        if(childClusterList.size() > 1) {
          k += childClusterList.size() - 1;
          if(prog != null) {
            if(k >= k_max) {
              prog.setTotal(k + 1);
            }
            prog.setProcessed(k, LOG);
          }
        }
      }
      if(clusters.size() == nextClusters.size()) {
        break;
      }
      // Improve-Params:
      innerKMeans.setInitializer(splitInitializer);
      splitInitializer.setInitialClusters(nextClusters);
      innerKMeans.setK(nextClusters.size());
      clustering = innerKMeans.run(relation);
      clusters.clear();
      clusters.addAll(clustering.getAllClusters());
    }

    // Ensure that the progress bar finished.
    if(prog != null) {
      prog.setTotal(k);
      prog.setProcessed(k, LOG);
    }
    LOG.statistics(new LongStatistic(key + ".num-clusters", clusters.size()));
    Clustering<M> result = new Clustering<>(clusters);
    Metadata.of(result).setLongName("X-Means Clustering");
    return result;
  }

  /**
   * Conditionally splits the clusters based on the information criterion.
   *
   * @param parentCluster Cluster to split
   * @param relation Data relation
   * @return Parent cluster when split decreases clustering quality or child
   *         clusters when split improves clustering.
   */
  protected List<Cluster<M>> splitCluster(Cluster<M> parentCluster, Relation<V> relation) {
    if(parentCluster.size() <= 1) {
      return Arrays.asList(parentCluster); // too small
    }
    Clustering<M> parentClustering = new Clustering<>(Arrays.asList(parentCluster));
    splitInitializer.setInitialMeans(splitCentroid(parentCluster, relation));
    innerKMeans.setK(2);
    Clustering<M> childClustering = innerKMeans.run(new ProxyView<V>(parentCluster.getIDs(), relation));

    double parentEvaluation = informationCriterion.quality(parentClustering, distance, relation);
    double childrenEvaluation = informationCriterion.quality(childClustering, distance, relation);

    if(LOG.isDebugging()) {
      LOG.debug("parentEvaluation: " + parentEvaluation);
      LOG.debug("childrenEvaluation: " + childrenEvaluation);
    }

    // Check if split is an improvement:
    return informationCriterion.isBetter(parentEvaluation, childrenEvaluation) ? Arrays.asList(parentCluster) : childClustering.getAllClusters();
  }

  /**
   * Split an existing centroid into two initial centers.
   *
   * @param parentCluster Existing cluster
   * @param relation Data relation
   * @return List of new centroids
   */
  protected double[][] splitCentroid(Cluster<? extends MeanModel> parentCluster, Relation<V> relation) {
    double[] parentCentroid = parentCluster.getModel().getMean().clone(); // Modified!

    // Compute size of cluster/region
    double radius = 0.;
    NumberVectorDistance<? super V> distance = this.distance; // localize
    for(DBIDIter it = parentCluster.getIDs().iter(); it.valid(); it.advance()) {
      double d = distance.distance(relation.get(it), DoubleVector.wrap(parentCentroid));
      radius = (d > radius) ? d : radius;
    }

    // Choose random vector
    Random random = rnd.getSingleThreadedRandom();
    final int dim = RelationUtil.dimensionality(relation);
    double[] randomVector = normalize(MathUtil.randomDoubleArray(dim, random));
    timesEquals(randomVector, (.4 + random.nextDouble() * .5) * radius);

    // Get the new centroids
    for(int d = 0; d < dim; d++) {
      double a = parentCentroid[d], b = randomVector[d];
      parentCentroid[d] = a - b;
      randomVector[d] = a + b;
    }
    return new double[][] { parentCentroid, randomVector };
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return innerKMeans.getInputTypeRestriction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Tibor Goldschwendt
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   * @param <M> Model type of inner algorithm
   */
  public static class Par<V extends NumberVector, M extends MeanModel> extends AbstractKMeans.Par<V> {
    /**
     * Parameter to specify the kMeans variant.
     */
    public static final OptionID INNER_KMEANS_ID = new OptionID("xmeans.kmeans", "kMeans algorithm to use.");

    /**
     * Minimum number of clusters.
     */
    public static final OptionID K_MIN_ID = new OptionID("xmeans.k_min", "The minimum number of clusters to find.");

    /**
     * Randomization seed.
     */
    public static final OptionID SEED_ID = new OptionID("xmeans.seed", "Random seed for splitting clusters.");

    /**
     * Quality measure to use for evaluating splits.
     */
    public static final OptionID INFORMATION_CRITERION_ID = new OptionID("xmeans.quality", "The quality measure to evaluate splits (e.g., AIC, BIC)");

    /**
     * Variant of kMeans
     */
    protected KMeans<V, M> innerKMeans;

    /**
     * Information criterion.
     */
    protected KMeansQualityMeasure<V> informationCriterion;

    /**
     * Minimum and maximum number of result clusters.
     */
    protected int k_min, k_max;

    /**
     * Random number generator.
     */
    protected RandomFactory random;

    @Override
    public void configure(Parameterization config) {
      // Do NOT invoke super.makeOptions to hide the "k" parameter.
      IntParameter kMinP = new IntParameter(K_MIN_ID, 2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      kMinP.grab(config, x -> k_min = x);
      IntParameter kMaxP = new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      kMaxP.grab(config, x -> k_max = x);
      // Non-formalized parameter constraint: k_min <= k_max
      if(k_min > k_max) {
        config.reportError(new WrongParameterValueException(kMinP, "must be at most", kMaxP, ""));
      }

      getParameterInitialization(config);
      getParameterMaxIter(config);
      getParameterDistance(config);

      new RandomParameter(SEED_ID).grab(config, x -> random = x);
      ObjectParameter<KMeans<V, M>> innerKMeansP = new ObjectParameter<>(INNER_KMEANS_ID, KMeans.class, HamerlyKMeans.class);
      if(config.grab(innerKMeansP)) {
        ChainedParameterization combinedConfig = new ChainedParameterization(new ListParameterization() //
            .addParameter(KMeans.K_ID, k_min) //
            .addParameter(KMeans.INIT_ID, new Predefined((double[][]) null)) //
            .addParameter(KMeans.MAXITER_ID, maxiter) //
            // Setting the distance to null if undefined at this point will
            // cause validation errors later. So fall back to the default.
            .addParameter(KMeans.DISTANCE_FUNCTION_ID, distance != null ? //
                distance : SquaredEuclideanDistance.STATIC), config);
        combinedConfig.errorsTo(config);
        innerKMeans = innerKMeansP.instantiateClass(combinedConfig);
        configureInformationCriterion(config);
      }
    }

    /**
     * Configure the information criterion option, to allow overriding by
     * {@link GMeans}.
     *
     * @param config Parameterization
     */
    protected void configureInformationCriterion(Parameterization config) {
      new ObjectParameter<KMeansQualityMeasure<V>>(INFORMATION_CRITERION_ID, KMeansQualityMeasure.class, BayesianInformationCriterionXMeans.class) //
          .grab(config, x -> informationCriterion = x);
    }

    @Override
    public XMeans<V, M> make() {
      return new XMeans<>(distance, k_min, k_max, maxiter, innerKMeans, initializer, informationCriterion, random);
    }
  }
}
