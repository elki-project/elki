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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.BetaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta.BestFitEstimator;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

import net.jafama.FastMath;

/**
 * Project the data using a Beta distribution.
 *
 * This is a crude heuristic, that may or may not work for your data set. There
 * currently is no theoretical foundation of why it may be sensible or not to do
 * this.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <V> vector type
 *
 * @assoc - - - NumberVector
 * @assoc - - - DistributionEstimator
 */
public class AttributeWiseBetaNormalization<V extends NumberVector> extends AttributeWiseCDFNormalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseBetaNormalization.class);

  /**
   * Number vector factory.
   */
  protected NumberVector.Factory<V> factory;

  /**
   * Expected outlier rate alpha.
   */
  protected double alpha = 0.01;

  /**
   * Constructor.
   *
   * @param estimators Distribution estimators
   */
  public AttributeWiseBetaNormalization(List<DistributionEstimator<?>> estimators, double alpha) {
    super(estimators);
    this.alpha = alpha;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }
    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = (SimpleTypeInformation<?>) objects.meta(r);
      final List<?> column = (List<?>) objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      final List<V> castColumn = (List<V>) column;
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final VectorFieldTypeInformation<V> castType = (VectorFieldTypeInformation<V>) type;
      factory = FilterUtil.guessFactory(castType);

      // Scan to find the best
      final int dim = castType.getDimensionality();
      dists = new ArrayList<>(dim);
      // Scratch space for testing:
      double[] test = new double[castColumn.size()];

      // We iterate over dimensions, this kind of filter needs fast random
      // access.
      Adapter adapter = new Adapter();
      for(int d = 0; d < dim; d++) {
        adapter.dim = d;
        Distribution dist = findBestFit(castColumn, adapter, d, test);
        if(LOG.isVerbose()) {
          LOG.verbose("Best fit for dimension " + d + ": " + dist.toString());
        }
        dists.add(dist);
      }

      // Beta distribution for projection
      double p = FastMath.pow(alpha, -1 / FastMath.sqrt(dim));
      BetaDistribution beta = new BetaDistribution(p, p);
      // Normalization scan
      double[] buf = new double[dim];
      for(int i = 0; i < objects.dataLength(); i++) {
        final V obj = castColumn.get(i);
        for(int d = 0; d < dim; d++) {
          // TODO: when available, use logspace for better numerical precision!
          buf[d] = beta.quantile(dists.get(d).cdf(obj.doubleValue(d)));
        }
        castColumn.set(i, factory.newNumberVector(buf));
      }
    }
    return objects;
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    throw new NotImplementedException();
  }

  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    throw new NotImplementedException();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for distribution estimators.
     */
    public static final OptionID DISTRIBUTIONS_ID = AttributeWiseCDFNormalization.Parameterizer.DISTRIBUTIONS_ID;

    /**
     * Shape parameter.
     */
    public static final OptionID ALPHA_ID = new OptionID("normalize.beta.alpha", "Alpha parameter to control the shape of the output distribution.");

    /**
     * Stores the distribution estimators
     */
    private List<DistributionEstimator<?>> estimators;

    /**
     * Expected outlier rate alpha.
     */
    private double alpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectListParameter<DistributionEstimator<?>> estP = new ObjectListParameter<>(DISTRIBUTIONS_ID, DistributionEstimator.class);
      estP.setDefaultValue(Arrays.asList(BestFitEstimator.class));
      if(config.grab(estP)) {
        estimators = estP.instantiateClasses(config);
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.1);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected AttributeWiseBetaNormalization<V> makeInstance() {
      return new AttributeWiseBetaNormalization<>(estimators, alpha);
    }
  }
}
