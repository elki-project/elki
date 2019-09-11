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
package elki.clustering.kmeans;

import java.util.LinkedList;

import elki.AbstractAlgorithm;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ProxyDatabase;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The bisecting k-means algorithm works by starting with an initial
 * partitioning into two clusters, then repeated splitting of the largest
 * cluster to get additional clusters.
 * <p>
 * Reference:
 * <p>
 * M. Steinbach, G. Karypis, V. Kumar<br>
 * A Comparison of Document Clustering Techniques<br>
 * KDD workshop on text mining. Vol. 400. No. 1
 *
 * @author Stephan Baier
 * @since 0.6.0
 *
 * @has - - - KMeans
 *
 * @param <V> Vector type
 * @param <M> Model type
 */
@Reference(authors = "M. Steinbach, G. Karypis, V. Kumar", //
    title = "A Comparison of Document Clustering Techniques", //
    booktitle = "KDD workshop on text mining. Vol. 400. No. 1", //
    url = "http://glaros.dtc.umn.edu/gkhome/fetch/papers/docclusterKDDTMW00.pdf", //
    bibkey = "conf/kdd/SteinbachKK00")
public class BisectingKMeans<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements KMeans<V, M> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(BisectingKMeans.class);

  /**
   * Variant of kMeans for the bisecting step.
   */
  private KMeans<V, M> innerkMeans;

  /**
   * Desired value of k.
   */
  private int k;

  /**
   * Constructor.
   *
   * @param k k parameter - number of result clusters
   * @param innerkMeans KMeans variant parameter - for bisecting step
   */
  public BisectingKMeans(int k, KMeans<V, M> innerkMeans) {
    super();
    this.k = k;
    this.innerkMeans = innerkMeans;
  }

  @Override
  public Clustering<M> run(Database database, Relation<V> relation) {
    ProxyDatabase proxyDB = new ProxyDatabase(relation.getDBIDs(), database);

    // Linked list is preferrable for scratch, as we will A) not need that many
    // clusters and B) be doing random removals of the largest cluster (often at
    // the head)
    LinkedList<Cluster<M>> currentClusterList = new LinkedList<>();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Bisecting k-means", k - 1, LOG) : null;

    for(int j = 0; j < this.k - 1; j++) {
      // Choose a cluster to split and project database to cluster
      if(currentClusterList.isEmpty()) {
        proxyDB = new ProxyDatabase(relation.getDBIDs(), database);
      }
      else {
        Cluster<M> largestCluster = null;
        for(Cluster<M> cluster : currentClusterList) {
          if(largestCluster == null || cluster.size() > largestCluster.size()) {
            largestCluster = cluster;
          }
        }
        assert largestCluster != null;
        currentClusterList.remove(largestCluster);
        proxyDB.setDBIDs(largestCluster.getIDs());
      }

      // Run the inner k-means algorithm:
      // FIXME: ensure we run on the correct relation in a multirelational
      // setting!
      Clustering<M> innerResult = innerkMeans.run(proxyDB);
      // Add resulting clusters to current result.
      currentClusterList.addAll(innerResult.getAllClusters());

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // add all current clusters to the result
    Clustering<M> result = new Clustering<>(currentClusterList);
    Metadata.of(result).setLongName("Bisecting k-Means Result");
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return innerkMeans.getInputTypeRestriction();
  }

  @Override
  public NumberVectorDistance<? super V> getDistance() {
    return innerkMeans.getDistance();
  }

  @Override
  public void setK(int k) {
    this.k = k;
  }

  @Override
  public void setDistance(NumberVectorDistance<? super V> distanceFunction) {
    innerkMeans.setDistance(distanceFunction);
  }

  @Override
  public void setInitializer(KMeansInitialization init) {
    innerkMeans.setInitializer(init);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   * @param <M> Model type
   */
  public static class Par<V extends NumberVector, M extends MeanModel> implements Parameterizer {
    /**
     * Parameter to specify the kMeans variant.
     */
    public static final OptionID KMEANS_ID = new OptionID("bisecting.kmeansvariant", "KMeans variant");

    /**
     * Variant of kMeans
     */
    protected KMeans<V, M> kMeansVariant;

    /**
     * Desired number of clusters.
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> k = x);
      ObjectParameter<KMeans<V, M>> kMeansVariantP = new ObjectParameter<>(KMEANS_ID, KMeans.class, BestOfMultipleKMeans.class);
      if(config.grab(kMeansVariantP)) {
        ListParameterization kMeansVariantParameters = new ListParameterization();

        // We will always invoke this with k=2!
        kMeansVariantParameters.addParameter(KMeans.K_ID, 2);

        ChainedParameterization combinedConfig = new ChainedParameterization(kMeansVariantParameters, config);
        combinedConfig.errorsTo(config);
        kMeansVariant = kMeansVariantP.instantiateClass(combinedConfig);
      }
    }

    @Override
    public BisectingKMeans<V, M> make() {
      return new BisectingKMeans<>(k, kMeansVariant);
    }
  }
}
