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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality.KMeansQualityMeasure;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Run K-Means multiple times, and keep the best run.
 *
 * @author Stephan Baier
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - KMeans
 * @has - - - KMeansQualityMeasure
 *
 * @param <V> Vector type
 * @param <M> Model type
 */
public class BestOfMultipleKMeans<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements KMeans<V, M> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(BestOfMultipleKMeans.class);

  /**
   * Number of trials to do.
   */
  private int trials;

  /**
   * Variant of kMeans for the bisecting step.
   */
  private KMeans<V, M> innerkMeans;

  /**
   * Quality measure which should be used.
   */
  private KMeansQualityMeasure<? super V> qualityMeasure;

  /**
   * Constructor.
   *
   * @param trials Number of trials to do.
   * @param innerkMeans K-Means variant to actually use.
   * @param qualityMeasure Quality measure
   */
  public BestOfMultipleKMeans(int trials, KMeans<V, M> innerkMeans, KMeansQualityMeasure<? super V> qualityMeasure) {
    super();
    this.trials = trials;
    this.innerkMeans = innerkMeans;
    this.qualityMeasure = qualityMeasure;
  }

  @Override
  public Clustering<M> run(Database database, Relation<V> relation) {
    if(!(innerkMeans.getDistanceFunction() instanceof PrimitiveDistanceFunction)) {
      throw new AbortException("K-Means results can only be evaluated for primitive distance functions, got: " + innerkMeans.getDistanceFunction().getClass());
    }
    @SuppressWarnings("unchecked")
    final NumberVectorDistanceFunction<? super NumberVector> df = (NumberVectorDistanceFunction<? super NumberVector>) innerkMeans.getDistanceFunction();

    Clustering<M> bestResult = null;
    double bestCost = Double.NaN;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("K-means iterations", trials, LOG) : null;
    for(int i = 0; i < trials; i++) {
      Clustering<M> currentCandidate = innerkMeans.run(database, relation);
      double currentCost = qualityMeasure.quality(currentCandidate, df, relation);
      LOG.verbose("Cost of candidate " + i + ": " + currentCost);

      if(qualityMeasure.isBetter(currentCost, bestCost)) {
        bestResult = currentCandidate;
        bestCost = currentCost;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return bestResult;
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
  public void setK(int k) {
    innerkMeans.setK(k);
  }

  @Override
  public void setDistanceFunction(NumberVectorDistanceFunction<? super V> distanceFunction) {
    innerkMeans.setDistanceFunction(distanceFunction);
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
  public static class Parameterizer<V extends NumberVector, M extends MeanModel> extends AbstractParameterizer {
    /**
     * Parameter to specify the iterations of the bisecting step.
     */
    public static final OptionID TRIALS_ID = new OptionID("kmeans.trials", "The number of trials to run.");

    /**
     * Parameter to specify the kMeans variant.
     */
    public static final OptionID KMEANS_ID = new OptionID("kmeans.algorithm", "KMeans variant to run multiple times.");

    /**
     * Parameter to specify the variant of quality measure.
     */
    public static final OptionID QUALITYMEASURE_ID = new OptionID("kmeans.qualitymeasure", "Quality measure variant for deciding which run to keep.");

    /**
     * Number of trials to perform.
     */
    protected int trials;

    /**
     * Variant of kMeans to use.
     */
    protected KMeans<V, M> kMeansVariant;

    /**
     * Quality measure.
     */
    protected KMeansQualityMeasure<? super V> qualityMeasure;

    @Override
    protected void makeOptions(Parameterization config) {
      IntParameter trialsP = new IntParameter(TRIALS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(trialsP)) {
        trials = trialsP.intValue();
      }

      ObjectParameter<KMeans<V, M>> kMeansVariantP = new ObjectParameter<>(KMEANS_ID, KMeans.class);
      if(config.grab(kMeansVariantP)) {
        kMeansVariant = kMeansVariantP.instantiateClass(config);
      }

      ObjectParameter<KMeansQualityMeasure<V>> qualityMeasureP = new ObjectParameter<>(QUALITYMEASURE_ID, KMeansQualityMeasure.class);
      if(config.grab(qualityMeasureP)) {
        qualityMeasure = qualityMeasureP.instantiateClass(config);
      }
    }

    @Override
    protected BestOfMultipleKMeans<V, M> makeInstance() {
      return new BestOfMultipleKMeans<>(trials, kMeansVariant, qualityMeasure);
    }
  }
}
