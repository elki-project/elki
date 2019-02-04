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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class to run PCA on given data.
 * <p>
 * The various methods will start PCA at different places (e.g. with database
 * IDs, database query results, a precomputed covariance matrix or eigenvalue
 * decomposition).
 * <p>
 * The runner can be parameterized by setting a covariance matrix builder (e.g.
 * to a weighted covariance matrix builder)
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 * @navassoc - create - PCAResult
 * @composed - - - CovarianceMatrixBuilder
 */
public class PCARunner {
  /**
   * The covariance computation class.
   */
  protected CovarianceMatrixBuilder covarianceMatrixBuilder;

  /**
   * Constructor.
   * 
   * @param covarianceMatrixBuilder Class for computing the covariance matrix
   */
  public PCARunner(CovarianceMatrixBuilder covarianceMatrixBuilder) {
    super();
    this.covarianceMatrixBuilder = covarianceMatrixBuilder;
  }

  /**
   * Run PCA on a collection of database IDs.
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processIds(DBIDs ids, Relation<? extends NumberVector> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processIds(ids, database));
  }

  /**
   * Run PCA on a QueryResult Collection.
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processQueryResult(DoubleDBIDList results, Relation<? extends NumberVector> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Process an existing covariance Matrix.
   * 
   * @param covarMatrix the matrix used for performing pca
   * @return PCA result
   */
  public PCAResult processCovarMatrix(double[][] covarMatrix) {
    // TODO: add support for a different implementation to do EVD?
    EigenvalueDecomposition evd = new EigenvalueDecomposition(covarMatrix);
    return processEVD(evd);
  }

  /**
   * Process an existing eigenvalue decomposition.
   * 
   * @param evd eigenvalue decomposition to use
   * @return PCA result
   */
  public PCAResult processEVD(EigenvalueDecomposition evd) {
    return new PCAResult(evd);
  }

  /**
   * Get covariance matrix builder.
   * 
   * @return covariance matrix builder in use
   */
  public CovarianceMatrixBuilder getCovarianceMatrixBuilder() {
    return covarianceMatrixBuilder;
  }

  /**
   * Set covariance matrix builder.
   * 
   * @param covarianceBuilder New covariance matrix builder.
   */
  public void setCovarianceMatrixBuilder(CovarianceMatrixBuilder covarianceBuilder) {
    this.covarianceMatrixBuilder = covarianceBuilder;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the PCA variant to use.
     */
    public static final OptionID PCARUNNER_ID = new OptionID("pca.variant", "The class to compute (filtered) PCA.");

    /**
     * Parameter to specify the class to compute the covariance matrix, must be
     * a subclass of {@link CovarianceMatrixBuilder}.
     */
    public static final OptionID PCA_COVARIANCE_MATRIX = new OptionID("pca.covariance", "Class used to compute the covariance matrix.");

    /**
     * The covariance computation class.
     */
    protected CovarianceMatrixBuilder covarianceMatrixBuilder;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<CovarianceMatrixBuilder> covarianceP = new ObjectParameter<>(PCA_COVARIANCE_MATRIX, CovarianceMatrixBuilder.class, StandardCovarianceMatrixBuilder.class);
      if(config.grab(covarianceP)) {
        covarianceMatrixBuilder = covarianceP.instantiateClass(config);
      }
    }

    @Override
    protected PCARunner makeInstance() {
      return new PCARunner(covarianceMatrixBuilder);
    }
  }
}