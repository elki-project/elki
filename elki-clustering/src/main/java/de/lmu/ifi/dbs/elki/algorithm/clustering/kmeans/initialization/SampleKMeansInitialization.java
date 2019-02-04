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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ModelUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Initialize k-means by running k-means on a sample of the data set only.
 * <p>
 * Reference:
 * <p>
 * The idea of finding centers on a sample can be found in:
 * <p>
 * P. S. Bradley, U. M. Fayyad<br>
 * Refining Initial Points for K-Means Clustering<br>
 * Proc. 15th Int. Conf. on Machine Learning (ICML 1998)
 * <p>
 * But Bradley and Fayyad also suggest to repeat this multiple times. This
 * implementation uses a single attempt only.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <V> Vector type
 */
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.SampleKMeansInitialization")
@Reference(authors = "P. S. Bradley, U. M. Fayyad", //
    title = "Refining Initial Points for K-Means Clustering", //
    booktitle = "Proc. 15th Int. Conf. on Machine Learning (ICML 1998)", //
    bibkey = "DBLP:conf/icml/BradleyF98")
public class SampleKMeansInitialization<V extends NumberVector> extends AbstractKMeansInitialization {
  /**
   * Variant of kMeans to use for initialization.
   */
  private KMeans<V, ?> innerkMeans;

  /**
   * Sample size.
   */
  private double rate;

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   * @param innerkMeans Inner k-means algorithm.
   * @param rate Sampling rate.
   */
  public SampleKMeansInitialization(RandomFactory rnd, KMeans<V, ?> innerkMeans, double rate) {
    super(rnd);
    this.innerkMeans = innerkMeans;
    this.rate = rate;
  }

  @Override
  public double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), rate, rnd);
    if(sample.size() < k) {
      throw new IllegalArgumentException("Sampling rate=" + rate + " from N=" + relation.size() + " yields only " + sample.size() + " < k objects.");
    }

    // Ugly cast, sorry
    @SuppressWarnings("unchecked")
    Relation<V> rel = (Relation<V>) relation;
    // FIXME: This does not necessarily hold. Check and fail!
    if(!distanceFunction.getInputTypeRestriction().isAssignableFromType(TypeUtil.NUMBER_VECTOR_FIELD)) {
      LoggingUtil.warning("Initializing k-means with k-means using specialized distance functions MAY fail, if the initialization method does require a distance defined on arbitrary number vectors.");
    }
    @SuppressWarnings("unchecked")
    NumberVectorDistanceFunction<? super V> pdf = (NumberVectorDistanceFunction<? super V>) distanceFunction;
    ProxyView<V> proxyv = new ProxyView<>(sample, rel);
    ProxyDatabase proxydb = new ProxyDatabase(sample, proxyv);

    innerkMeans.setK(k);
    innerkMeans.setDistanceFunction(pdf);
    Clustering<?> clusters = innerkMeans.run(proxydb, proxyv);

    double[][] means = new double[clusters.getAllClusters().size()][];
    int i = 0;
    for(Cluster<?> cluster : clusters.getAllClusters()) {
      means[i++] = ModelUtil.getPrototype(cluster.getModel(), relation).toArray();
    }
    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeansInitialization.Parameterizer {
    /**
     * Parameter to specify the kMeans variant.
     */
    public static final OptionID KMEANS_ID = new OptionID("kmeans.algorithm", "KMeans variant to run multiple times.");

    /**
     * Parameter to specify the sampling rate.
     */
    public static final OptionID SAMPLE_ID = new OptionID("kmeans.samplesize", "Sample set size (if > 1) or sampling rante (if < 1).");

    /**
     * Inner k-means algorithm to use.
     */
    protected KMeans<V, ?> innerkMeans;

    /**
     * Sampling rate.
     */
    protected double rate;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<KMeans<V, ?>> kMeansVariantP = new ObjectParameter<>(KMEANS_ID, KMeans.class);
      if(config.grab(kMeansVariantP)) {
        ListParameterization kMeansVariantParameters = new ListParameterization();

        // We will always invoke this with k as requested from outside!
        kMeansVariantParameters.addParameter(KMeans.K_ID, 13);
        kMeansVariantParameters.addParameter(KMeans.DISTANCE_FUNCTION_ID, SquaredEuclideanDistanceFunction.class);

        ChainedParameterization combinedConfig = new ChainedParameterization(kMeansVariantParameters, config);
        combinedConfig.errorsTo(config);
        innerkMeans = kMeansVariantP.instantiateClass(combinedConfig);
      }

      DoubleParameter sampleP = new DoubleParameter(SAMPLE_ID);
      if(config.grab(sampleP)) {
        rate = sampleP.doubleValue();
      }
    }

    @Override
    protected SampleKMeansInitialization<V> makeInstance() {
      return new SampleKMeansInitialization<>(rnd, innerkMeans, rate);
    }
  }
}
