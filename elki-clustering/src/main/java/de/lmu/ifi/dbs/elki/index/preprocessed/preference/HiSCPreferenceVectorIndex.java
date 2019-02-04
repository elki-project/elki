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
package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.HiSC;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.EmptyDataException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger,
 * Ina Müller-Gorman, Arthur Zimek<br>
 * Finding Hierarchies of Subspace Clusters<br>
 * Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in
 * Databases (PKDD'06)
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @see HiSC
 *
 * @param <V> Vector type
 */
@Title("HiSC Preprocessor")
@Description("Computes the preference vector of objects of a certain database according to the HiSC algorithm.")
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Petre Kriegel, Peer Kröger, Ina Müller-Gorman, Arthur Zimek", //
    title = "Finding Hierarchies of Subspace Clusters", //
    booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06)", //
    url = "https://doi.org/10.1007/11871637_42", //
    bibkey = "DBLP:conf/pkdd/AchtertBKKMZ06")
public class HiSCPreferenceVectorIndex<V extends NumberVector> extends AbstractPreferenceVectorIndex<V> implements PreferenceVectorIndex<V> {
  /**
   * Logger to use.
   */
  private static final Logging LOG = Logging.getLogger(HiSCPreferenceVectorIndex.class);

  /**
   * The maximum absolute variance along a coordinate axis.
   */
  protected double alpha;

  /**
   * The number of nearest neighbors considered to determine the preference
   * vector.
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param relation Relation in use
   * @param alpha Alpha value
   * @param k k value
   */
  public HiSCPreferenceVectorIndex(Relation<V> relation, double alpha, int k) {
    super(relation);
    this.alpha = alpha;
    this.k = k;
  }

  @Override
  public void initialize() {
    if(relation == null || relation.size() <= 0) {
      throw new EmptyDataException();
    }
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, long[].class);

    KNNQuery<V> knnQuery = QueryUtil.getKNNQuery(relation, EuclideanDistanceFunction.STATIC, k);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Preprocessing preference vector", relation.size(), LOG) : null;
    long start = System.currentTimeMillis();
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      storage.put(it, determinePreferenceVector(relation, it, knnQuery.getKNNForDBID(it, k)));
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".runtime.ms", System.currentTimeMillis() - start));
    }
  }

  /**
   * Determines the preference vector according to the specified neighbor ids.
   *
   * @param relation the database storing the objects
   * @param id the id of the object for which the preference vector should be
   *        determined
   * @param neighborIDs the ids of the neighbors
   * @return the preference vector
   */
  private long[] determinePreferenceVector(Relation<V> relation, DBIDRef id, DBIDs neighborIDs) {
    NumberVector p = relation.get(id);
    // variances
    final int size = neighborIDs.size(), dim = p.getDimensionality();
    double[] sumsq = new double[dim];
    for(DBIDIter iter = neighborIDs.iter(); iter.valid(); iter.advance()) {
      NumberVector o = relation.get(iter);
      for(int d = 0; d < dim; d++) {
        final double diff = o.doubleValue(d) - p.doubleValue(d);
        sumsq[d] += diff * diff;
      }
    }

    // preference vector
    long[] preferenceVector = BitsUtil.zero(dim);
    for(int d = 0; d < dim; d++) {
      if(sumsq[d] < alpha * size) {
        BitsUtil.setI(preferenceVector, d);
      }
    }
    return preferenceVector;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "HiSC Preference Vectors";
  }

  @Override
  public String getShortName() {
    return "hisc-pref";
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
  }

  /**
   * Factory class.
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @navassoc - create - HiSCPreferenceVectorIndex
   *
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector> extends AbstractPreferenceVectorIndex.Factory<V> {
    /**
     * The maximum absolute variance along a coordinate axis.
     */
    protected double alpha;

    /**
     * The number of nearest neighbors considered to determine the preference
     * vector.
     */
    protected int k;

    /**
     * Constructor.
     *
     * @param alpha Alpha
     * @param k k
     */
    public Factory(double alpha, int k) {
      super();
      this.alpha = alpha;
      this.k = k;
    }

    /**
     * Get the alpha parameter.
     *
     * @return Alpha
     */
    public double getAlpha() {
      return alpha;
    }

    @Override
    public HiSCPreferenceVectorIndex<V> instantiate(Relation<V> relation) {
      final int usek = k > 0 ? k : 3 * RelationUtil.dimensionality(relation);
      return new HiSCPreferenceVectorIndex<>(relation, alpha, usek);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
      /**
       * The maximum absolute variance along a coordinate axis.
       */
      protected double alpha;

      /**
       * The number of nearest neighbors considered to determine the preference
       * vector.
       */
      protected int k = 0;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final DoubleParameter alphaP = new DoubleParameter(HiSC.Parameterizer.ALPHA_ID, HiSC.Parameterizer.DEFAULT_ALPHA) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
        if(config.grab(alphaP)) {
          alpha = alphaP.doubleValue();
        }

        final IntParameter kP = new IntParameter(HiSC.Parameterizer.K_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .setOptional(true);
        if(config.grab(kP)) {
          k = kP.intValue();
        }
      }

      @Override
      protected Factory<V> makeInstance() {
        return new Factory<>(alpha, k);
      }
    }
  }
}
