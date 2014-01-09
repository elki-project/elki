package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Initialize k-means by running k-means on a sample of the data set only.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class SampleKMeansInitialization<V extends NumberVector<?>, D extends Distance<?>> extends AbstractKMeansInitialization<V> {
  /**
   * Variant of kMeans for the bisecting step.
   */
  private KMeans<V, D, ?> innerkMeans;

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
  public SampleKMeansInitialization(RandomFactory rnd, KMeans<V, D, ?> innerkMeans, double rate) {
    super(rnd);
    this.innerkMeans = innerkMeans;
    this.rate = rate;
  }

  @Override
  public List<V> chooseInitialMeans(Database database, Relation<V> relation, int k, PrimitiveDistanceFunction<? super NumberVector<?>, ?> distanceFunction) {
    final int samplesize = (int) Math.ceil(rate * relation.size());
    final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), samplesize, rnd);

    ProxyView<V> proxyv = new ProxyView<>(database, sample, relation);
    ProxyDatabase proxydb = new ProxyDatabase(sample, proxyv);

    innerkMeans.setK(k);
    @SuppressWarnings("unchecked")
    PrimitiveDistanceFunction<? super NumberVector<?>, D> df = (PrimitiveDistanceFunction<? super NumberVector<?>, D>) distanceFunction;
    innerkMeans.setDistanceFunction(df);
    Clustering<? extends MeanModel<V>> clusters = innerkMeans.run(proxydb, proxyv);
    List<V> means = new ArrayList<>();
    for (Cluster<? extends MeanModel<V>> cluster : clusters.getAllClusters()) {
      means.add(cluster.getModel().getMean());
    }

    return means;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<V extends NumberVector<?>, D extends Distance<?>> extends AbstractKMeansInitialization.Parameterizer<V> {
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
    protected KMeans<V, D, ?> innerkMeans;
    
    /**
     * Sampling rate.
     */
    protected double rate;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<KMeans<V, D, ?>> kMeansVariantP = new ObjectParameter<>(KMEANS_ID, KMeans.class);
      if (config.grab(kMeansVariantP)) {
        ListParameterization kMeansVariantParameters = new ListParameterization();

        // We will always invoke this with k as requested from outside!
        kMeansVariantParameters.addParameter(KMeans.K_ID, 13);
        kMeansVariantParameters.addParameter(KMeans.DISTANCE_FUNCTION_ID, SquaredEuclideanDistanceFunction.class);

        ChainedParameterization combinedConfig = new ChainedParameterization(kMeansVariantParameters, config);
        combinedConfig.errorsTo(config);
        innerkMeans = kMeansVariantP.instantiateClass(combinedConfig);
      }
      
      DoubleParameter sampleP = new DoubleParameter(SAMPLE_ID);
      if (config.grab(sampleP)) {
        rate = sampleP.doubleValue();
      }
    }

    @Override
    protected SampleKMeansInitialization<V, D> makeInstance() {
      return new SampleKMeansInitialization<>(rnd, innerkMeans, rate);
    }
  }
}
