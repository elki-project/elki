package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the local neighborhood to be considered in the PCA as the neighbors
 * within an epsilon range query of an object.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.uses RangeQuery
 * 
 * @param <NV> Vector type
 */
@Title("Range Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on epsilon range queries.")
public class RangeQueryFilteredPCAIndex<NV extends NumberVector> extends AbstractFilteredPCAIndex<NV> {
  // TODO: lose DoubleDistance restriction.
  /**
   * Logger.
   */
  private static final Logging LOG = Logging.getLogger(RangeQueryFilteredPCAIndex.class);

  /**
   * The kNN query instance we use.
   */
  private final RangeQuery<NV> rangeQuery;

  /**
   * Query epsilon.
   */
  private final double epsilon;

  /**
   * Constructor.
   * 
   * @param database Database to use
   * @param pca PCA Runner to use
   * @param rangeQuery Range Query to use
   * @param epsilon Query range
   */
  public RangeQueryFilteredPCAIndex(Relation<NV> database, PCAFilteredRunner pca, RangeQuery<NV> rangeQuery, double epsilon) {
    super(database, pca);
    this.rangeQuery = rangeQuery;
    this.epsilon = epsilon;
  }

  @Override
  protected DoubleDBIDList objectsForPCA(DBIDRef id) {
    return rangeQuery.getRangeForDBID(id, epsilon);
  }

  @Override
  public String getLongName() {
    return "kNN-based local filtered PCA";
  }

  @Override
  public String getShortName() {
    return "kNNFilteredPCA";
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
   * @apiviz.stereotype factory
   * @apiviz.uses RangeQueryFilteredPCAIndex oneway - - «create»
   */
  public static class Factory<V extends NumberVector> extends AbstractFilteredPCAIndex.Factory<V, RangeQueryFilteredPCAIndex<V>> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered in the PCA, must be suitable to the distance function
     * specified.
     * 
     * Key: {@code -localpca.epsilon}
     */
    public static final OptionID EPSILON_ID = new OptionID("localpca.epsilon", "The maximum radius of the neighborhood to be considered in the PCA.");

    /**
     * Holds the value of {@link #EPSILON_ID}.
     */
    protected double epsilon;

    /**
     * Constructor.
     * 
     * @param pcaDistanceFunction distance function
     * @param pca PCA
     * @param epsilon range value
     */
    public Factory(DistanceFunction<V> pcaDistanceFunction, PCAFilteredRunner pca, double epsilon) {
      super(pcaDistanceFunction, pca);
      this.epsilon = epsilon;
    }

    @Override
    public RangeQueryFilteredPCAIndex<V> instantiate(Relation<V> relation) {
      // TODO: set bulk flag, once the parent class supports bulk.
      RangeQuery<V> rangequery = QueryUtil.getRangeQuery(relation, pcaDistanceFunction);
      return new RangeQueryFilteredPCAIndex<>(relation, pca, rangequery, epsilon);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector> extends AbstractFilteredPCAIndex.Factory.Parameterizer<NV, RangeQueryFilteredPCAIndex<NV>> {
      protected double epsilon;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID);
        if(config.grab(epsilonP)) {
          epsilon = epsilonP.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<>(pcaDistanceFunction, pca, epsilon);
      }
    }
  }
}