package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class to run PCA on given data.
 * 
 * The various methods will start PCA at different places (e.g. with database
 * IDs, database query results, a precomputed covariance matrix or eigenvalue
 * decomposition).
 * 
 * The runner can be parameterized by setting a covariance matrix builder (e.g.
 * to a weighted covariance matrix builder)
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses PCAResult oneway - - «create»
 * @apiviz.composedOf CovarianceMatrixBuilder
 * 
 * @param <V> Vector type
 */
public class PCARunner<V extends NumberVector<? extends V, ?>> implements Parameterizable {
  /**
   * Parameter to specify the class to compute the covariance matrix, must be a
   * subclass of {@link CovarianceMatrixBuilder}.
   * <p>
   * Default value: {@link CovarianceMatrixBuilder}
   * </p>
   * <p>
   * Key: {@code -pca.covariance}
   * </p>
   */
  public static final OptionID PCA_COVARIANCE_MATRIX = OptionID.getOrCreateOptionID("pca.covariance", "Class used to compute the covariance matrix.");

  /**
   * The covariance computation class.
   */
  protected CovarianceMatrixBuilder<V> covarianceMatrixBuilder;

  /**
   * Constructor.
   * 
   * @param covarianceMatrixBuilder Class for computing the covariance matrix
   */
  public PCARunner(CovarianceMatrixBuilder<V> covarianceMatrixBuilder) {
    super();
    this.covarianceMatrixBuilder = covarianceMatrixBuilder;
  }

  /**
   * Run PCA on the complete database
   * 
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processDatabase(Relation<? extends V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processDatabase(database));
  }

  /**
   * Run PCA on a collection of database IDs
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processIds(DBIDs ids, Relation<? extends V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processIds(ids, database));
  }

  /**
   * Run PCA on a QueryResult Collection
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  public <D extends NumberDistance<?, ?>> PCAResult processQueryResult(Collection<? extends DistanceResultPair<D>> results, Relation<? extends V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Process an existing covariance Matrix
   * 
   * @param covarMatrix the matrix used for performing pca
   * @return PCA result
   */
  public PCAResult processCovarMatrix(Matrix covarMatrix) {
    // TODO: add support for a different implementation to do EVD?
    EigenvalueDecomposition evd = new EigenvalueDecomposition(covarMatrix);
    return processEVD(evd);
  }

  /**
   * Process an existing eigenvalue decomposition
   * 
   * @param evd eigenvalue decomposition to use
   * @return PCA result
   */
  public PCAResult processEVD(EigenvalueDecomposition evd) {
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    return new PCAResult(eigenPairs);
  }

  /**
   * Get covariance matrix builder
   * 
   * @return covariance matrix builder in use
   */
  public CovarianceMatrixBuilder<V> getCovarianceMatrixBuilder() {
    return covarianceMatrixBuilder;
  }

  /**
   * Set covariance matrix builder.
   * 
   * @param covarianceBuilder New covariance matrix builder.
   */
  public void setCovarianceMatrixBuilder(CovarianceMatrixBuilder<V> covarianceBuilder) {
    this.covarianceMatrixBuilder = covarianceBuilder;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<? extends V, ?>> extends AbstractParameterizer {
    /**
     * The covariance computation class.
     */
    protected CovarianceMatrixBuilder<V> covarianceMatrixBuilder;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<CovarianceMatrixBuilder<V>> covarianceP = new ObjectParameter<CovarianceMatrixBuilder<V>>(PCA_COVARIANCE_MATRIX, CovarianceMatrixBuilder.class, StandardCovarianceMatrixBuilder.class);
      if(config.grab(covarianceP)) {
        covarianceMatrixBuilder = covarianceP.instantiateClass(config);
      }
    }

    @Override
    protected PCARunner<V> makeInstance() {
      return new PCARunner<V>(covarianceMatrixBuilder);
    }
  }
}