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

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.quality.KMeansQualityMeasure;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class BestOfMultipleKMeans<V extends NumberVector, M extends MeanModel> implements KMeans<V, M> {
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
  public TypeInformation[] getInputTypeRestriction() {
    return innerkMeans.getInputTypeRestriction();
  }

  @Override
  public Clustering<M> run(Relation<V> relation) {
    @SuppressWarnings("unchecked")
    NumberVectorDistance<? super NumberVector> df = (NumberVectorDistance<? super NumberVector>) innerkMeans.getDistance();

    Clustering<M> bestResult = null;
    double bestCost = Double.NaN;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("K-means iterations", trials, LOG) : null;
    for(int i = 0; i < trials; i++) {
      Clustering<M> currentCandidate = innerkMeans.run(relation);
      double currentCost = qualityMeasure.quality(currentCandidate, df, relation);
      if(LOG.isVerbose()) {
        LOG.verbose("Cost of candidate " + i + ": " + currentCost);
      }

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
  public NumberVectorDistance<? super V> getDistance() {
    return innerkMeans.getDistance();
  }

  @Override
  public void setK(int k) {
    innerkMeans.setK(k);
  }

  @Override
  public void setDistance(NumberVectorDistance<? super V> distance) {
    innerkMeans.setDistance(distance);
  }

  @Override
  public void setInitializer(KMeansInitialization init) {
    innerkMeans.setInitializer(init);
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
    public void configure(Parameterization config) {
      new IntParameter(TRIALS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> trials = x);
      new ObjectParameter<KMeans<V, M>>(KMEANS_ID, KMeans.class) //
          .grab(config, x -> kMeansVariant = x);
      new ObjectParameter<KMeansQualityMeasure<V>>(QUALITYMEASURE_ID, KMeansQualityMeasure.class) //
          .grab(config, x -> qualityMeasure = x);
    }

    @Override
    public BestOfMultipleKMeans<V, M> make() {
      return new BestOfMultipleKMeans<>(trials, kMeansVariant, qualityMeasure);
    }
  }
}
