package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.LimitEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.composedOf PCAFilteredRunner
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
@Title("4C Preprocessor")
@Description("Computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n" + "The PCA is based on epsilon range queries.")
public class FourCSubspaceIndex<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex<V, D, PCAFilteredResult> {
  /**
   * Our logger
   */
  private final static Logging logger = Logging.getLogger(FourCSubspaceIndex.class);

  /**
   * The Filtered PCA Runner
   */
  private PCAFilteredRunner<V> pca;

  /**
   * Full constructor.
   * 
   * @param relation Relation
   * @param epsilon Epsilon value
   * @param rangeQueryDistanceFunction
   * @param minpts MinPts value
   * @param pca PCA runner
   */
  public FourCSubspaceIndex(Relation<V> relation, D epsilon, DistanceFunction<V, D> rangeQueryDistanceFunction, int minpts, PCAFilteredRunner<V> pca) {
    super(relation, epsilon, rangeQueryDistanceFunction, minpts);
    this.pca = pca;
  }

  @Override
  protected PCAFilteredResult computeProjection(DBID id, List<DistanceResultPair<D>> neighbors, Relation<V> database) {
    ModifiableDBIDs ids = DBIDUtil.newArray(neighbors.size());
    for(DistanceResultPair<D> neighbor : neighbors) {
      ids.add(neighbor.getDBID());
    }
    PCAFilteredResult pcares = pca.processIds(ids, database);

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append(id).append(" "); //.append(database.getObjectLabelQuery().get(id));
      msg.append("\ncorrDim ").append(pcares.getCorrelationDimension());
      logger.debugFine(msg.toString());
    }
    return pcares;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public String getLongName() {
    return "4C local Subspaces";
  }

  @Override
  public String getShortName() {
    return "4C-subspaces";
  }

  /**
   * Factory class for 4C preprocessors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses FourCSubspaceIndex oneway - - «creates»
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Factory<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex.Factory<V, D, FourCSubspaceIndex<V, D>> {
    /**
     * The default value for delta.
     */
    public static final double DEFAULT_DELTA = LimitEigenPairFilter.DEFAULT_DELTA;

    /**
     * The Filtered PCA Runner
     */
    private PCAFilteredRunner<V> pca;

    /**
     * Constructor.
     * 
     * @param epsilon
     * @param rangeQueryDistanceFunction
     * @param minpts
     * @param pca
     */
    public Factory(D epsilon, DistanceFunction<V, D> rangeQueryDistanceFunction, int minpts, PCAFilteredRunner<V> pca) {
      super(epsilon, rangeQueryDistanceFunction, minpts);
      this.pca = pca;
    }

    @Override
    public FourCSubspaceIndex<V, D> instantiate(Relation<V> relation) {
      return new FourCSubspaceIndex<V, D>(relation, epsilon, rangeQueryDistanceFunction, minpts, pca);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex.Factory.Parameterizer<V, D, Factory<V, D>> {
      /**
       * The Filtered PCA Runner
       */
      private PCAFilteredRunner<V> pca;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        // flag absolute
        boolean absolute = false;
        Flag absoluteF = new Flag(LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);
        if(config.grab(absoluteF)) {
          absolute = absoluteF.getValue();
        }

        // Parameter delta
        double delta = 0.0;
        DoubleParameter deltaP = new DoubleParameter(LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA, new GreaterEqualConstraint(0), DEFAULT_DELTA);
        if(config.grab(deltaP)) {
          delta = deltaP.getValue();
        }
        // Absolute flag doesn't have a sensible default value for delta.
        if(absolute && deltaP.tookDefaultValue()) {
          config.reportError(new WrongParameterValueException("Illegal parameter setting: " + "Flag " + absoluteF.getName() + " is set, " + "but no value for " + deltaP.getName() + " is specified."));
        }

        // if (optionHandler.isSet(DELTA_P)) {
        // delta = (Double) optionHandler.getOptionValue(DELTA_P);
        // try {
        // if (!absolute && delta < 0 || delta > 1)
        // throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D);
        // } catch (NumberFormatException e) {
        // throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D, e);
        // }
        // } else if (!absolute) {
        // delta = LimitEigenPairFilter.DEFAULT_DELTA;
        // } else {
        // throw new WrongParameterValueException("Illegal parameter setting: "
        // +
        // "Flag " + ABSOLUTE_F + " is set, " + "but no value for " + DELTA_P +
        // " is specified.");
        // }

        // Parameterize PCA
        ListParameterization pcaParameters = new ListParameterization();
        // eigen pair filter
        pcaParameters.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, LimitEigenPairFilter.class.getName());
        // abs
        if(absolute) {
          pcaParameters.addFlag(LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);
        }
        // delta
        pcaParameters.addParameter(LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA, delta);
        // big value
        pcaParameters.addParameter(PCAFilteredRunner.BIG_ID, 50);
        // small value
        pcaParameters.addParameter(PCAFilteredRunner.SMALL_ID, 1);
        Class<PCAFilteredRunner<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
        pca = pcaParameters.tryInstantiate(cls);
        for(ParameterException e : pcaParameters.getErrors()) {
          LoggingUtil.warning("Error in internal parameterization: " + e.getMessage());
        }

        final ArrayList<ParameterConstraint<Number>> deltaCons = new ArrayList<ParameterConstraint<Number>>();
        // TODO: this constraint is already set in the parameter itself, since
        // it
        // also applies to the relative case, right? -- erich
        // deltaCons.add(new GreaterEqualConstraint(0));
        deltaCons.add(new LessEqualConstraint(1));

        GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Number, Double>(deltaP, deltaCons, absoluteF, false);
        config.checkConstraint(gpc);
      }

      @Override
      protected Factory<V, D> makeInstance() {
        return new Factory<V, D>(epsilon, rangeQueryDistanceFunction, minpts, pca);
      }
    }
  }
}