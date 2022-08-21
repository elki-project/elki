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
package elki.math.statistics.intrinsicdimensionality;

import elki.data.NumberVector;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.math.linearalgebra.pca.PCAResult;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.StandardCovarianceMatrixBuilder;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Local PCA based ID estimator.
 *
 * @author Erik Thordsen
 * @since 0.8.0
 */
public class LPCAEstimator implements IntrinsicDimensionalityEstimator<NumberVector> {
  /**
   * Class to perform PCA
   */
  protected PCARunner pcaRunner;

  /**
   * Eigenvalue filter
   */
  protected EigenPairFilter eigenFilter;

  /**
   * Constructor.
   *
   * @param eigenFilter Filter to choose the number of eigenvalues to keep
   */
  public LPCAEstimator(EigenPairFilter eigenFilter) {
    this.pcaRunner = new PCARunner(new StandardCovarianceMatrixBuilder());
    this.eigenFilter = eigenFilter;
  }

  @Override
  public double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<? extends NumberVector> distq, DBIDRef cur, int k) {
    return this.estimate(knnq.getKNN(cur, k), distq.getRelation());
  }

  @Override
  public double estimate(RangeSearcher<DBIDRef> rnq, DistanceQuery<? extends NumberVector> distq, DBIDRef cur, double range) {
    return this.estimate(rnq.getRange(cur, range), distq.getRelation());
  }

  /**
   * Returns an ID estimate based on the specified filter for the given point
   * DBID set and relation.
   * 
   * @param ids neighbor objects
   * @param relation data vector relation
   * @return estimated intrinsic dimensionality
   */
  protected double estimate(DBIDs ids, Relation<? extends NumberVector> relation) {
    PCAResult pcaResult = this.pcaRunner.processIds(ids, relation);
    int filterResult = this.eigenFilter.filter(pcaResult.getEigenvalues());
    return (double) filterResult;
  }

  /**
   * Parameterization class.
   *
   * @author Erik Thordsen
   */
  public static class Par implements Parameterizer {
    /**
     * Option for the EigenPairFilter to use on eigenvalues.
     */
    public static final OptionID FILTER_ID = new OptionID("lpca.filter", "EigenPairFilter to use on eigenvalues.");

    /**
     * EigenPairFilter to use.
     */
    private EigenPairFilter eigenPairFilter;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<EigenPairFilter>(FILTER_ID, EigenPairFilter.class, PercentageEigenPairFilter.class) //
          .grab(config, x -> eigenPairFilter = x);
    }

    @Override
    public LPCAEstimator make() {
      return new LPCAEstimator(eigenPairFilter);
    }
  }
}
